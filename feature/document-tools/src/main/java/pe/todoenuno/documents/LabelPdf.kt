package pe.todoenuno.documents

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.OutputStream
import kotlin.math.min

/** A4 a 72 ppp; 24 etiquetas por hoja, hasta cuatro hojas, exportación SAF. */
object LabelPdf {
    fun write(stream:OutputStream,entry:LabelData):Int {
        require(entry.title.trim().isNotBlank()) { "La etiqueta necesita un nombre." }
        val pages=LabelLayout.pages(entry.quantity)
        val pdf=PdfDocument()
        try {
            val qr=if(entry.code.isBlank())null else runCatching{qrBitmap(entry.code.take(200))}.getOrNull()
            try {
                for(pageIndex in 0 until pages) {
                    val info=PdfDocument.PageInfo.Builder(
                        LabelLayout.PAGE_WIDTH,LabelLayout.PAGE_HEIGHT,pageIndex+1
                    ).create()
                    val page=pdf.startPage(info)
                    val canvas=page.canvas
                    canvas.drawColor(Color.WHITE)
                    val start=pageIndex*LabelLayout.PER_PAGE
                    val amount=min(LabelLayout.PER_PAGE,entry.quantity-start)
                    repeat(amount) { index ->
                        val (x,y)=LabelLayout.position(index)
                        drawLabel(canvas,x,y,entry,qr)
                    }
                    pdf.finishPage(page)
                }
                pdf.writeTo(stream)
            } finally {qr?.recycle()}
        } finally {pdf.close()}
        return pages
    }

    private fun drawLabel(c:Canvas,x:Float,y:Float,e:LabelData,qr:Bitmap?) {
        val w=LabelLayout.labelWidth
        val h=LabelLayout.labelHeight
        val white=paint(Color.WHITE)
        val outline=paint(Color.LTGRAY).apply {style=Paint.Style.STROKE;strokeWidth=0.7f}
        c.drawRoundRect(RectF(x,y,x+w,y+h),6f,6f,white)
        c.drawRoundRect(RectF(x,y,x+w,y+h),6f,6f,outline)
        val ink=when(e.theme){LabelTheme.COMMERCIAL->Color.rgb(25,43,88);else->Color.rgb(21,33,45)}
        if(e.theme==LabelTheme.COMMERCIAL){
            c.drawRoundRect(RectF(x+1,y+1,x+w-1,y+20),5f,5f,paint(Color.rgb(30,61,121)))
        }
        val titleColor=if(e.theme==LabelTheme.COMMERCIAL)Color.WHITE else ink
        val titleTop=if(e.theme==LabelTheme.COMMERCIAL)y+15 else y+21
        text(c,e.title,x+10,titleTop,12f,titleColor,true,w-20f)
        val bodyTop=if(e.theme==LabelTheme.COMMERCIAL)y+38 else y+40
        text(c,e.subtitle,x+10,bodyTop,9f,ink,maxWidth=if(qr!=null)w-70f else w-20f)
        if(e.price.isNotBlank())text(c,e.price,x+10,bodyTop+18,13f,ink,true,if(qr!=null)w-70f else w-20f)
        if(e.code.isNotBlank())text(c,"Código: ${e.code}",x+10,y+h-12,8f,ink,maxWidth=w-20f)
        if(qr!=null){
            val side=43f
            c.drawBitmap(qr,null,RectF(x+w-side-8,y+25,x+w-8,y+25+side),paint(Color.WHITE))
        }
        if(e.theme==LabelTheme.INVENTORY){
            c.drawLine(x+8,y+h-22,x+w-8,y+h-22,paint(Color.LTGRAY))
        }
    }

    private fun text(
        canvas:Canvas, value:String, x:Float, y:Float, size:Float, color:Int,
        bold:Boolean=false, maxWidth:Float=160f
    ){
        val p=paint(color).apply {
            textSize=size
            typeface=if(bold)Typeface.create(Typeface.DEFAULT,Typeface.BOLD) else Typeface.DEFAULT
        }
        var shortened=value.replace("\\n"," ").trim()
        if(p.measureText(shortened)>maxWidth){
            while(shortened.isNotEmpty() && p.measureText(shortened+"…")>maxWidth)
                shortened=shortened.dropLast(1)
            shortened+="…"
        }
        canvas.drawText(shortened,x,y,p)
    }
    private fun paint(color:Int)=Paint(Paint.ANTI_ALIAS_FLAG).apply{this.color=color}
    private fun qrBitmap(text:String):Bitmap {
        val n=120
        val matrix=MultiFormatWriter().encode(text,BarcodeFormat.QR_CODE,n,n)
        val pixels=IntArray(n*n) { index ->
            if(matrix.get(index%n,index/n))Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels,n,n,Bitmap.Config.ARGB_8888)
    }
}
