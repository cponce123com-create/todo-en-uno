package pe.todoenuno.peru

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class QuakeReport(
    val magnitude:Double?,
    val depthKm:Double?,
    val date:String,
    val time:String,
    val reference:String,
    val latitude:Double?,
    val longitude:Double?,
)

object IgpService {
    const val PAGE="https://ultimosismo.igp.gob.pe/productos/reportes-sismicos"
    // Fuente geográfica pública del IGP: servicio ArcGIS UltimoSismo, capa 0.
    const val ENDPOINT="https://ide.igp.gob.pe/arcgis/rest/services/monitoreocensis/UltimoSismo/MapServer/0/query?where=1%3D1&outFields=%2A&returnGeometry=false&f=json"

    fun decode(json:String):List<QuakeReport> {
        val root=JSONObject(json)
        if(root.has("error"))error("El servicio del IGP devolvió un error.")
        val array=root.optJSONArray("features")?:return emptyList()
        return newestFirst((0 until array.length()).mapNotNull {idx->
            val attrs=array.optJSONObject(idx)?.optJSONObject("attributes")?:return@mapNotNull null
            QuakeReport(
                attrs.optDouble("magnitud").takeIf{it.isFinite()},
                attrs.optDouble("profundidad").takeIf{it.isFinite()},
                attrs.optString("fecha_local"),
                attrs.optString("hora_local"),
                attrs.optString("referencia","Sin referencia"),
                attrs.optDouble("latitud").takeIf{it.isFinite()},
                attrs.optDouble("longitud").takeIf{it.isFinite()}
            )
        }).take(30)
    }
    fun newestFirst(events:List<QuakeReport>):List<QuakeReport> {
        val formats=listOf("dd/MM/yyyy HH:mm:ss","dd/MM/yyyy HH:mm","yyyy-MM-dd HH:mm:ss","yyyy-MM-dd HH:mm")
        fun timestamp(event:QuakeReport):Long {
            val raw="${event.date} ${event.time}".trim()
            for(pattern in formats) {
                val parsed=runCatching {
                    SimpleDateFormat(pattern,Locale("es","PE")).apply { isLenient=false;timeZone=java.util.TimeZone.getTimeZone("America/Lima") }.parse(raw)
                }.getOrNull()
                if(parsed!=null)return parsed.time
            }
            return 0L
        }
        return events.sortedByDescending(::timestamp)
    }
    fun fetch():List<QuakeReport> {
        val conn=(URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            connectTimeout=9000
            readTimeout=9000
            requestMethod="GET"
            setRequestProperty("Accept","application/json")
        }
        return try {
            require(conn.responseCode==200){"Servidor IGP no disponible."}
            val raw=conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val result=StringBuilder()
                val buffer=CharArray(4096)
                while(true) {
                    val count=reader.read(buffer)
                    if(count<0)break
                    require(result.length+count<=200000){"Respuesta demasiado grande."}
                    result.append(buffer,0,count)
                }
                result.toString()
            }
            decode(raw)
        }finally{conn.disconnect()}
    }
}

