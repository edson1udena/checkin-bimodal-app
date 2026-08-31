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
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.incarail.checkinbimodal.data.MockPassengerRepository
import com.incarail.checkinbimodal.data.PassengerRepository
import com.incarail.checkinbimodal.data.SalesApiPassengerRepository
import com.incarail.checkinbimodal.ui.screens.CrewScreen
import com.incarail.checkinbimodal.ui.screens.FrequencyListScreen
import com.incarail.checkinbimodal.ui.screens.LoginScreen
import com.incarail.checkinbimodal.ui.screens.PassengerDetailScreen
import com.incarail.checkinbimodal.ui.screens.PassengerListScreen
import com.incarail.checkinbimodal.ui.screens.SummaryScreen
import com.incarail.checkinbimodal.ui.theme.CheckinBimodalTheme
import com.incarail.checkinbimodal.viewmodel.CheckinViewModel

/**
 * Punto de entrada. La navegación entre las 4 pantallas se maneja con un estado simple
 * (enum AppScreen) en vez de Navigation-Compose para mantener el prototipo legible;
 * la dependencia navigation-compose ya está en build.gradle.kts por si se prefiere
 * migrar a un NavHost más adelante (recomendable cuando se sumen más pantallas).
 *
 * Elección de repositorio: si local.properties tiene configurada la integración real
 * (ver INTEGRACION_DATOS.md), se usa [SalesApiPassengerRepository]; si no, la app sigue
 * funcionando con [MockPassengerRepository] — así una compilación sin ese secreto (por
 * ejemplo, la de GitHub Actions) nunca se rompe por su ausencia.
 */
private fun buildRepository(): PassengerRepository =
    if (BuildConfig.SALES_API_BASE_URL.isNotBlank()) SalesApiPassengerRepository() else MockPassengerRepository()

class MainActivity : ComponentActivity() {

    private val viewModel: CheckinViewModel by viewModels {
        viewModelFactory { initializer { CheckinViewModel(buildRepository()) } }
    }

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

private enum class AppScreen { LOGIN, FREQUENCIES, PASSENGERS, CREW, DETAIL, SUMMARY }

@Composable
private fun App(viewModel: CheckinViewModel) {
    var screen by remember { mutableStateOf(AppScreen.LOGIN) }

    when (screen) {
        AppScreen.LOGIN -> LoginScreen(onLoggedIn = { name ->
            viewModel.setUserName(name)
            screen = AppScreen.FREQUENCIES
        })
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
            onOpenCrew = { screen = AppScreen.CREW },
            onOpenDetail = { pax ->
                viewModel.selectPassenger(pax)
                screen = AppScreen.DETAIL
            },
        )
        AppScreen.CREW -> CrewScreen(
            viewModel = viewModel,
            onBack = { screen = AppScreen.PASSENGERS },
        )
        AppScreen.DETAIL -> PassengerDetailScreen(
            viewModel = viewModel,
            onBack = { screen = AppScreen.PASSENGERS },
        )
        AppScreen.SUMMARY -> SummaryScreen(
            viewModel = viewModel,
            onDone = { screen = AppScreen.FREQUENCIES },
        )
    }
}
