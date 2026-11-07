package pe.todoenuno.personal

import org.junit.Assert.*
import org.junit.Test

class PainterPaletteTest {
    @Test fun endpoints() {
        val a=PainterPalette.colors[0]
        val b=PainterPalette.colors[1]
        assertEquals(a.rgb,PainterPalette.mix(a,b,0f))
        assertEquals(b.rgb,PainterPalette.mix(a,b,1f))
    }
    @Test fun hexIsValid() {
        assertEquals("#FFFFFF",PainterPalette.hex(0xFFFFFFFF.toInt()))
        assertEquals(7,PainterPalette.hex(0xFF112233.toInt()).length)
    }
}
