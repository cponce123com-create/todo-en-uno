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
import pe.todoenuno.readers.EpubImporter
import pe.todoenuno.documents.PhotoPdfComposer
import pe.todoenuno.documents.PhotoGrid
import pe.todoenuno.documents.PhotoLayout
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

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
    @Test fun invalidEpubImportPreservesExistingBook() = isolated { dir,_ ->
        val target=epub(dir,"Mi libro anterior")
        val previous=target.readBytes()
        assertTrue(runCatching{EpubImporter.importBook(ByteArrayInputStream("invalid".toByteArray()),target)}.isFailure)
        assertArrayEquals(previous,target.readBytes())
        assertEquals("Mi libro anterior",SimpleEpubReader.parse(target).chapters.single().text)
        assertFalse(dir.listFiles()!!.any{it.name.startsWith("epub-import-")})
    }
    @Test fun failedInputPreservesBookAndCleansTemporaryFile() = isolated { dir,_ ->
        val target=epub(dir,"Anterior")
        val previous=target.readBytes()
        val failing=object:InputStream(){override fun read():Int=throw java.io.IOException("Interrupted input")}
        assertTrue(runCatching{EpubImporter.importBook(failing,target)}.isFailure)
        assertArrayEquals(previous,target.readBytes())
        assertFalse(dir.listFiles()!!.any{it.name.startsWith("epub-import-")})
    }
    @Test fun validEpubImportReplacesBookAfterValidation() = isolated { dir,_ ->
        val target=File(dir,"saved.epub")
        epub(dir,"Anterior").copyTo(target)
        val incoming=epub(dir,"Nuevo texto completo").readBytes()
        val book=EpubImporter.importBook(ByteArrayInputStream(incoming),target)
        assertEquals("Nuevo texto completo",book.chapters.single().text)
        assertEquals(book,SimpleEpubReader.parse(target))
    }
    @Test fun realPhotographProducesReadablePdf() = isolated { dir,_ ->
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val image=File(dir,"photo.jpg")
        val bitmap=Bitmap.createBitmap(240,180,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)
        try {image.outputStream().use{assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,90,it))}}
        finally {bitmap.recycle()}
        val pdf=File(dir,"output.pdf")
        pdf.outputStream().use { output ->
            assertEquals(1,PhotoPdfComposer.compose(context,listOf(PhotoLayout(Uri.fromFile(image))),PhotoGrid.FULL,output))
        }
        ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use{renderer ->
                assertEquals(1,renderer.pageCount)
                renderer.openPage(0).use{page ->assertEquals(595,page.width);assertEquals(842,page.height)}
            }
        }
    }
    @Test fun invalidPhotoFailsWithoutWritingPdf() = isolated { dir,_ ->
        val image=File(dir,"broken.jpg").apply{writeText("not an image")}
        val output=ByteArrayOutputStream()
        val result=runCatching {
            PhotoPdfComposer.compose(InstrumentationRegistry.getInstrumentation().targetContext,
                listOf(PhotoLayout(Uri.fromFile(image))),PhotoGrid.FULL,output)
        }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0,output.size())
    }

}
