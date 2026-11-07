package pe.todoenuno.location

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.Instant

class SunMathTest {
    @Test fun sunsetAfterSunriseLima(){
        val date=LocalDate.of(2026,11,7)
        val a=SunMath.utcMinutes(date,-12.0464,-77.0428,true)!!
        val b=SunMath.utcMinutes(date,-12.0464,-77.0428,false)!!
        assertTrue(b>a)
        assertTrue(b-a in 650.0..820.0)
    }
    @Test fun polarSunCanBeAbsent(){
        val date=LocalDate.of(2026,6,21)
        assertNull(SunMath.utcMinutes(date,89.0,0.0,true))
    }
    @Test fun moonCycleWrapped(){
        val fraction=SunMath.moonFraction(Instant.parse("2026-11-07T12:00:00Z"))
        assertTrue(fraction>=0 && fraction<1)
    }
}
