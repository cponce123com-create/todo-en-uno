package pe.todoenuno.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

class CardinalTest {
    @Test fun cardinales() {
        assertEquals("N", cardinal(0f))
        assertEquals("E", cardinal(90f))
        assertEquals("SO", cardinal(225f))
        assertEquals("N", cardinal(359f))
    }
}
