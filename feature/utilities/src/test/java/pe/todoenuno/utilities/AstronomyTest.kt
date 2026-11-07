package pe.todoenuno.utilities
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.ZoneId

class AstronomyTest {
    @Test fun amanecerLima() {
        val date=LocalDate.of(2026,11,7)
        val rise=Astronomy.localTime(date,-12.0464,-77.0428,true,ZoneId.of("America/Lima"))!!
        val set=Astronomy.localTime(date,-12.0464,-77.0428,false,ZoneId.of("America/Lima"))!!
        assertTrue(rise.hour in 4..7)
        assertTrue(set.hour in 17..20)
    }
    @Test fun phaseBounds() {
        for(day in 1..28) {
            val p=Astronomy.moonPhase(LocalDate.of(2026,2,day))
            assertTrue(p>=0.0 && p<1.0)
            assertTrue(Astronomy.illuminatedFraction(p) in 0.0..1.0)
        }
    }
    @Test fun polarDayReturnNull() {
        val sunrise=Astronomy.solarUtcHours(LocalDate.of(2026,6,21),89.0,10.0,true)
        assertNull(sunrise)
    }
}
