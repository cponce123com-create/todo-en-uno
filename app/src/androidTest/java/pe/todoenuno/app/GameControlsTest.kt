package pe.todoenuno.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.todoenuno.games.GameControl
import pe.todoenuno.games.GamesScreen

@RunWith(AndroidJUnit4::class)
class GameControlsTest {
    @get:Rule val compose=createComposeRule()

    @Test fun touchDownActsBeforeFingerIsReleasedAndDoesNotDoubleFire() {
        var count=0
        compose.setContent {MaterialTheme {
            GameControl("↑","Subir",true,Modifier.testTag("control")){count++}
        }}
        compose.onNodeWithTag("control").performTouchInput{down(center)}
        compose.runOnIdle{assertEquals(1,count)}
        compose.onNodeWithTag("control").performTouchInput{up()}
        compose.runOnIdle{assertEquals(1,count)}
    }
    @Test fun holdingRepeatsAndReleaseStopsActions() {
        var count=0
        compose.setContent {MaterialTheme {
            GameControl("Fuego","Disparar",true,Modifier.testTag("control"),80L){count++}
        }}
        compose.onNodeWithTag("control").performTouchInput{down(center)}
        compose.waitUntil(2000){count>=3}
        compose.onNodeWithTag("control").performTouchInput{up()}
        var released=0
        compose.runOnIdle{released=count}
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle{assertEquals(released,count)}
    }
    @Test fun disablingControlCancelsHeldAction() {
        var count=0
        var enabled by mutableStateOf(true)
        compose.setContent {MaterialTheme {
            GameControl("→","Derecha",enabled,Modifier.testTag("control"),80L){count++}
        }}
        compose.onNodeWithTag("control").performTouchInput{down(center)}
        compose.waitUntil(2000){count>=2}
        compose.runOnIdle{enabled=false}
        compose.waitForIdle()
        var stopped=0
        compose.runOnIdle{stopped=count}
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle{assertEquals(stopped,count)}
        compose.onNodeWithTag("control").assertIsNotEnabled()
        compose.onNodeWithTag("control").performTouchInput{up()}
    }
    @Test fun movingAndFiringCanBeHeldTogether() {
        val moves=java.util.concurrent.atomic.AtomicInteger()
        val shots=java.util.concurrent.atomic.AtomicInteger()
        compose.setContent {MaterialTheme {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                GameControl("←","Mover",true,Modifier.testTag("move"),80L){moves.incrementAndGet()}
                GameControl("Fuego","Disparar",true,Modifier.testTag("fire"),100L){shots.incrementAndGet()}
            }
        }}
        val left=compose.onNodeWithTag("move").fetchSemanticsNode().boundsInRoot.center
        val right=compose.onNodeWithTag("fire").fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput {down(0,left);down(1,right)}
        compose.waitUntil(2000){moves.get()>=2 && shots.get()>=2}
        compose.onRoot().performTouchInput {up(0);up(1)}
    }
    @Test fun accessibilityClickActsOnceAndPausedGameDisablesControls() {
        compose.setContent {MaterialTheme {GamesScreen("shooter")}}
        compose.onNodeWithContentDescription("Nave: disparar").assertIsNotEnabled()
        compose.onNodeWithText("Empezar").performClick()
        compose.onNodeWithContentDescription("Nave: disparar").assertIsEnabled().performClick()
        compose.onNodeWithText("Pausa").performClick()
        compose.onNodeWithContentDescription("Nave: disparar").assertIsNotEnabled()
        compose.onNodeWithText("Continuar partida").assertExists()
    }
}
