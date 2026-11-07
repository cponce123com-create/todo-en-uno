package pe.todoenuno.math

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.pow

object FinancialMath {
    fun splitBill(amount: Double, tipPercent: Double, people: Int): Double {
        require(amount >= 0 && tipPercent >= 0 && people > 0)
        return amount * (1.0 + tipPercent / 100.0) / people
    }
    fun monthlyPayment(principal: Double, annualPercent: Double, months: Int): Double {
        require(principal >= 0 && annualPercent >= 0 && months > 0)
        if (annualPercent == 0.0) return principal / months
        val r = annualPercent / 1200.0
        return principal * r / (1.0 - (1.0 + r).pow(-months))
    }
}

@Composable
fun FinancialToolsScreen() {
    var bill by rememberSaveable { mutableStateOf("100") }
    var tip by rememberSaveable { mutableStateOf("10") }
    var people by rememberSaveable { mutableStateOf("2") }
    var capital by rememberSaveable { mutableStateOf("10000") }
    var annual by rememberSaveable { mutableStateOf("12") }
    var months by rememberSaveable { mutableStateOf("24") }
    val fmt = DecimalFormat("#,##0.00",DecimalFormatSymbols(Locale.forLanguageTag("es-PE")))
    @Composable fun numberInput(label:String, value:String, update:(String)->Unit){
        OutlinedTextField(value=value,onValueChange={update(it.take(16))},
            label={Text(label)},modifier=Modifier.fillMaxWidth(),
            singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
    }
    val each = runCatching {
        FinancialMath.splitBill(bill.replace(',','.').toDouble(),tip.replace(',','.').toDouble(),people.toInt())
    }.getOrNull()?.takeIf { it.isFinite() }
    val monthly = runCatching {
        FinancialMath.monthlyPayment(capital.replace(',','.').toDouble(),annual.replace(',','.').toDouble(),months.toInt())
    }.getOrNull()?.takeIf { it.isFinite() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Cálculos financieros",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Dividir una cuenta",style=MaterialTheme.typography.titleMedium)
        numberInput("Total de la cuenta", bill){bill=it}
        numberInput("Propina (%)",tip){tip=it}
        numberInput("Personas",people){people=it}
        Text("Pago por persona: ${each?.let { fmt.format(it) } ?: "Revisa los datos"}",
            color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.titleLarge)
        androidx.compose.material3.HorizontalDivider()
        Text("Préstamo (cuota fija orientativa)",style=MaterialTheme.typography.titleMedium)
        numberInput("Capital",capital){capital=it}
        numberInput("Tasa anual nominal (%)",annual){annual=it}
        numberInput("Plazo en meses",months){months=it}
        Text("Cuota aproximada: ${monthly?.let { fmt.format(it) } ?: "Revisa los datos"}",
            color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.titleLarge)
        Text("No incluye seguros, comisiones ni TCEA; no sustituye una cotización bancaria.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
