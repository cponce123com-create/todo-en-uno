package pe.todoenuno.utilities

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.security.SecureRandom

@Composable
fun QrGeneratorScreen() {
    var text by rememberSaveable { mutableStateOf("") }
    val bitmap = remember(text) {
        if (text.isBlank() || text.length > 500) null
        else runCatching { makeQr(text) }.getOrNull()
    }
    Column(Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Generador QR", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = text, onValueChange = { text = it.take(500) }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Texto o dirección web") }, minLines = 2, maxLines = 4
        )
        if (bitmap == null) Text("Introduce un texto para generar su código QR.")
        else {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Código QR generado",
                modifier = Modifier.size(280.dp))
            Text("QR generado sin Internet. Puedes escanearlo con otra cámara.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun makeQr(text: String): Bitmap {
    val width = 500
    val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, width,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2))
    val pixels = IntArray(width * width) { index ->
        if (matrix.get(index % width, index / width)) android.graphics.Color.BLACK
        else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(pixels, width, width, Bitmap.Config.ARGB_8888)
}

object PasswordFactory {
    private val random = SecureRandom()
    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!@#%&*-_"

    fun generate(length: Int, symbols: Boolean = true): String {
        require(length in 8..64)
        val groups = mutableListOf(LOWER, UPPER, DIGITS)
        if (symbols) groups.add(SYMBOLS)
        val pool = groups.joinToString("")
        val chars = mutableListOf<Char>()
        groups.forEach { chars.add(it[random.nextInt(it.length)]) }
        while (chars.size < length) chars.add(pool[random.nextInt(pool.length)])
        for (i in chars.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp
        }
        return chars.joinToString("")
    }
}

@Composable
fun PasswordGeneratorScreen() {
    val context = LocalContext.current
    var length by rememberSaveable { mutableIntStateOf(18) }
    var symbols by rememberSaveable { mutableStateOf(true) }
    var password by remember { mutableStateOf(PasswordFactory.generate(length, symbols)) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Generador de contraseñas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(password, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 20.dp))
        Text("Longitud: $length caracteres")
        Slider(value = length.toFloat(), onValueChange = { length = it.toInt() },
            valueRange = 8f..32f, steps = 23)
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Incluir símbolos", modifier = Modifier.weight(1f))
            Switch(checked = symbols, onCheckedChange = { symbols = it })
        }
        Button(onClick = { password = PasswordFactory.generate(length, symbols) }) {
            Text("Generar contraseña nueva")
        }
        Button(onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = ClipData.newPlainText("Contraseña", password)
            if (Build.VERSION.SDK_INT >= 33) {
                clip.description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
            clipboard.setPrimaryClip(clip)
        }) {
            Text("Copiar contraseña")
        }
        Text("Generación con SecureRandom y sin guardar las claves en archivos ni servidores.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
    }
}
