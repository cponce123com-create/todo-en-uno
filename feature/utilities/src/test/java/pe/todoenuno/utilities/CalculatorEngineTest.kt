package pe.todoenuno.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {
    private fun keys(vararg tokens: String): CalcState =
        tokens.fold(CalcState()) { current, key -> CalculatorEngine.press(current, key) }

    @Test fun suma() {
        assertEquals("5", keys("2", "+", "3", "=").display)
    }

    @Test fun divideEntreCero() {
        assertTrue(keys("9", "÷", "0", "=").error)
    }

    @Test fun operacionesEncadenadas() {
        assertEquals("20", keys("2", "+", "3", "×", "4", "=").display)
    }

    @Test fun calculadoraCientifica() {
        assertEquals("3.141592653589793",keys("π").display)
        assertEquals("4",keys("9","√","x²","AC","2","x²").display)
        assertEquals("0",keys("0","sin").display)
        assertTrue(keys("0","1/x").error)
        assertTrue(keys("9","±","√").error)
    }

    @Test fun porcentaje() {
        assertEquals("0.25", keys("2", "5", "%").display)
    }

    @Test fun reinicio() {
        assertEquals(CalcState(), keys("8", "+", "2", "AC"))
    }
}
