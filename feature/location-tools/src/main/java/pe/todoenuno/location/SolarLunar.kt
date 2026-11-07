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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

/**
 * NOAA approximation for civil sunrise/sunset (atmospheric refraction 90.833°).
 * No network. Results near poles can be absent and elevations/weather are not modeled.
 */
object SunMath {
    fun utcMinutes(date:LocalDate, latitude:Double, longitude:Double, rising:Boolean):Double? {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
        val n=date.dayOfYear.toDouble()
        val lngHour=longitude/15.0
        val t=n+(((if(rising)6.0 else 18.0)-lngHour)/24.0)
        val m=(0.9856*t)-3.289
        val l=norm((m+1.916*sin(rad(m))+0.020*sin(2.0*rad(m))+282.634))
        var ra=norm(deg(atan(0.91764*tan(rad(l)))))
        ra+=floor(l/90.0)*90.0-floor(ra/90.0)*90.0
        ra/=15.0
        val sinDec=0.39782*sin(rad(l))
        val cosDec=cos(asin(sinDec))
        val cosH=(cos(rad(90.833))-(sinDec*sin(rad(latitude))))/(cosDec*cos(rad(latitude)))
        if(!cosH.isFinite() || cosH < -1.0 || cosH > 1.0) return null
        val h=if(rising)360.0-deg(acos(cosH)) else deg(acos(cosH))
        val local=h/15.0+ra-(0.06571*t)-6.622
        val utcMinutes=(local-lngHour)*60.0
        return ((utcMinutes%1440.0)+1440.0)%1440.0
    }
    private fun norm(x:Double):Double=((x%360.0)+360.0)%360.0
    private fun rad(x:Double)=Math.toRadians(x)
    private fun deg(x:Double)=Math.toDegrees(x)
    fun moonFraction(time:Instant):Double {
        val newMoon=Instant.parse("2000-01-06T18:14:00Z")
        val days=(time.epochSecond-newMoon.epochSecond)/86400.0
        return (((days/29.530588853)%1.0)+1.0)%1.0
    }
    fun moonLabel(fraction:Double):String=when {
        fraction < 0.035 || fraction>=0.965->"Luna nueva"
        fraction<0.22->"Creciente"
        fraction<0.285->"Cuarto creciente"
        fraction<0.465->"Gibosa creciente"
        fraction<0.535->"Luna llena"
        fraction<0.715->"Gibosa menguante"
        fraction<0.785->"Cuarto menguante"
        else->"Menguante"
    }
}

@Composable
fun SolarLunarScreen() {
    var latitude by rememberSaveable{mutableStateOf("-12.0464")}
    var longitude by rememberSaveable{mutableStateOf("-77.0428")}
    val lat=latitude.replace(',','.').toDoubleOrNull()?.takeIf{it in -90.0..90.0}
    val lon=longitude.replace(',','.').toDoubleOrNull()?.takeIf{it in -180.0..180.0}
    val zone=remember{ZoneId.systemDefault()}
    val today=remember{LocalDate.now(zone)}
    val fmt=remember{DateTimeFormatter.ofPattern("HH:mm",Locale.forLanguageTag("es-PE"))}
    val moon=SunMath.moonFraction(today.atStartOfDay(zone).toInstant())
    fun formatted(minutes:Double?):String {
        if(minutes==null)return "No hay salida o puesta en esta latitud/fecha"
        val instant=today.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds((minutes*60).toLong())
        return fmt.format(instant.atZone(zone))
    }
    val rise=if(lat!=null && lon!=null)SunMath.utcMinutes(today,lat,lon,true) else null
    val set=if(lat!=null && lon!=null)SunMath.utcMinutes(today,lat,lon,false) else null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(15.dp)) {
        Text("Sol y Luna",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Coordenadas editables. Valor inicial: Lima, Perú.")
        OutlinedTextField(value=latitude,onValueChange={latitude=it.take(18)},
            modifier=Modifier.fillMaxWidth(),label={Text("Latitud (−90 a 90)")},
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
        OutlinedTextField(value=longitude,onValueChange={longitude=it.take(18)},
            modifier=Modifier.fillMaxWidth(),label={Text("Longitud (−180 a 180)")},
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
        if(lat==null || lon==null) Text("Introduce coordenadas válidas.",color=MaterialTheme.colorScheme.error)
        else {
            Text("Amanecer: ${formatted(rise)}",style=MaterialTheme.typography.titleLarge)
            Text("Atardecer: ${formatted(set)}",style=MaterialTheme.typography.titleLarge)
            Text("Hora dorada: aproximadamente la primera hora después del amanecer y antes del atardecer.")
        }
        Text("Fase lunar: ${SunMath.moonLabel(moon)}",style=MaterialTheme.typography.titleLarge)
        Text("Fracción del ciclo lunar: ${(moon*100).toInt()}%")
        Text("Fecha: $today · Zona horaria del teléfono: ${zone.id}")
        Text("Horas y fase aproximadas con cálculos offline; la altitud, obstáculos y atmósfera pueden alterar los valores reales.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
