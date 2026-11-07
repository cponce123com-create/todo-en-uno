package pe.todoenuno.recorder
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
class PitchDetectorTest {
    @Test fun detectA4(){
        val rate=22050
        val buffer=ShortArray(4096){(sin(2.0*PI*440.0*it/rate)*20000.0).toInt().toShort()}
        val detected=PitchDetector.frequency(buffer,rate)!!
        assertEquals(440.0,detected,7.0)
        val note=PitchDetector.note(detected)!!
        assertEquals("La4",note.first)
    }
    @Test fun silence(){
        assertNull(PitchDetector.frequency(ShortArray(4096),22050))
    }
}
