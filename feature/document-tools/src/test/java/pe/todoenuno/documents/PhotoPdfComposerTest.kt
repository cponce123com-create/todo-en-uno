package pe.todoenuno.documents

import org.junit.Assert.*
import org.junit.Test

class PhotoPdfComposerTest {
    @Test fun pagesForDifferentLayouts(){
        assertEquals(10,PhotoPdfComposer.pages(10,PhotoGrid.FULL))
        assertEquals(5,PhotoPdfComposer.pages(10,PhotoGrid.TWO))
        assertEquals(3,PhotoPdfComposer.pages(10,PhotoGrid.FOUR))
        assertEquals(1,PhotoPdfComposer.pages(1,PhotoGrid.FOUR))
    }
    @Test(expected=IllegalArgumentException::class)
    fun rejectMoreThanTen(){PhotoPdfComposer.pages(11,PhotoGrid.FULL)}
}
