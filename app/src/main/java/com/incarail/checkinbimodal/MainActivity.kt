package com.incarail.checkinbimodal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.incarail.checkinbimodal.ui.screens.FrequencyListScreen
import com.incarail.checkinbimodal.ui.screens.LoginScreen
import com.incarail.checkinbimodal.ui.screens.PassengerListScreen
import com.incarail.checkinbimodal.ui.screens.SummaryScreen
import com.incarail.checkinbimodal.ui.theme.CheckinBimodalTheme
import com.incarail.checkinbimodal.viewmodel.CheckinViewModel

/**
 * Punto de entrada. La navegación entre las 4 pantallas se maneja con un estado simple
 * (enum AppScreen) en vez de Navigation-Compose para mantener el prototipo legible;
 * la dependencia navigation-compose ya está en build.gradle.kts por si se prefiere
 * migrar a un NavHost más adelante (recomendable cuando se sumen más pantallas).
 */
class MainActivity : ComponentActivity() {

    private val viewModel: CheckinViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CheckinBimodalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App(viewModel)
                }
            }
        }
    }
}

private enum class AppScreen { LOGIN, FREQUENCIES, PASSENGERS, SUMMARY }

@Composable
private fun App(viewModel: CheckinViewModel) {
    var screen by remember { mutableStateOf(AppScreen.LOGIN) }

    when (screen) {
        AppScreen.LOGIN -> LoginScreen(onLoggedIn = { screen = AppScreen.FREQUENCIES })
        AppScreen.FREQUENCIES -> FrequencyListScreen(
            viewModel = viewModel,
            onSelect = { freq ->
                viewModel.selectFrequency(freq)
                screen = AppScreen.PASSENGERS
            },
        )
        AppScreen.PASSENGERS -> PassengerListScreen(
            viewModel = viewModel,
            onBack = { screen = AppScreen.FREQUENCIES },
            onClosed = { screen = AppScreen.SUMMARY },
        )
        AppScreen.SUMMARY -> SummaryScreen(
            viewModel = viewModel,
            onDone = { screen = AppScreen.FREQUENCIES },
        )
    }
}
