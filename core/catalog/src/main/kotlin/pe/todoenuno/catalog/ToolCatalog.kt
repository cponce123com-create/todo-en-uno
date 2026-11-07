package pe.todoenuno.catalog

enum class ToolCategory(val title: String) {
    RAPIDAS("Rápidas"),
    CALCULO("Cálculo"),
    TIEMPO("Tiempo")
}

data class ToolDefinition(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val category: ToolCategory,
)

object ToolCatalog {
    val tools = listOf(
        ToolDefinition("flashlight", "Linterna", "Ilumina al instante", "🔦", ToolCategory.RAPIDAS),
        ToolDefinition("calculator", "Calculadora", "Operaciones rápidas", "🧮", ToolCategory.CALCULO),
        ToolDefinition("converter", "Conversores", "Longitud, masa y temperatura", "🔄", ToolCategory.CALCULO),
        ToolDefinition("stopwatch", "Cronómetro", "Tiempo con precisión", "⏱️", ToolCategory.TIEMPO),
        ToolDefinition("countdown", "Temporizador", "Cuenta regresiva en pantalla", "⏳", ToolCategory.TIEMPO),
        ToolDefinition("worldclock", "Reloj mundial", "Hora en distintas ciudades", "🌍", ToolCategory.TIEMPO),
    )

    fun find(id: String): ToolDefinition? = tools.firstOrNull { it.id == id }
}
