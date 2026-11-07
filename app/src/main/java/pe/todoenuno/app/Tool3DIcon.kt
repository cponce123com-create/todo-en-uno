package pe.todoenuno.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.todoenuno.catalog.ToolDefinition

/**
 * Insignia estilo 3D sin imágenes pesadas.
 * Capa posterior, bisel, gradiente y brillo dan profundidad, incluso sin conexión.
 * La imagen conceptual 3D del diseño se conserva fuera del paquete por ahora.
 */
@Composable
fun Tool3DIcon(tool: ToolDefinition) {
    val index = tool.id.sumOf(Char::code) % 5
    val colors = listOf(
        Color(0xFF007F83) to Color(0xFF00C6AC),
        Color(0xFF315D9E) to Color(0xFF87B4FF),
        Color(0xFFB65E48) to Color(0xFFF9B67B),
        Color(0xFF7554B1) to Color(0xFFBEA8F6),
        Color(0xFF306857) to Color(0xFF8BCD9F)
    )
    val (dark, light) = colors[index]
    Box(
        modifier = Modifier.size(62.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(52.dp)
                .offset(x = 2.dp, y = 5.dp)
                .background(dark.copy(alpha = .7f), RoundedCornerShape(17.dp))
        )
        Box(
            Modifier.size(54.dp)
                .graphicsLayer { rotationZ = -3f }
                .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = dark, spotColor = dark)
                .background(
                    Brush.linearGradient(listOf(light, dark, dark.copy(alpha = .95f))),
                    RoundedCornerShape(18.dp)
                )
                .border(
                    1.5.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = .85f), light.copy(alpha = .25f))),
                    RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.align(Alignment.TopStart).offset(x = 11.dp, y = 5.dp)
                    .width(28.dp).height(4.dp)
                    .background(Color.White.copy(alpha = .44f), RoundedCornerShape(8.dp))
            )
            Text(tool.emoji, fontSize = 28.sp)
        }
    }
}
