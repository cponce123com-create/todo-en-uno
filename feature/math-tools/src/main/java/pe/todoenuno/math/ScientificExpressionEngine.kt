package pe.todoenuno.math

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.*

enum class AngleUnit { DEG, RAD }

data class Evaluation(val value: Double, val display: String)

/**
 * Parser recursivo local: no eval, JavaScript, red ni motores externos.
 * Admite precedencia, paréntesis, potencias asociativas a la derecha,
 * multiplicación implícita, factorial, trigonometría y DEG/RAD.
 */
object ScientificExpressionEngine {
    fun evaluate(source: String, angle: AngleUnit = AngleUnit.DEG): Result<Evaluation> =
        runCatching {
            require(source.length in 1..240) { "La expresión debe tener entre 1 y 240 caracteres." }
            val parser = Parser(source, angle)
            val result = parser.parse()
            require(result.isFinite()) { "Resultado fuera de rango." }
            Evaluation(result, format(result))
        }

    private fun format(number: Double): String {
        if (number == 0.0) return "0"
        val decimal = BigDecimal.valueOf(number).round(MathContext(13)).stripTrailingZeros()
        return if (decimal.precision() > 17 || decimal.scale() < -7) decimal.toEngineeringString()
        else decimal.toPlainString()
    }

    private class Parser(private val input: String, private val angle: AngleUnit) {
        private var i = 0

        fun parse(): Double {
            val value = sum()
            skip()
            require(i == input.length) { "Símbolo inesperado en posición ${i + 1}." }
            return finite(value)
        }
        private fun skip() { while (i < input.length && input[i].isWhitespace()) i++ }
        private fun consume(char: Char): Boolean {
            skip()
            if (i < input.length && input[i] == char) { i++; return true }
            return false
        }
        private fun sum(): Double {
            var x = product()
            while (true) x = when {
                consume('+') -> finite(x + product())
                consume('-') || consume('−') -> finite(x - product())
                else -> return x
            }
        }
        private fun product(): Double {
            var x = unary()
            while (true) {
                x = when {
                    consume('×') || consume('*') -> finite(x * unary())
                    consume('÷') || consume('/') -> {
                        val divisor = unary()
                        require(divisor != 0.0) { "No se puede dividir entre cero." }
                        finite(x / divisor)
                    }
                    implicitNext() -> finite(x * unary())
                    else -> return x
                }
            }
        }
        private fun implicitNext(): Boolean {
            skip()
            if (i >= input.length) return false
            val c = input[i]
            // 2π, 3(2+1), 3sin(30) y 2e
            return c == '(' || c == 'π' || c.isLetter()
        }
        private fun unary(): Double = when {
            consume('+') -> unary()
            consume('-') || consume('−') -> -unary()
            else -> power()
        }
        private fun power(): Double {
            val base = postfix()
            return if (consume('^')) finite(base.pow(unary())) else base
        }
        private fun postfix(): Double {
            var x = primary()
            while (true) x = when {
                consume('!') -> factorial(x)
                consume('%') -> x / 100.0
                else -> return x
            }
        }
        private fun primary(): Double {
            skip()
            if (consume('(')) {
                val value = sum()
                require(consume(')')) { "Falta cerrar paréntesis." }
                return value
            }
            if (consume('π')) return Math.PI
            if (i < input.length && (input[i].isDigit() || input[i] == '.')) return number()
            if (i < input.length && input[i].isLetter()) {
                val start = i
                while (i < input.length && input[i].isLetter()) i++
                val name = input.substring(start, i).lowercase()
                if (name == "pi") return Math.PI
                if (name == "e") return Math.E
                require(name in functions) { "Función desconocida: $name" }
                require(consume('(')) { "Usa paréntesis: $name(...)." }
                val argument = sum()
                require(consume(')')) { "Falta cerrar la función $name." }
                return function(name, argument)
            }
            error("Falta un número en posición ${i + 1}.")
        }
        private fun number(): Double {
            val from = i
            var seenDot = false
            var digits = 0
            while (i < input.length) {
                val c = input[i]
                if (c.isDigit()) { digits++; i++ }
                else if (c == '.' && !seenDot) { seenDot = true; i++ }
                else break
            }
            require(digits > 0) { "Número incompleto." }
            if (i < input.length && (input[i] == 'E' || input[i] == 'e')) {
                val at = i + 1
                val digitIndex = if (at < input.length && input[at] in "+-") at + 1 else at
                if (digitIndex < input.length && input[digitIndex].isDigit()) {
                    i = digitIndex
                    while (i < input.length && input[i].isDigit()) i++
                }
            }
            return finite(input.substring(from, i).toDouble())
        }
        private fun function(name: String, x: Double): Double {
            val radians = if (angle == AngleUnit.DEG) Math.toRadians(x) else x
            val result = when (name) {
                "sin" -> sin(radians)
                "cos" -> cos(radians)
                "tan" -> {
                    require(abs(cos(radians)) > 1e-12) { "Tangente indefinida." }
                    tan(radians)
                }
                "asin" -> {
                    require(x in -1.0..1.0) { "asin admite valores entre -1 y 1." }
                    val y = asin(x)
                    if (angle == AngleUnit.DEG) Math.toDegrees(y) else y
                }
                "acos" -> {
                    require(x in -1.0..1.0) { "acos admite valores entre -1 y 1." }
                    val y = acos(x)
                    if (angle == AngleUnit.DEG) Math.toDegrees(y) else y
                }
                "atan" -> {
                    val y = atan(x)
                    if (angle == AngleUnit.DEG) Math.toDegrees(y) else y
                }
                "sqrt" -> {
                    require(x >= 0.0) { "Raíz de valor negativo." }
                    sqrt(x)
                }
                "cbrt" -> Math.cbrt(x)
                "ln" -> {
                    require(x > 0.0) { "El logaritmo exige un valor positivo." }
                    ln(x)
                }
                "log" -> {
                    require(x > 0.0) { "El logaritmo exige un valor positivo." }
                    log10(x)
                }
                "abs" -> abs(x)
                "exp" -> exp(x)
                else -> error("Función desconocida")
            }
            return finite(result)
        }
        private fun factorial(x: Double): Double {
            require(x >= 0.0 && x <= 170.0 && x == floor(x)) {
                "Factorial: utiliza enteros entre 0 y 170."
            }
            var result = 1.0
            for (n in 2..x.toInt()) result *= n
            return result
        }
        private fun finite(x: Double): Double {
            require(x.isFinite()) { "Operación no definida o demasiado grande." }
            return x
        }
        private companion object {
            val functions = setOf("sin","cos","tan","asin","acos","atan","sqrt","cbrt","log","ln","abs","exp")
        }
    }
}
