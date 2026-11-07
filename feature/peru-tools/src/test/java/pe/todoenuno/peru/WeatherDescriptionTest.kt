package pe.todoenuno.peru

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherDescriptionTest {
    @Test fun knownCodesHaveHumanReadableDescriptions() {
        assertEquals("Despejado",weatherDescription(0))
        assertEquals("Lluvia",weatherDescription(63))
        assertEquals("Tormenta",weatherDescription(95))
    }
    @Test fun unknownCodeIsNotMisrepresented() {
        assertEquals("Código meteorológico WMO 777",weatherDescription(777))
    }
}
