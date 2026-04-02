package com.downloader.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.downloader.core.domain.model.DownloadJob
import com.downloader.core.domain.model.DownloadStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = if (intent?.action == android.content.Intent.ACTION_SEND) {
            intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
        } else {
            null
        }

        setContent {
            DownloaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(initialUrl = sharedUrl, vm = viewModel)
                }
            }
        }
    }
}

@Composable
fun DownloaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}

@Composable
fun MainScreen(initialUrl: String?, vm: MainViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            val flags = result.data?.flags ?: 0
            val takeFlags = flags and (
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            vm.updateDownloadFolderUri(uri.toString())
        }
    }
    val urlError = remember(state.url) {
        val trimmed = state.url.trim()
        if (trimmed.isNotBlank() && !trimmed.startsWith("http")) "Enter a valid URL" else null
    }

    LaunchedEffect(initialUrl) {
        initialUrl?.let { vm.setInitialUrl(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Downloader MVP", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = state.url,
            onValueChange = vm::onUrlChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Media URL") },
            singleLine = true,
            isError = urlError != null
        )

        urlError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { clipboardManager.getText()?.text?.toString()?.let { vm.onUrlChanged(it) } },
                modifier = Modifier.weight(1f)
            ) {
                Text("Paste")
            }
            Button(
                onClick = {
                    folderPickerLauncher.launch(android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Choose Folder")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = vm::fetchMediaInfo, modifier = Modifier.weight(1f)) {
                Text("Fetch Info")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { vm.enqueueDownload(audioOnly = false) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Download Video")
            }
            Button(
                onClick = { vm.enqueueDownload(audioOnly = true) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Download Audio")
            }
        }

        if (state.isLoadingInfo) {
            CircularProgressIndicator()
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        state.infoMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Text(
            text = "Folder: ${state.downloadFolderUri ?: "Default Downloads"}",
            style = MaterialTheme.typography.bodySmall
        )

        state.mediaInfo?.let { info ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(info.title ?: "Unknown title", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Uploader: ${info.uploader ?: "Unknown"}")
                    Text("Duration: ${info.duration ?: "Unknown"}")
                }
            }
        }

        Text("Queue", style = MaterialTheme.typography.titleMedium)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.jobs, key = { it.id }) { job ->
                JobRow(
                    job = job,
                    onCancel = { vm.cancelJob(job.id) },
                    onRetry = { vm.retryJob(job.id) }
                )
            }
        }
    }
}

@Composable
private fun JobRow(
    job: DownloadJob,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(job.title ?: job.url, fontWeight = FontWeight.Medium)
            Text("Status: ${job.status.name}")
            job.errorMessage?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (job.status == DownloadStatus.QUEUED || job.status == DownloadStatus.RUNNING) {
                    Button(onClick = onCancel) {
                        Text("Cancel")
                    }
                }
                if (job.status == DownloadStatus.FAILED || job.status == DownloadStatus.CANCELLED) {
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DownloaderTheme {
        Text("Downloader MVP")
    }
}