package pe.todoenuno.app

import androidx.compose.runtime.Composable
import pe.todoenuno.audio.AudioPlayerScreen
import pe.todoenuno.games.GamesScreen
import pe.todoenuno.personal.QuickNotesScreen
import pe.todoenuno.personal.PainterColorsScreen
import pe.todoenuno.personal.IdVaultScreen
import pe.todoenuno.peru.EarthquakesPeruScreen
import pe.todoenuno.peru.WeatherScreen
import pe.todoenuno.peru.ParanormalScreen
import pe.todoenuno.readers.BibleScreen
import pe.todoenuno.readers.RecipeEpubScreen
import pe.todoenuno.readers.ConstitutionScreen
import pe.todoenuno.documents.PhotoPdfComposerScreen
import pe.todoenuno.camera.CameraToolScreen
import pe.todoenuno.camera.CameraLaserLevelScreen
import pe.todoenuno.documents.LabelDesignerScreen
import pe.todoenuno.finance.CurrencyScreen
import pe.todoenuno.education.PeriodicTableScreen
import pe.todoenuno.education.PhysicsToolsScreen
import pe.todoenuno.education.NotableTrianglesScreen
import pe.todoenuno.location.LocationToolsScreen
import pe.todoenuno.math.MathToolsScreen
import pe.todoenuno.recorder.SoundMeterScreen
import pe.todoenuno.recorder.TunerScreen
import pe.todoenuno.recorder.VoiceRecorderScreen
import pe.todoenuno.sensor.SensorToolsScreen
import pe.todoenuno.utilities.ToolScreen

/** Registrar por dominio evita acoplar las implementaciones del hardware al catálogo. */
enum class ToolDomain { MATH, SENSOR, LOCATION, UTILITIES, AUDIO, RECORDER, FINANCE, CAMERA, EDUCATION, DOCUMENTS, GAMES, PERSONAL, PERU, READERS, PARANORMAL }

object ToolRouting {
    private val math = setOf("calculator", "converter", "finance", "age", "bmi")
    private val sensors = setOf("flashlight", "compass", "level", "ruler", "magnetic")
    private val locations = setOf("gps", "distance", "worldclock", "solar")
    private val utilities = setOf(
        "stopwatch", "countdown", "qr", "password", "pomodoro",
        "text", "color", "morse", "device", "metronome", "network"
    )
    private val camera = setOf("mirror", "magnifier", "qrscan","laserlevel")
    private val educational = setOf("periodic", "physics", "triangles")
    private val games = setOf("snake","blocks","shooter","2048","breakout","fifteen")
    private val personal = setOf("notes","idvault","paintmix")
    private val peruvian = setOf("earthquakes","constitution","weather")
    private val reading = setOf("bible","recipes")
    fun domain(id: String): ToolDomain? = when (id) {
        in math -> ToolDomain.MATH
        in sensors -> ToolDomain.SENSOR
        in locations -> ToolDomain.LOCATION
        in utilities -> ToolDomain.UTILITIES
        "audio" -> ToolDomain.AUDIO
        "recorder", "sound", "tuner" -> ToolDomain.RECORDER
        "currency" -> ToolDomain.FINANCE
        in camera -> ToolDomain.CAMERA
        in educational -> ToolDomain.EDUCATION
        "labels","photopdf" -> ToolDomain.DOCUMENTS
        in games -> ToolDomain.GAMES
        in personal -> ToolDomain.PERSONAL
        in peruvian -> ToolDomain.PERU
        in reading -> ToolDomain.READERS
        "paranormal" -> ToolDomain.PARANORMAL
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
        ToolDomain.CAMERA -> if(id=="laserlevel")CameraLaserLevelScreen() else CameraToolScreen(id)
        ToolDomain.DOCUMENTS -> if(id=="photopdf")PhotoPdfComposerScreen() else LabelDesignerScreen()
        ToolDomain.GAMES -> GamesScreen(id)
        ToolDomain.PERSONAL -> when(id) {
            "idvault" -> IdVaultScreen()
            "notes" -> QuickNotesScreen()
            else -> PainterColorsScreen()
        }
        ToolDomain.PERU -> when(id) {
            "earthquakes" -> EarthquakesPeruScreen()
            "weather" -> WeatherScreen()
            else -> ConstitutionScreen()
        }
        ToolDomain.PARANORMAL -> ParanormalScreen()
        ToolDomain.READERS -> if(id=="bible")BibleScreen() else RecipeEpubScreen()
        ToolDomain.EDUCATION -> when(id){
            "periodic"->PeriodicTableScreen()
            "physics"->PhysicsToolsScreen()
            else->NotableTrianglesScreen()
        }
        null -> androidx.compose.material3.Text("Herramienta no disponible")
    }
}
