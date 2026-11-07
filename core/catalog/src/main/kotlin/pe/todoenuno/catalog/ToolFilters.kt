package pe.todoenuno.catalog

/** Cálculo puro, reproducible con pruebas JVM y sin Android. */
enum class CatalogMode { HOME, ALL, FAVORITES }

object ToolFilters {
    fun visible(
        catalog: List<ToolDefinition>,
        mode: CatalogMode,
        query: String,
        category: String,
        favorites: Set<String>,
    ): List<ToolDefinition> {
        val search = query.trim()
        val filtered = catalog.filter { tool ->
            (mode != CatalogMode.FAVORITES || tool.id in favorites) &&
                (mode != CatalogMode.ALL || category == "Todas" || tool.category.name == category) &&
                (search.isBlank() || listOf(tool.title, tool.description, tool.category.title)
                    .any { it.contains(search, ignoreCase = true) })
        }
        return if (mode == CatalogMode.HOME && search.isEmpty())
            filtered.sortedWith(compareByDescending<ToolDefinition> { it.id in favorites }
                .thenBy { it.title }).take(9)
        else filtered
    }
}
