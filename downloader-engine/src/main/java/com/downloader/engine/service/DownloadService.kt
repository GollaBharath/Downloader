package com.downloader.engine.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.downloader.core.domain.model.DownloadJob
import com.downloader.core.domain.model.DownloadPhase
import com.downloader.core.domain.model.DownloadProgress
import com.downloader.core.domain.model.DownloadStatus as JobStatus
import com.downloader.core.domain.repository.DownloadJobRepository
import com.downloader.core.domain.repository.ProgressRepository
import com.downloader.engine.executor.DownloadExecutor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var jobRepository: DownloadJobRepository
    
    @Inject
    lateinit var progressRepository: ProgressRepository
    
    @Inject
    lateinit var downloadExecutor: DownloadExecutor

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val queueMutex = Mutex()
    private var currentJobId: String? = null
    private var activeDownloadTask: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        const val ACTION_START_DOWNLOAD = "com.downloader.action.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.downloader.action.CANCEL_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.downloader.action.PAUSE_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOAD = "com.downloader.action.RESUME_DOWNLOAD"
        
        const val EXTRA_JOB_ID = "job_id"
        
        private const val NOTIFICATION_CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 1001
        
        fun startDownload(context: Context, jobId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_JOB_ID, jobId)
            }
            context.startForegroundService(intent)
        }
        
        fun cancelDownload(context: Context, jobId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_JOB_ID, jobId)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createIdleNotification())
        initializeWakeLock()
        requestQueueProcessing()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID)
                jobId?.let { startDownload(it) }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID)
                jobId?.let { cancelDownload(it) }
            }
            ACTION_PAUSE_DOWNLOAD -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID)
                jobId?.let { pauseDownload(it) }
            }
            ACTION_RESUME_DOWNLOAD -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID)
                jobId?.let { resumeDownload(it) }
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        serviceScope.cancel()
    }

    private fun startDownload(jobId: String) {
        serviceScope.launch {
            val job = jobRepository.getJob(jobId)
            if (job != null && job.status == JobStatus.RUNNING) {
                return@launch
            }
            requestQueueProcessing()
        }
    }

    private fun cancelDownload(jobId: String) {
        serviceScope.launch {
            downloadExecutor.cancelDownload(jobId)
            val job = jobRepository.getJob(jobId)
            job?.let { 
                val cancelledJob = it.copy(
                    status = JobStatus.CANCELLED,
                    completedAt = System.currentTimeMillis(),
                    errorMessage = null
                )
                jobRepository.updateJob(cancelledJob)
            }
            if (currentJobId == jobId) {
                activeDownloadTask?.cancel(CancellationException("Cancelled by user"))
                currentJobId = null
            }
            requestQueueProcessing()
        }
    }

    private fun pauseDownload(jobId: String) {
        serviceScope.launch {
            downloadExecutor.pauseDownload(jobId)
            val job = jobRepository.getJob(jobId)
            job?.let { 
                val pausedJob = it.copy(status = JobStatus.QUEUED)  // For MVP, paused jobs go back to queued
                jobRepository.updateJob(pausedJob)
            }
            if (currentJobId == jobId) {
                activeDownloadTask?.cancel(CancellationException("Paused by user"))
                currentJobId = null
            }
            requestQueueProcessing()
        }
    }

    private fun resumeDownload(jobId: String) {
        serviceScope.launch {
            val job = jobRepository.getJob(jobId)
            job?.let { 
                val resumedJob = it.copy(status = JobStatus.QUEUED)
                jobRepository.updateJob(resumedJob)
            }
            requestQueueProcessing()
        }
    }

    private fun requestQueueProcessing() {
        serviceScope.launch {
            queueMutex.withLock {
                if (activeDownloadTask?.isActive == true) {
                    return@withLock
                }

                val nextJob = jobRepository.getNextQueuedJob()
                if (nextJob == null) {
                    if (currentJobId == null) {
                        updateIdleNotification()
                        stopSelf()
                    }
                    return@withLock
                }

                activeDownloadTask = serviceScope.launch {
                    processDownload(nextJob)
                }
            }
        }
    }

    private suspend fun processDownload(job: DownloadJob) {
        currentJobId = job.id
        acquireWakeLock()

        val runningJob = job.copy(
            status = JobStatus.RUNNING,
            errorMessage = null,
            completedAt = null
        )
        jobRepository.updateJob(runningJob)

        var terminalPhaseHandled = false

        try {
            downloadExecutor.executeDownload(runningJob).collect { progress ->
                updateNotification(runningJob, progress)
                progressRepository.updateProgress(progress)

                when (progress.phase) {
                    DownloadPhase.COMPLETED -> {
                        terminalPhaseHandled = true
                        val completedJob = runningJob.copy(
                            status = JobStatus.SUCCEEDED,
                            completedAt = System.currentTimeMillis(),
                            errorMessage = null
                        )
                        jobRepository.updateJob(completedJob)
                    }

                    DownloadPhase.FAILED -> {
                        terminalPhaseHandled = true
                        val message = progress.errorMessage ?: "Unknown error"
                        val failedJob = runningJob.copy(
                            status = JobStatus.FAILED,
                            errorMessage = message,
                            completedAt = System.currentTimeMillis()
                        )
                        jobRepository.updateJob(failedJob)
                    }

                    else -> Unit
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            val message = e.message ?: "Unknown error"
            val failedJob = runningJob.copy(
                status = JobStatus.FAILED,
                errorMessage = message,
                completedAt = System.currentTimeMillis()
            )
            jobRepository.updateJob(failedJob)
            updateNotification(runningJob, createFailedProgress(runningJob.id, message))
        } finally {
            if (!terminalPhaseHandled) {
                val latestJob = jobRepository.getJob(runningJob.id)
                if (latestJob?.status == JobStatus.RUNNING) {
                    val failedJob = runningJob.copy(
                        status = JobStatus.FAILED,
                        errorMessage = "Download ended unexpectedly",
                        completedAt = System.currentTimeMillis()
                    )
                    jobRepository.updateJob(failedJob)
                    updateNotification(runningJob, createFailedProgress(runningJob.id, "Download ended unexpectedly"))
                }
            }

            currentJobId = null
            releaseWakeLock()

            queueMutex.withLock {
                activeDownloadTask = null
            }
            requestQueueProcessing()
        }
    }

    private fun initializeWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Downloader:DownloadWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    private fun acquireWakeLock() {
        wakeLock?.let { lock ->
            if (!lock.isHeld) {
                lock.acquire(30 * 60 * 1000L)
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
    }

    private fun updateIdleNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createIdleNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Download Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for download progress"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createIdleNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download Service")
            .setContentText("Ready to download")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(job: DownloadJob, progress: DownloadProgress) {
        val notification = when (progress.phase) {
            DownloadPhase.QUEUED -> createQueuedNotification(job)
            DownloadPhase.FETCHING_INFO -> createFetchingInfoNotification(job)
            DownloadPhase.DOWNLOADING -> createDownloadingNotification(job, progress)
            DownloadPhase.POST_PROCESSING -> createPostProcessingNotification(job, progress)
            DownloadPhase.MERGING -> createPostProcessingNotification(job, progress)
            DownloadPhase.COMPLETED -> createCompletedNotification(job)
            DownloadPhase.FAILED -> createFailedNotification(job, progress.errorMessage)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createQueuedNotification(job: DownloadJob): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download Queued")
            .setContentText(job.title ?: job.url)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
    }

    private fun createFetchingInfoNotification(job: DownloadJob): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Fetching video info...")
            .setContentText(job.title ?: job.url)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
    }

    private fun createDownloadingNotification(job: DownloadJob, progress: DownloadProgress): Notification {
        val cancelIntent = PendingIntent.getService(
            this, 0,
            Intent(this, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_JOB_ID, job.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading...")
            .setContentText(job.title ?: job.url)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress.percentComplete.toInt(), false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()
    }

    private fun createPostProcessingNotification(job: DownloadJob, progress: DownloadProgress): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Processing...")
            .setContentText(job.title ?: job.url)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress.percentComplete.toInt(), false)
            .build()
    }

    private fun createCompletedNotification(job: DownloadJob): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText(job.title ?: job.url)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
    }

    private fun createFailedNotification(job: DownloadJob, error: String?): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText(error ?: "Unknown error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
    }

    private fun createCompletedProgress(jobId: String): DownloadProgress {
        return DownloadProgress(
            jobId = jobId,
            phase = DownloadPhase.COMPLETED,
            percentComplete = 100f
        )
    }

    private fun createFailedProgress(jobId: String, error: String): DownloadProgress {
        return DownloadProgress(
            jobId = jobId,
            phase = DownloadPhase.FAILED,
            percentComplete = 0f,
            errorMessage = error
        )
    }
}