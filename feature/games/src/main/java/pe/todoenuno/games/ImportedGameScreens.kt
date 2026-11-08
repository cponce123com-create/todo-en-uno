package pe.todoenuno.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive

@Composable
private fun Credits(id:String,author:String) {
    var opened by remember{mutableStateOf(false)}
    val context=LocalContext.current
    val license=remember(id){context.assets.open("licenses/$id.txt").bufferedReader().use{it.readText()}}
    TextButton(onClick={opened=true}){Text("Créditos y licencia")}
    if(opened)AlertDialog(onDismissRequest={opened=false},title={Text(author)},
        text={Text(license,Modifier.heightIn(max=340.dp).verticalScroll(rememberScrollState()),
            style=MaterialTheme.typography.bodySmall)},
        confirmButton={TextButton(onClick={opened=false}){Text("Cerrar")}})
}

@Composable
private fun SquareBoard(modifier:Modifier,content:@Composable ColumnScope.()->Unit) {
    BoxWithConstraints(modifier,contentAlignment=Alignment.Center) {
        val side=minOf(maxWidth,maxHeight)
        Column(Modifier.size(side).background(MaterialTheme.colorScheme.surfaceContainerHighest,RoundedCornerShape(18.dp))
            .padding(5.dp),verticalArrangement=Arrangement.spacedBy(5.dp),content=content)
    }
}

@Composable
internal fun TileGameScreen() {
    val context=LocalContext.current
    val prefs=remember(context){context.getSharedPreferences("offline_puzzle_progress",0)}
    var state by remember{mutableStateOf(TileEngine.decode(prefs.getString("2048",null))?:TileEngine.start())}
    var previous by remember{mutableStateOf<TileState?>(null)}
    val ui=rememberGameUi("2048",state.score,state.won||state.lost)
    LaunchedEffect(state){prefs.edit().putString("2048",TileEngine.encode(state)).apply()}
    fun reset(){state=TileEngine.start();previous=null}
    fun move(direction:Direction) {
        if(!ui.active)return
        val next=TileEngine.move(state,direction)
        if(next!=state){previous=state;state=next}
    }
    val swipeAction by rememberUpdatedState<(Direction)->Unit>({move(it)})
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        GameHeader("2048 · Une los números",state.score,ui,onRestart={reset()})
        Text("Desliza o toca las flechas. Une números iguales hasta llegar a 2048.",style=MaterialTheme.typography.bodySmall)
        Box(Modifier.fillMaxWidth().weight(1f).swipe(ui.active){swipeAction(it)},contentAlignment=Alignment.Center) {
            SquareBoard(Modifier.fillMaxSize()) {
                repeat(4) { row ->
                    Row(Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        repeat(4) { column ->
                            val value=state.cells[row*4+column]
                            val bright=value>=128
                            val color=when(value) {
                                0->Color(0xFF253047);2->Color(0xFFEEE5DA);4->Color(0xFFE8D6BA)
                                8->Color(0xFFF5AE6A);16->Color(0xFFF18A59);32->Color(0xFFF56F59)
                                64->Color(0xFFE64F48);else->Color(0xFFD69E25)
                            }
                            Surface(Modifier.weight(1f).fillMaxHeight()
                                .semantics{contentDescription="Fila ${row+1}, columna ${column+1}: ${if(value==0)"vacío" else value}"},
                                color=color,shape=RoundedCornerShape(10.dp)) {
                                Box(contentAlignment=Alignment.Center){Text(if(value==0)"" else "$value",
                                    color=if(bright || value>=8)Color.White else Color(0xFF3C3540),
                                    fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)}
                            }
                        }
                    }
                }
            }
            GameOverlay(ui,state.won){reset()}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("←" to Direction.LEFT,"↑" to Direction.UP,"↓" to Direction.DOWN,"→" to Direction.RIGHT).forEach{(symbol,direction)->
                GameControl(symbol,"2048: ${when(direction){Direction.LEFT->"izquierda";Direction.RIGHT->"derecha";Direction.UP->"arriba";Direction.DOWN->"abajo"}}",
                    ui.active,Modifier.weight(1f)){move(direction)}
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(enabled=previous!=null,onClick={previous?.let{state=it;previous=null;ui.play()}}){Text("Deshacer")}
            Credits("2048","2048 · Gabriele Cirulli · MIT")
        }
    }
}

