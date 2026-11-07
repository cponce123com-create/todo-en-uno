package pe.todoenuno.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val BoardColor=Color(0xFF0C1729)
private val Cyan=Color(0xFF62E5EC)
private val Gold=Color(0xFFFFD56A)
private val Coral=Color(0xFFFF7587)

@Composable
fun GamesScreen(id:String) {
    when(id) {
        "snake"->SnakeGameScreen()
        "blocks"->BlockGameScreen()
        "shooter"->ShooterGameScreen()
        else -> Text("Juego no disponible")
    }
}

private fun DrawScope.grid(width:Int,height:Int):Pair<Float,Offset> {
    val cell=minOf(size.width/width,size.height/height)
    val origin=Offset((size.width-cell*width)/2,(size.height-cell*height)/2)
    for(x in 0..width)drawLine(Color.White.copy(alpha=.055f),origin+Offset(x*cell,0f),
        origin+Offset(x*cell,height*cell),1f)
    for(y in 0..height)drawLine(Color.White.copy(alpha=.055f),origin+Offset(0f,y*cell),
        origin+Offset(width*cell,y*cell),1f)
    return cell to origin
}

@Composable
private fun Board(modifier:Modifier,ui:GameUi,won:Boolean=false,onRestart:()->Unit,
                  gestures:Modifier=Modifier,draw:DrawScope.()->Unit) {
    Box(modifier.background(BoardColor,RoundedCornerShape(20.dp))
        .border(1.dp,if(ui.flash.value>0)Gold else Color(0xFF283E59),RoundedCornerShape(20.dp)),
        contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(6.dp).then(gestures)) {
            draw()
            if(ui.effects && ui.flash.value>0)drawRect(Gold.copy(alpha=ui.flash.value))
        }
        GameOverlay(ui,won,onRestart)
    }
}

/** Uses the dominant drag axis after a threshold; buttons remain available for accessibility. */
private fun Modifier.swipe(enabled:Boolean,onDirection:(Direction)->Unit):Modifier=pointerInput(enabled) {
    if(enabled) {
        var drag=Offset.Zero
        detectDragGestures(onDragStart={drag=Offset.Zero},onDragCancel={drag=Offset.Zero},
            onDragEnd={drag=Offset.Zero}) { change,amount ->
            change.consume()
            drag+=amount
            val threshold=28.dp.toPx()
            if(abs(drag.x)>=threshold || abs(drag.y)>=threshold) {
                onDirection(if(abs(drag.x)>abs(drag.y)) {
                    if(drag.x>0)Direction.RIGHT else Direction.LEFT
                } else if(drag.y>0)Direction.DOWN else Direction.UP)
                drag=Offset.Zero
            }
        }
    }
}

