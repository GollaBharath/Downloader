package com.downloader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.downloader.app.ui.MainUiState
import com.downloader.app.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: MainUiState,
    vm: MainViewModel
) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Downloader",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // URL Input Flow
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = state.url,
                onValueChange = vm::onUrlChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste media URL here...") },
                singleLine = true,
                isError = urlError != null,
                shape = RoundedCornerShape(16.dp)
            )

            if (urlError != null) {
                Text(urlError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { clipboardManager.getText()?.text?.toString()?.let { vm.onUrlChanged(it) } },
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("Paste URL", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = {
                        folderPickerLauncher.launch(android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE))
                    },
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("Output Dir", fontWeight = FontWeight.SemiBold)
                }
            }
            
            Button(
                onClick = vm::fetchMediaInfo,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text("Fetch Media Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        if (state.isLoadingInfo) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // Error & Info States
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        state.infoMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }

        // Media Info Card
        state.mediaInfo?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(info.title ?: "Unknown title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (info.isPlaylist) {
                        Text(
                            "Playlist Detected: ${info.playlistEntries?.size ?: 0} Videos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("Uploader: ${info.uploader ?: "Unknown"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    Text("Duration: ${formatDurationHumanReadable(info.duration)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Download Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { vm.enqueueDownload(audioOnly = false) },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text("Video", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { vm.enqueueDownload(audioOnly = true) },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text("Audio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatDurationHumanReadable(duration: String?): String {
    if (duration.isNullOrBlank()) return "Unknown"
    val parts = duration.split(":")
    return when (parts.size) {
        3 -> { // HH:MM:SS
            val h = parts[0].toIntOrNull() ?: 0
            val m = parts[1].toIntOrNull() ?: 0
            val s = parts[2].toIntOrNull() ?: 0
            if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
        }
        2 -> { // MM:SS
            val m = parts[0].toIntOrNull() ?: 0
            val s = parts[1].toIntOrNull() ?: 0
            if (m > 0) "${m}m ${s}s" else "${s}s"
        }
        1 -> { // SS
            val s = parts[0].toIntOrNull() ?: 0
            if (s >= 60) {
                val m = s / 60
                val remS = s % 60
                if (m >= 60) {
                    val h = m / 60
                    val remM = m % 60
                    "${h}h ${remM}m ${remS}s"
                } else {
                    "${m}m ${remS}s"
                }
            } else {
                "${s}s"
            }
        }
        else -> duration
    }
}
