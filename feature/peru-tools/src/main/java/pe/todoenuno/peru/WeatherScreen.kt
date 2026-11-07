package pe.todoenuno.peru

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

fun weatherDescription(code:Int):String = when(code) {
                0 -> "Despejado"
                1,2 -> "Mayormente despejado"
                3 -> "Nublado"
                45,48 -> "Niebla"
                51,53,55,56,57 -> "Llovizna"
                61,63,65,66,67 -> "Lluvia"
                71,73,75,77 -> "Nieve"
                80,81,82 -> "Chubascos"
                85,86 -> "Chubascos de nieve"
                95,96,99 -> "Tormenta"
                else -> "Código meteorológico WMO $code"
            }

private data class WeatherReport(val place:String,val temperature:Double,val apparent:Double,val humidity:Int,val wind:Double,val code:Int,val observed:String)
private fun weatherJson(url:String):JSONObject {
    val conn=(URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout=9000;readTimeout=9000;setRequestProperty("Accept","application/json")
    }
    return try {
        require(conn.responseCode==200){"Servicio meteorológico no disponible"}
        val raw=conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            val buffer=CharArray(4096)
            val result=StringBuilder()
            while(true) {
                val count=reader.read(buffer)
                if(count<0)break
                require(result.length+count<=100000){"Respuesta demasiado grande"}
                result.append(buffer,0,count)
            }
            result.toString()
        }
        JSONObject(raw)
    } finally { conn.disconnect() }
}
private fun lookupWeather(city:String):WeatherReport {
    require(city.length in 2..80){"Escribe una ciudad válida."}
    val geo=weatherJson("https://geocoding-api.open-meteo.com/v1/search?name="+URLEncoder.encode(city,"UTF-8")+"&count=1&language=es&format=json")
    val location=geo.optJSONArray("results")?.optJSONObject(0) ?: error("No se encontró la ciudad.")
    val lat=location.getDouble("latitude");val lon=location.getDouble("longitude")
    val data=weatherJson("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&timezone=auto")
    val current=data.getJSONObject("current")
    return WeatherReport(
        location.optString("name")+", "+location.optString("country"),
        current.getDouble("temperature_2m"),current.getDouble("apparent_temperature"),
        current.getInt("relative_humidity_2m"),current.getDouble("wind_speed_10m"),
        current.getInt("weather_code"),data.getJSONObject("current").optString("time")
    )
}
@Composable
fun WeatherScreen() {
    val scope=rememberCoroutineScope()
    var city by remember { mutableStateOf("Lima") }
    var report by remember { mutableStateOf<WeatherReport?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Clima y temperatura",style=MaterialTheme.typography.headlineSmall)
        Text("Consulta condiciones meteorológicas actuales por ciudad. Fuente: Open-Meteo. No requiere permiso de ubicación.")
        OutlinedTextField(value=city,onValueChange={city=it.take(80)},label={Text("Ciudad")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Button(enabled=!busy && city.isNotBlank(),onClick={
            busy=true;error="";report=null
            scope.launch {
                runCatching { withContext(Dispatchers.IO){lookupWeather(city.trim())} }
                    .onSuccess { report=it }.onFailure { error=it.message ?: "Error de conexión" }
                busy=false
            }
        }) { Text(if(busy)"Consultando…" else "Consultar clima") }
        if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
        report?.let {
            Text(it.place,style=MaterialTheme.typography.titleLarge)
            Text("${it.temperature} °C",style=MaterialTheme.typography.displayMedium)
            Text("Sensación térmica: ${it.apparent} °C")
            Text("Humedad: ${it.humidity}% · Viento: ${it.wind} km/h")
            Text("Condición: "+weatherDescription(it.code))
            Text("Actualizado por el proveedor: ${it.observed} (hora local de la ciudad)",style=MaterialTheme.typography.bodySmall)
        }
        Text("La temperatura corresponde a una estación/modelo meteorológico, no a un sensor del teléfono. Una cámara térmica real requiere hardware infrarrojo.",style=MaterialTheme.typography.bodySmall)
    }
}
