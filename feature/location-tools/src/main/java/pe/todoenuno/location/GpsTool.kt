package pe.todoenuno.location

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale

@Composable
fun GpsToolScreen() {
    val context=LocalContext.current
    val manager=remember(context){context.getSystemService(Context.LOCATION_SERVICE) as LocationManager}
    var granted by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
    ) }
    var location by remember { mutableStateOf<Location?>(null) }
    var message by remember { mutableStateOf("La ubicación solo se consulta con esta pantalla abierta.") }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
        granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (!granted) message = "Activa ubicación precisa para consultar GPS."
    }
    DisposableEffect(granted,manager) {
        val listener=LocationListener { updated -> location=updated; message="Datos GPS recibidos" }
        if(granted) {
            runCatching {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,1f,listener)
            }.onFailure { message="Activa GPS y comprueba los permisos del sistema." }
        }
        onDispose { manager.removeUpdates(listener) }
    }
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(17.dp)) {
        Text("GPS y coordenadas",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        if(!granted) Button(onClick={launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))}){
            Text("Autorizar ubicación precisa")
        }
        location?.let {
            Text("Latitud: ${formatGps(it.latitude)}")
            Text("Longitud: ${formatGps(it.longitude)}")
            Text("Precisión informada: ${it.accuracy.toInt()} m")
            Text("Altitud: ${if(it.hasAltitude()) formatGps(it.altitude)+" m" else "No disponible"}")
            Text("Velocidad: ${if(it.hasSpeed()) String.format(Locale.ROOT,"%.1f km/h",it.speed*3.6) else "No disponible"}")
            Button(onClick={
                val lat=formatGps(it.latitude)
                val lon=formatGps(it.longitude)
                val uri=Uri.parse("geo:$lat,$lon?q=$lat,$lon")
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,uri)) }
                    .onFailure { message="No tienes instalada una aplicación que abra mapas." }
            }) { Text("Abrir ubicación en una app de mapas") }
        } ?: Text(if(granted)"Buscando señal GPS… (puede requerir salir al exterior)" else "Permiso pendiente.")
        Text(message,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Mapa externo opcional: al pulsar Abrir, compartes coordenadas con otra aplicación. No se guardan en Todo en Uno.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
private fun formatGps(value: Double):String=String.format(Locale.ROOT,"%.6f",value)
