package pe.todoenuno.sensor

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import kotlin.random.Random

private enum class TorchMode(val title:String){
    NORMAL("Normal"),SOS("SOS"),STROBE("Estrobo"),CANDLE("Vela")
}

/** Flash físico opcional. Apaga la luz siempre al salir de la pantalla. */
@Composable
fun FlashlightScreen() {
    val context=LocalContext.current
    val manager=remember(context){context.getSystemService(Context.CAMERA_SERVICE) as CameraManager}
    val flashId=remember(manager) {
        runCatching{
            manager.cameraIdList.firstOrNull{
                manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)==true
            }
        }.getOrNull()
    }
    val maxStrength=remember(manager,flashId) {
        if(Build.VERSION.SDK_INT>=33 && flashId!=null)
            runCatching{
                manager.getCameraCharacteristics(flashId).get(
                    CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL
                )?:1
            }.getOrDefault(1)
        else 1
    }.coerceAtLeast(1)
    var strength by remember { mutableFloatStateOf(1f) }
    var enabled by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(TorchMode.NORMAL) }
    var owned by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val latestOwned by rememberUpdatedState(owned)
    val latestMode by rememberUpdatedState(mode)

    fun switch(on:Boolean,level:Int=strength.toInt()) {
        val id=flashId?:return
        runCatching {
            if(on && Build.VERSION.SDK_INT>=33 && maxStrength>1)
                manager.turnOnTorchWithStrengthLevel(id,level.coerceIn(1,maxStrength))
            else manager.setTorchMode(id,on)
        }.onSuccess {
            owned=on;enabled=on;error=null
        }.onFailure {
            error="El flash no está disponible o está ocupado por otra app."
            enabled=false
        }
    }

    DisposableEffect(manager,flashId) {
        val callback=object:CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId:String,isEnabled:Boolean) {
                if(cameraId==flashId)enabled=isEnabled
            }
            override fun onTorchModeUnavailable(cameraId:String) {
                if(cameraId==flashId)enabled=false
            }
        }
        if(flashId!=null)manager.registerTorchCallback(callback,Handler(Looper.getMainLooper()))
        onDispose {
            if(flashId!=null) {
                if(latestOwned || latestMode!=TorchMode.NORMAL)
                    runCatching{manager.setTorchMode(flashId,false)}
                manager.unregisterTorchCallback(callback)
            }
        }
    }
    LaunchedEffect(mode,flashId) {
        if(mode==TorchMode.NORMAL || flashId==null)return@LaunchedEffect
        try {
            when(mode) {
                TorchMode.SOS -> while(true) {
                    // Morse: S (...), O (---), S (...)
                    for((count,duration) in listOf(3 to 200L,3 to 600L,3 to 200L)) {
                        repeat(count) {
                            switch(true)
                            delay(duration)
                            switch(false)
                            delay(200L)
                        }
                        delay(450L)
                    }
                    delay(1300L)
                }
                TorchMode.STROBE -> while(true) {
                    switch(true);delay(250L)
                    switch(false);delay(250L)
                }
                TorchMode.CANDLE -> {
                    switch(true,level=1)
                    while(true){
                        if(Build.VERSION.SDK_INT>=33 && maxStrength>1)
                            switch(true,level=Random.nextInt(1,maxStrength+1).coerceAtMost(2))
                        delay(600L)
                    }
                }
                TorchMode.NORMAL -> Unit
            }
        } finally {
            runCatching{manager.setTorchMode(flashId,false)}
            owned=false
            enabled=false
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement=Arrangement.Center,
        horizontalAlignment=Alignment.CenterHorizontally) {
        Text("🔦",fontSize=90.sp)
        Text("Linterna avanzada",style=MaterialTheme.typography.headlineMedium,
            fontWeight=FontWeight.Bold)
        Text(
            if(flashId==null)"No hay flash compatible"
            else if(mode!=TorchMode.NORMAL)"Modo ${mode.title} activo"
            else if(enabled)"Encendida" else "Apagada",
            modifier=Modifier.padding(vertical=20.dp)
        )
        Button(
            enabled=flashId!=null && mode==TorchMode.NORMAL,
            onClick={switch(!enabled)}
        ) {Text(if(enabled)"Apagar" else "Encender")}
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TorchMode.entries.forEach { option ->
                FilterChip(
                    enabled=flashId!=null,
                    selected=mode==option,
                    onClick={
                        if(option==TorchMode.NORMAL) {
                            mode=TorchMode.NORMAL
                            if(enabled)switch(false)
                        }else {
                            if(enabled)switch(false)
                            mode=if(mode==option)TorchMode.NORMAL else option
                        }
                    },
                    label={Text(option.title)}
                )
            }
        }
        if(Build.VERSION.SDK_INT>=33 && maxStrength>1 && mode==TorchMode.NORMAL) {
            Text("Brillo del flash: ${strength.toInt()} / $maxStrength")
            Slider(
                value=strength,
                onValueChange={strength=it.coerceIn(1f,maxStrength.toFloat())},
                valueRange=1f..maxStrength.toFloat(),
                steps=(maxStrength-2).coerceAtLeast(0),
                onValueChangeFinished={if(enabled)switch(true)}
            )
        }
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Text(
            "SOS es una señal luminosa, no una llamada de emergencia. El estrobo parpadea a 2 Hz " +
                "y puede resultar molesto para personas fotosensibles. No apuntar a ojos ni utilizar al conducir.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(top=18.dp)
        )
    }
}
