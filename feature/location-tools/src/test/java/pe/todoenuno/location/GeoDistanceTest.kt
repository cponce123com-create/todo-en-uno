package pe.todoenuno.location

import org.junit.Assert.*
import org.junit.Test

class GeoDistanceTest {
    @Test fun zero(){assertEquals(0.0,GeoDistance.km(-12.0,-77.0,-12.0,-77.0),1e-10)}
    @Test fun quarterCircle(){
        val km=GeoDistance.km(0.0,0.0,0.0,90.0)
        assertEquals(10007.557,km,0.5)
    }
    @Test(expected=IllegalArgumentException::class)
    fun invalid(){GeoDistance.km(91.0,0.0,0.0,0.0)}
}
