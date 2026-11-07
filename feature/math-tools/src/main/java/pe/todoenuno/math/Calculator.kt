package pe.todoenuno.math

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal

/** Máquina de estados sin dependencias Android, fácil de probar. */
data class CalcState(
    val display: String = "0",
    val memory: Double? = null,
    val operator: String? = null,
    val newOperand: Boolean = false,
    val error: Boolean = false,
)

object CalculatorEngine {
    private val operators = setOf("+", "−", "×", "÷")

    fun press(old: CalcState, key: String): CalcState {
        if (key == "AC") return CalcState()
        if (old.error) return if (key.length == 1 && key[0].isDigit()) CalcState(display = key) else old
        val current = old.display.toDoubleOrNull() ?: 0.0
        return when {
            key.length == 1 && key[0].isDigit() -> {
                val text = if (old.newOperand || old.display == "0") key else old.display + key
                old.copy(display = text.take(18), newOperand = false)
            }
            key == "." -> {
                if (old.newOperand) old.copy(display = "0.", newOperand = false)
                else if ("." in old.display) old else old.copy(display = old.display + ".")
            }
            key == "±" -> old.copy(display = if (current == 0.0) "0" else format(-current))
            key == "%" -> old.copy(display = format(current / 100.0))
            key == "π" -> old.copy(display = format(Math.PI), newOperand = false)
            key in setOf("√","x²","1/x","sin","cos","tan","log","ln") -> {
                val v = when(key) {
                    "√" -> if(current<0) return error() else kotlin.math.sqrt(current)
                    "x²" -> current*current
                    "1/x" -> if(current==0.0) return error() else 1.0/current
                    "sin" -> kotlin.math.sin(Math.toRadians(current))
                    "cos" -> kotlin.math.cos(Math.toRadians(current))
                    "tan" -> {
                        val radians=Math.toRadians(current)
                        if(kotlin.math.abs(kotlin.math.cos(radians))<1e-12) return error()
                        kotlin.math.tan(radians)
                    }
                    "log" -> if(current<=0) return error() else kotlin.math.log10(current)
                    else -> if(current<=0) return error() else kotlin.math.ln(current)
                }
                if(v.isFinite()) old.copy(display=format(v),newOperand=true) else error()
            }
            key in operators -> {
                if (old.memory != null && old.operator != null && !old.newOperand) {
                    val result = calculate(old.memory, current, old.operator) ?: return error()
                    old.copy(display = format(result), memory = result, operator = key, newOperand = true)
                } else old.copy(memory = current, operator = key, newOperand = true)
            }
            key == "=" -> {
                if (old.memory == null || old.operator == null || old.newOperand) old
                else {
                    val result = calculate(old.memory, current, old.operator) ?: return error()
                    CalcState(display = format(result), newOperand = true)
                }
            }
            else -> old
        }
    }

    private fun calculate(a: Double, b: Double, op: String): Double? {
        val value = when (op) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            "÷" -> if (b == 0.0) return null else a / b
            else -> return null
        }
        return value.takeIf { it.isFinite() }
    }

    private fun format(value: Double): String {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().take(20)
    }

    private fun error() = CalcState(display = "Error", error = true)
}

private val keypad = listOf(
    listOf("AC", "±", "%", "÷"),
    listOf("7", "8", "9", "×"),
    listOf("4", "5", "6", "−"),
    listOf("1", "2", "3", "+"),
    listOf("0", ".", "="),
)

@Composable
fun CalculatorScreen() {
    var state by remember { mutableStateOf(CalcState()) }
    var scientific by remember { mutableStateOf(false) }
    CalculatorBody(state = state, scientific=scientific, onToggle={scientific=!scientific}, onKey = { state = CalculatorEngine.press(state, it) })
}

@Composable
private fun CalculatorBody(state: CalcState,scientific:Boolean,onToggle:()->Unit,onKey: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        TextButton(onClick=onToggle) { Text(if(scientific) "Modo estándar" else "Modo científico") }
        Text(
            text = state.display,
            textAlign = TextAlign.End,
            fontSize = 46.sp,
            lineHeight = 54.sp,
            fontWeight = FontWeight.Light,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)
        )
        if(scientific) {
            listOf(listOf("sin","cos","tan","π"),listOf("√","x²","log","ln"),listOf("1/x")).forEach { keys ->
                Row(Modifier.fillMaxWidth().padding(bottom=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    keys.forEach { key ->
                        Button(onClick={onKey(key)},modifier=Modifier.weight(1f).height(46.dp),
                            shape=RoundedCornerShape(13.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor=MaterialTheme.colorScheme.onTertiaryContainer),
                            contentPadding=androidx.compose.foundation.layout.PaddingValues(0.dp)){
                            Text(key,fontSize=15.sp)
                        }
                    }
                }
            }
        }
        keypad.forEach { keys ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                keys.forEach { key ->
                    val isOperator = key in setOf("+", "−", "÷", "×", "=")
                    Button(
                        onClick = { onKey(key) },
                        modifier = Modifier.weight(if (keys.size == 3 && key == "0") 2f else 1f).height(if(scientific) 51.dp else 69.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = if (isOperator) ButtonDefaults.buttonColors()
                        else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(key, fontSize = if(scientific) 19.sp else 24.sp)
                    }
                }
            }
        }
    }
}
