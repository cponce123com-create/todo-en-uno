package pe.todoenuno.sensor

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    val manager=remember(context){context.getSystemService(Context.CAMERA_SERVICE) as CameraManager}
    val flashId=remember(manager){
        runCatching{manager.cameraIdList.firstOrNull{
            manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)==true
        }}.getOrNull()
    }
    val maxStrength=remember(manager,flashId){
        if(Build.VERSION.SDK_INT>=33 && flashId!=null)
            runCatching {manager.getCameraCharacteristics(flashId)
                .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1}.getOrDefault(1)
        else 1
    }.coerceAtLeast(1)
    var strength by remember{mutableFloatStateOf(1f)}
    var enabled by remember{mutableStateOf(false)}
    var sos by remember{mutableStateOf(false)}
    var owned by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    val latestOwned by rememberUpdatedState(owned)
    val latestSos by rememberUpdatedState(sos)
    fun switch(on:Boolean) {
        val id=flashId ?: return
        runCatching {
            if(on && Build.VERSION.SDK_INT>=33 && maxStrength>1)
                manager.turnOnTorchWithStrengthLevel(id,strength.toInt().coerceIn(1,maxStrength))
            else manager.setTorchMode(id,on)
        }.onSuccess { owned=on;enabled=on;error=null }
            .onFailure { enabled=false;error="No se puede controlar el flash. Puede estar ocupado." }
    }
    DisposableEffect(manager,flashId) {
        val callback=object:CameraManager.TorchCallback(){
            override fun onTorchModeChanged(cameraId:String,isEnabled:Boolean){
                if(cameraId==flashId)enabled=isEnabled
            }
            override fun onTorchModeUnavailable(cameraId:String){
                if(cameraId==flashId)enabled=false
            }
        }
        if(flashId!=null)manager.registerTorchCallback(callback,Handler(Looper.getMainLooper()))
        onDispose {
            if(flashId!=null){
                if(latestOwned || latestSos)runCatching{manager.setTorchMode(flashId,false)}
                manager.unregisterTorchCallback(callback)
            }
        }
    }
    LaunchedEffect(sos,flashId) {
        if(sos && flashId!=null) {
            try {
                while(true){
                    // Morse SOS: ... --- ... ; pauses between groups
                    for((group,duration) in listOf(3 to 180L,3 to 540L,3 to 180L)) {
                        repeat(group){
                            switch(true)
                            delay(duration)
                            switch(false)
                            delay(180L)
                        }
                        delay(450L)
                    }
                    delay(1100L)
                }
            } finally {
                runCatching{manager.setTorchMode(flashId,false)}
                owned=false;enabled=false
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        Text(if(enabled)"🔆" else "🔦",fontSize=86.sp)
        Text("Linterna",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(when{
            flashId==null->"Flash no disponible"
            sos->"SOS activo: ... --- ..."
            enabled->"Encendida"
            else->"Apagada"
        },modifier=Modifier.padding(vertical=20.dp))
        Button(enabled=flashId!=null && !sos,onClick={switch(!enabled)}){
            Text(if(enabled)"Apagar" else "Encender")
        }
        OutlinedButton(enabled=flashId!=null,onClick={
            if(!sos) {
                if(enabled)switch(false)
                sos=true
            }else sos=false
        },modifier=Modifier.padding(top=8.dp)){
            Text(if(sos)"Detener SOS" else "Activar señal SOS")
        }
        if(maxStrength>1 && Build.VERSION.SDK_INT>=33 && !sos){
            Text("Brillo del flash: ${strength.toInt()} / $maxStrength",
                modifier=Modifier.padding(top=20.dp))
            Slider(value=strength,onValueChange={strength=it.coerceIn(1f,maxStrength.toFloat())},
                valueRange=1f..maxStrength.toFloat(),steps=(maxStrength-2).coerceAtLeast(0),
                onValueChangeFinished={if(enabled)switch(true)})
        }
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Text("SOS visual, no envía mensajes ni llama a emergencias. El flash se apaga al salir de la pantalla. No dirigir el destello a los ojos.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(top=26.dp))
    }
}
