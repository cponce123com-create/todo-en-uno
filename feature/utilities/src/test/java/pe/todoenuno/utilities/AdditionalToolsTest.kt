package pe.todoenuno.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdditionalToolsTest {
    @Test fun texto() {
        assertEquals("Hola mundo",TextTransformer.removeDuplicateSpaces(" Hola  mundo "))
        assertEquals(2,TextTransformer.words("Hola\\n mundo".replace("\\n","\n")))
        assertEquals(0,TextTransformer.words(""))
    }
    @Test fun colorHex() {
        assertEquals("#22A397",ColorCodec.hex(34,163,151))
        assertEquals(Triple(34,163,151),ColorCodec.parse("#22A397"))
        assertNull(ColorCodec.parse("#xyz"))
    }
    @Test fun cuenta() {
        assertEquals(55.0,FinancialMath.splitBill(100.0,10.0,2),0.001)
        assertEquals(500.0,FinancialMath.monthlyPayment(1000.0,0.0,2),0.001)
        assertTrue(FinancialMath.monthlyPayment(10000.0,12.0,24)>450.0)
    }
    @Test fun morse() {
        assertEquals(".... --- .-.. .-",MorseCodec.encode("HOLA"))
        assertEquals("HOLA",MorseCodec.decode(".... --- .-.. .-"))
    }
}
