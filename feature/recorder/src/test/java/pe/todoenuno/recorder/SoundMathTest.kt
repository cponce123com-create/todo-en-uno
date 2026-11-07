package pe.todoenuno.recorder

import org.junit.Assert.assertEquals
import org.junit.Test

class SoundMathTest {
    @Test fun silencio(){assertEquals(-90.0,SoundMath.dbfs(shortArrayOf(),0),0.001)}
    @Test fun amplitudMaxima(){
        val samples=shortArrayOf(32767,(-32768).toShort())
        assertEquals(0.0,SoundMath.dbfs(samples,2),0.01)
    }
}
