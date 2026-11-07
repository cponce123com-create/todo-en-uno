package pe.todoenuno.readers

import android.text.Html
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.zip.ZipInputStream

data class EpubChapter(val title:String,val text:String)
data class EpubBook(val title:String,val chapters:List<EpubChapter>)

object SimpleEpubReader {
    /** Extrae solo contenido textual EPUB 2/3 sin ejecutar HTML ni JavaScript. */
    fun parse(file:File):EpubBook {
        require(file.length() in 1..30L*1024*1024){"EPUB vacío o demasiado grande (máximo 30 MB)."}
        val files=mutableMapOf<String,ByteArray>()
        var entries=0
        var bytes=0L
        ZipInputStream(FileInputStream(file)).use { zip ->
            while(true) {
                val item=zip.nextEntry?:break
                entries++
                require(entries<2500){"EPUB con demasiados archivos."}
                if(item.isDirectory)continue
                val name=item.name.replace('\\','/')
                val capture=name.endsWith(".opf",true)||
                    name.endsWith(".xhtml",true)||
                    name.endsWith(".html",true)||
                    name=="META-INF/container.xml"
                val out=if(capture)ByteArrayOutputStream() else null
                val buffer=ByteArray(8192)
                while(true) {
                    val n=zip.read(buffer)
                    if(n<0)break
                    bytes+=n
                    require(bytes<=80L*1024*1024){"EPUB demasiado grande al descomprimir."}
                    if(out!=null){
                        require(out.size()+n<=2*1024*1024){"Capítulo HTML demasiado grande."}
                        out.write(buffer,0,n)
                    }
                }
                if(out!=null)files[name]=out.toByteArray()
                zip.closeEntry()
            }
        }
        val container=files["META-INF/container.xml"]?.toString(Charsets.UTF_8)
        val rootPath=Regex("full-path=['\"]([^'\"]+)['\"]")
            .find(container.orEmpty())?.groupValues?.get(1)
        val opfName=rootPath?.takeIf{it in files}
            ?: files.keys.firstOrNull{it.endsWith(".opf",true)}
            ?: error("No se encontró el índice del EPUB.")
        val parser=XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(ByteArrayInputStream(files.getValue(opfName)),"UTF-8")
        val hrefById=mutableMapOf<String,String>()
        val spine=mutableListOf<String>()
        var title=""
        while(parser.eventType!=XmlPullParser.END_DOCUMENT){
            if(parser.eventType==XmlPullParser.START_TAG){
                when(parser.name.lowercase()) {
                    "item" -> {
                        val id=parser.getAttributeValue(null,"id")
                        val href=parser.getAttributeValue(null,"href")
                        if(id!=null&&href!=null)hrefById[id]=href.substringBefore('#')
                    }
                    "itemref" -> parser.getAttributeValue(null,"idref")?.let{spine+=it}
                    "title" -> if(title.isEmpty()) title=runCatching{parser.nextText()}.getOrDefault("")
                }
            }
            parser.next()
        }
        val base=opfName.substringBeforeLast('/',"").let{if(it.isBlank())"" else "$it/"}
        val ordered=if(spine.isEmpty())hrefById.keys.toList() else spine
        val chapters=ordered.mapNotNull {id ->
            val href=hrefById[id]?:return@mapNotNull null
            val path=runCatching{URI(base).resolve(href).normalize().path}.getOrNull()
                ?:return@mapNotNull null
            val html=files[path]?.toString(Charsets.UTF_8)?:return@mapNotNull null
            val safe=html.replace(Regex("<(script|style)[^>]*>[\\s\\S]*?</\\1>",RegexOption.IGNORE_CASE),"")
            val text=Html.fromHtml(safe,Html.FROM_HTML_MODE_COMPACT).toString()
                .replace(Regex("\\n{3,}"),"\n\n").trim().take(65000)
            if(text.isBlank())null else EpubChapter("Sección ${chaptersTitle(id,href)}",text)
        }.take(400)
        require(chapters.isNotEmpty()){"El EPUB no contiene capítulos de texto compatibles o está protegido."}
        return EpubBook(title.ifBlank{file.nameWithoutExtension},chapters)
    }
    private fun chaptersTitle(id:String,href:String)=id.ifBlank{href.substringAfterLast('/')}
}
