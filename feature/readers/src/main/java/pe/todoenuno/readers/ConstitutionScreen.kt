package pe.todoenuno.readers

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Slider
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object ConstitutionSource {
    const val OFFICIAL="https://www3.congreso.gob.pe/biblioteca/constituciones-peru/"
}
@Composable
fun ConstitutionScreen() {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val file=remember(context){File(context.filesDir,"legal/constitucion-peru.pdf")}
    val epubFile=remember(context){File(context.filesDir,"legal/constitucion-peru.epub")}
    var epubBook by remember { mutableStateOf<EpubBook?>(null) }
    val prefs=remember(context){context.getSharedPreferences("constitution_reader",0)}
    var epubChapter by remember { mutableIntStateOf(prefs.getInt("epubChapter",0)) }
    var epubFont by remember { mutableStateOf(prefs.getFloat("epubFont",18f).coerceIn(14f,28f)) }
    var epubSearch by remember { mutableStateOf("") }
    var epubMode by remember { mutableStateOf(prefs.getBoolean("epubMode",false)) }
    var page by remember{mutableIntStateOf(prefs.getInt("page",0))}
    var pages by remember{mutableIntStateOf(0)}
    var image by remember{mutableStateOf<Bitmap?>(null)}
    var error by remember{mutableStateOf("")}
    var documentLoaded by remember{mutableStateOf(file.exists())}
    var reloaded by remember{mutableIntStateOf(0)}
    var jump by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    LaunchedEffect(Unit) {
        if(epubFile.exists()) epubBook=runCatching {
            withContext(Dispatchers.IO){SimpleEpubReader.parse(epubFile)}
        }.getOrNull()
        epubBook?.let { epubChapter=epubChapter.coerceIn(0,it.chapters.lastIndex) }
    }
    val epubPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null) {
            busy=true
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        epubFile.parentFile?.mkdirs()
                        val temp=File(epubFile.parentFile,"constitution-epub.tmp")
                        try {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                temp.outputStream().use { output ->
                                    val buffer=ByteArray(8192)
                                    var total=0L
                                    while(true) {
                                        val count=input.read(buffer)
                                        if(count<0)break
                                        total+=count
                                        require(total<=30L*1024*1024){"Máximo 30 MB."}
                                        output.write(buffer,0,count)
                                    }
                                }
                            } ?: throw IllegalStateException("No se pudo abrir el EPUB")
                            val parsed=SimpleEpubReader.parse(temp)
                            val backup=File(epubFile.parentFile,"constitution-epub.previous")
                            if(epubFile.exists()) {
                                backup.delete()
                                require(epubFile.renameTo(backup)){"No se pudo respaldar el EPUB anterior"}
                            }
                            if(!temp.renameTo(epubFile)) {
                                if(backup.exists()) check(backup.renameTo(epubFile)){"No se pudo restaurar el EPUB anterior"}
                                throw IllegalStateException("No se pudo guardar el EPUB")
                            }
                            backup.delete()
                            parsed
                        } finally { temp.delete() }
                    }
                }.onSuccess { epubBook=it;epubChapter=0;epubMode=true;error="" }
                    .onFailure { error="No se pudo importar el EPUB: ${it.message}" }
                busy=false
            }
        }
    }
    val picker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ){uri->
        if(uri!=null){
            busy=true
            scope.launch {
                runCatching{
                    withContext(Dispatchers.IO){
                        file.parentFile?.mkdirs()
                        val tmp=File(file.parentFile,"constitution.tmp")
                        context.contentResolver.openInputStream(uri)?.use{ins->
                            tmp.outputStream().use{out->
                                val buf=ByteArray(8192)
                                var bytes=0L
                                while(true){
                                    val n=ins.read(buf)
                                    if(n<0)break
                                    bytes+=n
                                    require(bytes<=30L*1024*1024){"Máximo 30 MB."}
                                    out.write(buf,0,n)
                                }
                            }
                        }?:throw IllegalStateException("No se pudo abrir el PDF")
                        ParcelFileDescriptor.open(tmp,ParcelFileDescriptor.MODE_READ_ONLY).use {fd->
                            PdfRenderer(fd).use {require(it.pageCount>0)}
                        }
                        // Keep the previous validated document until replacement succeeds.
                        val backup=File(file.parentFile,"constitution.previous.pdf")
                        if(file.exists()) {
                            backup.delete()
                            require(file.renameTo(backup)){"No se pudo respaldar la edición anterior"}
                        }
                        if(!tmp.renameTo(file)) {
                            if(backup.exists()) check(backup.renameTo(file)){"No se pudo restaurar la edición anterior"}
                            throw IllegalStateException("No se pudo guardar la Constitución")
                        }
                        backup.delete()
                    }
                }.onSuccess{
                    documentLoaded=true;page=0;reloaded++
                    prefs.edit().putInt("page",0).apply()
                    error=""
                }.onFailure{error="No se pudo importar el PDF: ${it.message}"}
                busy=false
            }
        }
    }
    LaunchedEffect(page,reloaded,documentLoaded,epubMode){
        if(!documentLoaded || epubMode)return@LaunchedEffect
        val result=runCatching {
            withContext(Dispatchers.IO){
                ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use{fd->
                    PdfRenderer(fd).use{renderer->
                        val index=page.coerceIn(0,renderer.pageCount-1)
                        val pageCount=renderer.pageCount
                        renderer.openPage(index).use{pdfPage->
                            val scale=minOf(1250f/pdfPage.width,1800f/pdfPage.height,2.5f).coerceAtMost(2.5f).coerceAtLeast(0.1f)
                            val bitmap=Bitmap.createBitmap(
                                (pdfPage.width*scale).toInt(),
                                (pdfPage.height*scale).toInt(),
                                Bitmap.Config.ARGB_8888
                            )
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            pdfPage.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pageCount to bitmap
                        }
                    }
                }
            }
        }
        result.onSuccess{
            pages=it.first
            // Compose puede seguir dibujando el bitmap anterior durante la recomposición.
            // No reciclarlo aquí: el GC lo libera cuando deje de estar referenciado.
            image=it.second
        }.onFailure{error="No se pudo mostrar la página del PDF."}
    }
    LaunchedEffect(epubChapter,epubFont,epubMode) {
        prefs.edit().putInt("epubChapter",epubChapter)
            .putFloat("epubFont",epubFont)
            .putBoolean("epubMode",epubMode).apply()
    }
    fun setPage(index:Int){
        page=index.coerceIn(0,(pages-1).coerceAtLeast(0))
        prefs.edit().putInt("page",page).apply()
    }
    Column(Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)){
        Text("Constitución Política del Perú",style=MaterialTheme.typography.headlineSmall)
        Text("Lector offline de la edición oficial que importes.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            style=MaterialTheme.typography.bodySmall)
        Button(onClick={
            context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(ConstitutionSource.OFFICIAL)))
        }){Text("Consultar edición oficial del Congreso")}
        Button(onClick={picker.launch(arrayOf("application/pdf"))},enabled=!busy){
            Text(if(busy)"Importando…" else "Importar y guardar PDF oficial")
        }
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        Button(onClick={epubPicker.launch(arrayOf("application/epub+zip","application/octet-stream"))},enabled=!busy){
            Text(if(busy)"Importando…" else "Importar edición EPUB")
        }
        if(epubBook!=null) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={epubMode=true}){Text("Leer EPUB")}
                if(documentLoaded) Button(onClick={epubMode=false}){Text("Leer PDF")}
            }
        }
        if(epubMode && epubBook!=null) {
            val book=epubBook!!
            Text(book.title,style=MaterialTheme.typography.titleMedium)
            OutlinedTextField(value=epubSearch,onValueChange={epubSearch=it.take(100)},label={Text("Buscar en el texto")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            if(epubSearch.isNotBlank()) {
                val matches=remember(book,epubSearch) {
                    book.chapters.mapIndexedNotNull { index,chapter ->
                        if(chapter.text.contains(epubSearch,ignoreCase=true))index else null
                    }
                }
                Text("Secciones con coincidencias: ${matches.size}")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={
                        epubChapter=matches.firstOrNull { it>epubChapter } ?: matches.firstOrNull() ?: epubChapter
                    },enabled=matches.isNotEmpty()){Text("Siguiente coincidencia")}
                    Button(onClick={
                        epubChapter=matches.lastOrNull { it<epubChapter } ?: matches.lastOrNull() ?: epubChapter
                    },enabled=matches.isNotEmpty()){Text("Anterior")}
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Button(onClick={epubChapter=(epubChapter-1).coerceAtLeast(0)},enabled=epubChapter>0){Text("‹")}
                Text("Sección ${epubChapter+1}/${book.chapters.size}")
                Button(onClick={epubChapter=(epubChapter+1).coerceAtMost(book.chapters.lastIndex)},enabled=epubChapter<book.chapters.lastIndex){Text("›")}
            }
            Slider(value=epubFont,onValueChange={epubFont=it},valueRange=14f..28f)
            LazyColumn(Modifier.weight(1f)) {
                item {
                    Text(book.chapters[epubChapter].title,style=MaterialTheme.typography.titleMedium)
                    Text(book.chapters[epubChapter].text,fontSize=epubFont.sp,lineHeight=(epubFont+6).sp)
                }
            }
        } else if(documentLoaded){
            Row(horizontalArrangement=Arrangement.SpaceEvenly,modifier=Modifier.fillMaxWidth()){
                Button(onClick={setPage(page-1)},enabled=page>0){Text("‹")}
                Text("Página ${page+1}/$pages",modifier=Modifier.padding(top=12.dp))
                Button(onClick={setPage(page+1)},enabled=page+1<pages){Text("›")}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()){
                OutlinedTextField(value=jump,onValueChange={jump=it.filter(Char::isDigit).take(4)},
                    label={Text("Ir a página")},modifier=Modifier.weight(1f),singleLine=true)
                Button(onClick={jump.toIntOrNull()?.let{setPage(it-1)}},enabled=pages>0){
                    Text("Ir")
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)){
                val bmp=image
                if(bmp!=null && !bmp.isRecycled)
                    Image(bmp.asImageBitmap(),contentDescription="Página ${page+1} de la Constitución",
                        modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
            }
        }else{
            Text("Aún no se ha importado una edición oficial. Abre el sitio del Congreso, descarga el PDF e impórtalo aquí. Después podrás consultarlo sin Internet.")
        }
        Text("Verifica la procedencia y vigencia de cualquier EPUB: no se garantiza que exista una edición EPUB oficial.",style=MaterialTheme.typography.bodySmall)
        Text("Las normas pueden modificarse. Verifica la fecha y edición de tu documento frente al portal oficial; el lector no actualiza automáticamente su contenido.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
    }
}
