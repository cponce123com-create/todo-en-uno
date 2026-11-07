package pe.todoenuno.app

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas end-to-end del inicio real con DataStore y Navigation Compose.
 * Se ejecutan en emulador; no son solo capturas estáticas.
 */
@RunWith(AndroidJUnit4::class)
class HomeNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun toolGuideReturnsToToolBeforeCatalog() {
        compose.onNodeWithTag("tab_all").performClick()
        compose.onNodeWithTag("tool_search").performTextInput("calculadora")
        compose.onNodeWithTag("tool_calculator").performClick()
        compose.onNodeWithText("Información").performClick()
        compose.onNodeWithText("Información de la herramienta").assertExists()
        compose.onNodeWithText("‹ Volver a la herramienta").performClick()
        compose.onNodeWithText("Información").assertExists()
        compose.onNodeWithText("‹ Volver").performClick()
        compose.onNodeWithTag("tab_all").assertExists()
    }

    @Test fun favoriteCanBeAddedAndRemovedFromUi() {
        compose.onNodeWithTag("tab_all").performClick()
        compose.onNodeWithTag("tool_search").performTextInput("metrónomo")
        val add = "Añadir Metrónomo a favoritos"
        if (compose.onAllNodes(androidx.compose.ui.test.hasContentDescription(add))
                .fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription(add).performClick()
        }
        compose.onNodeWithTag("tab_favorites").performClick()
        compose.waitUntil(7_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasTestTag("tool_metronome"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("tool_metronome").assertExists()
        compose.onNodeWithContentDescription("Quitar Metrónomo de favoritos").performClick()
        compose.waitUntil(7_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasTestTag("tool_metronome"))
                .fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("tool_metronome").assertDoesNotExist()
    }
}
