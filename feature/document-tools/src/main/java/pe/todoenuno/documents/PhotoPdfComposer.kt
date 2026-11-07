package pe.todoenuno.documents

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

data class PhotoLayout(
    val uri:Uri,
    val zoom:Float=1f,
    val horizontal:Float=0f,
    val vertical:Float=0f,
    val rotation:Int=0,
)
enum class PhotoGrid(val title:String,val columns:Int,val rows:Int){
    FULL("Una por página",1,1),
    TWO("Dos por página",1,2),
    FOUR("Cuatro por página",2,2)
}
object PhotoPdfComposer {
    const val MAX_IMAGES=10
    private const val PAGE_W=595
    private const val PAGE_H=842
    fun pages(count:Int,grid:PhotoGrid):Int {
        require(count in 1..MAX_IMAGES)
        return (count+grid.columns*grid.rows-1)/(grid.columns*grid.rows)
    }
    fun compose(context:Context,images:List<PhotoLayout>,grid:PhotoGrid,out:OutputStream):Int {
        require(images.size in 1..MAX_IMAGES)
        val perPage=grid.columns*grid.rows
        val count=pages(images.size,grid)
        val pdf=PdfDocument()
        try{
            repeat(count){pageNumber->
                val page=pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W,PAGE_H,pageNumber+1).create())
                try {
                    val canvas=page.canvas
                    canvas.drawColor(android.graphics.Color.WHITE)
                    for(slot in 0 until perPage) {
                        val index=pageNumber*perPage+slot
                        if(index>=images.size)break
                        val image=images[index]
                        val margin=22f
                        val gap=10f
                        val width=(PAGE_W-2*margin-gap*(grid.columns-1))/grid.columns
                        val height=(PAGE_H-2*margin-gap*(grid.rows-1))/grid.rows
                        val col=slot%grid.columns
                        val row=slot/grid.columns
                        val rect=RectF(margin+col*(width+gap),margin+row*(height+gap),
                            margin+col*(width+gap)+width,margin+row*(height+gap)+height)
                        val bitmap=readSampled(context,image.uri)
                        try {draw(canvas,bitmap,rect,image)} finally {bitmap.recycle()}
                    }
                } finally {pdf.finishPage(page)}
            }
            pdf.writeTo(out)
        }finally{pdf.close()}
        return count
    }
    private fun readSampled(context:Context,uri:Uri):Bitmap {
        val resolver=context.contentResolver
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        val input=resolver.openInputStream(uri)?:error("No se pudo leer la fotografía")
        // Bounds-only decoding returns null even for a valid image.
        input.use{BitmapFactory.decodeStream(it,null,bounds)}
        require(bounds.outWidth>0 && bounds.outHeight>0){"Imagen no válida"}
        var sample=1
        while(max(bounds.outWidth,bounds.outHeight)/sample>2000)sample*=2
        val options=BitmapFactory.Options().apply{inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888}
        return resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it,null,options)
        }?:error("El archivo de imagen no es compatible")
    }
    private fun draw(c:Canvas,bmp:Bitmap,rect:RectF,model:PhotoLayout){
        val saved=c.save()
        try {
            c.clipRect(rect)
            val width=if(model.rotation%180==0)bmp.width.toFloat() else bmp.height.toFloat()
            val height=if(model.rotation%180==0)bmp.height.toFloat() else bmp.width.toFloat()
            val fit=min(rect.width()/width,rect.height()/height)
            val scale=fit*model.zoom.coerceIn(.5f,2.5f)
            val centerX=rect.centerX()+model.horizontal.coerceIn(-1f,1f)*rect.width()*.35f
            val centerY=rect.centerY()+model.vertical.coerceIn(-1f,1f)*rect.height()*.35f
            c.translate(centerX,centerY)
            c.rotate(model.rotation.toFloat())
            c.scale(scale,scale)
            c.drawBitmap(bmp,-bmp.width/2f,-bmp.height/2f,Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }finally{c.restoreToCount(saved)}
    }
}
