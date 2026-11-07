package pe.todoenuno.peru

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
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
    var simulation by remember { mutableStateOf(false) }
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
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
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
    }
}
