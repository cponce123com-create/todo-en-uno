package pe.todoenuno.utilities

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {
    @Test fun kilometrosAMetros() {
        assertEquals(1500.0, UnitConverter.convert(1.5, UnitGroup.LENGTH, "km", "m"), 0.00001)
    }

    @Test fun medidasAmpliadas() {
        assertEquals(10000.0,UnitConverter.convert(1.0,UnitGroup.AREA,"ha","m2"),0.01)
        assertEquals(1000.0,UnitConverter.convert(1.0,UnitGroup.VOLUME,"m3","L"),0.01)
        assertEquals(36.0,UnitConverter.convert(10.0,UnitGroup.SPEED,"ms","kmh"),0.01)
        assertEquals(1024.0,UnitConverter.convert(1.0,UnitGroup.DATA,"KiB","B"),0.01)
        assertEquals(3600.0,UnitConverter.convert(1.0,UnitGroup.TIME,"h","s"),0.01)
    }

    @Test fun celsiusAFahrenheit() {
        assertEquals(212.0, UnitConverter.convert(100.0, UnitGroup.TEMP, "C", "F"), 0.00001)
    }

    @Test fun librasAKilogramos() {
        assertEquals(0.45359237, UnitConverter.convert(1.0, UnitGroup.MASS, "lb", "kg"), 0.00000001)
    }
}
