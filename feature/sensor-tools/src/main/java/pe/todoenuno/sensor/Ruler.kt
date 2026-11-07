package pe.todoenuno.sensor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun RulerScreen() {
    val context=LocalContext.current
    val xdpi=context.resources.displayMetrics.xdpi
    var correction by remember { mutableFloatStateOf(1f) }
    val pxCm=(xdpi/2.54f)*correction
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Regla en pantalla",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Mide colocando el objeto junto al borde de la pantalla. Máximo según el ancho disponible.")
        Text("Ajuste fino: ${(correction*100).roundToInt()}%")
        Slider(value=correction,onValueChange={correction=it},valueRange=0.80f..1.20f)
        Text("Calibra con una regla real o tarjeta de tamaño conocido.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
        val palette=MaterialTheme.colorScheme
        Canvas(Modifier.fillMaxWidth().height(135.dp)) {
            drawRect(palette.surfaceVariant)
            val usable=size.width
            val count=(usable/pxCm*10).toInt().coerceIn(0,300)
            for(i in 0..count){
                val x=i*pxCm/10f
                val length=when {i%10==0->70f;i%5==0->48f;else->25f}
                drawLine(palette.onSurfaceVariant,androidx.compose.ui.geometry.Offset(x,12f),
                    androidx.compose.ui.geometry.Offset(x,12f+length),2f)
            }
        }
        Text("Escala aproximada: el fabricante del teléfono puede informar una densidad física incorrecta.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
