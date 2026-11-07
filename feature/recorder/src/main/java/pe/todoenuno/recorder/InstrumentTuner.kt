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

/** Detector básico por autocorrelación, frecuencia fundamental aproximada. */
object PitchDetector {
    fun frequency(samples:ShortArray, sampleRate:Int):Double? {
        if(samples.size<2048 || sampleRate<=0)return null
        var rms=0.0
        for(v in samples)rms+=v.toDouble()*v.toDouble()
        rms=sqrt(rms/samples.size)/32768.0
        if(rms<0.008)return null
        val minLag=(sampleRate/1000.0).toInt().coerceAtLeast(12)
        val maxLag=(sampleRate/75.0).toInt().coerceAtMost(samples.size/3)
        var bestLag=0;var bestScore=0.0
        var previous=0.0
        for(lag in minLag..maxLag) {
            var correlation=0.0
            var energyA=0.0;var energyB=0.0
            // Saltar muestras ahorra CPU y conserva precisión para afinación básica.
            var i=0
            while(i<samples.size-maxLag) {
                val a=samples[i].toDouble()
                val b=samples[i+lag].toDouble()
                correlation+=a*b
                energyA+=a*a
                energyB+=b*b
                i+=2
            }
            val score=correlation/sqrt((energyA*energyB).coerceAtLeast(1.0))
            if(score>bestScore){bestScore=score;bestLag=lag}
        }
        if(bestScore<0.82 || bestLag<=0)return null
        val f=sampleRate.toDouble()/bestLag
        return f.takeIf { it in 75.0..1000.0 }
    }
    fun note(frequency:Double):Pair<String,Int>? {
        if(!frequency.isFinite()||frequency<=0.0)return null
        val midi=69.0+12.0*log2(frequency/440.0)
        val nearest=midi.roundToInt()
        val names=listOf("Do","Do♯","Re","Re♯","Mi","Fa","Fa♯","Sol","Sol♯","La","La♯","Si")
        val name=names[((nearest%12)+12)%12]+(nearest/12-1)
        return name to ((midi-nearest)*100.0).roundToInt()
    }
}

@Composable
fun InstrumentTunerScreen() {
    val context=LocalContext.current
    var permission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
    ) }
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){permission=it}
    var frequency by remember { mutableStateOf<Double?>(null) }
    var error by remember {mutableStateOf<String?>(null)}
    LaunchedEffect(permission) {
        if(!permission)return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val rate=22050
            val buf=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
            if(buf<=0){error="Micrófono no disponible.";return@withContext}
            val size=4096
            val record=try {
                AudioRecord(MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,maxOf(buf*2,size*2))
            }catch(e:SecurityException){error="Acceso al micrófono denegado.";return@withContext}
            try {
                if(record.state!=AudioRecord.STATE_INITIALIZED)error("Error de micrófono")
                record.startRecording()
                val samples=ShortArray(size)
                while(currentCoroutineContext().isActive) {
                    var offset=0
                    while(offset<size && currentCoroutineContext().isActive) {
                        val n=record.read(samples,offset,size-offset)
                        if(n<=0)break
                        offset+=n
                    }
                    if(offset==size)frequency=PitchDetector.frequency(samples,rate)
                }
            } catch(e:Exception) {
                error="No se pudo analizar el audio. Cierra otras apps que usen el micrófono."
            } finally {
                runCatching{record.stop()}
                record.release()
            }
        }
    }
    val label=frequency?.let{PitchDetector.note(it)}
    Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.Center) {
        Text("Afinador cromático",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        if(!permission)Button(onClick={request.launch(Manifest.permission.RECORD_AUDIO)}){
            Text("Autorizar micrófono")
        } else {
            Text(label?.first ?: "Escuchando…",style=MaterialTheme.typography.displayLarge,
                modifier=Modifier.padding(vertical=23.dp))
            Text(frequency?.let{"%.1f Hz".format(java.util.Locale.ROOT,it)} ?: "Toca una nota sostenida",
                style=MaterialTheme.typography.titleLarge)
            label?.second?.let{ cents ->
                Text("${if(cents>0)"+" else ""}$cents cents",
                    color=if(abs(cents)<=5)MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.tertiary,
                    style=MaterialTheme.typography.headlineSmall)
            }
        }
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Text("Afinador aproximado para sonidos monofónicos de 75 a 1000 Hz. A4 = 440 Hz. El ruido, armónicos y micrófono pueden alterar los resultados.",
            modifier=Modifier.padding(top=24.dp),style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
