package pe.todoenuno.games

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

// Native adaptation of Jake Gordon's Breakout collision/paddle-steering mechanics.
// Copyright (c) 2011-2016 Jake Gordon and contributors. MIT: assets/licenses/breakout.txt.
// Original graphics, levels and synthesized audio; no upstream sound assets are bundled.
data class BallPoint(val x:Float,val y:Float)
data class BreakoutState(
    val paddle:Float=180f,val ball:BallPoint=BallPoint(180f,466f),
    val vx:Float=2.6f,val vy:Float=-4.1f,val launched:Boolean=false,
    val bricks:Set<Int> =(0 until 30).toSet(),val score:Int=0,val lives:Int=3,
    val hit:BallPoint?=null
) {
    val won get()=bricks.isEmpty()
    val lost get()=lives==0
}
object BreakoutEngine {
    const val WIDTH=360f
    const val HEIGHT=520f
    const val PADDLE_Y=480f
    const val HALF_PADDLE=38f
    const val RADIUS=6f
    fun start()=BreakoutState()
    fun paddle(s:BreakoutState,x:Float):BreakoutState {
        if(s.lost || s.won || !x.isFinite())return s
        val center=x.coerceIn(HALF_PADDLE,WIDTH-HALF_PADDLE)
        return s.copy(paddle=center,ball=if(s.launched)s.ball else BallPoint(center,466f))
    }
    fun launch(s:BreakoutState)=if(s.lost || s.won || s.launched)s else s.copy(launched=true)
    fun brickLeft(id:Int)=16f+(id%6)*56f
    fun brickTop(id:Int)=54f+(id/6)*27f
    fun step(s:BreakoutState,millis:Long=16L):BreakoutState {
        if(s.lost || s.won || !s.launched)return s
        val factor=millis.coerceIn(0,48)/16f
        val steps=ceil(maxOf(abs(s.vx),abs(s.vy))*factor/(RADIUS*.5f)).toInt().coerceAtLeast(1)
        var x=s.ball.x;var y=s.ball.y;var vx=s.vx;var vy=s.vy
        var bricks=s.bricks;var score=s.score;var hit:BallPoint?=null
        repeat(steps) {
            val oldX=x;val oldY=y
            x+=vx*factor/steps;y+=vy*factor/steps
            if(x<RADIUS){x=RADIUS;vx=abs(vx)}
            if(x>WIDTH-RADIUS){x=WIDTH-RADIUS;vx=-abs(vx)}
            if(y<RADIUS){y=RADIUS;vy=abs(vy)}
            if(vy>0 && oldY+RADIUS<=PADDLE_Y && y+RADIUS>=PADDLE_Y &&
                x+RADIUS>=s.paddle-HALF_PADDLE && x-RADIUS<=s.paddle+HALF_PADDLE) {
                val speed=sqrt(vx*vx+vy*vy)
                val offset=((x-s.paddle)/HALF_PADDLE).coerceIn(-.88f,.88f)
                vx=speed*offset
                vy=-sqrt((speed*speed-vx*vx).coerceAtLeast(1f))
                y=PADDLE_Y-RADIUS
            }
            val brick=bricks.firstOrNull { id ->
                val left=brickLeft(id);val top=brickTop(id)
                x+RADIUS>=left && x-RADIUS<=left+48f && y+RADIUS>=top && y-RADIUS<=top+18f
            }
            if(brick!=null) {
                val left=brickLeft(brick)
                if(oldX+RADIUS<=left || oldX-RADIUS>=left+48f)vx=-vx else vy=-vy
                hit=BallPoint(x,y)
                bricks=bricks-brick;score+=10
                if(bricks.isEmpty())return s.copy(ball=BallPoint(x,y),vx=vx,vy=vy,bricks=bricks,score=score,hit=hit)
            }
            if(y>HEIGHT+RADIUS) {
                return s.copy(ball=BallPoint(s.paddle,466f),vx=2.6f,vy=-4.1f,
                    launched=false,lives=(s.lives-1).coerceAtLeast(0),bricks=bricks,score=score,hit=null)
            }
        }
        return s.copy(ball=BallPoint(x,y),vx=vx,vy=vy,bricks=bricks,score=score,hit=hit)
    }
}
