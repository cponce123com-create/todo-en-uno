package pe.todoenuno.app

import android.graphics.Canvas as NativeCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import pe.todoenuno.catalog.ToolDefinition

/**
 * Iconografía vectorial 3D ligera para todas las herramientas.
 * Cada símbolo se dibuja a mano y NO utiliza emojis dependientes del fabricante.
 * El fondo tiene espesor, bisel y brillo, como una ficha tridimensional.
 */
@Composable
fun Tool3DIcon(tool: ToolDefinition) {
    val swatches = listOf(
        0xFF148E90.toInt() to 0xFF08565F.toInt(),
        0xFF5486BF.toInt() to 0xFF2B467B.toInt(),
        0xFFE58C73.toInt() to 0xFFA64E56.toInt(),
        0xFF9E80CE.toInt() to 0xFF5A4688.toInt(),
        0xFF73B9A1.toInt() to 0xFF316D66.toInt()
    )
    val (light, dark) = swatches[tool.id.sumOf { it.code } % swatches.size]
    Canvas(Modifier.size(64.dp).clearAndSetSemantics {}) {
        val c=drawContext.canvas.nativeCanvas
        val saves=c.save()
        c.scale(size.width/100f,size.height/100f)
        renderTile(c,light,dark)
        renderGlyph(c,tool.id)
        c.restoreToCount(saves)
    }
}

private val ivory=0xFFFFF5DE.toInt()
private val navy=0xFF174450.toInt()
private val coral=0xFFFF8B70.toInt()
private val gold=0xFFFDD16F.toInt()
private val mint=0xFF9EF4DA.toInt()

private fun brush(color:Int,stroke:Float=0f):Paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{
    this.color=color
    style=if(stroke>0f)Paint.Style.STROKE else Paint.Style.FILL
    strokeWidth=stroke
    strokeCap=Paint.Cap.ROUND
    strokeJoin=Paint.Join.ROUND
}
private fun rr(c:NativeCanvas,l:Float,t:Float,r:Float,b:Float,rad:Float,color:Int) {
    c.drawRoundRect(RectF(l,t,r,b),rad,rad,brush(color))
}
private fun circle(c:NativeCanvas,x:Float,y:Float,rad:Float,color:Int) {
    c.drawCircle(x,y,rad,brush(color))
}
private fun line(c:NativeCanvas,x1:Float,y1:Float,x2:Float,y2:Float,color:Int,width:Float=5f) {
    c.drawLine(x1,y1,x2,y2,brush(color,width))
}
private fun oval(c:NativeCanvas,l:Float,t:Float,r:Float,b:Float,color:Int,stroke:Float=0f){
    c.drawOval(RectF(l,t,r,b),brush(color,stroke))
}
private fun text(c:NativeCanvas,token:String,x:Float,y:Float,size:Float=30f,color:Int=ivory) {
    val p=brush(color).apply {
        textSize=size
        textAlign=Paint.Align.CENTER
        typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD)
        setShadowLayer(2f,0f,2f,navy)
    }
    c.drawText(token,x,y,p)
}
private fun triangle(c:NativeCanvas,ax:Float,ay:Float,bx:Float,by:Float,cx:Float,cy:Float,color:Int){
    val path=Path().apply{moveTo(ax,ay);lineTo(bx,by);lineTo(cx,cy);close()}
    c.drawPath(path,brush(color))
}
private fun renderTile(c:NativeCanvas,light:Int,dark:Int) {
    rr(c,15f,16f,91f,95f,22f,0x660C2631)
    rr(c,10f,12f,90f,90f,21f,dark)
    val paint=brush(light).apply{
        shader=LinearGradient(11f,3f,88f,88f,light,dark,Shader.TileMode.CLAMP)
    }
    c.drawRoundRect(RectF(7f,4f,86f,84f),22f,22f,paint)
    c.drawRoundRect(RectF(9f,5f,84f,81f),21f,21f,brush(0x55FFFFFF,2f))
    line(c,23f,12f,62f,12f,0x55FFFFFF,4f)
}

