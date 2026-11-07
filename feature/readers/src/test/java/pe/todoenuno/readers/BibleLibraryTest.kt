package pe.todoenuno.readers

import org.junit.Assert.*
import org.junit.Test

class BibleLibraryTest {
    @Test fun canonicalBooks(){
        assertEquals(66,BibleLibrary.books.size)
        assertEquals("Génesis",BibleLibrary.books.first())
        assertEquals("Apocalipsis",BibleLibrary.books.last())
    }
}
