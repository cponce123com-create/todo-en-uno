package pe.todoenuno.utilities

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class UnitChoice(val id: String, val name: String, val factor: Double)

enum class UnitGroup(val title: String, val units: List<UnitChoice>) {
    LENGTH("Longitud", listOf(
        UnitChoice("m", "Metros", 1.0), UnitChoice("km", "Kilómetros", 1000.0),
        UnitChoice("cm", "Centímetros", 0.01), UnitChoice("mi", "Millas", 1609.344),
        UnitChoice("ft", "Pies", 0.3048)
    )),
    MASS("Masa", listOf(
        UnitChoice("kg", "Kilogramos", 1.0), UnitChoice("g", "Gramos", 0.001),
        UnitChoice("lb", "Libras", 0.45359237), UnitChoice("oz", "Onzas", 0.028349523125)
    )),
    TEMP("Temperatura", listOf(
        UnitChoice("C", "Celsius", 1.0), UnitChoice("F", "Fahrenheit", 1.0),
        UnitChoice("K", "Kelvin", 1.0)
    ));
}

object UnitConverter {
    fun convert(value: Double, group: UnitGroup, from: String, to: String): Double {
        require(value.isFinite())
        val a = requireNotNull(group.units.firstOrNull { it.id == from })
        val b = requireNotNull(group.units.firstOrNull { it.id == to })
        if (group == UnitGroup.TEMP) {
            val celsius = when (from) { "F" -> (value - 32.0) * 5.0 / 9.0; "K" -> value - 273.15; else -> value }
            return when (to) { "F" -> celsius * 9.0 / 5.0 + 32.0; "K" -> celsius + 273.15; else -> celsius }
        }
        return value * a.factor / b.factor
    }
}

@Composable
fun ConverterScreen() {
    var group by remember { mutableStateOf(UnitGroup.LENGTH) }
    var from by remember(group) { mutableStateOf(group.units[0].id) }
    var to by remember(group) { mutableStateOf(group.units[1].id) }
    var number by rememberSaveable { mutableStateOf("1") }
    val input = number.replace(',', '.').toDoubleOrNull()
    val result = if (input != null && input.isFinite()) UnitConverter.convert(input, group, from, to) else null
    val formatter = remember { DecimalFormat("#,##0.######", DecimalFormatSymbols(Locale.forLanguageTag("es-PE"))) }

    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Conversor de unidades", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            UnitGroup.entries.forEach { item ->
                FilterChip(selected = group == item, onClick = { group = item }, label = { Text(item.title) })
            }
        }
        OutlinedTextField(
            value = number, onValueChange = { number = it.take(20) },
            modifier = Modifier.fillMaxWidth(), label = { Text("Valor a convertir") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
        )
        Text("Desde", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            group.units.forEach { unit ->
                FilterChip(selected = from == unit.id, onClick = { from = unit.id }, label = { Text(unit.name) })
            }
        }
        Text("Hasta", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            group.units.forEach { unit ->
                FilterChip(selected = to == unit.id, onClick = { to = unit.id }, label = { Text(unit.name) })
            }
        }
        Text("Resultado", style = MaterialTheme.typography.labelLarge)
        Text(
            text = result?.let { formatter.format(it) + " " + to } ?: "Escribe un número válido",
            style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text("Cálculos disponibles sin conexión.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
