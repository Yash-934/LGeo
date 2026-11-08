package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CategoryTopicsScreen
import com.example.ui.LearnByImageViewModel
import com.example.ui.MainTabsScreen
import com.example.ui.QuizPlayerScreen
import com.example.ui.ScreenRoute
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LearnByImageApp()
                }
            }
        }
    }
}

@Composable
fun LearnByImageApp() {
    val context = LocalContext.current
    val viewModel: LearnByImageViewModel = viewModel(
        factory = LearnByImageViewModel.provideFactory(context)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ऑफलाइन हिंदी डेटाबेस लोड हो रहा है...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    } else {
        when (val route = uiState.currentRoute) {
            ScreenRoute.MainTabs -> {
                MainTabsScreen(uiState = uiState, viewModel = viewModel)
            }
            is ScreenRoute.CategoryTopics -> {
                CategoryTopicsScreen(
                    categoryId = route.categoryId,
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
            is ScreenRoute.QuizPlayer -> {
                QuizPlayerScreen(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}
