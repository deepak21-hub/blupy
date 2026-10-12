package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.BlueyAppView
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.BlueyTheme
import com.example.viewmodel.BlueyViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BlueyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen awake while Bluey lives under monitor
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            BlueyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BlueyRoot(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun BlueyRoot(viewModel: BlueyViewModel) {
    var showDashboard by remember { mutableStateOf(false) }

    Crossfade(targetState = showDashboard, label = "screen_transition") { isDashboard ->
        if (isDashboard) {
            BlueyAppView(
                viewModel = viewModel,
                onClose = { showDashboard = false }
            )
        } else {
            HomeScreen(
                viewModel = viewModel,
                onOpenDashboard = { showDashboard = true }
            )
        }
    }
}
