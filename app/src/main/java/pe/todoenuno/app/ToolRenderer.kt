package pe.todoenuno.app

import androidx.compose.runtime.Composable
import pe.todoenuno.audio.AudioPlayerScreen
import pe.todoenuno.camera.CameraToolScreen
import pe.todoenuno.finance.CurrencyScreen
import pe.todoenuno.location.LocationToolsScreen
import pe.todoenuno.math.MathToolsScreen
import pe.todoenuno.recorder.SoundMeterScreen
import pe.todoenuno.recorder.TunerScreen
import pe.todoenuno.recorder.VoiceRecorderScreen
import pe.todoenuno.sensor.SensorToolsScreen
import pe.todoenuno.utilities.ToolScreen

/** Registrar por dominio evita acoplar las implementaciones del hardware al catálogo. */
enum class ToolDomain { MATH, SENSOR, LOCATION, UTILITIES, AUDIO, RECORDER, FINANCE, CAMERA }

object ToolRouting {
    private val math = setOf("calculator", "converter", "finance", "age", "bmi")
    private val sensors = setOf("flashlight", "compass", "level", "ruler", "magnetic")
    private val locations = setOf("gps", "distance", "worldclock", "solar")
    private val utilities = setOf(
        "stopwatch", "countdown", "qr", "password", "pomodoro",
        "text", "color", "morse", "device", "metronome", "network"
    )
    private val camera = setOf("mirror", "magnifier", "qrscan")
    fun domain(id: String): ToolDomain? = when (id) {
        in math -> ToolDomain.MATH
        in sensors -> ToolDomain.SENSOR
        in locations -> ToolDomain.LOCATION
        in utilities -> ToolDomain.UTILITIES
        "audio" -> ToolDomain.AUDIO
        "recorder", "sound", "tuner" -> ToolDomain.RECORDER
        "currency" -> ToolDomain.FINANCE
        in camera -> ToolDomain.CAMERA
        else -> null
    }
}

@Composable
fun AppToolRenderer(id: String) {
    when (ToolRouting.domain(id)) {
        ToolDomain.MATH -> MathToolsScreen(id)
        ToolDomain.SENSOR -> SensorToolsScreen(id)
        ToolDomain.LOCATION -> LocationToolsScreen(id)
        ToolDomain.UTILITIES -> ToolScreen(id)
        ToolDomain.AUDIO -> AudioPlayerScreen()
        ToolDomain.RECORDER -> when (id) {
            "recorder" -> VoiceRecorderScreen()
            "sound" -> SoundMeterScreen()
            else -> TunerScreen()
        }
        ToolDomain.FINANCE -> CurrencyScreen()
        ToolDomain.CAMERA -> CameraToolScreen(id)
        null -> androidx.compose.material3.Text("Herramienta no disponible")
    }
}
