package pe.todoenuno.games

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ImportedGamesTest {
    private fun row(vararg values:Int)=TileState(values.toList()+List(16-values.size){0})
    @Test fun tilesMergeOncePerMove() {
        val s=TileEngine.move(row(2,2,4,0),Direction.LEFT,Random(1))
        assertEquals(listOf(4,4),s.cells.take(2));assertEquals(4,s.score)
    }
    @Test fun fourEqualTilesCreateTwoMerges() {
        val s=TileEngine.move(row(2,2,2,2),Direction.LEFT,Random(2))
        assertEquals(listOf(4,4),s.cells.take(2));assertEquals(8,s.score)
    }
    @Test fun rightAndDownUseCorrectTraversalOrder() {
        assertEquals(listOf(4,4),TileEngine.move(row(2,2,4,0),Direction.RIGHT,Random(1)).cells.slice(2..3))
        val cells=MutableList(16){0}.apply{this[0]=2;this[4]=2;this[8]=4}
        val s=TileEngine.move(TileState(cells),Direction.DOWN,Random(2))
        assertEquals(4,s.cells[8]);assertEquals(4,s.cells[12])
    }
    @Test fun invalidTileMoveDoesNotSpawnOrScore() {
        val s=row(2,4,8,16)
        assertEquals(s,TileEngine.move(s,Direction.LEFT,Random(1)))
    }
    @Test fun tileLossRequiresNoGapsAndNoAdjacentMerge() {
        val s=TileState(listOf(2,4,2,4,4,2,4,2,2,4,2,4,4,2,4,2))
        assertTrue(s.lost);assertEquals(s,TileEngine.move(s,Direction.UP))
        assertFalse(s.copy(cells=s.cells.toMutableList().apply{this[0]=0}).lost)
    }
    @Test fun mergingTo2048Wins() {
        val s=TileEngine.move(row(1024,1024),Direction.LEFT,Random(3))
        assertTrue(s.won);assertEquals(2048,s.score)
    }
    @Test fun tileProgressRoundTripsAndRejectsCorruption() {
        val s=TileEngine.move(TileEngine.start(Random(2)),Direction.DOWN,Random(3))
        assertEquals(s,TileEngine.decode(TileEngine.encode(s)))
        assertNull(TileEngine.decode("0;3,5"));assertNull(TileEngine.decode(null))
        assertNull(TileEngine.decode("0;"+List(16){0}.joinToString(",")))
    }
    @Test fun shuffledFifteenBoardsAreSolvableAndNotCompleted() {
        repeat(40) {
            val s=FifteenEngine.start(Random(it))
            assertFalse(s.won);assertEquals(0,s.moves)
            assertEquals(s,FifteenEngine.decode(FifteenEngine.encode(s)))
        }
    }
    @Test fun slidingWholeRowMovesEveryTileAndCountsDistance() {
        val s=FifteenEngine.slide(FifteenEngine.solved(),12,allowCompleted=true)
        assertEquals(listOf(0,13,14,15),s.cells.takeLast(4));assertEquals(3,s.moves)
        val won=FifteenEngine.slide(s,15)
        assertTrue(won.won);assertEquals(6,won.moves)
    }
    @Test fun nonAlignedFifteenClickIsIgnored() {
        val s=FifteenEngine.slide(FifteenEngine.solved(),14,allowCompleted=true)
        assertEquals(s,FifteenEngine.slide(s,0));assertFalse(FifteenEngine.movable(s,0))
        assertEquals(s,FifteenEngine.slide(s,-1))
    }
    @Test fun unsolvableAndDuplicateFifteenProgressIsRejected() {
        val cells=FifteenEngine.solved().cells.toMutableList().apply{this[0]=2;this[1]=1}
        assertNull(FifteenEngine.decode("0;"+cells.joinToString(",")))
        assertNull(FifteenEngine.decode("0;"+List(16){1}.joinToString(",")))
    }
    @Test fun breakoutWaitsForLaunchAndPaddleKeepsBall() {
        val s=BreakoutEngine.paddle(BreakoutEngine.start(),100f)
        assertEquals(100f,s.ball.x,0f);assertEquals(s,BreakoutEngine.step(s))
        assertTrue(BreakoutEngine.launch(s).launched)
    }
    @Test fun breakoutPaddleStaysInsideCourt() {
        assertEquals(38f,BreakoutEngine.paddle(BreakoutEngine.start(),-100f).paddle,0f)
        assertEquals(322f,BreakoutEngine.paddle(BreakoutEngine.start(),1000f).paddle,0f)
    }
    @Test fun breakoutBrickHitAddsPointsAndRemovesBrick() {
        val s=BreakoutState(ball=BallPoint(40f,82f),vx=0f,vy=-4.1f,launched=true)
        val next=BreakoutEngine.step(s,32)
        assertEquals(10,next.score);assertFalse(0 in next.bricks);assertTrue(next.vy>0)
    }
    @Test fun breakoutLastBrickWins() {
        val s=BreakoutState(ball=BallPoint(40f,82f),vx=0f,vy=-4.1f,launched=true,bricks=setOf(0))
        val next=BreakoutEngine.step(s,32)
        assertTrue(next.won);assertEquals(next,BreakoutEngine.step(next))
    }
    @Test fun breakoutPaddleReflectsDownwardBall() {
        val s=BreakoutState(ball=BallPoint(180f,471f),vx=0f,vy=4f,launched=true)
        assertTrue(BreakoutEngine.step(s).vy<0)
    }
    @Test fun breakoutMissRemovesOnlyOneLifeAndRequiresNewLaunch() {
        val s=BreakoutState(ball=BallPoint(10f,525f),vx=0f,vy=4f,launched=true,lives=2)
        val next=BreakoutEngine.step(s)
        assertEquals(1,next.lives);assertFalse(next.launched)
        assertEquals(next,BreakoutEngine.step(next))
        assertTrue(BreakoutEngine.step(s.copy(lives=1)).lost)
    }
    @Test fun breakoutLongFrameIsCappedAndWallsReflect() {
        val s=BreakoutState(ball=BallPoint(7f,300f),vx=-4f,vy=1f,launched=true)
        assertEquals(BreakoutEngine.step(s,48),BreakoutEngine.step(s,9999))
        assertTrue(BreakoutEngine.step(s).vx>0)
    }
}
