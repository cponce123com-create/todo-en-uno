package pe.todoenuno.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialMathTest {
    @Test fun dividirCuenta() {
        assertEquals(55.0, FinancialMath.splitBill(100.0, 10.0, 2), 0.001)
        assertEquals(500.0, FinancialMath.monthlyPayment(1000.0, 0.0, 2), 0.001)
        assertTrue(FinancialMath.monthlyPayment(10000.0, 12.0, 24) > 450.0)
    }
}