private fun renderGlyph(c:NativeCanvas,id:String){
    when(id) {
        "flashlight" -> {
            val count=c.save();c.rotate(-34f,49f,47f)
            rr(c,38f,25f,60f,70f,7f,navy)
            rr(c,40f,30f,58f,64f,5f,ivory)
            rr(c,37f,22f,61f,34f,5f,coral)
            circle(c,49f,22f,11f,gold);circle(c,49f,22f,7f,ivory)
            rr(c,46f,43f,52f,50f,3f,navy)
            c.restoreToCount(count)
        }
        "calculator" -> {
            rr(c,27f,20f,72f,70f,9f,navy)
            rr(c,29f,22f,70f,67f,7f,ivory)
            rr(c,35f,27f,64f,38f,3f,0xFF72BCAB.toInt())
            for(x in 0..2)for(y in 0..2)rr(c,35f+x*11,43f+y*8,42f+x*11,49f+y*8,2f,
                if(x==2)coral else navy)
        }
        "converter" -> {text(c,"↔",48f,64f,57f)}
        "stopwatch","countdown","worldclock","pomodoro" -> {
            if(id=="pomodoro"){
                circle(c,49f,48f,23f,coral);circle(c,43f,43f,17f,0xFFFFAA83.toInt())
                triangle(c,41f,26f,51f,16f,56f,30f,mint);text(c,"25",49f,57f,19f,navy)
            } else if(id=="countdown"){
                line(c,34f,28f,63f,28f,ivory,5f);line(c,34f,69f,63f,69f,ivory,5f)
                triangle(c,36f,32f,62f,32f,49f,49f,gold)
                triangle(c,36f,65f,62f,65f,49f,50f,ivory)
            } else {
                circle(c,49f,48f,27f,navy);circle(c,49f,48f,23f,ivory)
                circle(c,49f,48f,19f,0xFF2E827B.toInt())
                line(c,49f,49f,49f,31f,mint,4f)
                line(c,49f,49f,63f,55f,coral,4f)
                circle(c,49f,49f,4f,gold)
                if(id=="worldclock") {
                    circle(c,49f,48f,20f,0xFF55AFB7.toInt())
                    line(c,49f,48f,49f,31f,ivory,3f)
                    line(c,49f,48f,62f,50f,coral,4f)
                    circle(c,49f,48f,4f,gold)
                }
            }
        }
        "audio" -> {
            c.drawArc(RectF(29f,24f,70f,70f),180f,180f,false,brush(ivory,8f))
            rr(c,24f,43f,38f,69f,7f,coral);rr(c,61f,43f,75f,69f,7f,coral)
            circle(c,50f,57f,12f,navy);text(c,"♫",51f,64f,22f,ivory)
        }
        "compass" -> {
            circle(c,49f,49f,27f,navy);circle(c,49f,49f,22f,ivory)
            triangle(c,49f,26f,42f,52f,56f,52f,coral)
            triangle(c,49f,72f,42f,52f,56f,52f,navy)
            circle(c,49f,49f,4f,gold)
        }
        "level" -> {
            rr(c,20f,35f,79f,64f,8f,navy)
            rr(c,25f,39f,74f,59f,7f,ivory)
            rr(c,29f,42f,70f,56f,5f,0xFFB5E984.toInt())
            line(c,42f,42f,42f,56f,navy,2f)
            line(c,57f,42f,57f,56f,navy,2f)
            circle(c,49f,49f,7f,mint)
        }
        "qr" -> {
            rr(c,26f,25f,72f,72f,6f,ivory)
            for ((x,y) in listOf(31f to 30f,56f to 30f,31f to 55f)){
                rr(c,x,y,x+13f,y+13f,2f,navy)
                rr(c,x+4f,y+4f,x+9f,y+9f,1f,ivory)
            }
            for ((x,y) in listOf(52f to 53f,63f to 58f,54f to 66f,65f to 66f))rr(c,x,y,x+5f,y+5f,1f,navy)
        }
        "password" -> {
            c.drawArc(RectF(35f,20f,63f,56f),180f,180f,false,brush(ivory,7f))
            rr(c,30f,43f,69f,72f,7f,navy)
            rr(c,33f,45f,66f,69f,5f,ivory)
            circle(c,49f,55f,4f,navy)
            line(c,49f,56f,49f,63f,navy,3f)
        }
        "recorder" -> {
            rr(c,41f,19f,57f,57f,8f,ivory)
            c.drawArc(RectF(33f,34f,66f,65f),0f,180f,false,brush(coral,6f))
            line(c,49f,65f,49f,75f,ivory,5f);line(c,39f,75f,60f,75f,ivory,5f)
        }
        "currency" -> {
            circle(c,38f,45f,19f,gold);circle(c,60f,58f,18f,ivory)
            text(c,"$",38f,53f,25f,navy)
            text(c,"S/",60f,65f,21f,navy)
        }
        "gps" -> {
            circle(c,49f,41f,19f,coral)
            triangle(c,31f,49f,67f,49f,49f,74f,coral)
            circle(c,49f,40f,8f,ivory)
        }
        "ruler" -> {
            val s=c.save();c.rotate(-28f,49f,49f)
            rr(c,21f,40f,79f,58f,5f,ivory)
            for(i in 0..10){
                val x=26f+i*4.7f
                line(c,x,42f,x,if(i%5==0)53f else 49f,navy,1.8f)
            }
            c.restoreToCount(s)
        }
        "text" -> {
            rr(c,28f,20f,71f,73f,6f,ivory)
            line(c,36f,34f,62f,34f,navy,4f)
            line(c,36f,44f,62f,44f,navy,3f)
            line(c,36f,54f,60f,54f,navy,3f)
            line(c,36f,63f,56f,63f,navy,3f)
        }
        "color" -> {
            circle(c,40f,40f,17f,coral)
            circle(c,61f,41f,17f,gold)
            circle(c,50f,58f,17f,mint)
            circle(c,50f,46f,7f,ivory)
        }
        "finance" -> {
            circle(c,49f,49f,26f,gold)
            circle(c,49f,49f,21f,ivory)
            text(c,"%",49f,60f,36f,navy)
        }
        "morse" -> {
            circle(c,28f,42f,7f,ivory)
            line(c,44f,42f,68f,42f,ivory,7f)
            circle(c,30f,61f,7f,ivory)
            line(c,48f,61f,71f,61f,ivory,7f)
        }
        "device" -> {
            rr(c,31f,18f,67f,77f,8f,ivory)
            rr(c,35f,24f,63f,66f,3f,navy)
            rr(c,40f,31f,57f,45f,3f,0xFF90CBAA.toInt())
            line(c,40f,52f,57f,52f,gold,3f)
            circle(c,49f,71f,3f,navy)
        }
        "metronome" -> {
            triangle(c,49f,22f,24f,71f,74f,71f,navy)
            triangle(c,49f,33f,32f,66f,66f,66f,ivory)
            line(c,50f,60f,65f,27f,coral,5f)
            circle(c,65f,27f,5f,gold)
        }
        "age" -> {
            rr(c,26f,25f,73f,72f,6f,ivory)
            rr(c,26f,25f,73f,38f,6f,coral)
            text(c,"31",49f,62f,26f,navy)
        }
        "bmi" -> {
            rr(c,23f,31f,74f,68f,8f,ivory)
            circle(c,49f,44f,12f,navy)
            triangle(c,49f,34f,44f,47f,55f,47f,coral)
            text(c,"KG",49f,63f,13f,navy)
        }
        "network" -> {
            c.drawArc(RectF(25f,20f,74f,66f),220f,100f,false,brush(ivory,6f))
            c.drawArc(RectF(34f,34f,65f,65f),220f,100f,false,brush(ivory,6f))
            circle(c,49f,65f,6f,gold)
        }
        "solar" -> {
            circle(c,37f,42f,18f,gold)
            circle(c,57f,51f,19f,navy)
            circle(c,65f,44f,19f,ivory)
            circle(c,60f,44f,18f,navy)
        }
        "magnetic" -> {
            c.drawArc(RectF(26f,23f,72f,69f),0f,180f,false,brush(ivory,13f))
            line(c,27f,49f,27f,64f,coral,13f)
            line(c,71f,49f,71f,64f,mint,13f)
        }
        "tuner" -> {
            rr(c,25f,28f,74f,67f,10f,ivory)
            circle(c,49f,46f,13f,navy)
            line(c,49f,46f,63f,37f,coral,4f)
            line(c,37f,60f,62f,60f,navy,4f)
        }
        "distance" -> {
            circle(c,28f,31f,9f,coral)
            triangle(c,19f,35f,37f,35f,28f,52f,coral)
            circle(c,69f,56f,9f,gold)
            triangle(c,60f,61f,78f,61f,69f,78f,gold)
            line(c,37f,47f,61f,58f,ivory,4f)
        }
        "periodic" -> {
            rr(c,25f,22f,72f,72f,8f,ivory)
            text(c,"Fe",49f,59f,29f,navy)
            text(c,"26",34f,34f,11f,coral)
        }
        "physics" -> {
            circle(c,49f,48f,7f,gold)
            c.drawOval(RectF(22f,35f,76f,62f),brush(ivory,4f))
            val sav=c.save();c.rotate(60f,49f,48f)
            c.drawOval(RectF(22f,35f,76f,62f),brush(coral,4f))
            c.restoreToCount(sav)
            val sav2=c.save();c.rotate(-60f,49f,48f)
            c.drawOval(RectF(22f,35f,76f,62f),brush(mint,4f))
            c.restoreToCount(sav2)
        }
        "triangles" -> {
            triangle(c,25f,72f,25f,26f,75f,72f,ivory)
            triangle(c,33f,63f,33f,42f,56f,63f,coral)
            line(c,33f,63f,43f,63f,navy,3f)
        }
        "labels" -> {
            rr(c,26f,22f,73f,73f,7f,ivory)
            rr(c,33f,32f,66f,51f,3f,navy)
            text(c,"PDF",49f,46f,13f,ivory)
            text(c,"QR",49f,65f,13f,coral)
        }
        "laserlevel" -> {
            rr(c,24f,30f,75f,69f,9f,navy)
            rr(c,28f,34f,71f,65f,6f,ivory)
            line(c,27f,50f,72f,50f,mint,4f)
            line(c,50f,28f,50f,72f,mint,3f)
            circle(c,50f,50f,5f,coral)
        }
        "snake" -> {
            rr(c,22f,38f,77f,64f,9f,0xFF266B37.toInt())
            for(i in 0..3)circle(c,30f+i*12f,50f,7f,mint)
            circle(c,71f,47f,4f,ivory)
        }
        "blocks" -> {
            rr(c,30f,28f,48f,46f,3f,coral)
            rr(c,49f,28f,67f,46f,3f,coral)
            rr(c,49f,47f,67f,65f,3f,gold)
            rr(c,30f,47f,48f,65f,3f,mint)
        }
        "shooter" -> {
            triangle(c,49f,19f,27f,67f,71f,67f,ivory)
            line(c,49f,32f,49f,72f,coral,5f)
            circle(c,49f,14f,5f,gold)
        }
        "photopdf" -> {
            rr(c,29f,21f,71f,75f,5f,ivory)
            rr(c,33f,27f,67f,53f,3f,mint)
            triangle(c,34f,53f,45f,39f,54f,53f,navy)
            triangle(c,44f,53f,55f,32f,68f,53f,coral)
            text(c,"PDF",49f,69f,11f,navy)
        }
        "earthquakes" -> {
            line(c,20f,53f,31f,53f,ivory,4f)
            line(c,31f,53f,37f,34f,coral,4f)
            line(c,37f,34f,44f,66f,coral,4f)
            line(c,44f,66f,54f,43f,coral,4f)
            line(c,54f,43f,65f,53f,ivory,4f)
        }
        "idvault" -> {
            rr(c,24f,29f,75f,68f,7f,ivory)
            circle(c,38f,45f,9f,navy)
            line(c,51f,40f,68f,40f,navy,3f)
            line(c,51f,49f,65f,49f,navy,3f)
        }
        "notes" -> {
            rr(c,27f,22f,72f,73f,5f,ivory)
            line(c,35f,38f,64f,38f,navy,3f)
            line(c,35f,49f,64f,49f,navy,3f)
            line(c,35f,60f,58f,60f,navy,3f)
        }
        "bible" -> {
            rr(c,27f,20f,73f,76f,5f,navy)
            rr(c,31f,23f,69f,72f,4f,ivory)
            line(c,50f,35f,50f,60f,coral,5f)
            line(c,39f,46f,61f,46f,coral,5f)
        }
        "paintmix" -> {
            circle(c,40f,42f,14f,coral)
            circle(c,61f,42f,14f,gold)
            circle(c,50f,58f,14f,mint)
        }
        "constitution" -> {
            rr(c,28f,25f,69f,72f,5f,ivory)
            text(c,"Art.",49f,48f,16f,navy)
            line(c,36f,57f,61f,57f,navy,3f)
        }
        "recipes" -> {
            rr(c,26f,28f,72f,73f,5f,ivory)
            rr(c,32f,35f,65f,42f,2f,coral)
            text(c,"🍴",49f,61f,25f,navy)
        }
        else -> text(c,id.take(2).uppercase(),49f,58f,25f)
    }
}
