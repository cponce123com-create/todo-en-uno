package pe.todoenuno.games

import kotlin.random.Random

data class GridPoint(val x:Int,val y:Int)
enum class Direction(val dx:Int,val dy:Int){
    UP(0,-1),DOWN(0,1),LEFT(-1,0),RIGHT(1,0)
}
data class SnakeState(
    val body:List<GridPoint>,val food:GridPoint,val direction:Direction=Direction.RIGHT,
    val requested:Direction=Direction.RIGHT,val score:Int=0,val lost:Boolean=false,
    val won:Boolean=false,val queued:Direction?=null
)
object SnakeEngine {
    const val WIDTH=18
    const val HEIGHT=22
    fun start(random:Random=Random.Default):SnakeState {
        val body=listOf(GridPoint(7,11),GridPoint(6,11),GridPoint(5,11))
        return SnakeState(body=body,food=newFood(body,random)!!)
    }
    private fun newFood(body:List<GridPoint>,r:Random):GridPoint? {
        val free=(0 until WIDTH*HEIGHT).asSequence()
            .map{GridPoint(it%WIDTH,it/WIDTH)}.filter{it !in body}.toList()
        return if(free.isEmpty())null else free[r.nextInt(free.size)]
    }
    fun turn(s:SnakeState,next:Direction):SnakeState {
        if(s.lost || s.won)return s
        val pending=s.requested!=s.direction
        val reference=if(pending)s.requested else s.direction
        val reverse=next.dx == -reference.dx && next.dy == -reference.dy
        if(reverse || next==reference)return s
        return if(!pending)s.copy(requested=next)
            else if(s.queued==null)s.copy(queued=next) else s
    }

    fun step(s:SnakeState,r:Random=Random.Default):SnakeState {
        if(s.lost||s.won)return s
        val next=GridPoint(s.body.first().x+s.requested.dx,s.body.first().y+s.requested.dy)
        val grows=next==s.food
        val used=if(grows)s.body else s.body.dropLast(1)
        if(next.x !in 0 until WIDTH || next.y !in 0 until HEIGHT || next in used)
            return s.copy(lost=true)
        val body=listOf(next)+s.body.take(if(grows)s.body.size else s.body.size-1)
        val food=if(grows)newFood(body,r) else s.food
        return s.copy(body=body,food=food?:next,direction=s.requested,
            score=s.score+if(grows)10 else 0,won=food==null,
            requested=s.queued?:s.requested,queued=null)
    }
}

