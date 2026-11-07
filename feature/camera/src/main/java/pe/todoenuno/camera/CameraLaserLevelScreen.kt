package pe.todoenuno.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.tan

/**
 * Nivel tipo láser VISUAL: dibuja líneas sobre la cámara; NO proyecta luz real.
 * La inclinación es orientativa y debe calibrarse en cada dispositivo.
 */
@Composable
fun CameraLaserLevelScreen() {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current
    val sensors=remember(context){
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val sensor=remember(sensors) {
        sensors.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }
    var roll by remember { mutableFloatStateOf(0f) }
    var reference by remember { mutableFloatStateOf(0f) }
    var granted by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
    ) }
    var error by remember { mutableStateOf<String?>(null) }
    val launch=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){
        granted=it
    }
    val previewView=remember(context){
        PreviewView(context).apply {
            implementationMode=PreviewView.ImplementationMode.COMPATIBLE
            scaleType=PreviewView.ScaleType.FILL_CENTER
        }
    }
    DisposableEffect(sensors,sensor) {
        val listener=object:SensorEventListener {
            override fun onSensorChanged(e:SensorEvent) {
                val x=e.values[0].toDouble()
                val y=e.values[1].toDouble()
                if(x.isFinite() && y.isFinite()) {
                    val degrees=Math.toDegrees(atan2(x,y)).toFloat()
                    roll=roll*0.82f+degrees*0.18f
                }
            }
            override fun onAccuracyChanged(s:Sensor?,accuracy:Int){}
        }
        if(sensor!=null)sensors.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_UI)
        onDispose{sensors.unregisterListener(listener)}
    }
    DisposableEffect(granted,lifecycle,previewView) {
        if(!granted)return@DisposableEffect onDispose {}
        val future=ProcessCameraProvider.getInstance(context)
        var cameraProvider:ProcessCameraProvider?=null
        var disposed=false
        future.addListener({
            if(!disposed) {
                runCatching {
                    val provider=future.get()
                    cameraProvider=provider
                    val preview=Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycle,CameraSelector.DEFAULT_BACK_CAMERA,preview)
                }.onFailure{ error="No fue posible iniciar la cámara." }
            }
        },ContextCompat.getMainExecutor(context))
        onDispose {
            disposed=true
            cameraProvider?.unbindAll()
        }
    }
    val adjusted=roll-reference
    val aligned=abs(adjusted)<1.5f
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Nivel láser sobre cámara",style=MaterialTheme.typography.titleLarge)
        Text(
            "Inclinación: ${"%.1f".format(java.util.Locale.ROOT,adjusted)}° · " +
                if(aligned)"Alineado" else "Ajustar orientación",
            color=if(aligned)MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
        )
        if(!granted) {
            Button(onClick={launch.launch(Manifest.permission.CAMERA)}){
                Text("Autorizar cámara")
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if(granted) androidx.compose.ui.viewinterop.AndroidView(
                factory={previewView},modifier=Modifier.fillMaxSize()
            )
            Canvas(Modifier.fillMaxSize()){
                val color=if(aligned)Color(0xFF72FF89) else Color(0xFFFFC64D)
                val middle=size.height/2
                val tilt=tan(Math.toRadians(adjusted.coerceIn(-60f,60f).toDouble())).toFloat()
                val raise=(size.width*tilt/2).coerceIn(-size.height/3,size.height/3)
                drawLine(Color.Black.copy(alpha=0.65f),
                    Offset(0f,middle+raise),Offset(size.width,middle-raise),strokeWidth=10f)
                drawLine(color,Offset(0f,middle+raise),
                    Offset(size.width,middle-raise),strokeWidth=4f)
                drawLine(color.copy(alpha=0.75f),
                    Offset(size.width/2,0f),Offset(size.width/2,size.height),strokeWidth=2f)
                drawCircle(color,radius=15f,center=center)
                drawCircle(Color.Black,radius=6f,center=center)
            }
        }
        Button(onClick={reference=roll},enabled=sensor!=null){
            Text("Calibrar tomando la posición actual como cero")
        }
        if(sensor==null)Text("No hay sensor de gravedad/acelerómetro disponible.",
            color=MaterialTheme.colorScheme.error)
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Text("La retícula se superpone a la imagen; no emite un láser físico ni mide distancias. Verifica con un nivel calibrado.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
