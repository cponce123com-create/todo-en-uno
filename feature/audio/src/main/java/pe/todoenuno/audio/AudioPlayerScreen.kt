package pe.todoenuno.audio

import android.content.ComponentName
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AudioPlayerScreen() {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("Selecciona un audio") }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableStateOf(1f) }

    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, AudioPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        var active = true
        future.addListener({
            runCatching { future.get() }
                .onSuccess { c ->
                    if (active) {
                        controller = c
                        playing = c.isPlaying
                        title = c.mediaMetadata.title?.toString() ?: "Selecciona un audio"
                    }
                }
                .onFailure { if (active) message = "No se pudo conectar al reproductor." }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            active = false
            MediaController.releaseFuture(future)
        }
    }
    DisposableEffect(controller) {
        val c = controller
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                playing = player.isPlaying
                title = player.mediaMetadata.title?.toString() ?: "Selecciona un audio"
                speed = player.playbackParameters.speed
            }
        }
        c?.addListener(listener)
        onDispose { c?.removeListener(listener) }
    }
    LaunchedEffect(controller) {
        while (true) {
            controller?.let { c ->
                position = c.currentPosition.coerceAtLeast(0L)
                duration = c.duration.takeIf { it > 0 } ?: 0L
            }
            delay(500L)
        }
    }

    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull() ?: "Audio local"
            runCatching {
                val item = MediaItem.Builder().setUri(uri)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(name).build()).build()
                controller?.run {
                    setMediaItem(item)
                    prepare()
                    play()
                }
                title = name
                message = null
            }.onFailure { message = "No se pudo abrir el archivo seleccionado." }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("🎧", style = MaterialTheme.typography.displayLarge)
        Text("Reproductor local", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Button(enabled = controller != null, onClick = { pickAudio.launch(arrayOf("audio/*")) }) {
            Text("Elegir audio del teléfono")
        }
        Text("${timeText(position)} / ${timeText(duration)}")
        Slider(
            enabled = duration > 0 && controller != null,
            value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
            onValueChange = { fraction ->
                position = (fraction * duration).toLong()
                controller?.seekTo(position)
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(enabled = controller != null, onClick = { controller?.seekBack() }) { Text("−10 s") }
            Button(enabled = controller != null && controller?.mediaItemCount != 0, onClick = {
                controller?.let { if (it.isPlaying) it.pause() else it.play() }
            }) { Text(if (playing) "Pausar" else "Reproducir") }
            OutlinedButton(enabled = controller != null, onClick = { controller?.seekForward() }) { Text("+10 s") }
        }
        Text("Velocidad: ${speed}×", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1f, 1.25f, 1.5f).forEach { rate ->
                OutlinedButton(
                    enabled = controller != null,
                    onClick = { controller?.setPlaybackSpeed(rate); speed = rate }
                ) { Text("${rate}×") }
            }
        }
        Text(
            "Sigue reproduciendo con la pantalla bloqueada. Controla pausa y reproducción desde la notificación del sistema.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private fun timeText(value: Long): String {
    val s = (value / 1000L).coerceAtLeast(0L)
    return String.format(Locale.ROOT, "%02d:%02d", s / 60L, s % 60L)
}
