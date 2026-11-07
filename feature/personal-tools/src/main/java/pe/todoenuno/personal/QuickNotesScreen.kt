package pe.todoenuno.personal

import android.content.Context
import android.util.AtomicFile
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
    companion object { private val lock=Any() }
    private val file get()=File(context.filesDir,"quick_notes.json")
    fun load():List<QuickNote> = synchronized(lock) {
        val atomic=AtomicFile(file)
        if(!file.exists() && !File(file.path+".bak").exists())return@synchronized emptyList()
        val arr=JSONArray(atomic.openRead().bufferedReader().use{it.readText()})
        (0 until arr.length()).map{index->
            val x=arr.getJSONObject(index)
            QuickNote(x.getString("id"),x.optString("title"),
                x.optString("text"),x.optLong("updated"))
        }
    }
    fun save(notes:List<QuickNote>) = synchronized(lock) {
        val arr=JSONArray()
        require(notes.size<=300){"Máximo 300 notas. Elimina una antes de crear otra."}
        notes.forEach { n->
            arr.put(JSONObject().put("id",n.id).put("title",n.title)
                .put("text",n.text).put("updated",n.updated))
        }
        val atomic=AtomicFile(file)
        val stream=atomic.startWrite()
        try {
            stream.write(arr.toString().toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch(t:Throwable) {
            atomic.failWrite(stream)
            throw t
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
    var busy by remember{mutableStateOf(true)}
    LaunchedEffect(store){
        try {
            notes=withContext(Dispatchers.IO){store.load()}
            busy=false
        } catch(e:Exception) {
            if(e is kotlinx.coroutines.CancellationException)throw e
            status="No se pudieron leer las notas. No se sobrescribirán los datos guardados."
        }
    }
    fun persist(next:List<QuickNote>,afterSave:()->Unit={}) {
        if(busy)return
        if(next.size>300){status="Máximo 300 notas. Elimina una antes de crear otra.";return}
        busy=true
        scope.launch{
            try {
                withContext(Dispatchers.IO){store.save(next)}
                notes=next
                status="Cambios guardados."
                afterSave()
            } catch(e:Exception) {
                if(e is kotlinx.coroutines.CancellationException)throw e
                status="No se pudo guardar la nota. Tu texto sigue disponible."
            } finally {busy=false}
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Notas rápidas",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Guardadas en el almacenamiento privado de este teléfono.")
        OutlinedTextField(value=filter,onValueChange={filter=it.take(80)},
            modifier=Modifier.fillMaxWidth(),label={Text("Buscar notas")},singleLine=true)
        OutlinedTextField(enabled=!busy,value=title,onValueChange={title=it.take(100)},
            modifier=Modifier.fillMaxWidth(),label={Text("Título")},singleLine=true)
        OutlinedTextField(enabled=!busy,value=body,onValueChange={body=it.take(20000)},
            modifier=Modifier.fillMaxWidth().padding(bottom=6.dp),
            label={Text("Escribe aquí tu nota")},minLines=4,maxLines=12)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Button(enabled=!busy,onClick={
                if(title.isNotBlank()||body.isNotBlank()) {
                    val id=opened?:UUID.randomUUID().toString()
                    val now=System.currentTimeMillis()
                    persist((notes.filterNot{it.id==id}+QuickNote(id,title,body,now))
                        .sortedByDescending{it.updated}) {
                        opened=null;title="";body=""
                    }
                }
            }) {Text(if(opened==null)"Guardar nota" else "Actualizar nota")}
            OutlinedButton(enabled=!busy,onClick={opened=null;title="";body=""}){Text("Nueva")}
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
                        TextButton(enabled=!busy,onClick={opened=note.id;title=note.title;body=note.text}){
                            Text("Editar")
                        }
                        TextButton(enabled=!busy,onClick={
                            persist(notes.filterNot{it.id==note.id}) {
                                if(opened==note.id){opened=null;title="";body=""}
                            }
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
