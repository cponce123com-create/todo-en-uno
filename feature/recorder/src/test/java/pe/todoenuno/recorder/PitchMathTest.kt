package pe.todoenuno.recorder

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PitchMathTest {
    @Test fun concertA(){
        val result=PitchMath.note(440.0)!!
        assertEquals("La4",result.note)
        assertEquals(0,result.cents)
    }
    @Test fun sinePitch(){
        val samples=ShortArray(4096){(sin(2*PI*440.0*it/16000.0)*16000).toInt().toShort()}
        val hz=PitchMath.detect(samples,16000)!!
        assertTrue(hz in 420.0..460.0)
    }
    @Test fun silence(){assertNull(PitchMath.detect(ShortArray(4096),16000))}
}
