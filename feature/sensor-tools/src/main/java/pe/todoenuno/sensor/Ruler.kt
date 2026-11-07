package pe.todoenuno.sensor

import android.content.Context
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Regla calibrada contra dimensión física; factor guardado por teléfono. */
@Composable
fun RulerScreen() {
    val context=LocalContext.current
    val prefs=remember(context){context.getSharedPreferences("ruler_calibration",Context.MODE_PRIVATE)}
    val xdpi=context.resources.displayMetrics.xdpi.takeIf{it>0}?:160f
    var factor by remember{mutableFloatStateOf(prefs.getFloat("factor",1f).coerceIn(0.6f,1.4f))}
    var inches by remember{mutableStateOf(false)}
    val pxPerUnit=xdpi*(if(inches)1f else 1f/2.54f)*factor
    val majorLabel=if(inches)"pulgada" else "centímetro"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Regla en pantalla",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Coloca el objeto junto al borde izquierdo en la marca cero.")
        androidx.compose.foundation.layout.Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            FilterChip(selected=!inches,onClick={inches=false},label={Text("cm / mm")})
            FilterChip(selected=inches,onClick={inches=true},label={Text("Pulgadas")})
        }
        Text("Calibración: ${(factor*100).roundToInt()}%. Ajusta usando una regla física conocida.")
        Slider(
            value=factor,
            onValueChange={factor=it},
            valueRange=0.6f..1.4f,
            onValueChangeFinished={prefs.edit().putFloat("factor",factor).apply()}
        )
        val palette=MaterialTheme.colorScheme
        Canvas(Modifier.fillMaxWidth().height(165.dp)) {
            drawRect(palette.surfaceVariant)
            val minor=if(inches)16f else 10f
            val maxTicks=(size.width/pxPerUnit*minor).toInt().coerceIn(0,600)
            val labelPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{
                color=palette.onSurfaceVariant.toArgb()
                textSize=26f
                isFakeBoldText=true
            }
            for(i in 0..maxTicks) {
                val x=i*pxPerUnit/minor
                val length=when {
                    i%minor.toInt()==0->92f
                    i%(minor/2).toInt()==0->63f
                    else->32f
                }
                drawLine(palette.onSurfaceVariant,Offset(x,10f),
                    Offset(x,10f+length),strokeWidth=2f)
                if(i%minor.toInt()==0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        (i/minor.toInt()).toString(),x+5f,137f,labelPaint
                    )
                }
            }
        }
        Text("Marcas pequeñas: ${if(inches)"1/16 pulgada" else "1 mm"}; marcas grandes: 1 $majorLabel.",
            style=MaterialTheme.typography.bodyMedium)
        Text("El valor se calcula usando densidad física (xdpi) y tu corrección guardada. Sin calibración externa no debe usarse para medidas de precisión.",
            style=MaterialTheme.typography.bodySmall,
            color=palette.onSurfaceVariant)
    }
}
