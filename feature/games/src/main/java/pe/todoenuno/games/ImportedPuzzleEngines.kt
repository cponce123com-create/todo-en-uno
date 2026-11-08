package pe.todoenuno.games

import kotlin.random.Random
import kotlin.math.abs

// Kotlin adaptation of 2048's directed traversal / single merge rules.
// Copyright (c) 2014 Gabriele Cirulli. MIT: assets/licenses/2048.txt.
data class TileState(val cells:List<Int>,val score:Int=0) {
    val won get()=cells.any{it>=2048}
    val lost get()=!TileEngine.movesAvailable(cells)
}
object TileEngine {
    fun start(r:Random=Random.Default)=spawn(spawn(TileState(List(16){0}),r),r)
    private fun spawn(s:TileState,r:Random):TileState {
        val free=s.cells.indices.filter{s.cells[it]==0}
        if(free.isEmpty())return s
        val next=s.cells.toMutableList()
        next[free[r.nextInt(free.size)]]=if(r.nextDouble()<.9)2 else 4
        return s.copy(cells=next)
    }
    fun move(s:TileState,d:Direction,r:Random=Random.Default):TileState {
        if(s.won || s.lost)return s
        val next=s.cells.toMutableList()
        var added=0
        repeat(4) { line ->
            val indices=(0..3).map { offset -> when(d) {
                Direction.LEFT->line*4+offset
                Direction.RIGHT->line*4+3-offset
                Direction.UP->offset*4+line
                Direction.DOWN->(3-offset)*4+line
            }}
            val values=indices.map{s.cells[it]}.filter{it!=0}
            val merged=mutableListOf<Int>()
            var i=0
            while(i<values.size) {
                if(i+1<values.size && values[i]==values[i+1]) {
                    val value=values[i]*2
                    merged+=value;added+=value;i+=2
                } else {merged+=values[i];i++}
            }
            while(merged.size<4)merged+=0
            indices.forEachIndexed { at,index ->next[index]=merged[at]}
        }
        if(next==s.cells)return s
        return spawn(TileState(next,s.score+added),r)
    }
    fun movesAvailable(cells:List<Int>):Boolean {
        if(cells.any{it==0})return true
        return cells.indices.any { i ->
            (i%4<3 && cells[i]==cells[i+1]) || (i/4<3 && cells[i]==cells[i+4])
        }
    }
    fun encode(s:TileState)="${s.score};${s.cells.joinToString(",")}"
    fun decode(raw:String?):TileState?=runCatching {
        val parts=raw!!.split(';');require(parts.size==2)
        val score=parts[0].toInt();require(score>=0)
        val cells=parts[1].split(',').map{it.toInt()}
        require(cells.size==16 && cells.all{it==0 || (it in 2..2048 && (it and (it-1))==0)})
        require(cells.count{it!=0}>=2)
        TileState(cells,score)
    }.getOrNull()
}

// Kotlin adaptation of Fifteen's aligned row/column sliding (interpret_move/execute_move).
// Simon Tatham and contributors. MIT: assets/licenses/fifteen.txt.
data class FifteenState(val cells:List<Int>,val moves:Int=0) {
    val won get()=cells==(1..15).toList()+0
}
object FifteenEngine {
    fun solved()=FifteenState((1..15).toList()+0)
    fun start(r:Random=Random.Default):FifteenState {
        // Legal random walk guarantees solvability without retrying arbitrary permutations.
        var s=solved()
        var previous=-1
        repeat(240) {
            val blank=s.cells.indexOf(0)
            val candidates=s.cells.indices.filter{
                abs(it%4-blank%4)+abs(it/4-blank/4)==1 && it!=previous
            }
            val target=candidates[r.nextInt(candidates.size)]
            s=slide(s,target,allowCompleted=true)
            previous=blank
        }
        if(s.won)s=slide(s,14,allowCompleted=true)
        return s.copy(moves=0)
    }
    fun movable(s:FifteenState,index:Int):Boolean {
        if(index !in 0..15 || s.won)return false
        val blank=s.cells.indexOf(0)
        return index!=blank && (index%4==blank%4 || index/4==blank/4)
    }
    fun slide(s:FifteenState,index:Int,allowCompleted:Boolean=false):FifteenState {
        if(index !in 0..15 || (s.won && !allowCompleted))return s
        val gap=s.cells.indexOf(0)
        if(index==gap || (gap%4!=index%4 && gap/4!=index/4))return s
        val step=if(index/4==gap/4){if(index>gap)1 else -1}else if(index>gap)4 else -4
        val next=s.cells.toMutableList()
        var at=gap
        var distance=0
        while(at!=index){next[at]=next[at+step];at+=step;distance++}
        next[index]=0
        return FifteenState(next,s.moves+distance)
    }
    fun encode(s:FifteenState)="${s.moves};${s.cells.joinToString(",")}"
    fun decode(raw:String?):FifteenState?=runCatching {
        val parts=raw!!.split(';');require(parts.size==2)
        val moves=parts[0].toInt();require(moves>=0)
        val cells=parts[1].split(',').map{it.toInt()}
        require(cells.sorted()==(0..15).toList())
        var inversions=0
        for(i in cells.indices)for(j in i+1 until cells.size)
            if(cells[i]!=0 && cells[j]!=0 && cells[i]>cells[j])inversions++
        val rowFromBottom=4-cells.indexOf(0)/4
        require((inversions+rowFromBottom)%2==1)
        FifteenState(cells,moves)
    }.getOrNull()
}
