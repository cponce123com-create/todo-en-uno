package pe.todoenuno.games

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameResponsivenessTest {
    @Test fun snakeBuffersTwoQuickTurnsInOrder() {
        val initial=SnakeState(listOf(GridPoint(8,8),GridPoint(7,8),GridPoint(6,8)),GridPoint(0,0))
        val queued=SnakeEngine.turn(SnakeEngine.turn(initial,Direction.UP),Direction.LEFT)
        val first=SnakeEngine.step(queued,Random(1))
        assertEquals(GridPoint(8,7),first.body.first())
        val second=SnakeEngine.step(first,Random(1))
        assertEquals(GridPoint(7,7),second.body.first())
        assertFalse(second.lost)
    }
    @Test fun snakeIgnoresReverseOfPendingTurn() {
        val queued=SnakeEngine.turn(SnakeEngine.turn(SnakeEngine.start(Random(1)),Direction.UP),Direction.DOWN)
        assertEquals(Direction.UP,queued.requested)
        assertNull(queued.queued)
    }
    @Test fun snakeInputQueueIsBoundedAndKeepsFirstTwoTurns() {
        var s=SnakeEngine.turn(SnakeEngine.start(Random(1)),Direction.UP)
        s=SnakeEngine.turn(s,Direction.LEFT)
        s=SnakeEngine.turn(s,Direction.RIGHT)
        assertEquals(Direction.LEFT,s.queued)
    }
    @Test fun rotatingLongPieceAtRightWallUsesKick() {
        val s=BlockState(List(180){0},FallingPiece(0,1,GridPoint(9,3)))
        val rotated=BlockEngine.rotate(s)
        assertNotEquals(s.piece.rotation,rotated.piece.rotation)
        assertTrue(BlockEngine.active(rotated).all{it.x in 0 until 10})
    }
    @Test fun ghostAgreesWithDropAndNeverChangesScore() {
        val s=BlockEngine.start(Random(3))
        val ghost=BlockEngine.landing(s)
        val dropped=BlockEngine.drop(s,Random(4))
        assertTrue(ghost.all{dropped.board[it.y*10+it.x]==s.piece.type+1})
        assertEquals(0,s.score)
        assertTrue(ghost.any{it.y==17})
    }
    @Test fun crossingShotAndEnemyCountAsHit() {
        val s=ArcadeState(ship=0,shots=listOf(GridPoint(7,6)),enemies=listOf(GridPoint(7,5)),frame=3)
        val result=ArcadeEngine.step(s,Random(1))
        assertEquals(10,result.score)
        assertTrue(result.shots.isEmpty())
        assertEquals(listOf(GridPoint(7,6)),result.hits)
    }
    @Test fun impactEffectsExpireOnNextTick() {
        val s=ArcadeState(frame=1,hits=listOf(GridPoint(2,2)))
        assertTrue(ArcadeEngine.step(s).hits.isEmpty())
    }
    @Test fun noMovementOrShotsAfterGameOver() {
        val s=ArcadeState(lost=true)
        assertEquals(s,ArcadeEngine.move(s,-1))
        assertEquals(s,ArcadeEngine.shoot(s))
    }
}
