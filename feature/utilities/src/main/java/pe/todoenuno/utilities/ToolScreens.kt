package pe.todoenuno.utilities

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Punto de entrada público del módulo: la app no necesita importar pantallas internas. */
@Composable
fun ToolScreen(id: String) {
    when (id) {
        "calculator" -> CalculatorScreen()
        "converter" -> ConverterScreen()
        "stopwatch" -> StopwatchScreen()
        "flashlight" -> FlashlightScreen()
        "countdown" -> CountdownScreen()
        "worldclock" -> WorldClockScreen()
        "compass" -> CompassScreen()
        "level" -> LevelScreen()
        "qr" -> QrGeneratorScreen()
        "password" -> PasswordGeneratorScreen()
        "gps" -> GpsToolScreen()
        "ruler" -> RulerScreen()
        "pomodoro" -> PomodoroScreen()
        "text" -> TextToolsScreen()
        "color" -> ColorToolsScreen()
        "finance" -> FinancialToolsScreen()
        "morse" -> MorseToolsScreen()
        "device" -> DeviceInfoScreen()
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Herramienta no disponible")
        }
    }
}
