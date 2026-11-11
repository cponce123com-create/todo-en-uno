package pe.todoenuno.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.todoenuno.games.GamesScreen

@RunWith(AndroidJUnit4::class)
class ImportedGamesNavigationTest {
    @get:Rule val compose=createComposeRule()
    @Test fun tileGameStartsAndShowsBundledLicense() {
        compose.setContent{MaterialTheme{GamesScreen("2048")}}
        compose.onNodeWithText("Empezar").performClick()
        compose.onNodeWithContentDescription("2048: izquierda").assertIsEnabled().performClick()
        compose.onNodeWithText("Créditos y licencia").performClick()
        compose.onNodeWithText("2048 · Gabriele Cirulli · MIT").assertExists()
        compose.onNodeWithText("Cerrar").performClick()
    }
    @Test fun fifteenShowsInstructionsAndCredits() {
        compose.setContent{MaterialTheme{GamesScreen("fifteen")}}
        compose.onNodeWithText("Empezar").performClick()
        compose.onNodeWithText("Deshacer").assertIsNotEnabled()
        compose.onNodeWithText("Créditos y licencia").performClick()
        compose.onNodeWithText("Fifteen · Simon Tatham y colaboradores · MIT").assertExists()
    }
    @Test fun breakoutRequiresExplicitLaunchAndPausesControls() {
        compose.setContent{MaterialTheme{GamesScreen("breakout")}}
        compose.onNodeWithContentDescription("Breakout: lanzar bola").assertIsNotEnabled()
        compose.onNodeWithText("Empezar").performClick()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Breakout: lanzar bola").assertIsEnabled()
        compose.onNodeWithContentDescription("Breakout: lanzar bola").performSemanticsAction(SemanticsActions.OnClick) {
            check(it()) { "La acción de lanzamiento rechazó el clic" }
        }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("En juego").assertExists()
        compose.onNodeWithContentDescription("Breakout: lanzar bola").assertIsNotEnabled()
        compose.mainClock.autoAdvance = true
        compose.onNodeWithText("Pausa").performClick()
        compose.onNodeWithContentDescription("Breakout: mover pala izquierda").assertIsNotEnabled()
    }
}
