package pe.todoenuno.utilities

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun StopwatchScreen() {
    var elapsed by rememberSaveable { mutableLongStateOf(0L) }
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    var running by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running) {
            elapsed = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
            delay(50)
        }
    }
    val centiseconds = elapsed / 10
    val hours = centiseconds / 360000
    val minutes = (centiseconds / 6000) % 60
    val seconds = (centiseconds / 100) % 60
    val partial = centiseconds % 100
    val shown = String.format(Locale.US, "%02d:%02d:%02d.%02d", hours, minutes, seconds, partial)

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Cronómetro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(shown, fontSize = 36.sp, modifier = Modifier.padding(vertical = 32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(onClick = {
                if (running) {
                    elapsed = SystemClock.elapsedRealtime() - startedAt
                    running = false
                } else {
                    startedAt = SystemClock.elapsedRealtime() - elapsed
                    running = true
                }
            }) { Text(if (running) "Pausar" else "Iniciar") }
            OutlinedButton(onClick = { running = false; elapsed = 0L; startedAt = 0L }) {
                Text("Reiniciar")
            }
        }
        Text("Tiempo medido con el reloj monotónico del dispositivo.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 20.dp))
    }
}
