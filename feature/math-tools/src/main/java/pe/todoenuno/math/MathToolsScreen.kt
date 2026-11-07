package pe.todoenuno.math

import androidx.compose.runtime.Composable

@Composable
fun MathToolsScreen(id: String) {
    when (id) {
        "calculator" -> CalculatorScreen()
        "converter" -> ConverterScreen()
        "finance" -> FinancialToolsScreen()
        "age" -> AgeScreen()
        "bmi" -> BmiScreen()
        else -> error("Math tool is not registered: $id")
    }
}
