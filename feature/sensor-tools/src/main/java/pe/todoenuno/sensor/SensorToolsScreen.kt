package pe.todoenuno.sensor

import androidx.compose.runtime.Composable

@Composable
fun SensorToolsScreen(id: String) {
    when (id) {
        "flashlight" -> FlashlightScreen()
        "compass" -> CompassScreen()
        "level" -> LevelScreen()
        "ruler" -> RulerScreen()
        "magnetic" -> MagneticFieldScreen()
        else -> error("Sensor tool is not registered: $id")
    }
}
