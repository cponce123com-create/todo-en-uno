package pe.todoenuno.utilities

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {
    @Test fun kilometrosAMetros() {
        assertEquals(1500.0, UnitConverter.convert(1.5, UnitGroup.LENGTH, "km", "m"), 0.00001)
    }

    @Test fun celsiusAFahrenheit() {
        assertEquals(212.0, UnitConverter.convert(100.0, UnitGroup.TEMP, "C", "F"), 0.00001)
    }

    @Test fun librasAKilogramos() {
        assertEquals(0.45359237, UnitConverter.convert(1.0, UnitGroup.MASS, "lb", "kg"), 0.00000001)
    }
}
