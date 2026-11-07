package pe.todoenuno.utilities

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FlashlightScreen() {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val flashId = remember(manager) {
        runCatching {
            manager.cameraIdList.firstOrNull { cameraId ->
                manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }
    var enabled by remember { mutableStateOf(false) }
    var owned by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(manager, flashId) {
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, isEnabled: Boolean) {
                if (cameraId == flashId) enabled = isEnabled
            }
            override fun onTorchModeUnavailable(cameraId: String) {
                if (cameraId == flashId) enabled = false
            }
        }
        if (flashId != null) manager.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
        onDispose {
            if (owned && flashId != null) runCatching { manager.setTorchMode(flashId, false) }
            if (flashId != null) manager.unregisterTorchCallback(callback)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (enabled) "🔆" else "🔦", fontSize = 90.sp)
        Text("Linterna", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(if (flashId == null) "Este dispositivo no tiene flash compatible" else if (enabled) "Encendida" else "Apagada",
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        Button(
            enabled = flashId != null,
            onClick = {
                if (flashId != null) {
                    val next = !enabled
                    runCatching { manager.setTorchMode(flashId, next) }
                        .onSuccess { owned = next; enabled = next; error = null }
                        .onFailure { error = "No se pudo utilizar el flash. Puede estar ocupado por otra aplicación." }
                }
            }
        ) { Text(if (enabled) "Apagar linterna" else "Encender linterna") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 18.dp)) }
        Text("La luz se apaga al salir de la herramienta.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 26.dp))
    }
}
