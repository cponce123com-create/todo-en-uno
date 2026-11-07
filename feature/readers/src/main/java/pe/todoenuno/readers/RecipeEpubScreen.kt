package pe.todoenuno.readers

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun RecipeEpubScreen() {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val file=remember(context){File(context.filesDir,"recipes/my_recipes.epub")}
    val prefs=remember(context){context.getSharedPreferences("epub_recipe_position",Context.MODE_PRIVATE)}
    var book by remember{mutableStateOf<EpubBook?>(null)}
    var chapter by remember{mutableIntStateOf(prefs.getInt("chapter",0))}
    var font by remember{mutableFloatStateOf(18f)}
    var error by remember{mutableStateOf("")}
    var loading by remember{mutableStateOf(true)}
    var changed by remember{mutableIntStateOf(0)}
    val picker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ){uri ->
        if(uri!=null){
            loading=true
            scope.launch {
                runCatching{
                    withContext(Dispatchers.IO){
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            EpubImporter.importBook(input,file)
                        }?:error("Archivo no disponible")
                    }
                }.onSuccess{
                    book=it;chapter=0;prefs.edit().putInt("chapter",0).apply()
                    error="";changed++
                }.onFailure{
                    if(it is kotlinx.coroutines.CancellationException)throw it
                    error="Importación fallida: ${it.message}. Se conserva el libro anterior."
                }
                loading=false
            }
        }
    }
    LaunchedEffect(changed){
        if(file.exists() && book==null){
            loading=true
            book=runCatching{withContext(Dispatchers.IO){SimpleEpubReader.parse(file)}}
                .onFailure{
                    if(it is kotlinx.coroutines.CancellationException)throw it
                    error="No se pudo abrir el EPUB guardado."
                }.getOrNull()
            chapter=chapter.coerceIn(0,((book?.chapters?.size?:1)-1).coerceAtLeast(0))
        }
        loading=false
    }
    fun setChapter(index:Int){
        val newIndex=index.coerceIn(0,((book?.chapters?.size?:1)-1).coerceAtLeast(0))
        chapter=newIndex
        prefs.edit().putInt("chapter",chapter).apply()
    }
    Column(Modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Text("Recetario EPUB",style=MaterialTheme.typography.headlineSmall)
        Text("Importa tu libro de recetas EPUB. Se almacena y se lee sin internet.",
            style=MaterialTheme.typography.bodySmall)
        Button(onClick={
            picker.launch(arrayOf("application/epub+zip","application/octet-stream"))
        },enabled=!loading){Text(if(loading)"Procesando…" else "Importar libro EPUB")}
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        val opened=book
        if(opened!=null){
            Text(opened.title,style=MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Button(onClick={setChapter(chapter-1)},enabled=chapter>0){Text("‹")}
                Text("Capítulo ${chapter+1} / ${opened.chapters.size}",
                    modifier=Modifier.padding(top=12.dp))
                Button(onClick={setChapter(chapter+1)},enabled=chapter<opened.chapters.lastIndex){Text("›")}
            }
            Text("Tamaño ${font.toInt()}",style=MaterialTheme.typography.labelSmall)
            Slider(font,onValueChange={font=it},valueRange=14f..28f)
            LazyColumn(Modifier.weight(1f)){
                item{
                    Text(opened.chapters[chapter.coerceIn(0,opened.chapters.lastIndex)].title,
                        style=MaterialTheme.typography.titleLarge)
                    Text(opened.chapters[chapter.coerceIn(0,opened.chapters.lastIndex)].text,
                        fontSize=font.sp,lineHeight=(font+7).sp)
                }
            }
        }else if(!loading){
            Text("Todavía no hay un recetario importado.",
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Lector EPUB textual básico: admite capítulos y conserva el progreso. No reproduce maquetaciones complejas, vídeos ni libros DRM.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
