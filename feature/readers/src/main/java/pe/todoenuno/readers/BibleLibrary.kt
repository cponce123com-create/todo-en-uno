package pe.todoenuno.readers

import android.content.Context
import android.text.Html
import org.json.JSONArray

data class ScriptureVerse(val number:Int,val text:String)

object BibleLibrary {
    const val ATTRIBUTION="Reina-Valera 1909 · Dominio público (CC0) · fuente: BibleAquifer"
    val books=listOf(
        "Génesis","Éxodo","Levítico","Números","Deuteronomio","Josué","Jueces","Rut",
        "1 Samuel","2 Samuel","1 Reyes","2 Reyes","1 Crónicas","2 Crónicas","Esdras","Nehemías",
        "Ester","Job","Salmos","Proverbios","Eclesiastés","Cantares","Isaías","Jeremías",
        "Lamentaciones","Ezequiel","Daniel","Oseas","Joel","Amós","Abdías","Jonás",
        "Miqueas","Nahúm","Habacuc","Sofonías","Hageo","Zacarías","Malaquías",
        "Mateo","Marcos","Lucas","Juan","Hechos","Romanos","1 Corintios",
        "2 Corintios","Gálatas","Efesios","Filipenses","Colosenses","1 Tesalonicenses",
        "2 Tesalonicenses","1 Timoteo","2 Timoteo","Tito","Filemón","Hebreos",
        "Santiago","1 Pedro","2 Pedro","1 Juan","2 Juan","3 Juan","Judas","Apocalipsis"
    )
    fun parse(json:String):Map<Int,List<ScriptureVerse>> {
        val arr=JSONArray(json)
        val grouped=mutableMapOf<Int,MutableList<ScriptureVerse>>()
        for(i in 0 until arr.length()) {
            val line=arr.getJSONObject(i)
            val ref=line.optString("index_reference")
            if(ref.length!=8)continue
            val chapter=ref.substring(2,5).toIntOrNull()?:continue
            val verse=ref.substring(5,8).toIntOrNull()?:continue
            val html=line.optString("content")
            val text=Html.fromHtml(html,Html.FROM_HTML_MODE_COMPACT).toString()
                .replace(Regex("\\s+")," ").trim()
                .replace(Regex("^\\d+\\s+"),"")
            grouped.getOrPut(chapter){mutableListOf()}+=ScriptureVerse(verse,text)
        }
        return grouped.toSortedMap()
    }
    fun load(context:Context,book:Int):Map<Int,List<ScriptureVerse>> {
        require(book in 1..66)
        val filename="rv1909/${book.toString().padStart(2,'0')}.json"
        return context.assets.open(filename).bufferedReader().use{parse(it.readText())}
    }
}
