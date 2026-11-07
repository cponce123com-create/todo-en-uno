package pe.todoenuno.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pe.todoenuno.catalog.ToolCatalog

class ToolRoutingTest {
    @Test fun everyCatalogToolHasRoute() {
        ToolCatalog.tools.forEach { tool ->
            assertNotNull("Falta ruta: ${tool.id}", ToolRouting.domain(tool.id))
        }
        assertEquals(49, ToolCatalog.tools.size)
    }

    @Test fun invalidIdDoesNotCrashNavigation() {
        assertNull(ToolRouting.domain("fake_tool"))
    }
}
