package pe.todoenuno.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScientificExpressionEngineTest {
    private fun value(input: String, angle: AngleUnit = AngleUnit.DEG) =
        ScientificExpressionEngine.evaluate(input, angle).getOrThrow().value

    @Test fun precedenciaYParentesis() {
        assertEquals(14.0,value("2+3×4"),1e-10)
        assertEquals(20.0,value("(2+3)×4"),1e-10)
        assertEquals(512.0,value("2^3^2"),1e-10)
        assertEquals(-4.0,value("-2^2"),1e-10)
    }
    @Test fun trigonometriaGradosYRadianes() {
        assertEquals(0.5,value("sin(30)"),1e-10)
        assertEquals(1.0,value("sin(π/2)",AngleUnit.RAD),1e-10)
        assertEquals(45.0,value("asin(0.7071067811865476)"),1e-8)
        assertEquals(0.0,value("cos(90)"),1e-10)
    }
    @Test fun raicesFactorialYNotacionCientifica() {
        assertEquals(5.0,value("sqrt(9)+cbrt(8)"),1e-10)
        assertEquals(120.0,value("5!"),1e-10)
        assertEquals(6.283185307179586,value("2π"),1e-10)
        assertEquals(0.001,value("1E-3"),1e-12)
        assertEquals(0.2,value("20%"),1e-10)
    }
    @Test fun validacionYSeguridad() {
        for (invalid in listOf("5/0","sqrt(-1)","log(0)","tan(90)","(-2)!","(2+3","abc(2)","")) {
            assertTrue("No rechazó $invalid",ScientificExpressionEngine.evaluate(invalid).isFailure)
        }
        assertFalse(ScientificExpressionEngine.evaluate("exp(10000)").isSuccess)
    }
}
