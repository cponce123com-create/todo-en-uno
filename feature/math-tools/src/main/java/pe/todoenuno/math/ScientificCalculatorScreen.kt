package pe.todoenuno.math

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Interfaz adaptativa de expresiones, compatible con modo claro/oscuro y sin conexión. */
@Composable
fun ScientificCalculatorScreen(onBasicMode: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("scientific_history", Context.MODE_PRIVATE)
    }
    var expression by rememberSaveable { mutableStateOf("") }
    var angle by rememberSaveable { mutableStateOf(AngleUnit.DEG) }
    var lastResult by rememberSaveable { mutableStateOf("0") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var history by remember { mutableStateOf(
        preferences.getString("items","").orEmpty().split("\u001e")
            .filter { it.isNotBlank() }.take(15)
    ) }

    fun addToken(token: String) {
        error = null
        expression = when (token) {
            "AC" -> ""
            "⌫" -> expression.dropLast(1)
            "x²" -> expression + "^2"
            "xʸ" -> expression + "^"
            "1/x" -> if (expression.isBlank()) "1/(" else "1/($expression)"
            "√" -> expression + "sqrt("
            "∛" -> expression + "cbrt("
            "±" -> if (expression.isBlank()) "-" else "-($expression)"
            "sin","cos","tan","asin","acos","atan","log","ln","abs","exp" -> expression + token + "("
            else -> expression + token
        }.take(240)
        if (token == "AC") lastResult = "0"
    }
    fun evaluate() {
        val raw = expression.trim()
        if (raw.isEmpty()) return
        ScientificExpressionEngine.evaluate(raw, angle).fold(
            onSuccess = { value ->
                lastResult = value.display
                error = null
                val text = "$raw = ${value.display} [${angle.name}]"
                history = (listOf(text) + history).distinct().take(15)
                preferences.edit().putString("items",history.joinToString("\u001e")).apply()
            },
            onFailure = { error = it.message ?: "Expresión no válida." }
        )
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact = maxHeight < 700.dp
    val keyHeight = ((maxHeight - if(compact) 230.dp else 300.dp) / 8).coerceIn(36.dp, 56.dp)
    Column(
        Modifier.fillMaxSize().padding(if(compact) 6.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick=onBasicMode) { Text("Modo estándar") }
            TextButton(onClick={showHistory=!showHistory}) {
                Text(if(showHistory)"Ocultar historial" else "Historial")
            }
        }
        ElevatedCard(
            modifier=Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier=Modifier.fillMaxWidth().padding(if(compact) 8.dp else 18.dp),
                verticalArrangement=Arrangement.spacedBy(6.dp)
            ) {
                Text("Expresión",style=MaterialTheme.typography.labelMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value=expression,
                    onValueChange={expression=it.take(240);error=null},
                    modifier=Modifier.fillMaxWidth(),
                    maxLines=if(compact) 1 else 2,
                    placeholder={Text("Ej. sin(45)^2 + cos(45)^2")},
                    keyboardOptions=KeyboardOptions.Default,
                )
                Text(lastResult,modifier=Modifier.fillMaxWidth(),
                    textAlign=TextAlign.End,fontSize=if(compact) 25.sp else 31.sp,
                    fontWeight=FontWeight.Bold,
                    color=MaterialTheme.colorScheme.primary)
                error?.let {
                    Text(it,color=MaterialTheme.colorScheme.error,
                        style=MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={
                        angle=if(angle==AngleUnit.DEG)AngleUnit.RAD else AngleUnit.DEG
                    }) {Text(angle.name)}
                    OutlinedButton(onClick={
                        val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Resultado",lastResult))
                    }) {Text("Copiar resultado")}
                }
            }
        }
        if(!compact) Text("Funciones científicas",style=MaterialTheme.typography.labelLarge,
            modifier=Modifier.padding(top=4.dp))
        val keys=listOf(
            listOf("asin","acos","atan","DEG/RAD","⌫"),
            listOf("sin","cos","tan","π","e"),
            listOf("log","ln","√","∛","!"),
            listOf("(",")","x²","xʸ","%"),
            listOf("7","8","9","÷","AC"),
            listOf("4","5","6","×","1/x"),
            listOf("1","2","3","−","±"),
            listOf("0",".","exp","+", "=")
        )
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                row.forEach { key ->
                    val operator=key in setOf("+","−","×","÷","=")
                    val modifier=Modifier.weight(1f).height(keyHeight)
                    Button(
                        onClick={
                            when(key) {
                                "DEG/RAD" -> angle=if(angle==AngleUnit.DEG)AngleUnit.RAD else AngleUnit.DEG
                                "=" -> evaluate()
                                "exp" -> addToken("exp")
                                else -> addToken(key)
                            }
                        },
                        modifier=modifier,
                        shape=RoundedCornerShape(13.dp),
                        contentPadding=androidx.compose.foundation.layout.PaddingValues(0.dp),
                        colors=ButtonDefaults.buttonColors(
                            containerColor=when {
                                key=="AC"->MaterialTheme.colorScheme.errorContainer
                                operator->MaterialTheme.colorScheme.primary
                                key.firstOrNull()?.isDigit()==true -> MaterialTheme.colorScheme.surfaceContainerHigh
                                else->MaterialTheme.colorScheme.tertiaryContainer
                            },
                            contentColor=when{
                                key=="AC"->MaterialTheme.colorScheme.onErrorContainer
                                operator->MaterialTheme.colorScheme.onPrimary
                                key.firstOrNull()?.isDigit()==true -> MaterialTheme.colorScheme.onSurface
                                else->MaterialTheme.colorScheme.onTertiaryContainer
                            }
                        )
                    ) {
                        Text(key,style=MaterialTheme.typography.labelMedium,
                            fontWeight=FontWeight.SemiBold,maxLines=1)
                    }
                }
            }
        }
        if(!compact) Text("Operaciones locales. Usa funciones como sin(45), sqrt(9), 5!, 2^3 y DEG/RAD.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if(showHistory) {
        AlertDialog(
            onDismissRequest={showHistory=false},
            title={Text("Historial de cálculos")},
            text={
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if(history.isEmpty()) Text("No hay cálculos guardados.")
                    history.forEach { item ->
                        TextButton(onClick={expression=item.substringBefore(" = ");showHistory=false}) {
                            Text(item,style=MaterialTheme.typography.bodySmall)
                        }
                    }
                    if(history.isNotEmpty()) TextButton(onClick={
                        history=emptyList()
                        preferences.edit().remove("items").apply()
                    }) {Text("Borrar historial")}
                }
            },
            confirmButton={TextButton(onClick={showHistory=false}){Text("Cerrar")}}
        )
    }
    }
}
