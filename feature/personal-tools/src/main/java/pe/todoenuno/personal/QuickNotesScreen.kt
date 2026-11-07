package pe.todoenuno.personal

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class QuickNote(val id:String,val title:String,val text:String,val updated:Long)

class NoteStore(private val context:Context) {
    private val file get()=File(context.filesDir,"quick_notes.json")
    fun load():List<QuickNote> = runCatching {
        if(!file.exists())return emptyList()
        val arr=JSONArray(file.readText())
        (0 until arr.length()).map{index->
            val x=arr.getJSONObject(index)
            QuickNote(x.getString("id"),x.optString("title"),
                x.optString("text"),x.optLong("updated"))
        }
    }.getOrDefault(emptyList())
    fun save(notes:List<QuickNote>) {
        val arr=JSONArray()
        notes.take(300).forEach { n->
            arr.put(JSONObject().put("id",n.id).put("title",n.title)
                .put("text",n.text).put("updated",n.updated))
        }
        val pending=File(file.parentFile,file.name+".tmp")
        pending.writeText(arr.toString())
        if(!pending.renameTo(file)) {
            file.writeText(arr.toString())
            pending.delete()
        }
    }
}

@Composable
fun QuickNotesScreen() {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val store=remember(context){NoteStore(context.applicationContext)}
    var notes by remember{mutableStateOf<List<QuickNote>>(emptyList())}
    var opened by remember{mutableStateOf<String?>(null)}
    var title by remember{mutableStateOf("")}
    var body by remember{mutableStateOf("")}
    var filter by remember{mutableStateOf("")}
    var status by remember{mutableStateOf("")}
    LaunchedEffect(store){ notes=withContext(Dispatchers.IO){store.load()} }
    fun persist(next:List<QuickNote>) {
        notes=next
        scope.launch{
            runCatching {withContext(Dispatchers.IO){store.save(next)}}
                .onFailure {status="No se pudo guardar la nota."}
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Notas rápidas",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Guardadas en el almacenamiento privado de este teléfono.")
        OutlinedTextField(value=filter,onValueChange={filter=it.take(80)},
            modifier=Modifier.fillMaxWidth(),label={Text("Buscar notas")},singleLine=true)
        OutlinedTextField(value=title,onValueChange={title=it.take(100)},
            modifier=Modifier.fillMaxWidth(),label={Text("Título")},singleLine=true)
        OutlinedTextField(value=body,onValueChange={body=it.take(20000)},
            modifier=Modifier.fillMaxWidth().padding(bottom=6.dp),
            label={Text("Escribe aquí tu nota")},minLines=4,maxLines=12)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Button(onClick={
                if(title.isNotBlank()||body.isNotBlank()) {
                    val id=opened?:UUID.randomUUID().toString()
                    val now=System.currentTimeMillis()
                    persist((notes.filterNot{it.id==id}+QuickNote(id,title,body,now))
                        .sortedByDescending{it.updated})
                    opened=null;title="";body="";status="Nota guardada."
                }
            }) {Text(if(opened==null)"Guardar nota" else "Actualizar nota")}
            OutlinedButton(onClick={opened=null;title="";body=""}){Text("Nueva")}
        }
        if(status.isNotBlank())Text(status,color=MaterialTheme.colorScheme.primary)
        Text("Tus notas (${notes.size})",style=MaterialTheme.typography.titleMedium)
        notes.filter {
            filter.isBlank()||it.title.contains(filter,true)||it.text.contains(filter,true)
        }.forEach { note->
            Card(Modifier.fillMaxWidth()){
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(note.title.ifBlank{"Nota sin título"},fontWeight=FontWeight.Bold)
                    Text(note.text.take(140),style=MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick={opened=note.id;title=note.title;body=note.text}){
                            Text("Editar")
                        }
                        TextButton(onClick={
                            persist(notes.filterNot{it.id==note.id})
                            if(opened==note.id){opened=null;title="";body=""}
                        }){Text("Eliminar")}
                    }
                }
            }
        }
        Text("Las notas no se sincronizan ni se envían a un servidor. Evita guardar contraseñas o datos sensibles aquí.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            style=MaterialTheme.typography.bodySmall)
    }
}
