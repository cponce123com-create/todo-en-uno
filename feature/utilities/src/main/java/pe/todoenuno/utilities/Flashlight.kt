package pe.todoenuno.utilities

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FlashlightScreen() {
    val context=LocalContext.current
    val manager=remember(context){ context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val id=remember(manager) {
        runCatching { manager.cameraIdList.firstOrNull {
            manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)==true
        } }.getOrNull()
    }
    val maxStrength=remember(manager,id) {
        if(Build.VERSION.SDK_INT>=33 && id!=null) runCatching {
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        }.getOrDefault(1) else 1
    }.coerceAtLeast(1)
    var strength by remember { mutableIntStateOf(1) }
    var enabled by remember { mutableStateOf(false) }
    var sos by remember { mutableStateOf(false) }
    var owned by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun setTorch(on:Boolean){
        if(id==null) return
        runCatching {
            if(on && Build.VERSION.SDK_INT>=33 && maxStrength>1)
                manager.turnOnTorchWithStrengthLevel(id,strength.coerceIn(1,maxStrength))
            else manager.setTorchMode(id,on)
        }.onSuccess {enabled=on;owned=on;error=null}
            .onFailure {error="No se pudo controlar la linterna. Puede estar ocupada."}
    }
    DisposableEffect(manager,id) {
        val callback=object:CameraManager.TorchCallback(){
            override fun onTorchModeChanged(cameraId:String,isEnabled:Boolean){if(cameraId==id)enabled=isEnabled}
            override fun onTorchModeUnavailable(cameraId:String){if(cameraId==id)enabled=false}
        }
        if(id!=null) manager.registerTorchCallback(callback,Handler(Looper.getMainLooper()))
        onDispose {
            if(id!=null && owned) runCatching {manager.setTorchMode(id,false)}
            if(id!=null) manager.unregisterTorchCallback(callback)
        }
    }
    LaunchedEffect(sos,id) {
        if(!sos||id==null)return@LaunchedEffect
        // Morse SOS: ... --- ...; pausas deliberadas, no efecto estroboscópico rápido.
        try {
            while(true) {
                listOf(300L,300L,300L,900L,900L,900L,300L,300L,300L).forEachIndexed {i,unit ->
                    setTorch(true);delay(unit);setTorch(false)
                    delay(if(i==2||i==5)900L else 300L)
                }
                delay(1800L)
            }
        } finally {runCatching{manager.setTorchMode(id,false)}}
    }
    Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.Center) {
        Text(if(enabled)"🔆" else "🔦",fontSize=78.sp)
        Text("Linterna",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(if(id==null)"Este equipo no tiene flash compatible"
            else if(sos)"SOS intermitente activo" else if(enabled)"Encendida" else "Apagada",
            modifier=Modifier.padding(16.dp))
        Button(enabled=id!=null,onClick={
            sos=false
            setTorch(!enabled)
        }) {Text(if(enabled && !sos)"Apagar" else "Encender")}
        OutlinedButton(enabled=id!=null,onClick={
            if(sos) {
                sos=false
                setTorch(false)
            } else {
                setTorch(false)
                owned=true
                sos=true
            }
        },modifier=Modifier.padding(top=10.dp)) {Text(if(sos)"Detener SOS" else "Enviar SOS con luz")}
        if(maxStrength>1) {
            Text("Intensidad: $strength / $maxStrength",modifier=Modifier.padding(top=12.dp))
            Slider(value=strength.toFloat(),onValueChange={
                strength=it.toInt().coerceIn(1,maxStrength)
            },onValueChangeFinished={
                if(enabled&&!sos)setTorch(true)
            },valueRange=1f..maxStrength.toFloat(),steps=(maxStrength-2).coerceAtLeast(0))
        }
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Text("SOS es una señal visual repetitiva; puede afectar a personas fotosensibles. Se detiene al salir de esta pantalla. El brillo regulable depende del teléfono.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(top=20.dp))
    }
}
