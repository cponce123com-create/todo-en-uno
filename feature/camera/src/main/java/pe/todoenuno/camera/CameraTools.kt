package pe.todoenuno.camera

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

/**
 * Cámara local: espejo frontal, lupa trasera y lector QR sin conexión.
 * Nunca abre URLs automáticamente después de escanearlas.
 */
@Composable
fun CameraToolScreen(mode:String) {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current
    var allowed by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
    ) }
    var notice by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){allowed=it}
    val previewView=remember(context,mode) {
        PreviewView(context).apply {
            implementationMode=PreviewView.ImplementationMode.COMPATIBLE
            scaleType=PreviewView.ScaleType.FILL_CENTER
        }
    }
    DisposableEffect(allowed,lifecycle,mode) {
        if(!allowed) return@DisposableEffect onDispose {}
        val future=ProcessCameraProvider.getInstance(context)
        val executor=Executors.newSingleThreadExecutor()
        var provider:ProcessCameraProvider?=null
        var disposed=false
        future.addListener({
            if(!disposed) runCatching {
                val ready=future.get()
                provider=ready
                val cameraSelector=if(mode=="mirror")CameraSelector.DEFAULT_FRONT_CAMERA
                    else CameraSelector.DEFAULT_BACK_CAMERA
                val preview=Preview.Builder().build().apply {
                    surfaceProvider=previewView.surfaceProvider
                }
                ready.unbindAll()
                if(mode=="qrscan") {
                    val analysis=ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    val scanner=MultiFormatReader()
                    var lastTime=0L
                    analysis.setAnalyzer(executor){ proxy ->
                        try {
                            if(result==null && SystemClock.elapsedRealtime()-lastTime>250L){
                                lastTime=SystemClock.elapsedRealtime()
                                val decoded=scanBarcode(proxy,scanner)
                                if(decoded!=null){
                                    ContextCompat.getMainExecutor(context).execute{result=decoded}
                                }
                            }
                        } catch (_:Exception) { }
                        finally { proxy.close() }
                    }
                    ready.bindToLifecycle(lifecycle,cameraSelector,preview,analysis)
                } else {
                    val camera=ready.bindToLifecycle(lifecycle,cameraSelector,preview)
                    if(mode=="magnifier") camera.cameraControl.setLinearZoom(0.5f)
                }
                notice=null
            }.onFailure { notice="No se pudo iniciar la cámara: ${it.localizedMessage}" }
        },ContextCompat.getMainExecutor(context))
        onDispose {
            disposed=true
            provider?.unbindAll()
            executor.shutdown()
        }
    }
    val title=when(mode){"mirror"->"Espejo";"magnifier"->"Lupa";else->"Escáner QR"}
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        if(!allowed) Button(onClick={request.launch(Manifest.permission.CAMERA)}) {
            Text("Autorizar cámara")
        }
        if(allowed) AndroidView(factory={previewView},
            modifier=Modifier.fillMaxWidth().weight(1f))
        notice?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        if(mode=="qrscan") {
            result?.let { text ->
                Text("Código detectado: $text",style=MaterialTheme.typography.bodyMedium)
                Button(onClick={
                    val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Código escaneado",text))
                }){Text("Copiar resultado")}
                Button(onClick={result=null}){Text("Escanear otro")}
            } ?: Text("Apunta al código QR. El análisis se realiza en tu dispositivo.")
        } else {
            Text(if(mode=="mirror")"Vista frontal en tiempo real."
                else "Vista trasera ampliada. La ampliación depende de la cámara.")
        }
    }
}

private fun scanBarcode(proxy:ImageProxy,reader:MultiFormatReader):String?{
    val w=proxy.width; val h=proxy.height
    val plane=proxy.planes.firstOrNull()?:return null
    val bytes=ByteArray(w*h)
    val buffer=plane.buffer
    val stride=plane.rowStride
    val pixelStride=plane.pixelStride
    for(y in 0 until h){
        for(x in 0 until w){
            val offset=y*stride+x*pixelStride
            if(offset<buffer.limit())bytes[y*w+x]=buffer.get(offset)
        }
    }
    val source=PlanarYUVLuminanceSource(bytes,w,h,0,0,w,h,false)
    return try {
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
    } catch (_:com.google.zxing.NotFoundException){null}
      finally { reader.reset() }
}
