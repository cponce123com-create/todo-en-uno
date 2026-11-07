package pe.todoenuno.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtraToolsTest {
    @Test fun longitudDeContrasena() {
        for (length in listOf(8, 18, 32, 64)) {
            assertEquals(length, PasswordFactory.generate(length).length)
        }
    }
    @Test fun clasesDeCaracteres() {
        val password = PasswordFactory.generate(24, symbols = true)
        assertTrue(password.any { it.isUpperCase() })
        assertTrue(password.any { it.isLowerCase() })
        assertTrue(password.any { it.isDigit() })
        assertTrue(password.any { it in "!@#%&*-_" })
    }
    @Test(expected = IllegalArgumentException::class)
    fun longitudInvalida() { PasswordFactory.generate(5) }
}
