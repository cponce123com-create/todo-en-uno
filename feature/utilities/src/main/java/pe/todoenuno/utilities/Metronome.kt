package pe.todoenuno.utilities

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MetronomeScreen() {
    var bpm by remember { mutableIntStateOf(100) }
    var running by remember { mutableStateOf(false) }
    var ticks by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC,72) }.getOrNull() }
    DisposableEffect(tone) { onDispose { tone?.release() } }
    LaunchedEffect(running,bpm,tone) {
        if(!running || tone==null) return@LaunchedEffect
        while(true) {
            runCatching { tone.startTone(ToneGenerator.TONE_PROP_BEEP,85) }
                .onFailure { error="El audio no está disponible."; running=false }
            ticks++
            delay(60_000L/bpm)
        }
    }
    Column(Modifier.fillMaxSize().padding(22.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.Center) {
        Text("Metrónomo",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text("$bpm BPM",style=MaterialTheme.typography.displayMedium,
            modifier=Modifier.padding(vertical=22.dp))
        Slider(value=bpm.toFloat(),onValueChange={bpm=it.toInt()},
            valueRange=40f..220f,steps=179)
        Text("Pulsos: $ticks")
        Button(enabled=tone!=null,onClick={running=!running},
            modifier=Modifier.padding(top=16.dp)){
            Text(if(running)"Detener" else "Iniciar")
        }
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Text("La señal funciona mientras esta herramienta está abierta. Usa el volumen multimedia del teléfono.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(top=25.dp))
    }
}
