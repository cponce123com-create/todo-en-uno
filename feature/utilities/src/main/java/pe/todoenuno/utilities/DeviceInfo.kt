package pe.todoenuno.utilities

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.app.ActivityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun DeviceInfoScreen() {
    val context=LocalContext.current
    val battery=context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val level=battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val activity=context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memory=ActivityManager.MemoryInfo().also { activity.getMemoryInfo(it) }
    val gib=1024.0*1024*1024
    val details=listOf(
        "Fabricante" to Build.MANUFACTURER,
        "Modelo" to Build.MODEL,
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Batería" to if (level in 0..100) "$level%" else "Sin datos",
        "Memoria RAM total" to String.format(Locale.ROOT,"%.2f GiB",memory.totalMem/gib),
        "RAM disponible" to String.format(Locale.ROOT,"%.2f GiB",memory.availMem/gib),
        "Núcleos lógicos" to Runtime.getRuntime().availableProcessors().toString(),
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Información del dispositivo",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        details.forEach { (name,value) ->
            Text(name,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value,style=MaterialTheme.typography.titleMedium)
            HorizontalDivider()
        }
        Text("Android limita el acceso a métricas globales de CPU, consumo por app y batería de otras aplicaciones.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
