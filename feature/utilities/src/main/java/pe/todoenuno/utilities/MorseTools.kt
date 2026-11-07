package pe.todoenuno.utilities

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

object MorseCodec {
    private val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val morse = listOf(
        ".-","-...","-.-.","-..",".","..-.","--.","....","..",".---",
        "-.-",".-..","--","-.","---",".--.","--.-",".-.","...","-",
        "..-","...-",".--","-..-","-.--","--..",
        "-----",".----","..---","...--","....-",".....","-....","--...","---..","----."
    )
    private val enc = chars.toList().zip(morse).toMap()
    private val dec = morse.zip(chars.toList()).toMap()
    fun encode(input: String): String = input.uppercase(Locale.ROOT).split(" ").joinToString(" / ") { word ->
        word.mapNotNull { enc[it] }.joinToString(" ")
    }
    fun decode(input: String): String = input.trim().split(Regex("\\s*/\\s*")).joinToString(" ") { word ->
        word.split(Regex("\\s+")).mapNotNull { dec[it] }.joinToString("")
    }
}

@Composable
fun MorseToolsScreen() {
    val context=LocalContext.current
    var text by rememberSaveable { mutableStateOf("HOLA") }
    var morse by rememberSaveable { mutableStateOf(MorseCodec.encode("HOLA")) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Código Morse",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        OutlinedTextField(value=text,onValueChange={text=it.take(500)},
            modifier=Modifier.fillMaxWidth(),label={Text("Texto")},minLines=2)
        Button(onClick={morse=MorseCodec.encode(text)}){Text("Texto a Morse")}
        OutlinedTextField(value=morse,onValueChange={morse=it.take(3000)},
            modifier=Modifier.fillMaxWidth(),label={Text("Código Morse")},minLines=2)
        Button(onClick={text=MorseCodec.decode(morse)}){Text("Morse a texto")}
        Button(onClick={
            val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Morse",morse))
        }){Text("Copiar Morse")}
        Text("Usa espacios entre letras y / entre palabras. No hay envío de datos.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
