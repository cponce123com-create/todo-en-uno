package pe.todoenuno.education

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import java.util.Locale

enum class NotableTriangle(val label: String,val small:Double,val large:Double,val hyp:Double,val angles:String) {
    TRIANGLE_45("45°–45°–90°",1.0,1.0,sqrt(2.0),"45°, 45° y 90°"),
    TRIANGLE_30("30°–60°–90°",1.0,sqrt(3.0),2.0,"30°, 60° y 90°"),
    TRIANGLE_345("3–4–5",3.0,4.0,5.0,"≈36.87°, ≈53.13° y 90°"),
}
enum class KnownSide(val label:String){SMALL("Cateto menor"),LARGE("Cateto mayor"),HYP("Hipotenusa")}
data class TriangleSolution(val small:Double,val large:Double,val hyp:Double)

object TriangleMath {
    fun solve(type:NotableTriangle,side:KnownSide,length:Double):TriangleSolution {
        require(length>0.0 && length.isFinite()) { "Introduce una longitud positiva." }
        val base=when(side){
            KnownSide.SMALL->type.small
            KnownSide.LARGE->type.large
            KnownSide.HYP->type.hyp
        }
        val factor=length/base
        return TriangleSolution(type.small*factor,type.large*factor,type.hyp*factor)
    }
}

@Composable
fun NotableTrianglesScreen() {
    var selected by rememberSaveable { mutableStateOf(NotableTriangle.TRIANGLE_30) }
    var side by rememberSaveable { mutableStateOf(KnownSide.SMALL) }
    var length by rememberSaveable { mutableStateOf("5") }
    val amount=length.replace(',','.').toDoubleOrNull()
    val solution=amount?.let { runCatching {TriangleMath.solve(selected,side,it)}.getOrNull() }
    val palette=MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Triángulos notables",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Calcula los lados de triángulos rectángulos frecuentes.")
        NotableTriangle.entries.forEach { type ->
            FilterChip(selected=selected==type,onClick={selected=type},
                label={Text(type.label)},modifier=Modifier.fillMaxWidth())
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Canvas(Modifier.fillMaxWidth().height(145.dp)) {
                    val p1=Offset(size.width*0.18f,size.height*0.86f)
                    val p2=Offset(size.width*0.18f,size.height*0.15f)
                    val p3=Offset(size.width*0.83f,size.height*0.86f)
                    drawLine(palette.primary,p1,p2,8f)
                    drawLine(palette.primary,p2,p3,8f)
                    drawLine(palette.primary,p3,p1,8f)
                    drawLine(palette.tertiary,Offset(p1.x,p1.y-22f),
                        Offset(p1.x+22f,p1.y-22f),4f)
                    drawLine(palette.tertiary,Offset(p1.x+22f,p1.y-22f),
                        Offset(p1.x+22f,p1.y),4f)
                }
                Text(selected.label,style=MaterialTheme.typography.titleLarge)
                Text("Ángulos: ${selected.angles}")
                Text("Razones: ${selected.small.format()}: ${selected.large.format()}: ${selected.hyp.format()}")
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()) {
                    KnownSide.entries.forEach { choice ->
                        FilterChip(
                            selected=choice==side,
                            onClick={side=choice},
                            label={Text(when(choice){
                                KnownSide.SMALL->"Menor"
                                KnownSide.LARGE->"Mayor"
                                KnownSide.HYP->"Hip."
                            })}
                        )
                    }
                }
                OutlinedTextField(
                    value=length,onValueChange={length=it.take(18)},
                    modifier=Modifier.fillMaxWidth(),
                    singleLine=true,label={Text("Longitud conocida (cm)")},
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal)
                )
                Text("Cateto menor: ${solution?.small?.format() ?: "—"} cm")
                Text("Cateto mayor: ${solution?.large?.format() ?: "—"} cm")
                Text("Hipotenusa: ${solution?.hyp?.format() ?: "—"} cm",
                    color=palette.primary,style=MaterialTheme.typography.titleLarge)
            }
        }
        Text("Las relaciones 45–45–90 y 30–60–90 son exactas. Los ángulos del triángulo 3–4–5 son aproximados.",
            style=MaterialTheme.typography.bodySmall,color=palette.onSurfaceVariant)
    }
}
private fun Double.format():String=String.format(Locale.ROOT,"%.3f",this).trimEnd('0').trimEnd('.')