data class FallingPiece(val type:Int,val rotation:Int,val origin:GridPoint)
data class BlockState(
    val board:List<Int>,val piece:FallingPiece,val score:Int=0,val cleared:Int=0,
    val lost:Boolean=false
)
object BlockEngine {
    const val WIDTH=10
    const val HEIGHT=18
    private val shapes=listOf(
        listOf(GridPoint(0,0),GridPoint(1,0),GridPoint(2,0),GridPoint(3,0)),
        listOf(GridPoint(0,0),GridPoint(1,0),GridPoint(0,1),GridPoint(1,1)),
        listOf(GridPoint(0,0),GridPoint(1,0),GridPoint(2,0),GridPoint(1,1)),
        listOf(GridPoint(0,0),GridPoint(1,0),GridPoint(1,1),GridPoint(2,1)),
        listOf(GridPoint(1,0),GridPoint(2,0),GridPoint(0,1),GridPoint(1,1)),
        listOf(GridPoint(0,0),GridPoint(0,1),GridPoint(1,1),GridPoint(2,1)),
        listOf(GridPoint(2,0),GridPoint(0,1),GridPoint(1,1),GridPoint(2,1)),
    )
    private fun cells(piece:FallingPiece):List<GridPoint> {
        var result=shapes[piece.type]
        repeat(piece.rotation%4) {
            result=result.map{GridPoint(-it.y,it.x)}
            val minX=result.minOf{it.x};val minY=result.minOf{it.y}
            result=result.map{GridPoint(it.x-minX,it.y-minY)}
        }
        return result.map{GridPoint(it.x+piece.origin.x,it.y+piece.origin.y)}
    }
    fun active(s:BlockState):List<GridPoint> = cells(s.piece)
    fun start(r:Random=Random.Default):BlockState =
        BlockState(List(WIDTH*HEIGHT){0},FallingPiece(r.nextInt(shapes.size),0,GridPoint(3,0)))
    private fun fits(s:BlockState,p:FallingPiece)=cells(p).all {
        it.x in 0 until WIDTH && it.y in 0 until HEIGHT && s.board[it.y*WIDTH+it.x]==0
    }
    fun shift(s:BlockState,delta:Int):BlockState {
        if(s.lost)return s
        val p=s.piece.copy(origin=s.piece.origin.copy(x=s.piece.origin.x+delta))
        return if(fits(s,p))s.copy(piece=p) else s
    }
    fun rotate(s:BlockState):BlockState {
        if(s.lost)return s
        val p=s.piece.copy(rotation=(s.piece.rotation+1)%4)
        // Small wall kicks let players rotate beside either wall.
        val kicked=listOf(0,-1,1,-2,2,-3,3).map { offset ->
            p.copy(origin=p.origin.copy(x=p.origin.x+offset))
        }.firstOrNull{fits(s,it)}
        return if(kicked!=null)s.copy(piece=kicked) else s
    }
    fun landing(s:BlockState):List<GridPoint> {
        var piece=s.piece
        repeat(HEIGHT) {
            val next=piece.copy(origin=piece.origin.copy(y=piece.origin.y+1))
            if(fits(s,next))piece=next
        }
        return cells(piece)
    }
    fun step(s:BlockState,r:Random=Random.Default):BlockState {
        if(s.lost)return s
        val below=s.piece.copy(origin=s.piece.origin.copy(y=s.piece.origin.y+1))
        if(fits(s,below))return s.copy(piece=below)
        val grid=s.board.toMutableList()
        cells(s.piece).forEach {
            if(it.x in 0 until WIDTH && it.y in 0 until HEIGHT)grid[it.y*WIDTH+it.x]=s.piece.type+1
        }
        val rows=(0 until HEIGHT).map {row->grid.subList(row*WIDTH,(row+1)*WIDTH).toList()}
        val kept=rows.filterNot{r0->r0.all{it!=0}}
        val cleared=HEIGHT-kept.size
        val board=List(cleared){List(WIDTH){0}}.flatten()+kept.flatten()
        val next=FallingPiece(r.nextInt(shapes.size),0,GridPoint(3,0))
        val candidate=BlockState(board,next,s.score+cleared*cleared*100,s.cleared+cleared)
        return if(fits(candidate,next))candidate else candidate.copy(lost=true)
    }
    fun drop(s:BlockState,r:Random=Random.Default):BlockState {
        if(s.lost)return s
        var moved=s
        repeat(HEIGHT){ val next=moved.piece.copy(origin=moved.piece.origin.copy(y=moved.piece.origin.y+1))
            if(fits(moved,next))moved=moved.copy(piece=next)
        }
        return step(moved,r)
    }
}

data class ArcadeState(
    val ship:Int=7,val shots:List<GridPoint> = emptyList(),
    val enemies:List<GridPoint> = emptyList(),val frame:Int=0,
    val score:Int=0,val lives:Int=3,val lost:Boolean=false,
    val hits:List<GridPoint> = emptyList()
)
object ArcadeEngine {
    const val WIDTH=15
    const val HEIGHT=20
    fun start()=ArcadeState()
    fun move(s:ArcadeState,delta:Int)=if(s.lost)s else s.copy(ship=(s.ship+delta).coerceIn(0,WIDTH-1))
    fun shoot(s:ArcadeState)=if(s.lost)s else s.copy(shots=(s.shots+GridPoint(s.ship,HEIGHT-2)).takeLast(8))
    fun step(s:ArcadeState,r:Random=Random.Default):ArcadeState {
        if(s.lost)return s
        val shots=s.shots.map{it.copy(y=it.y-1)}.filter{it.y>=0}.toMutableList()
        val enemies=s.enemies.map{it.copy(y=it.y+if(s.frame%3==0)1 else 0)}.toMutableList()
        if(s.frame%8==0)enemies+=GridPoint(r.nextInt(WIDTH),0)
        var score=s.score
        val hits=mutableListOf<GridPoint>()
        val remaining=enemies.filter {enemy->
            // Shots and enemies can cross between ticks without sharing a final cell.
            val enemyDelta=if(s.frame%3==0)1 else 0
            val hit=shots.indexOfFirst{
                it.x==enemy.x && it.y<=enemy.y && it.y+1>=enemy.y-enemyDelta
            }
            if(hit>=0){shots.removeAt(hit);score+=10;hits+=enemy;false}else true
        }
        val damage=remaining.count{it.y>=HEIGHT-2 && it.x==s.ship}+
            remaining.count{it.y>=HEIGHT}
        val alive=remaining.filter{it.y<HEIGHT-2 || it.x!=s.ship}.filter{it.y<HEIGHT}
        val lives=(s.lives-damage).coerceAtLeast(0)
        return s.copy(shots=shots,enemies=alive,score=score,lives=lives,
            lost=lives==0,frame=s.frame+1,hits=hits)
    }
}