@Composable
internal fun FifteenGameScreen() {
    val context=LocalContext.current
    val prefs=remember(context){context.getSharedPreferences("offline_puzzle_progress",0)}
    var state by remember{mutableStateOf(FifteenEngine.decode(prefs.getString("fifteen",null))?:FifteenEngine.start())}
    var previous by remember{mutableStateOf<FifteenState?>(null)}
    val ui=rememberGameUi("fifteen",if(state.won)10000/state.moves.coerceAtLeast(1) else 0,state.won)
    LaunchedEffect(state){prefs.edit().putString("fifteen",FifteenEngine.encode(state)).apply()}
    fun reset(){state=FifteenEngine.start();previous=null}
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        GameHeader("Fifteen · Ordena del 1 al 15",state.moves,ui,metric="Movimientos",showRecord=false,onRestart={reset()})
        Text("Toca una ficha en la fila o columna del hueco para deslizarla. Las fichas disponibles están resaltadas.",style=MaterialTheme.typography.bodySmall)
        Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center) {
            SquareBoard(Modifier.fillMaxSize()) {
                repeat(4) { row ->
                    Row(Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        repeat(4) { column ->
                            val index=row*4+column
                            val value=state.cells[index]
                            if(value==0)Box(Modifier.weight(1f).fillMaxHeight().semantics{contentDescription="Hueco vacío"})
                            else GameControl("$value","Mover ficha $value",ui.active && FifteenEngine.movable(state,index),
                                Modifier.weight(1f).fillMaxHeight()) {
                                val next=FifteenEngine.slide(state,index)
                                if(next!=state){previous=state;state=next}
                            }
                        }
                    }
                }
            }
            GameOverlay(ui,true){reset()}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(enabled=previous!=null,onClick={previous?.let{state=it;previous=null;ui.play()}}){Text("Deshacer")}
            Credits("fifteen","Fifteen · Simon Tatham y colaboradores · MIT")
        }
    }
}

@Composable
internal fun BreakoutGameScreen() {
    var state by remember{mutableStateOf(BreakoutEngine.start())}
    val ui=rememberGameUi("breakout",state.score,state.won||state.lost,state.lives)
    LaunchedEffect(ui.active,state.launched,ui.epoch) {
        var previous=0L
        if(ui.active && state.launched)while(isActive) {
            withFrameNanos { now ->
                if(previous!=0L)state=BreakoutEngine.step(state,((now-previous)/1_000_000).coerceIn(1,48))
                previous=now
            }
        }
    }
    val movePaddle by rememberUpdatedState<(Float)->Unit>({state=BreakoutEngine.paddle(state,it)})
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        GameHeader("Breakout · Rompe los bloques",state.score,ui,onRestart={state=BreakoutEngine.start()})
        Text("Vidas ${state.lives} · Arrastra la pala o mantén las flechas. Toca Lanzar para sacar la bola.",style=MaterialTheme.typography.bodySmall)
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0C1729),RoundedCornerShape(18.dp))
            .border(1.dp,Color(0xFF294360),RoundedCornerShape(18.dp)),contentAlignment=Alignment.Center) {
            Canvas(Modifier.fillMaxSize().pointerInput(ui.active) {
                if(ui.active)detectDragGestures(onDragStart={ point ->
                    val scale=minOf(size.width/360f,size.height/520f)
                    if(scale>0)movePaddle((point.x-(size.width-360f*scale)/2)/scale)
                }) {change,_->
                    change.consume()
                    val scale=minOf(size.width/360f,size.height/520f)
                    if(scale>0)movePaddle((change.position.x-(size.width-360f*scale)/2)/scale)
                }
            }) {
                val scale=minOf(size.width/360f,size.height/520f)
                val origin=Offset((size.width-360f*scale)/2,(size.height-520f*scale)/2)
                fun point(x:Float,y:Float)=origin+Offset(x*scale,y*scale)
                val palette=listOf(Color(0xFFFF7587),Color(0xFFFFB56A),Color(0xFFFFD96A),Color(0xFF65DEB5),Color(0xFF70B5FA))
                state.bricks.forEach { id ->
                    drawRoundRect(palette[id/6],point(BreakoutEngine.brickLeft(id),BreakoutEngine.brickTop(id)),
                        Size(48f*scale,18f*scale),CornerRadius(4f*scale))
                }
                drawRoundRect(Color(0xFF62E5EC),point(state.paddle-38f,480f),Size(76f*scale,10f*scale),CornerRadius(5f*scale))
                if(ui.effects)drawCircle(Color.White.copy(alpha=.16f),10f*scale,point(state.ball.x,state.ball.y))
                drawCircle(Color.White,6f*scale,point(state.ball.x,state.ball.y))
                if(ui.effects)state.hit?.let{drawCircle(Color(0xFFFFD56A),15f*scale,point(it.x,it.y),style=Stroke(2f*scale))}
                if(ui.effects && ui.flash.value>0)drawRect(Color.White.copy(alpha=ui.flash.value))
            }
            GameOverlay(ui,state.won){state=BreakoutEngine.start()}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            GameControl("←","Breakout: mover pala izquierda",ui.active,Modifier.weight(1f),40L){state=BreakoutEngine.paddle(state,state.paddle-14f)}
            GameControl(if(state.launched)"En juego" else "Lanzar","Breakout: lanzar bola",ui.active && !state.launched,Modifier.weight(1.6f)){
                state=BreakoutEngine.launch(state)
            }
            GameControl("→","Breakout: mover pala derecha",ui.active,Modifier.weight(1f),40L){state=BreakoutEngine.paddle(state,state.paddle+14f)}
        }
        Credits("breakout","Breakout · Jake Gordon y colaboradores · MIT")
    }
}
