package com.downloader.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.downloader.app.ui.screens.DownloadsScreen
import com.downloader.app.ui.screens.HomeScreen
import com.downloader.app.ui.screens.JobsScreen
import com.downloader.app.ui.theme.DownloaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Relaxation for Intent.ACTION_VIEW with file:// URIs as fallback
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        val sharedUrl = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            null
        }

        setContent {
            DownloaderTheme {
                MainAppScreen(initialUrl = sharedUrl, vm = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(initialUrl: String?, vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    var currentTab by remember { mutableStateOf(0) }

    LaunchedEffect(initialUrl) {
        initialUrl?.let { vm.setInitialUrl(it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Text("Home", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Text("Downloads", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Text("Jobs", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                0 -> HomeScreen(state = state, vm = vm)
                1 -> DownloadsScreen(state = state, vm = vm)
                2 -> JobsScreen(state = state, vm = vm)
            }
        }
    }
}