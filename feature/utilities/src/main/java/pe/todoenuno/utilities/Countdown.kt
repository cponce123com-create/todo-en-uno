package pe.todoenuno.utilities

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun CountdownScreen() {
    var minutes by rememberSaveable { mutableStateOf("5") }
    var remaining by rememberSaveable { mutableLongStateOf(300000L) }
    var endTime by rememberSaveable { mutableLongStateOf(0L) }
    var running by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running) {
            remaining = (endTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            if (remaining == 0L) { running = false; break }
            delay(100)
        }
    }
    val totalSeconds = (remaining + 999L) / 1000L
    val shown = String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Temporizador", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(shown, fontSize = 54.sp, modifier = Modifier.padding(vertical = 25.dp))
        OutlinedTextField(
            value = minutes,
            onValueChange = { minutes = it.filter(Char::isDigit).take(3) },
            label = { Text("Minutos") },
            singleLine = true,
            enabled = !running,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Button(onClick = {
                if (running) {
                    remaining = (endTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                    running = false
                } else {
                    if (remaining <= 0L) remaining = (minutes.toLongOrNull() ?: 0L).coerceIn(0L, 999L) * 60_000L
                    if (remaining > 0L) {
                        endTime = SystemClock.elapsedRealtime() + remaining
                        running = true
                    }
                }
            }) { Text(if (running) "Pausar" else "Iniciar") }
            OutlinedButton(onClick = {
                running = false
                remaining = (minutes.toLongOrNull() ?: 0L).coerceIn(0L, 999L) * 60_000L
            }) { Text("Reiniciar") }
        }
        Text("Este temporizador funciona con la pantalla de la herramienta abierta. Las alarmas en segundo plano llegarán en una versión posterior.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 25.dp))
    }
}
