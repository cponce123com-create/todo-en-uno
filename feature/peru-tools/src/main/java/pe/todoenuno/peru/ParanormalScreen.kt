package pe.todoenuno.peru

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun ParanormalScreen() {
    val context=LocalContext.current
    val manager=remember(context){context.getSystemService(Context.SENSOR_SERVICE) as SensorManager}
    val sensor=remember(manager){manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)}
    var field by remember { mutableStateOf<Float?>(null) }
    var boardLetter by remember { mutableStateOf("INICIO") }
    var boardHistory by remember { mutableStateOf("") }
    var lamp by remember { mutableStateOf("APAGADA") }
    var light by remember { mutableIntStateOf(0) }
    var spiritText by remember { mutableStateOf("Silencio") }
    DisposableEffect(manager,sensor) {
        val listener=object:SensorEventListener {
            override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int) {}
            override fun onSensorChanged(event:SensorEvent) {
                if(event.values.size>=3) {
                    val x=event.values[0];val y=event.values[1];val z=event.values[2]
                    field=sqrt(x*x+y*y+z*z)
                }
            }
        }
        if(sensor!=null) manager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { manager.unregisterListener(listener) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Laboratorio paranormal",style=MaterialTheme.typography.headlineSmall)
        Text("ENTRETENIMIENTO: estas funciones no detectan espíritus ni fenómenos sobrenaturales.",color=MaterialTheme.colorScheme.error)
        Text("Campo magnético físico: "+(field?.let { "%.1f µT".format(it) } ?: "Sensor no disponible"))
        Text("La lectura es del magnetómetro del móvil y puede variar por metales o aparatos eléctricos.")
        HorizontalDivider()
        Text("REM-POD virtual",style=MaterialTheme.typography.titleMedium)
        Text("Indicador luminoso simulado: nivel $light de 4")
        Button(onClick={light=Random.nextInt(0,5)}){Text("Generar respuesta luminosa simulada")}
        HorizontalDivider()
        Text("Spirit box simulada",style=MaterialTheme.typography.titleMedium)
        Text(spiritText)
        Button(onClick={
            spiritText=listOf("…ruido blanco…","¿Hola?","…interferencia…","Aquí","…silencio…","Tal vez").random()
        }){Text("Simular barrido de voces")}
        Text("Las frases se generan aleatoriamente; no provienen de una comunicación real.",style=MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text("Tablero Ouija virtual",style=MaterialTheme.typography.titleMedium)
        Text("Juego recreativo: selecciona letras o genera movimientos aleatorios. No detecta espíritus.")
        Text("Indicador: $boardLetter",style=MaterialTheme.typography.titleLarge)
        listOf("ABCDEFGHI","JKLMNOPQR","STUVWXYZ").forEach { letters ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                letters.forEach { letter ->
                    Box(
                        Modifier.weight(1f).height(42.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(6.dp))
                            .clickable { boardLetter=letter.toString();boardHistory=(boardHistory+letter).takeLast(100) },
                        contentAlignment=Alignment.Center
                    ) { Text(letter.toString(),style=MaterialTheme.typography.labelSmall) }
                }
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={boardLetter="SÍ";boardHistory+=" SÍ"}) { Text("SÍ") }
            Button(onClick={boardLetter="NO";boardHistory+=" NO"}) { Text("NO") }
            Button(onClick={boardLetter="ADIÓS";boardHistory+=" ADIÓS"}) { Text("ADIÓS") }
        }
        Button(onClick={
            val letter=('A'..'Z').random()
            boardLetter=letter.toString()
            boardHistory=(boardHistory+letter).takeLast(100)
        }) { Text("Mover indicador al azar") }
        Text("Secuencia: "+boardHistory.ifBlank { "Sin movimientos" })
        Button(onClick={boardHistory="";boardLetter="INICIO"}) { Text("Reiniciar tablero") }
        HorizontalDivider()
        Text("Luz de pantalla",style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={lamp="ROJA"}) { Text("Roja") }
            Button(onClick={lamp="AZUL"}) { Text("Azul") }
            Button(onClick={lamp="APAGADA"}) { Text("Apagar") }
        }
        if(lamp!="APAGADA") Box(
            Modifier.fillMaxWidth().height(110.dp).background(if(lamp=="ROJA")Color.Red else Color.Blue),
            contentAlignment=Alignment.Center
        ) { Text("Luz $lamp",color=Color.White) }
        Text("Las luces son estáticas, sin destellos.",style=MaterialTheme.typography.bodySmall)
    }
}
