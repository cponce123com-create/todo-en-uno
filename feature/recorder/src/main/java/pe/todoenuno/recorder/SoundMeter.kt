package pe.todoenuno.recorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

object SoundMath {
    /** dBFS relativo a la amplitud máxima PCM16, no dB SPL calibrados. */
    fun dbfs(samples:ShortArray,count:Int):Double {
        if(count<=0) return -90.0
        var sum=0.0
        for(i in 0 until count) {
            val v=samples[i].toDouble()/32768.0
            sum += v*v
        }
        val rms=sqrt(sum/count)
        return (20*log10(rms.coerceAtLeast(0.00001))).coerceIn(-90.0,0.0)
    }
}

@Composable
fun SoundMeterScreen() {
    val context=LocalContext.current
    var granted by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
    ) }
    var db by remember { mutableDoubleStateOf(-90.0) }
    var message by remember { mutableStateOf<String?>(null) }
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ granted=it }
    LaunchedEffect(granted) {
        if(!granted)return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val rate=16000
            val min=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT)
            if(min<=0) {
                message="No se puede acceder al micrófono."
                return@withContext
            }
            val record=AudioRecord(MediaRecorder.AudioSource.MIC,rate,
                AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,min*2)
            try {
                if(record.state!=AudioRecord.STATE_INITIALIZED)error("Micrófono no disponible")
                record.startRecording()
                val samples=ShortArray(min)
                while(currentCoroutineContext().isActive) {
                    val read=record.read(samples,0,samples.size)
                    if(read>0) db=SoundMath.dbfs(samples,read)
                    else break
                }
            } catch (e:Exception) {
                message="No se puede medir. Comprueba los permisos o cierra otras grabadoras."
            } finally {
                runCatching { record.stop() }
                record.release()
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(22.dp),verticalArrangement=Arrangement.Center,
        horizontalAlignment=Alignment.CenterHorizontally) {
        Text("Medidor de sonido",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        if(!granted) Button(onClick={request.launch(Manifest.permission.RECORD_AUDIO)}){
            Text("Autorizar micrófono")
        } else {
            Text("${db.toInt()} dBFS",style=MaterialTheme.typography.displayMedium,
                modifier=Modifier.padding(vertical=25.dp))
            LinearProgressIndicator(progress={((db+90)/90).toFloat().coerceIn(0f,1f)})
        }
        Text("Escala digital relativa (dBFS). No mide decibelios de presión sonora calibrados ni sirve para evaluar exposición profesional al ruido.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(top=25.dp))
        message?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }
}
