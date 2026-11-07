package pe.todoenuno.games

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {
    @Test fun snakeMovesAndRejectsReverse() {
        val s=SnakeEngine.start(Random(2))
        assertEquals(Direction.RIGHT,SnakeEngine.turn(s,Direction.LEFT).requested)
        val a=SnakeEngine.step(s,Random(2))
        assertEquals(s.body.first().x+1,a.body.first().x)
        assertEquals(s.body.size,a.body.size)
    }
    @Test fun snakeHitsWall() {
        val s=SnakeState(body=listOf(GridPoint(17,3)),food=GridPoint(0,0))
        assertTrue(SnakeEngine.step(s).lost)
    }
    @Test fun blocksGridRemainsConsistent() {
        var s=BlockEngine.start(Random(2))
        repeat(100){s=BlockEngine.drop(s,Random(it))}
        assertEquals(180,s.board.size)
        assertTrue(s.board.all{it in 0..7})
    }
    @Test fun shooterBoundaries() {
        var s=ArcadeEngine.start()
        repeat(30){s=ArcadeEngine.move(s,-1)}
        assertEquals(0,s.ship)
        assertEquals(1,ArcadeEngine.shoot(s).shots.size)
    }
}
