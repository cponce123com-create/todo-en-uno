package pe.todoenuno.app

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.todoenuno.personal.NoteStore
import pe.todoenuno.personal.QuickNote
import pe.todoenuno.readers.SimpleEpubReader

@RunWith(AndroidJUnit4::class)
class DataIntegrityTest {
    private fun isolated(block:(File,NoteStore)->Unit) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val dir=File(context.cacheDir,"integrity-${java.util.UUID.randomUUID()}").apply{mkdirs()}
        val isolated=object:ContextWrapper(context) { override fun getFilesDir():File=dir }
        try {block(dir,NoteStore(isolated))} finally {dir.deleteRecursively()}
    }
    @Test fun notesSurviveReplacingAndDeleting() = isolated { _,store ->
        val first=QuickNote("a","Título","Original",1)
        store.save(listOf(first))
        store.save(listOf(first.copy(text="Actualizada",updated=2),QuickNote("b","Otra","Texto",3)))
        assertEquals("Actualizada",store.load().first().text)
        store.save(store.load().filterNot{it.id=="a"})
        assertEquals(listOf("b"),store.load().map{it.id})
    }
    @Test fun tooManyNotesPreservePreviousFile() = isolated { _,store ->
        val existing=listOf(QuickNote("saved","Título","Conservar",1))
        store.save(existing)
        assertTrue(runCatching{store.save((0..300).map{QuickNote("$it","","",0)})}.isFailure)
        assertEquals(existing,store.load())
    }
    @Test fun interruptedAtomicWriteRecoversBackup() = isolated { dir,store ->
        val saved=listOf(QuickNote("saved","Título","Conservar",1))
        store.save(saved)
        val original=File(dir,"quick_notes.json")
        assertTrue(original.renameTo(File(dir,"quick_notes.json.bak")))
        original.writeText("incompleto")
        assertEquals(saved,store.load())
    }
    @Test fun corruptNotesAreReportedInsteadOfReturningEmpty() = isolated { dir,store ->
        File(dir,"quick_notes.json").writeText("no es JSON")
        assertTrue(runCatching{store.load()}.isFailure)
    }
    private fun epub(dir:File,text:String,count:Int=1):File {
        val file=File(dir,"test.epub")
        ZipOutputStream(file.outputStream()).use { zip ->
            fun entry(name:String,body:String) {
                zip.putNextEntry(ZipEntry(name));zip.write(body.toByteArray());zip.closeEntry()
            }
            entry("META-INF/container.xml","""<container><rootfiles><rootfile full-path="book.opf"/></rootfiles></container>""")
            val items=(0 until count).joinToString(""){"<item id=\"c$it\" href=\"c$it.xhtml\" media-type=\"application/xhtml+xml\"/>"}
            val spine=(0 until count).joinToString(""){"<itemref idref=\"c$it\"/>"}
            entry("book.opf","<package><metadata><title>Prueba</title></metadata><manifest>$items</manifest><spine>$spine</spine></package>")
            repeat(count){entry("c$it.xhtml","<html><body><p>$text</p></body></html>")}
        }
        return file
    }
    @Test fun epubKeepsCompleteText() = isolated { dir,_ ->
        val book=SimpleEpubReader.parse(epub(dir,"Texto completo"))
        assertEquals("Texto completo",book.chapters.single().text)
    }
    @Test fun oversizedChapterIsRejected() = isolated { dir,_ ->
        assertTrue(runCatching{SimpleEpubReader.parse(epub(dir,"a".repeat(65001)))}.isFailure)
    }
    @Test fun oversizedBookIsRejected() = isolated { dir,_ ->
        assertTrue(runCatching{SimpleEpubReader.parse(epub(dir,"Texto",401))}.isFailure)
    }
}
