package pe.todoenuno.readers

import android.util.AtomicFile
import java.io.File
import java.io.InputStream

/** Validate a bounded temporary copy before atomically replacing the stored book. */
object EpubImporter {
    private val lock=Any()
    fun importBook(input:InputStream,target:File):EpubBook = synchronized(lock) {
        val parent=target.parentFile?:error("Destino no válido")
        check(parent.isDirectory || parent.mkdirs()) {"No se pudo preparar el almacenamiento"}
        val temporary=File.createTempFile("epub-import-",".tmp",parent)
        try {
            temporary.outputStream().use { output ->
                val buffer=ByteArray(8192)
                var total=0L
                while(true) {
                    val count=input.read(buffer)
                    if(count<0)break
                    total+=count
                    require(total<=30L*1024*1024){"Máximo EPUB 30 MB."}
                    output.write(buffer,0,count)
                }
            }
            val book=SimpleEpubReader.parse(temporary)
            val atomic=AtomicFile(target)
            val output=atomic.startWrite()
            try {
                temporary.inputStream().use{it.copyTo(output)}
                atomic.finishWrite(output)
            } catch(t:Throwable) {
                atomic.failWrite(output)
                throw t
            }
            book
        } finally {temporary.delete()}
    }
}
