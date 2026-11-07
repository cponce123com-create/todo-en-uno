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
    val prefs=remember(context){context.getSharedPreferences("constitution_reader",0)}
    var page by remember{mutableIntStateOf(prefs.getInt("page",0))}
    var pages by remember{mutableIntStateOf(0)}
    var image by remember{mutableStateOf<Bitmap?>(null)}
    var error by remember{mutableStateOf("")}
    var documentLoaded by remember{mutableStateOf(file.exists())}
    var reloaded by remember{mutableIntStateOf(0)}
    var jump by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
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
                        }?:error("No se pudo abrir el PDF")
                        require(tmp.renameTo(file)){"No se pudo guardar la Constitución"}
                        ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use {fd->
                            PdfRenderer(fd).use {require(it.pageCount>0)}
                        }
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
    LaunchedEffect(page,reloaded,documentLoaded){
        if(!documentLoaded)return@LaunchedEffect
        val result=runCatching {
            withContext(Dispatchers.IO){
                ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use{fd->
                    PdfRenderer(fd).use{renderer->
                        val index=page.coerceIn(0,renderer.pageCount-1)
                        val pageCount=renderer.pageCount
                        renderer.openPage(index).use{pdfPage->
                            val scale=(1250f/pdfPage.width).coerceIn(1f,2.5f)
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
            image?.recycle()
            image=it.second
        }.onFailure{error="No se pudo mostrar la página del PDF."}
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
        if(documentLoaded){
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
        Text("Las normas pueden modificarse. Verifica la fecha y edición de tu documento frente al portal oficial; el lector no actualiza automáticamente su contenido.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
    }
}
