package pe.todoenuno.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.*

object GeoDistance {
    /** Spherical great-circle length, not walking/driving route length or terrain slope. */
    fun km(lat1:Double,lon1:Double,lat2:Double,lon2:Double):Double {
        require(lat1 in -90.0..90.0 && lat2 in -90.0..90.0)
        require(lon1 in -180.0..180.0 && lon2 in -180.0..180.0)
        val r=6371.0088
        val dLat=Math.toRadians(lat2-lat1)
        val dLon=Math.toRadians(lon2-lon1)
        val a=sin(dLat/2).pow(2.0)+cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(dLon/2).pow(2.0)
        return r*2*asin(sqrt(a.coerceIn(0.0,1.0)))
    }
}

@Composable
fun DistanceCalculatorScreen(){
    var lat1 by rememberSaveable{mutableStateOf("-12.0464")}
    var lon1 by rememberSaveable{mutableStateOf("-77.0428")}
    var lat2 by rememberSaveable{mutableStateOf("-11.1213")}
    var lon2 by rememberSaveable{mutableStateOf("-75.3537")}
    val nums=listOf(lat1,lon1,lat2,lon2).map{it.replace(',','.').toDoubleOrNull()}
    val km=if(nums.all{it!=null})runCatching{
        GeoDistance.km(nums[0]!!,nums[1]!!,nums[2]!!,nums[3]!!)
    }.getOrNull() else null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Distancia entre puntos",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Ingresa coordenadas decimales (ejemplo inicial: Lima y Selva Central).")
        @Composable fun field(title:String,value:String,change:(String)->Unit){
            OutlinedTextField(value=value,onValueChange={change(it.take(18))},
                label={Text(title)},singleLine=true,modifier=Modifier.fillMaxWidth(),
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
        }
        field("Latitud de origen",lat1){lat1=it}
        field("Longitud de origen",lon1){lon1=it}
        field("Latitud de destino",lat2){lat2=it}
        field("Longitud de destino",lon2){lon2=it}
        Text(km?.let{String.format(Locale.ROOT,"%.2f km",it)} ?: "Coordenadas inválidas",
            style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
        Text("Distancia geodésica aproximada en línea recta, no ruta terrestre. Funciona offline y no solicita ubicación.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
