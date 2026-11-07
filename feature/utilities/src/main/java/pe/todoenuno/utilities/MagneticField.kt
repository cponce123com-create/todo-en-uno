package pe.todoenuno.utilities

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@Composable
fun MagneticFieldScreen() {
    val context=LocalContext.current
    val manager=remember(context){context.getSystemService(Context.SENSOR_SERVICE) as SensorManager}
    val sensor=remember(manager){manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)}
    var strength by remember{mutableFloatStateOf(0f)}
    DisposableEffect(sensor,manager) {
        val listener=object:SensorEventListener{
            override fun onSensorChanged(event:SensorEvent){
                val x=event.values[0];val y=event.values[1];val z=event.values[2]
                strength=sqrt(x*x+y*y+z*z)
            }
            override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
        }
        if(sensor!=null)manager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_UI)
        onDispose{manager.unregisterListener(listener)}
    }
    Column(Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
        Text("Campo magnético",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        if(sensor==null) Text("Este equipo no cuenta con magnetómetro.")
        else {
            Text("${strength.toInt()} µT",style=MaterialTheme.typography.displayLarge,modifier=Modifier.padding(26.dp))
            LinearProgressIndicator(progress={(strength/200f).coerceIn(0f,1f)})
            Text("Aprox. 25–65 µT en muchos lugares de la Tierra (variable por región).",
                modifier=Modifier.padding(top=20.dp))
        }
        Text("Detecta cambios en el campo magnético, no identifica metales ni garantiza encontrar objetos. Sensibilidad variable.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(top=24.dp))
    }
}
