package com.rekluzlabs.makokolorize

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.rekluzlabs.makokolorize.ui.screens.HomeScreen
import com.rekluzlabs.makokolorize.ui.screens.ResultScreen
import com.rekluzlabs.makokolorize.ui.screens.RestoreRoute
import com.rekluzlabs.makokolorize.ui.screens.SettingsScreen
import com.rekluzlabs.makokolorize.ui.screens.SplashScreen
import com.rekluzlabs.makokolorize.ui.theme.MakokolorizeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MakokolorizeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

sealed class AppScreen {
    data object Splash : AppScreen()
    data object Home : AppScreen()
    data object Settings : AppScreen()
    data class Restore(val imageUri: Uri) : AppScreen()
    data class Result(val imageUri: Uri, val resultUri: Uri) : AppScreen()
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }
    val context = LocalContext.current

    BackHandler(enabled = currentScreen !is AppScreen.Home) {
        currentScreen = when (currentScreen) {
            is AppScreen.Restore -> AppScreen.Home
            is AppScreen.Result -> {
                val result = currentScreen as AppScreen.Result
                AppScreen.Restore(result.imageUri)
            }
            is AppScreen.Settings -> AppScreen.Home
            else -> AppScreen.Home
        }
    }

    Crossfade(targetState = currentScreen, label = "screen") { screen ->
        when (screen) {
            is AppScreen.Splash -> {
                SplashScreen(
                    onNavigate = { currentScreen = AppScreen.Home },
                    context = context
                )
            }

            is AppScreen.Home -> {
                HomeScreen(
                    onImageSelected = { uri ->
                        currentScreen = AppScreen.Restore(uri)
                    },
                    onNavigateToSettings = {
                        currentScreen = AppScreen.Settings
                    }
                )
            }

            is AppScreen.Settings -> {
                SettingsScreen(
                    onBack = { currentScreen = AppScreen.Home }
                )
            }

            is AppScreen.Restore -> {
                RestoreRoute(
                    imageUri = screen.imageUri,
                    onResultReady = { resultUri ->
                        currentScreen = AppScreen.Result(screen.imageUri, resultUri)
                    },
                    onBack = { currentScreen = AppScreen.Home }
                )
            }

            is AppScreen.Result -> {
                ResultScreen(
                    originalUri = screen.imageUri,
                    resultUri = screen.resultUri,
                    onBack = {
                        currentScreen = AppScreen.Restore(screen.imageUri)
                    },
                    onReRun = {
                        currentScreen = AppScreen.Restore(screen.imageUri)
                    }
                )
            }
        }
    }
}
