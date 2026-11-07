package pe.todoenuno.utilities

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Solo utilidades del dominio productividad/tiempo y sistema. */
@Composable
fun ToolScreen(id: String) {
    when (id) {
        "stopwatch" -> StopwatchScreen()
        "countdown" -> CountdownScreen()
        "qr" -> QrGeneratorScreen()
        "password" -> PasswordGeneratorScreen()
        "pomodoro" -> PomodoroScreen()
        "text" -> TextToolsScreen()
        "color" -> ColorToolsScreen()
        "morse" -> MorseToolsScreen()
        "device" -> DeviceInfoScreen()
        "metronome" -> MetronomeScreen()
        "network" -> NetworkInfoScreen()
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Herramienta no disponible")
        }
    }
}