@Composable
private fun SnakeGameScreen() {
    var state by remember{mutableStateOf(SnakeEngine.start())}
    val ui=rememberGameUi("snake",state.score,state.lost||state.won)
    GameLoop(ui,150L){state=SnakeEngine.step(state)}
    val turn by rememberUpdatedState<(Direction)->Unit>({state=SnakeEngine.turn(state,it)})
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        GameHeader("Snake · Come y crece",state.score,ui){state=SnakeEngine.start()}
        Text("Desliza o usa las flechas. Evita las paredes y tu cuerpo.",style=MaterialTheme.typography.bodySmall)
        Board(Modifier.fillMaxWidth().weight(1f),ui,state.won,{state=SnakeEngine.start()},
            Modifier.swipe(ui.active){turn(it)}) {
            val (cell,origin)=grid(SnakeEngine.WIDTH,SnakeEngine.HEIGHT)
            val food=origin+Offset((state.food.x+.5f)*cell,(state.food.y+.5f)*cell)
            if(ui.effects)drawCircle(Coral.copy(alpha=.17f),cell*.7f,food)
            drawCircle(Coral,cell*.34f,food)
            drawCircle(Color.White.copy(alpha=.7f),cell*.09f,food-Offset(cell*.10f,cell*.10f))
            state.body.forEachIndexed { index,p ->
                val corner=origin+Offset(p.x*cell+1f,p.y*cell+1f)
                drawRoundRect(if(index==0)Cyan else Color(0xFF32AD94),corner,
                    Size((cell-2f).coerceAtLeast(1f),(cell-2f).coerceAtLeast(1f)),CornerRadius(cell*.25f))
                if(index==0) {
                    val center=origin+Offset((p.x+.5f)*cell,(p.y+.5f)*cell)
                    val forward=Offset(state.direction.dx*cell*.20f,state.direction.dy*cell*.20f)
                    val across=Offset(-state.direction.dy*cell*.20f,state.direction.dx*cell*.20f)
                    drawCircle(BoardColor,cell*.09f,center+forward+across)
                    drawCircle(BoardColor,cell*.09f,center+forward-across)
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center) {
            GameControl("↑","Snake: subir",ui.active){turn(Direction.UP)}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            GameControl("←","Snake: izquierda",ui.active,Modifier.weight(1f)){turn(Direction.LEFT)}
            GameControl("↓","Snake: bajar",ui.active,Modifier.weight(1f)){turn(Direction.DOWN)}
            GameControl("→","Snake: derecha",ui.active,Modifier.weight(1f)){turn(Direction.RIGHT)}
        }
    }
}

@Composable
private fun BlockGameScreen() {
    var state by remember{mutableStateOf(BlockEngine.start())}
    val ui=rememberGameUi("blocks",state.score,state.lost)
    GameLoop(ui,530L){state=BlockEngine.step(state)}
    val colors=remember{listOf(Cyan,Gold,Color(0xFFB99DFF),Color(0xFF6EE7A1),Coral,Color(0xFF73A8FF),Color(0xFFFFAC6A))}
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        GameHeader("Bloques · Completa las filas",state.score,ui){state=BlockEngine.start()}
        Text("${state.cleared} filas · La sombra muestra dónde caerá la pieza.",style=MaterialTheme.typography.bodySmall)
        Board(Modifier.fillMaxWidth().weight(1f),ui,onRestart={state=BlockEngine.start()}) {
            val (cell,origin)=grid(BlockEngine.WIDTH,BlockEngine.HEIGHT)
            fun tile(p:GridPoint,color:Color,ghost:Boolean=false) {
                if(p.x in 0 until BlockEngine.WIDTH && p.y in 0 until BlockEngine.HEIGHT) {
                    val corner=origin+Offset(p.x*cell+1f,p.y*cell+1f)
                    val extent=Size((cell-2f).coerceAtLeast(1f),(cell-2f).coerceAtLeast(1f))
                    drawRoundRect(color,corner,extent,CornerRadius(3f),style=if(ghost)Stroke(2f) else androidx.compose.ui.graphics.drawscope.Fill)
                    if(!ghost && ui.effects)drawLine(Color.White.copy(alpha=.4f),corner+Offset(2f,2f),
                        corner+Offset((cell-5f).coerceAtLeast(2f),2f),2f)
                }
            }
            state.board.forEachIndexed { index,value ->
                if(value>0)tile(GridPoint(index%BlockEngine.WIDTH,index/BlockEngine.WIDTH),colors[(value-1)%7])
            }
            BlockEngine.landing(state).forEach{tile(it,Color.White.copy(alpha=.35f),true)}
            BlockEngine.active(state).forEach{tile(it,colors[state.piece.type])}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            GameControl("←","Bloques: mover izquierda",ui.active,Modifier.weight(1f),85L){state=BlockEngine.shift(state,-1)}
            GameControl("↻","Bloques: girar pieza",ui.active,Modifier.weight(1f)){state=BlockEngine.rotate(state)}
            GameControl("→","Bloques: mover derecha",ui.active,Modifier.weight(1f),85L){state=BlockEngine.shift(state,1)}
            GameControl("↓","Bloques: bajar despacio",ui.active,Modifier.weight(1f),65L){state=BlockEngine.step(state)}
        }
        GameControl("⇓ Soltar pieza","Bloques: soltar pieza al fondo",ui.active,Modifier.fillMaxWidth()){
            state=BlockEngine.drop(state)
        }
        Text("Mantén ← → o ↓ para repetir. Toca ↻ para girar.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ShooterGameScreen() {
    var state by remember{mutableStateOf(ArcadeEngine.start())}
    val ui=rememberGameUi("shooter",state.score,state.lost,state.lives)
    GameLoop(ui,100L){state=ArcadeEngine.step(state)}
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        GameHeader("Nave · Defiende el espacio",state.score,ui){state=ArcadeEngine.start()}
        Text("Vidas ${"♥".repeat(state.lives)} · Mantén disparar y muévete a la vez.",style=MaterialTheme.typography.bodySmall)
        Board(Modifier.fillMaxWidth().weight(1f),ui,onRestart={state=ArcadeEngine.start()}) {
            val cell=minOf(size.width/ArcadeEngine.WIDTH,size.height/ArcadeEngine.HEIGHT)
            val origin=Offset((size.width-cell*ArcadeEngine.WIDTH)/2,(size.height-cell*ArcadeEngine.HEIGHT)/2)
            repeat(34) { i ->
                val x=((i*73+19)%997)/997f*size.width
                val y=((i*127+31)%991)/991f*size.height
                drawCircle(Color.White.copy(alpha=if(i%3==0).5f else .2f),if(i%4==0)2f else 1f,Offset(x,y))
            }
            val ship=origin+Offset((state.ship+.5f)*cell,(ArcadeEngine.HEIGHT-1.7f)*cell)
            if(ui.effects && ui.active)drawRoundRect(Gold.copy(alpha=.8f),ship+Offset(-cell*.14f,cell*.3f),
                Size(cell*.28f,cell*(if(state.frame%2==0).65f else .4f)),CornerRadius(cell*.1f))
            val path=Path().apply{
                moveTo(ship.x,ship.y-cell*.8f);lineTo(ship.x-cell*.48f,ship.y+cell*.35f)
                lineTo(ship.x,ship.y+cell*.13f);lineTo(ship.x+cell*.48f,ship.y+cell*.35f);close()
            }
            drawPath(path,Cyan)
            drawCircle(Color.White,cell*.10f,ship-Offset(0f,cell*.23f))
            state.shots.forEach {
                val center=origin+Offset((it.x+.5f)*cell,it.y*cell)
                if(ui.effects)drawLine(Gold.copy(alpha=.15f),center,center+Offset(0f,cell),cell*.4f)
                drawLine(Gold,center,center+Offset(0f,cell*.6f),cell*.13f)
            }
            state.enemies.forEach {
                val center=origin+Offset((it.x+.5f)*cell,(it.y+.5f)*cell)
                drawCircle(Coral,cell*.4f,center)
                drawCircle(Color(0xFFA13E60),cell*.12f,center+Offset(cell*.1f,-cell*.12f))
            }
            if(ui.effects)state.hits.forEach {
                val center=origin+Offset((it.x+.5f)*cell,(it.y+.5f)*cell)
                drawCircle(Gold,cell*.65f,center,style=Stroke(cell*.09f))
                repeat(8) { spark ->
                    val angle=spark*Math.PI/4
                    val vector=Offset(cos(angle).toFloat(),sin(angle).toFloat())
                    drawLine(Gold,center+vector*cell*.5f,center+vector*cell*.95f,cell*.09f)
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            GameControl("←","Nave: mover izquierda",ui.active,Modifier.weight(1f),65L){state=ArcadeEngine.move(state,-1)}
            GameControl("● Fuego","Nave: disparar",ui.active,Modifier.weight(1.6f),140L){state=ArcadeEngine.shoot(state)}
            GameControl("→","Nave: mover derecha",ui.active,Modifier.weight(1f),65L){state=ArcadeEngine.move(state,1)}
        }
        Text("Todos los juegos funcionan sin conexión.",style=MaterialTheme.typography.bodySmall)
    }
}
