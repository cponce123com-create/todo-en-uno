package pe.todoenuno.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolFiltersTest {
    @Test fun homeShowsNineMax() {
        val all = ToolFilters.visible(ToolCatalog.tools, CatalogMode.HOME, "", "Todas", emptySet())
        assertEquals(9, all.size)
    }
    @Test fun favoriteFirst() {
        val favorites = setOf("flashlight", "calculator")
        val result = ToolFilters.visible(ToolCatalog.tools, CatalogMode.HOME, "", "Todas", favorites)
        assertTrue(result.take(2).all { it.id in favorites })
    }
    @Test fun filtersByCategoryAndSearch() {
        val result = ToolFilters.visible(
            ToolCatalog.tools, CatalogMode.ALL, "calcul", "CALCULO", emptySet()
        )
        assertTrue(result.any { it.id == "calculator" })
        assertTrue(result.all { it.category == ToolCategory.CALCULO })
    }
    @Test fun favoritesAreIsolated() {
        val result = ToolFilters.visible(
            ToolCatalog.tools, CatalogMode.FAVORITES, "", "Todas", setOf("flashlight")
        )
        assertEquals(listOf("flashlight"), result.map { it.id })
    }
}
