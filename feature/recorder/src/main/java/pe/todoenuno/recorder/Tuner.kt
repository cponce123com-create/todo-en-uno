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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlin.math.*

data class PitchResult(val hz:Double,val note:String,val cents:Int)

object PitchMath {
    private val names=listOf("Do","Do♯","Re","Re♯","Mi","Fa","Fa♯","Sol","Sol♯","La","La♯","Si")
    fun note(hz:Double):PitchResult? {
        if(!hz.isFinite() || hz<50.0 || hz>2000.0)return null
        val midi=69.0+12.0*log2(hz/440.0)
        val nearest=midi.roundToInt()
        val cents=((midi-nearest)*100.0).roundToInt()
        return PitchResult(hz,names[nearest.mod(12)]+(nearest/12-1),cents)
    }

    /**
     * Basic normalized autocorrelation; monophonic sustained notes only.
     * Not an FFT spectrum or a professional tuner. DC removal and silence gating.
     */
    fun detect(samples:ShortArray,rate:Int):Double? {
        if(samples.size<2048)return null
        val n=samples.size
        val avg=samples.sumOf{it.toDouble()}/n
        val data=DoubleArray(n){(samples[it]-avg)/32768.0}
        val power=data.sumOf{it*it}/n
        if(power<0.00002)return null
        val minLag=(rate/1000).coerceAtLeast(1)
        val maxLag=(rate/65).coerceAtMost(n/3)
        var bestLag=0;var best=0.0
        for(lag in minLag..maxLag) {
            var cross=0.0;var s1=0.0;var s2=0.0
            var i=0
            while(i<n-lag){
                val a=data[i];val b=data[i+lag]
                cross+=a*b;s1+=a*a;s2+=b*b
                i+=2
            }
            val corr=cross/sqrt(s1*s2+1e-12)
            if(corr>best){best=corr;bestLag=lag}
        }
        if(best<0.76 || bestLag==0)return null
        return rate.toDouble()/bestLag
    }
}

@Composable
fun TunerScreen() {
    val context=LocalContext.current
    var allowed by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
    ) }
    var pitch by remember { mutableStateOf<PitchResult?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){allowed=it}
    LaunchedEffect(allowed){
        if(!allowed)return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val rate=16000
            val min=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
            if(min<=0){message="Micrófono no compatible";return@withContext}
            val recorder=AudioRecord(MediaRecorder.AudioSource.MIC,rate,
                AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,maxOf(min*2,8192))
            try {
                if(recorder.state!=AudioRecord.STATE_INITIALIZED)error("No disponible")
                recorder.startRecording()
                val frame=ShortArray(4096)
                while(currentCoroutineContext().isActive) {
                    var read=0
                    while(read<frame.size && currentCoroutineContext().isActive) {
                        val count=recorder.read(frame,read,frame.size-read)
                        if(count<=0)break
                        read+=count
                    }
                    if(read==frame.size) pitch=PitchMath.detect(frame,rate)?.let{PitchMath.note(it)}
                }
            } catch(_:Exception){message="No se puede leer el micrófono."}
            finally {runCatching{recorder.stop()};recorder.release()}
        }
    }
    Column(Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
        Text("Afinador",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        if(!allowed) Button(onClick={request.launch(Manifest.permission.RECORD_AUDIO)}){
            Text("Autorizar micrófono")
        } else {
            Text(pitch?.note ?: "Toca una nota sostenida",
                style=MaterialTheme.typography.headlineLarge,modifier=Modifier.padding(top=25.dp))
            pitch?.let {
                Text("${it.hz.toInt()} Hz",style=MaterialTheme.typography.titleLarge)
                Text("Desviación: ${it.cents} cents")
            }
        }
        message?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Text("Afinador aproximado para notas individuales. El ruido, los armónicos y el micrófono afectan la precisión.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(top=25.dp))
    }
}