@Composable
fun EarthquakesPeruScreen() {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val prefs=remember(context){context.getSharedPreferences("igp_last_query",Context.MODE_PRIVATE)}
    var reports by remember{mutableStateOf<List<QuakeReport>>(runCatching {
        val saved=JSONArray(prefs.getString("reports","[]"))
        (0 until saved.length()).map { i ->
            val o=saved.getJSONObject(i)
            QuakeReport(
                o.optDouble("magnitude").takeIf{!o.isNull("magnitude") && it.isFinite()},
                o.optDouble("depthKm").takeIf{!o.isNull("depthKm") && it.isFinite()},
                o.optString("date"),o.optString("time"),o.optString("reference"),
                o.optDouble("latitude").takeIf{!o.isNull("latitude") && it.isFinite()},
                o.optDouble("longitude").takeIf{!o.isNull("longitude") && it.isFinite()}
            )
        }
    }.getOrDefault(emptyList()))}
    var status by remember{mutableStateOf("")}
    var updated by remember{mutableStateOf(prefs.getLong("updated",0L))}
    var loading by remember{mutableStateOf(false)}
    var showAll by remember{mutableStateOf(false)}

    fun refresh(){
        if(loading)return
        loading=true
        scope.launch{
            runCatching{withContext(Dispatchers.IO){IgpService.fetch()}}
                .onSuccess {list->
                    // Keep a bounded local history across app restarts; never invent events.
                    reports=IgpService.newestFirst((list+reports).distinctBy {
                        "${it.date}|${it.time}|${it.reference}|${it.magnitude}"
                    }).take(100)
                    updated=System.currentTimeMillis()
                    val json=JSONArray()
                    reports.forEach { event ->
                        json.put(JSONObject().apply {
                            put("magnitude",event.magnitude ?: JSONObject.NULL)
                            put("depthKm",event.depthKm ?: JSONObject.NULL)
                            put("date",event.date);put("time",event.time)
                            put("reference",event.reference)
                            put("latitude",event.latitude ?: JSONObject.NULL)
                            put("longitude",event.longitude ?: JSONObject.NULL)
                        })
                    }
                    prefs.edit().putLong("updated",updated).putString("reports",json.toString()).apply()
                    status=if(list.isEmpty())"El servicio no devolvió eventos." else "Consulta completada."
                }.onFailure{status="No fue posible consultar el IGP. Revisa tu conexión."}
            loading=false
        }
    }
    LaunchedEffect(Unit){refresh()}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("Sismos registrados en Perú",style=MaterialTheme.typography.headlineSmall,
            fontWeight=FontWeight.Bold)
        Text("Datos oficiales del Instituto Geofísico del Perú (IGP / CENSIS).",
            style=MaterialTheme.typography.bodyMedium)
        Text("No predice terremotos ni sustituye una alerta temprana o avisos de INDECI.",
            color=MaterialTheme.colorScheme.error,
            style=MaterialTheme.typography.bodySmall)
        Button(onClick={refresh()},enabled=!loading) {
            Text(if(loading)"Consultando…" else "Actualizar desde IGP")
        }
        if(status.isNotBlank())Text(status)
        if(updated>0L)Text(
            "Última consulta: ${SimpleDateFormat("dd/MM/yyyy HH:mm",Locale("es","PE")).format(Date(updated))}",
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            style=MaterialTheme.typography.bodySmall)
        if(reports.isEmpty() && !loading)Text("Pulsa actualizar para consultar el evento disponible.")
        Text("Últimos ${minOf(5,reports.size)} reportes guardados")
        if(reports.size<5)Text("La fuente consultada publica el último sismo. El historial local irá acumulando los eventos que consultes; aún no hay cinco guardados.",
            style=MaterialTheme.typography.bodySmall)
        (if(showAll)reports else reports.take(5)).forEach {event->
            Card(Modifier.fillMaxWidth()){
                Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                    Text("Magnitud: ${event.magnitude ?: "N/D"}",
                        style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text(event.reference)
                    Text("Fecha: ${event.date} ${event.time} (hora reportada por IGP)")
                    Text("Profundidad: ${event.depthKm?.let{"$it km"} ?: "N/D"}")
                    if(event.latitude!=null && event.longitude!=null)
                        Text("Lat. ${event.latitude} · Lon. ${event.longitude}")
                }
            }
        }
        if(reports.size>5) Button(onClick={showAll=!showAll}) {
            Text(if(showAll)"Mostrar solo los últimos cinco" else "Ver historial local (${reports.size} eventos)")
        }
        Button(onClick={
            context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(IgpService.PAGE)))
        },modifier=Modifier.fillMaxWidth()) {
            Text("Ver reportes y antecedentes en portal oficial")
        }
        Text("El historial local conserva eventos consultados previamente (máximo 100), pero no reemplaza una fuente histórica oficial. La app no crea ni modifica magnitudes.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
