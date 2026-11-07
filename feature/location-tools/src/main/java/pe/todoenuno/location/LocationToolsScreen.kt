package pe.todoenuno.location

import androidx.compose.runtime.Composable

@Composable
fun LocationToolsScreen(id: String) {
    when (id) {
        "gps" -> GpsToolScreen()
        "distance" -> DistanceCalculatorScreen()
        "worldclock" -> WorldClockScreen()
        "solar" -> SolarLunarScreen()
        else -> error("Location tool is not registered: $id")
    }
}
