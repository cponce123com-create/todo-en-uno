package pe.todoenuno.utilities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val clockZones = listOf(
    "Lima · Perú" to "America/Lima",
    "Ciudad de México" to "America/Mexico_City",
    "Nueva York" to "America/New_York",
    "Madrid" to "Europe/Madrid",
    "Londres" to "Europe/London",
    "Tokio" to "Asia/Tokyo",
)

@Composable
fun WorldClockScreen() {
    var instant by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            instant = Instant.now()
            delay(1000L)
        }
    }
    val formatter = remember { DateTimeFormatter.ofPattern("EEE dd MMM · HH:mm:ss", Locale.forLanguageTag("es-PE")) }

    Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Reloj mundial", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Hora local de seis ciudades, sin conexión.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        clockZones.forEach { (place, zone) ->
            val local = instant.atZone(ZoneId.of(zone))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(place, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(formatter.format(local), style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
