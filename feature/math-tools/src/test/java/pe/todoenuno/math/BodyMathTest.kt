package pe.todoenuno.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BodyMathTest{
    @Test fun indiceCorporal(){
        assertEquals(25.0,BodyMath.bmi(81.0,180.0)!!,0.001)
        assertNull(BodyMath.bmi(-1.0,170.0))
    }
    @Test fun edad(){
        val result=BodyMath.age(LocalDate.of(2000,1,15),LocalDate.of(2026,11,7))!!
        assertEquals(26,result.years)
        assertEquals(9,result.months)
        assertNull(BodyMath.age(LocalDate.of(2030,1,1),LocalDate.of(2026,11,7)))
    }
}
