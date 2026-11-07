package pe.todoenuno.documents

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

/**
 * Mini editor de composición local, máximo 10 fotos por documento.
 * Ajustes por imagen y layouts A4 de 1/2/4 imágenes, sin subir fotos a la nube.
 */
@Composable
fun PhotoPdfComposerScreen() {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var images by remember {mutableStateOf<List<PhotoLayout>>(emptyList())}
    var selected by remember{mutableStateOf(0)}
    var grid by remember{mutableStateOf(PhotoGrid.FULL)}
    var thumbnail by remember{mutableStateOf<Bitmap?>(null)}
    var status by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    val pick=rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selectedUris ->
        val unique=selectedUris.distinct().filterNot {uri->images.any{it.uri==uri}}
        val free=PhotoPdfComposer.MAX_IMAGES-images.size
        images=images+unique.take(free).map{PhotoLayout(it)}
        if(selectedUris.size>free)status="Se añadieron como máximo 10 imágenes."
        selected=selected.coerceIn(0,(images.size-1).coerceAtLeast(0))
    }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri ->
        if(uri!=null && images.isNotEmpty()) {
            busy=true
            val snapshot=images.toList()
            val type=grid
            scope.launch{
                runCatching {
                    withContext(Dispatchers.IO){
                        context.contentResolver.openOutputStream(uri)?.use{
                            PhotoPdfComposer.compose(context,snapshot,type,it)
                        }?:error("No se pudo crear el archivo.")
                    }
                }.onSuccess{status="PDF guardado correctamente ($it página/s)."}
                    .onFailure{status="Error al exportar: ${it.message}"}
                busy=false
            }
        }
    }
    val current=images.getOrNull(selected)
    LaunchedEffect(current?.uri) {
        thumbnail=null
        if(current!=null)thumbnail=withContext(Dispatchers.IO){
            runCatching {
                context.contentResolver.openInputStream(current.uri)?.use{
                    val opts=BitmapFactory.Options().apply{inSampleSize=8}
                    BitmapFactory.decodeStream(it,null,opts)
                }
            }.getOrNull()
        }
    }
    fun change(update:(PhotoLayout)->PhotoLayout) {
        images=images.mapIndexed {i,old->if(i==selected)update(old) else old}
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Creador de PDF con fotos",style=MaterialTheme.typography.headlineSmall)
        Text("Hasta 10 imágenes, con orden, rotación, zoom y ubicación por hoja A4.")
        Button(onClick={pick.launch("image/*")},enabled=images.size<10){
            Text("Añadir imágenes (${images.size}/10)")
        }
        if(images.isNotEmpty()) {
            Text("Imágenes: toca una posición para editar",style=MaterialTheme.typography.titleSmall)
            images.chunked(5).forEachIndexed {row,list->
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    list.forEachIndexed {column,_->
                        val index=row*5+column
                        FilterChip(selected=selected==index,onClick={selected=index},
                            label={Text("${index+1}")})
                    }
                }
            }
            Card(Modifier.fillMaxWidth()){
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    val bmp=thumbnail
                    if(bmp!=null && !bmp.isRecycled) {
                        Image(bmp.asImageBitmap(),"Foto seleccionada",
                            modifier=Modifier.fillMaxWidth().height(235.dp),
                            contentScale=ContentScale.Fit)
                    } else Text("Vista previa no disponible para este archivo.")
                    Text("Imagen ${selected+1}",style=MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        TextButton(onClick={
                            if(selected>0) {
                                val a=images.toMutableList()
                                val temp=a[selected-1]
                                a[selected-1]=a[selected];a[selected]=temp
                                images=a;selected--
                            }
                        }){Text("Anterior ↑")}
                        TextButton(onClick={
                            if(selected<images.lastIndex){
                                val a=images.toMutableList()
                                val temp=a[selected+1]
                                a[selected+1]=a[selected];a[selected]=temp
                                images=a;selected++
                            }
                        }){Text("Siguiente ↓")}
                        TextButton(onClick={
                            images=images.filterIndexed{i,_->i!=selected}
                            selected=selected.coerceAtMost((images.size-1).coerceAtLeast(0))
                        }){Text("Quitar")}
                    }
                    if(current!=null){
                        Text("Escala: ${(current.zoom*100).toInt()} %")
                        Slider(current.zoom,{v->change{it.copy(zoom=v)}},valueRange=.5f..2.5f)
                        Text("Desplazamiento horizontal")
                        Slider(current.horizontal,{v->change{it.copy(horizontal=v)}},valueRange=-1f..1f)
                        Text("Desplazamiento vertical")
                        Slider(current.vertical,{v->change{it.copy(vertical=v)}},valueRange=-1f..1f)
                        TextButton(onClick={change{it.copy(rotation=(it.rotation+90)%360)}}){
                            Text("Girar 90°")
                        }
                    }
                }
            }
            Text("Distribución",style=MaterialTheme.typography.titleMedium)
            PhotoGrid.entries.forEach{type->
                FilterChip(selected=type==grid,onClick={grid=type},label={Text(type.title)},
                    modifier=Modifier.fillMaxWidth())
            }
            Button(onClick={export.launch("Fotos-Todo-en-Uno.pdf")},
                enabled=!busy,modifier=Modifier.fillMaxWidth()){
                Text(if(busy)"Exportando..." else "Crear y guardar PDF")
            }
        }
        if(status.isNotBlank())Text(status,color=MaterialTheme.colorScheme.primary)
        Text("Todas las imágenes se procesan en este teléfono. El PDF se guarda donde tú elijas. El editor ofrece un layout sencillo, no las funciones completas de Canva.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
