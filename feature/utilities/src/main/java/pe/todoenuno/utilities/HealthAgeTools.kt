package pe.todoenuno.utilities

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
import java.time.LocalDate
import java.time.Period
import kotlin.math.pow

object BodyMath {
    fun bmi(kilos:Double,cm:Double): Double? {
        if(!kilos.isFinite() || !cm.isFinite() || kilos<=0 || cm<=0) return null
        return kilos/(cm/100.0).pow(2.0)
    }
    fun age(birthday:LocalDate,today:LocalDate=LocalDate.now()):Period? =
        if(birthday.isAfter(today)) null else Period.between(birthday,today)
}

@Composable
fun BmiScreen(){
    var kg by rememberSaveable{mutableStateOf("70")}
    var cm by rememberSaveable{mutableStateOf("170")}
    val result=BodyMath.bmi(kg.replace(',','.').toDoubleOrNull()?:Double.NaN,
        cm.replace(',','.').toDoubleOrNull()?:Double.NaN)
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(15.dp)){
        Text("Índice de masa corporal",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        OutlinedTextField(value=kg,onValueChange={kg=it.take(12)},label={Text("Peso en kg")},
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
        OutlinedTextField(value=cm,onValueChange={cm=it.take(12)},label={Text("Estatura en cm")},
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
        Text(result?.let { "IMC: %.1f".format(java.util.Locale.ROOT,it) } ?: "Revisa los datos",
            color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.headlineMedium)
        Text("Cálculo informativo para personas adultas. El IMC no diagnostica una condición de salud y no sustituye evaluación profesional.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AgeScreen(){
    var day by rememberSaveable{mutableStateOf("1")}
    var month by rememberSaveable{mutableStateOf("1")}
    var year by rememberSaveable{mutableStateOf("2000")}
    val born=runCatching{LocalDate.of(year.toInt(),month.toInt(),day.toInt())}.getOrNull()
    val age=born?.let{BodyMath.age(it)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Calculadora de edad",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        @Composable fun dateField(label:String,value:String,onChange:(String)->Unit){
            OutlinedTextField(value=value,onValueChange={onChange(it.filter(Char::isDigit).take(4))},
                label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth(),
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
        }
        dateField("Día",day){day=it}
        dateField("Mes",month){month=it}
        dateField("Año",year){year=it}
        Text(age?.let{"${it.years} años, ${it.months} meses y ${it.days} días"}?:"Fecha no válida o futura",
            style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
        Text("Cálculo local con calendario gregoriano del teléfono.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
