package pe.todoenuno.utilities

import android.content.ClipData
import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

object TextTransformer {
    fun removeDuplicateSpaces(value: String) = value.replace(Regex("[ \\t]+"), " ").trim()
    fun words(value: String): Int = value.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    fun upper(value: String) = value.uppercase(Locale.forLanguageTag("es-PE"))
    fun lower(value: String) = value.lowercase(Locale.forLanguageTag("es-PE"))
}

@Composable
fun TextToolsScreen() {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Herramientas de texto", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        OutlinedTextField(value = text, onValueChange = { text = it.take(30000) },
            modifier = Modifier.fillMaxWidth().height(190.dp), label = { Text("Escribe o pega texto") })
        Text("Caracteres: ${text.length} · Palabras: ${TextTransformer.words(text)} · Líneas: ${text.lines().size}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { text = TextTransformer.upper(text) }) { Text("MAYÚSCULAS") }
            OutlinedButton(onClick = { text = TextTransformer.lower(text) }) { Text("minúsculas") }
        }
        OutlinedButton(onClick = { text = TextTransformer.removeDuplicateSpaces(text) }) {
            Text("Quitar espacios repetidos")
        }
        Button(onClick = {
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("Texto", text))
        }, enabled = text.isNotEmpty()) { Text("Copiar texto") }
        Text("Todo se procesa en el dispositivo, sin enviar el texto a ningún servidor.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

object ColorCodec {
    fun hex(r: Int, g: Int, b: Int): String =
        "#%02X%02X%02X".format(Locale.ROOT, r.coerceIn(0,255), g.coerceIn(0,255), b.coerceIn(0,255))
    fun parse(s: String): Triple<Int,Int,Int>? {
        val valid = Regex("^#?[0-9a-fA-F]{6}$")
        if (!valid.matches(s.trim())) return null
        val value = s.trim().removePrefix("#").toLong(16).toInt()
        return Triple((value shr 16) and 255, (value shr 8) and 255, value and 255)
    }
}

@Composable
fun ColorToolsScreen() {
    val context = LocalContext.current
    var red by rememberSaveable { mutableIntStateOf(34) }
    var green by rememberSaveable { mutableIntStateOf(163) }
    var blue by rememberSaveable { mutableIntStateOf(151) }
    var entered by rememberSaveable { mutableStateOf("#22A397") }
    val color = Color(red, green, blue)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Colores", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth().height(130.dp).background(color, RoundedCornerShape(24.dp))
        )
        listOf(Triple("Rojo",red,0),Triple("Verde",green,1),Triple("Azul",blue,2)).forEach { item ->
            Text("${item.first}: ${item.second}")
            Slider(value = item.second.toFloat(), onValueChange = {
                when(item.third){0 -> red=it.toInt();1 -> green=it.toInt();else -> blue=it.toInt()}
            }, valueRange=0f..255f)
        }
        val hex = ColorCodec.hex(red,green,blue)
        Text("HEX: $hex  •  RGB: ($red, $green, $blue)",
            style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = entered, onValueChange = { entered = it.take(7) },
            label={Text("Escribe un HEX de 6 dígitos")}, singleLine=true)
        OutlinedButton(onClick = { ColorCodec.parse(entered)?.let {
            red = it.first; green = it.second; blue = it.third
        }}, enabled = ColorCodec.parse(entered)!=null) { Text("Aplicar HEX") }
        Button(onClick = {
            val manager=context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("Color HEX",hex))
        }) { Text("Copiar $hex") }
        Text("Selector RGB sin conexión. El cuentagotas de cámara llegará en otra versión.",
            style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
