package pe.todoenuno.readers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BibleScreen() {
    val context=LocalContext.current
    val prefs=remember(context){context.getSharedPreferences("bible_progress",0)}
    var book by rememberSaveable{mutableIntStateOf(prefs.getInt("book",1).coerceIn(1,66))}
    var chapter by rememberSaveable{mutableIntStateOf(prefs.getInt("chapter",1))}
    var browse by rememberSaveable{mutableStateOf(false)}
    var verses by remember{mutableStateOf<Map<Int,List<ScriptureVerse>>>(emptyMap())}
    var error by remember{mutableStateOf("")}
    var font by remember{mutableFloatStateOf(18f)}
    LaunchedEffect(book){
        error=""
        verses=try{withContext(Dispatchers.IO){BibleLibrary.load(context,book)}}
        catch(e:Exception){error="No se pudieron abrir los textos incluidos de RV1909.";emptyMap()}
        if(verses.isNotEmpty() && chapter !in verses)chapter=verses.keys.first()
    }
    fun choose(newBook:Int,newChapter:Int) {
        book=newBook.coerceIn(1,66)
        chapter=newChapter.coerceAtLeast(1)
        prefs.edit().putInt("book",book).putInt("chapter",chapter).apply()
    }
    Column(Modifier.fillMaxSize().padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
        Text("Biblia Reina-Valera 1909",style=MaterialTheme.typography.headlineSmall,
            fontWeight=FontWeight.Bold)
        Text(BibleLibrary.ATTRIBUTION,style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick={browse=!browse},modifier=Modifier.fillMaxWidth()) {
            Text("${BibleLibrary.books[book-1]} · Capítulo $chapter ▾")
        }
        if(browse){
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                items(BibleLibrary.books.size){index ->
                    TextButton(onClick={choose(index+1,1);browse=false}){
                        Text("${index+1}. ${BibleLibrary.books[index]}")
                    }
                }
            }
        }else{
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                TextButton(onClick={choose(book,(chapter-1).coerceAtLeast(1))}){Text("‹ Cap.")}
                Text("Capítulo $chapter",modifier=Modifier.weight(1f).padding(top=12.dp))
                TextButton(onClick={choose(book,(chapter+1).coerceAtMost(verses.keys.maxOrNull()?:chapter))}){
                    Text("Cap. ›")
                }
            }
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
            Text("Tamaño del texto: ${font.toInt()}",style=MaterialTheme.typography.labelSmall)
            Slider(font,onValueChange={font=it},valueRange=14f..28f)
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
                items(verses[chapter].orEmpty(),key={it.number}) { v ->
                    Text("${v.number}  ${v.text}",fontSize=font.sp,lineHeight=(font+7).sp)
                }
            }
        }
    }
}
