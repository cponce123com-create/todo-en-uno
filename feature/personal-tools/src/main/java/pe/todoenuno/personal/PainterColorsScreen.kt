package pe.todoenuno.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt

data class Pigment(val name:String,val rgb:Int)
object PainterPalette {
    val colors=listOf(
        Pigment("Rojo",0xFFE53935.toInt()),Pigment("Amarillo",0xFFFFD839.toInt()),
        Pigment("Azul",0xFF2254BC.toInt()),Pigment("Blanco",0xFFFFFFFF.toInt()),
        Pigment("Negro",0xFF222222.toInt()),Pigment("Verde",0xFF2A9D5A.toInt()),
        Pigment("Naranja",0xFFFF8C24.toInt()),Pigment("Violeta",0xFF873CB4.toInt()),
        Pigment("Cian",0xFF1AC5C9.toInt()),Pigment("Magenta",0xFFE42C91.toInt())
    )
    fun mix(a:Pigment,b:Pigment,ratio:Float):Int {
        val t=ratio.coerceIn(0f,1f)
        fun component(rgb:Int,shift:Int)=(rgb shr shift) and 0xFF
        // Es una mezcla de pantalla orientativa, no una reproducción de pigmentos reales.
        val r=(component(a.rgb,16)*(1-t)+component(b.rgb,16)*t).roundToInt()
        val g=(component(a.rgb,8)*(1-t)+component(b.rgb,8)*t).roundToInt()
        val blue=(component(a.rgb,0)*(1-t)+component(b.rgb,0)*t).roundToInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or blue
    }
    fun hex(value:Int)=String.format(Locale.ROOT,"#%06X",value and 0xFFFFFF)
}

@Composable
fun PainterColorsScreen() {
    var first by remember{mutableStateOf(PainterPalette.colors[0])}
    var second by remember{mutableStateOf(PainterPalette.colors[1])}
    var ratio by remember{mutableFloatStateOf(.5f)}
    val result=PainterPalette.mix(first,second,ratio)
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Tabla de mezcla de colores",style=MaterialTheme.typography.headlineSmall)
        Text("Elige dos colores para ver una simulación digital de la mezcla.")
        Text("Color A: ${first.name}",style=MaterialTheme.typography.titleMedium)
        PainterPalette.colors.chunked(5).forEach{row->
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                row.forEach{p->
                    FilterChip(selected=first==p,onClick={first=p},
                        label={Text(p.name,style=MaterialTheme.typography.labelSmall)})
                }
            }
        }
        Text("Color B: ${second.name}",style=MaterialTheme.typography.titleMedium)
        PainterPalette.colors.chunked(5).forEach{row->
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                row.forEach{p->
                    FilterChip(selected=second==p,onClick={second=p},
                        label={Text(p.name,style=MaterialTheme.typography.labelSmall)})
                }
            }
        }
        Text("Proporción de color B: ${(ratio*100).roundToInt()} %")
        Slider(value=ratio,onValueChange={ratio=it})
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(95.dp)
                .background(Color(first.rgb),RoundedCornerShape(16.dp)))
            Box(Modifier.weight(1f).height(95.dp)
                .background(Color(result),RoundedCornerShape(16.dp)))
            Box(Modifier.weight(1f).height(95.dp)
                .background(Color(second.rgb),RoundedCornerShape(16.dp)))
        }
        Text("Resultado digital: ${PainterPalette.hex(result)}",
            style=MaterialTheme.typography.titleLarge)
        Text("Atención, pintores: los pigmentos reales se mezclan de forma sustractiva y dependen de la marca, opacidad, soporte y luz. Esta vista usa interpolación RGB y no predice con precisión el color físico. Prueba primero en una muestra.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
