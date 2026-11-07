package pe.todoenuno.documents

enum class LabelTheme(val title:String) {
    SIMPLE("Minimalista"),COMMERCIAL("Comercial"),INVENTORY("Inventario")
}
data class LabelData(
    val title:String,
    val subtitle:String,
    val price:String,
    val code:String,
    val quantity:Int,
    val theme:LabelTheme,
)

object LabelLayout {
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    const val COLUMNS = 3
    const val ROWS = 8
    const val PER_PAGE = COLUMNS*ROWS
    const val MARGIN = 18f
    const val GAP = 7f
    val labelWidth: Float get() = (PAGE_WIDTH-2*MARGIN-GAP*(COLUMNS-1))/COLUMNS
    val labelHeight: Float get() = (PAGE_HEIGHT-2*MARGIN-GAP*(ROWS-1))/ROWS

    fun pages(quantity:Int):Int {
        require(quantity in 1..96) { "Cantidad permitida: entre 1 y 96 etiquetas." }
        return (quantity+PER_PAGE-1)/PER_PAGE
    }
    fun position(indexWithinPage:Int):Pair<Float,Float> {
        require(indexWithinPage in 0 until PER_PAGE)
        val row=indexWithinPage/COLUMNS
        val column=indexWithinPage%COLUMNS
        return Pair(MARGIN+column*(labelWidth+GAP),MARGIN+row*(labelHeight+GAP))
    }
}
