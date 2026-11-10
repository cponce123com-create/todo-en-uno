package pe.todoenuno.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.todoenuno.games.GamesScreen

@RunWith(AndroidJUnit4::class)
class GameLandscapeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shooterPlacesMovementAndFireOnOppositeSidesOfBoard() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(720.dp, 320.dp)) { GamesScreen("shooter") }
            }
        }
        val left = compose.onNodeWithContentDescription("Nave: mover izquierda").fetchSemanticsNode().boundsInRoot.center.x
        val board = compose.onNodeWithTag("game_board").fetchSemanticsNode().boundsInRoot.center.x
        val fire = compose.onNodeWithContentDescription("Nave: disparar").fetchSemanticsNode().boundsInRoot.center.x
        assertTrue(left < board && board < fire)
    }
}
