package pe.todoenuno.utilities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

/** NOAA sunrise equation (approximate); local solar time and moon phase, no API/network. */
object Astronomy {
    private fun sinD(d:Double)=sin(Math.toRadians(d))
    private fun cosD(d:Double)=cos(Math.toRadians(d))
    private fun norm(d:Double)=((d%360.0)+360.0)%360.0
    fun solarUtcHours(date:LocalDate, latitude:Double, longitude:Double, sunrise:Boolean):Double? {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
        val n=date.dayOfYear
        val t=n+((if(sunrise) 6.0 else 18.0)-longitude/15.0)/24.0
        val m=0.9856*t-3.289
        val l=norm(m+1.916*sinD(m)+0.020*sinD(2*m)+282.634)
        val ra0=norm(Math.toDegrees(atan(0.91764*tan(Math.toRadians(l)))))
        val ra=(ra0+(floor(l/90.0)*90.0-floor(ra0/90.0)*90.0))/15.0
        val sinDec=0.39782*sinD(l)
        val cosDec=cos(asin(sinDec))
        val cosH=(cosD(90.833)-sinDec*sinD(latitude))/(cosDec*cosD(latitude))
        if(!cosH.isFinite() || cosH>1.0 || cosH< -1.0) return null
        val h=(if(sunrise) 360.0-Math.toDegrees(acos(cosH)) else Math.toDegrees(acos(cosH)))/15.0
        val utc=h+ra-0.06571*t-6.622-longitude/15.0
        return (utc%24.0+24.0)%24.0
    }
    fun localTime(date:LocalDate, lat:Double, lon:Double, sunrise:Boolean, zone:ZoneId):LocalTime? {
        val utc=solarUtcHours(date,lat,lon,sunrise)?:return null
        val offsetSeconds=zone.rules.getOffset(date.atTime(12,0).atZone(zone).toInstant()).totalSeconds
        val localSeconds=((utc*3600.0).roundToLong()+offsetSeconds).floorMod(86400)
        return LocalTime.ofSecondOfDay(localSeconds)
    }
    private fun Long.floorMod(divisor:Long)=((this%divisor)+divisor)%divisor
    /** Lunation fraction 0 = new moon, .5 = full moon, approximate (+/- ~1 day). */
    fun moonPhase(date:LocalDate):Double {
        val reference=Instant.parse("2000-01-06T18:14:00Z")
        val days=ChronoUnit.SECONDS.between(reference,date.atTime(12,0).atZone(ZoneId.of("UTC")).toInstant())/86400.0
        val fraction=days/29.530588853
        return ((fraction%1.0)+1.0)%1.0
    }
    fun illuminatedFraction(phase:Double)=(1.0-cos(2.0*PI*phase))/2.0
    fun phaseName(phase:Double)=when {
        phase<0.06 || phase>=0.94 -> "Luna nueva"
        phase<0.19 -> "Creciente"
        phase<0.31 -> "Cuarto creciente"
        phase<0.44 -> "Gibosa creciente"
        phase<0.56 -> "Luna llena"
        phase<0.69 -> "Gibosa menguante"
        phase<0.81 -> "Cuarto menguante"
        else -> "Menguante"
    }
}

@Composable
fun SunMoonScreen() {
    val today=LocalDate.now()
    var latitude by rememberSaveable { mutableStateOf("-12.0464") }
    var longitude by rememberSaveable { mutableStateOf("-77.0428") }
    val lat=latitude.replace(',','.').toDoubleOrNull()?.takeIf { it.isFinite() && it in -89.0..89.0 }
    val lon=longitude.replace(',','.').toDoubleOrNull()?.takeIf { it.isFinite() && it in -180.0..180.0 }
    val zone=ZoneId.systemDefault()
    val formatter=DateTimeFormatter.ofPattern("HH:mm",Locale.forLanguageTag("es-PE"))
    val phase=Astronomy.moonPhase(today)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement=Arrangement.spacedBy(15.dp)) {
        Text("Sol y Luna",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Cálculos astronómicos aproximados · ${today} · Zona horaria: ${zone.id}",
            style=MaterialTheme.typography.bodySmall)
        Text("Ubicación de referencia: Lima, Perú. Puedes cambiar sus coordenadas.")
        OutlinedTextField(value=latitude,onValueChange={latitude=it.take(15)},modifier=Modifier.fillMaxWidth(),
            label={Text("Latitud (−90 a 90)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
        OutlinedTextField(value=longitude,onValueChange={longitude=it.take(15)},modifier=Modifier.fillMaxWidth(),
            label={Text("Longitud (−180 a 180)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
        if(lat!=null && lon!=null) {
            val rise=Astronomy.localTime(today,lat,lon,true,zone)
            val set=Astronomy.localTime(today,lat,lon,false,zone)
            Text("☀️ Amanecer: ${rise?.format(formatter) ?: "Sin amanecer hoy"}",style=MaterialTheme.typography.titleLarge)
            Text("🌇 Atardecer: ${set?.format(formatter) ?: "Sin atardecer hoy"}",style=MaterialTheme.typography.titleLarge)
            if(rise!=null&&set!=null) {
                val goldenStart=rise.plusMinutes(60)
                val goldenEnd=set.minusMinutes(60)
                Text("Hora dorada orientativa: ${rise.format(formatter)}–${goldenStart.format(formatter)} y ${goldenEnd.format(formatter)}–${set.format(formatter)}")
            }
        } else Text("Escribe coordenadas válidas.",color=MaterialTheme.colorScheme.error)
        androidx.compose.material3.HorizontalDivider()
        Text("🌙 ${Astronomy.phaseName(phase)}",style=MaterialTheme.typography.titleLarge)
        Text("Iluminación estimada: ${(Astronomy.illuminatedFraction(phase)*100).roundToInt()}%")
        Text("Sin conexión y sin acceder a tu ubicación. Resultados aproximados (no válidos para navegación náutica, astronomía de precisión ni situaciones de seguridad).",
            color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
    }
}
