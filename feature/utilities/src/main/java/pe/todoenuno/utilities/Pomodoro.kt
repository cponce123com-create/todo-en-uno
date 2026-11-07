package pe.todoenuno.utilities

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun PomodoroScreen() {
    var focus by remember { mutableStateOf(true) }
    var remaining by remember { mutableLongStateOf(25 * 60_000L) }
    var endAt by remember { mutableLongStateOf(0L) }
    var active by remember { mutableStateOf(false) }
    var cycles by remember { mutableLongStateOf(0L) }
    val full = if (focus) 25 * 60_000L else 5 * 60_000L
    LaunchedEffect(active) {
        while (active) {
            remaining = (endAt-SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            if(remaining==0L) {
                active=false
                if(focus) cycles++
                focus=!focus
                remaining=if(focus) 25*60_000L else 5*60_000L
                break
            }
            delay(250)
        }
    }
    Column(Modifier.fillMaxSize().padding(22.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Text("Pomodoro",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(if(focus)"Enfoque · 25 minutos" else "Descanso · 5 minutos",
            modifier=Modifier.padding(12.dp))
        Text(String.format(Locale.ROOT,"%02d:%02d",
            (remaining+999)/60_000,((remaining+999)/1000)%60),
            fontSize=52.sp,modifier=Modifier.padding(22.dp))
        Text("Sesiones terminadas: $cycles")
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.padding(top=20.dp)){
            Button(onClick={
                if(active) {
                    remaining=(endAt-SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                    active=false
                } else {
                    endAt=SystemClock.elapsedRealtime()+remaining
                    active=true
                }
            }) {Text(if(active)"Pausar" else "Iniciar")}
            OutlinedButton(onClick={active=false;remaining=full}){Text("Reiniciar")}
        }
        OutlinedButton(onClick={
            active=false
            focus=!focus
            remaining=if(focus)25*60_000L else 5*60_000L
        },modifier=Modifier.padding(top=8.dp)) {Text("Cambiar a ${if(focus)"descanso" else "enfoque"}")}
        Text("Por ahora permanece activo mientras esta herramienta está abierta. No envía alertas en segundo plano.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(top=28.dp))
    }
}
