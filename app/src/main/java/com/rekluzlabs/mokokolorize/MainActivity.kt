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
import com.rekluzlabs.makokolorize.ui.screens.MainScreen
import com.rekluzlabs.makokolorize.ui.screens.PickerScreen
import com.rekluzlabs.makokolorize.ui.screens.ResultScreen
import com.rekluzlabs.makokolorize.ui.screens.SplashScreen
import com.rekluzlabs.makokolorize.ui.theme.Mako_colorizeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Mako_colorizeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

sealed class AppScreen {
    data object Splash : AppScreen()
    data object Picker : AppScreen()
    data class Main(val imageUri: Uri) : AppScreen()
    data class Result(val imageUri: Uri) : AppScreen()
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }
    val context = LocalContext.current

    BackHandler(enabled = currentScreen !is AppScreen.Splash) {
        currentScreen = when (currentScreen) {
            is AppScreen.Main -> AppScreen.Picker
            is AppScreen.Result -> AppScreen.Picker
            else -> AppScreen.Splash
        }
    }

    Crossfade(targetState = currentScreen, label = "screen") { screen ->
        when (screen) {
            is AppScreen.Splash -> {
                SplashScreen(
                    onNavigate = { currentScreen = AppScreen.Picker },
                    context = context
                )
            }

            is AppScreen.Picker -> {
                PickerScreen(
                    onImageSelected = { uri ->
                        currentScreen = AppScreen.Main(uri)
                    }
                )
            }

            is AppScreen.Main -> {
                MainScreen(
                    imageUri = screen.imageUri,
                    onResultReady = {
                        currentScreen = AppScreen.Result(screen.imageUri)
                    },
                    onBack = { currentScreen = AppScreen.Picker }
                )
            }

            is AppScreen.Result -> {
                ResultScreen(
                    originalUri = screen.imageUri,
                    onBack = { currentScreen = AppScreen.Picker }
                )
            }
        }
    }
}
