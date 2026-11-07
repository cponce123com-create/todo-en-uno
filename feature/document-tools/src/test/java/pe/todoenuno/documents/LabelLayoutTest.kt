package pe.todoenuno.documents

import org.junit.Assert.*
import org.junit.Test

class LabelLayoutTest {
    @Test fun sheetCapacity() {
        assertEquals(24,LabelLayout.PER_PAGE)
        assertEquals(1,LabelLayout.pages(24))
        assertEquals(2,LabelLayout.pages(25))
        assertEquals(4,LabelLayout.pages(96))
    }
    @Test fun cellsFitA4() {
        for(index in 0 until LabelLayout.PER_PAGE) {
            val (x,y)=LabelLayout.position(index)
            assertTrue(x>=LabelLayout.MARGIN)
            assertTrue(y>=LabelLayout.MARGIN)
            assertTrue(x+LabelLayout.labelWidth<=LabelLayout.PAGE_WIDTH)
            assertTrue(y+LabelLayout.labelHeight<=LabelLayout.PAGE_HEIGHT)
        }
    }
    @Test(expected=IllegalArgumentException::class)
    fun noZero(){LabelLayout.pages(0)}
}
