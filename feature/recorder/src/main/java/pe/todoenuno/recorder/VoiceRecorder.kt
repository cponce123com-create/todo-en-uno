package pe.todoenuno.recorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class RecorderEngine(private val context: Context) {
    private var active: MediaRecorder? = null
    private var output: File? = null

    val isRecording get() = active != null

    fun start(): Result<Unit> = runCatching {
        check(active == null)
        val dir = File(context.filesDir,"recordings").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.ROOT).format(Date())
        val file = File(dir,"Grabacion_$timestamp.m4a")
        @Suppress("DEPRECATION")
        val recorder=if(Build.VERSION.SDK_INT>=31) MediaRecorder(context) else MediaRecorder()
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            output=file
            active=recorder
        } catch(e:Exception) {
            recorder.release()
            file.delete()
            throw e
        }
    }

    fun stop(): Result<File?> = runCatching {
        val recorder=active ?: return@runCatching null
        val saved=output
        active=null
        output=null
        try {
            recorder.stop()
            saved?.takeIf { it.exists() && it.length() > 1024L }
        } catch (e:RuntimeException) {
            saved?.delete()
            throw e
        } finally { recorder.release() }
    }
}

@Composable
fun VoiceRecorderScreen() {
    val context=LocalContext.current
    val engine=remember(context){RecorderEngine(context.applicationContext)}
    val folder=remember(context){File(context.filesDir,"recordings").apply { mkdirs() }}
    var recordings by remember { mutableStateOf(listRecordings(folder)) }
    var recording by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope=rememberCoroutineScope()
    var exporting by remember{mutableStateOf(false)}
    var toExport by remember { mutableStateOf<File?>(null) }
    var allowed by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
    ) }

    val requestMic=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ granted ->
        allowed=granted
        if(!granted) notice="No se concedió acceso al micrófono."
    }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4")){ uri ->
        val file=toExport
        if(uri!=null && file!=null) scope.launch {
            exporting=true
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { input -> input.copyTo(out) }
                    } ?: error("No se pudo crear el archivo")
                }
                notice="Grabación exportada correctamente."
            } catch(e:Exception) {
                if(e is kotlinx.coroutines.CancellationException)throw e
                notice="Error al exportar: ${e.localizedMessage}"
            } finally {exporting=false}
        }
        toExport=null
    }
    val latestPlayer by rememberUpdatedState(player)
    DisposableEffect(engine) {
        onDispose {
            if(engine.isRecording) engine.stop()
            latestPlayer?.release()
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Grabadora de voz",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text(if(recording) "🔴 Grabando…" else "Listo para grabar",
            style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
        if(!allowed) Button(onClick={requestMic.launch(Manifest.permission.RECORD_AUDIO)}){
            Text("Autorizar micrófono")
        }
        Button(enabled=allowed,onClick={
            if(recording) {
                engine.stop().onSuccess { saved ->
                    notice=if(saved!=null)"Grabación guardada." else "Grabación demasiado breve."
                }.onFailure { notice="Error al guardar: ${it.localizedMessage}" }
                recording=false
                recordings=listRecordings(folder)
            } else {
                player?.release();player=null;playing=null
                engine.start().onSuccess { recording=true;notice=null }
                    .onFailure { notice="No se pudo iniciar: ${it.localizedMessage}" }
            }
        }) { Text(if(recording)"Detener y guardar" else "Iniciar grabación") }
        Text("Grabación en primer plano: se guarda automáticamente al detener. Salir de esta pantalla detiene la captura.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
        notice?.let { Text(it,color=MaterialTheme.colorScheme.primary) }
        HorizontalDivider()
        Text("Tus grabaciones (${recordings.size})",
            style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        if(recordings.isEmpty()) Text("Todavía no hay audios guardados.")
        recordings.forEach { file ->
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(file.name,style=MaterialTheme.typography.bodyMedium)
                Text("${file.length()/1024} KB",color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick={
                        if(playing==file.path) {
                            player?.release();player=null;playing=null
                        } else {
                            player?.release()
                            val candidate=MediaPlayer()
                            player=candidate
                            playing=file.path
                            runCatching {
                                candidate.setDataSource(file.absolutePath)
                                candidate.setOnPreparedListener {
                                    if(player===it)it.start()
                                }
                                candidate.setOnCompletionListener {
                                    if(player===it){player=null;playing=null}
                                    it.release()
                                }
                                candidate.setOnErrorListener { failed,_,_ ->
                                    if(player===failed){player=null;playing=null}
                                    failed.release()
                                    notice="No se puede reproducir este archivo."
                                    true
                                }
                                candidate.prepareAsync()
                            }.onFailure {
                                candidate.release()
                                player=null;playing=null
                                notice="No se puede reproducir este archivo."
                            }
                        }
                    }) {Text(if(playing==file.path)"Detener" else "Escuchar")}
                    OutlinedButton(enabled=!exporting && toExport==null,onClick={toExport=file;export.launch(file.name)}) {Text("Exportar")}
                }
                HorizontalDivider()
            }
        }
    }
}

private fun listRecordings(folder: File): List<File> =
    folder.listFiles()?.filter { it.isFile && it.extension.equals("m4a",true) }
        ?.sortedByDescending { it.lastModified() } ?: emptyList()
