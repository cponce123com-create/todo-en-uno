package pe.todoenuno.finance

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class ExchangeRate(val base: String,val quote: String,val rate: Double,val date: String)

class ExchangeRepository(private val context: Context) {
    private val prefs=context.getSharedPreferences("exchange_rates",Context.MODE_PRIVATE)
    fun cached(base:String,quote:String):ExchangeRate? {
        val raw=prefs.getString("${base}_${quote}",null)?:return null
        return runCatching {
            val data=JSONObject(raw)
            ExchangeRate(base,quote,data.getDouble("rate"),data.getString("date"))
        }.getOrNull()
    }
    suspend fun fetch(base:String,quote:String):ExchangeRate=withContext(Dispatchers.IO) {
        require(base.matches(Regex("[A-Z]{3}")) && quote.matches(Regex("[A-Z]{3}")))
        val url=URL("https://api.frankfurter.dev/v2/rate/$base/$quote")
        val conn=(url.openConnection() as HttpURLConnection)
        try {
            conn.connectTimeout=10000;conn.readTimeout=10000
            conn.requestMethod="GET"
            conn.setRequestProperty("Accept","application/json")
            if(conn.responseCode!=200) error("Servidor: HTTP ${conn.responseCode}")
            val body=conn.inputStream.bufferedReader().use { it.readText() }
            val json=JSONObject(body)
            val rate=json.getDouble("rate")
            val date=json.getString("date")
            require(rate>0 && rate.isFinite())
            val snapshot=ExchangeRate(base,quote,rate,date)
            prefs.edit().putString("${base}_${quote}",JSONObject().put("rate",rate).put("date",date).toString()).apply()
            snapshot
        } finally {conn.disconnect()}
    }
}

@Composable
fun CurrencyScreen() {
    val context=LocalContext.current
    val repo=remember(context){ExchangeRepository(context.applicationContext)}
    val scope=rememberCoroutineScope()
    var base by rememberSaveable { mutableStateOf("USD") }
    var quote by rememberSaveable { mutableStateOf("PEN") }
    var amount by rememberSaveable { mutableStateOf("100") }
    var snapshot by remember { mutableStateOf<ExchangeRate?>(null) }
    var fetching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val codes=listOf("PEN","USD","EUR","GBP","BRL","MXN","CLP","COP")
    LaunchedEffect(base,quote) {
        snapshot=if(base==quote) ExchangeRate(base,quote,1.0,"Sin conversión") else repo.cached(base,quote)
        error=null
    }
    val fmt=remember{DecimalFormat("#,##0.00",DecimalFormatSymbols(Locale.forLanguageTag("es-PE")))}
    val number=amount.replace(',','.').toDoubleOrNull()?.takeIf { it.isFinite() && it>=0 }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)) {
        Text("Tipo de cambio",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Divisa de origen",style=MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(9.dp)){
            codes.forEach { code->FilterChip(selected=code==base,onClick={base=code},label={Text(code)})}
        }
        Text("Divisa de destino",style=MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(9.dp)){
            codes.forEach { code->FilterChip(selected=code==quote,onClick={quote=code},label={Text(code)})}
        }
        OutlinedTextField(value=amount,onValueChange={amount=it.take(18)},
            label={Text("Monto")},singleLine=true,modifier=Modifier.fillMaxWidth(),
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
        Button(enabled=!fetching && base!=quote,onClick={
            fetching=true;error=null
            scope.launch {
                runCatching { repo.fetch(base,quote) }
                    .onSuccess { snapshot=it }
                    .onFailure { error="No se pudo actualizar. ${it.localizedMessage}" }
                fetching=false
            }
        }){Text(if(fetching)"Actualizando…" else "Actualizar cotización")}
        val current=snapshot?.takeIf { it.base==base && it.quote==quote }
        Text("Resultado: ${if(number!=null && current!=null)fmt.format(number*current.rate)+" "+quote else "Sin cotización"}",
            style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
        if(current!=null){
            Text("1 $base = ${current.rate} $quote")
            Text("Fecha de la tasa: ${current.date}. Fuente: Frankfurter (referencias de bancos centrales).")
        } else Text("Sin cotización guardada. Conecta a Internet y pulsa Actualizar.")
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Text("La última tasa consultada se conserva offline. Son tipos de referencia (punto medio), no precios de compra/venta ni cotizaciones BCRP.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
