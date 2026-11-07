package pe.todoenuno.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun GamesScreen(id:String) {
    when(id) {
        "snake"->SnakeGameScreen()
        "blocks"->BlockGameScreen()
        "shooter"->ShooterGameScreen()
        else -> Text("Juego no disponible")
    }
}

@Composable
private fun Header(name:String,score:Int,paused:Boolean,onPause:()->Unit,onRestart:()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(name,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically) {
            Text("Puntos: $score",style=MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick=onPause){ Text(if(paused)"Continuar" else "Pausa") }
            OutlinedButton(onClick=onRestart){Text("Reiniciar")}
        }
    }
}
@Composable
private fun GameButton(title:String,modifier:Modifier=Modifier,action:()->Unit) {
    Button(onClick=action,modifier=modifier){Text(title)}
}

@Composable
private fun SnakeGameScreen() {
    var state by remember{mutableStateOf(SnakeEngine.start())}
    var paused by remember{mutableStateOf(false)}
    LaunchedEffect(paused){
        while(!paused && !state.lost && !state.won){
            delay(165L)
            state=SnakeEngine.step(state)
        }
    }
    val scheme=MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Header("Snake clásico · Offline",state.score,paused,
            onPause={paused=!paused},onRestart={state=SnakeEngine.start();paused=false})
        Canvas(
            Modifier.fillMaxWidth().weight(1f)
                .background(scheme.surfaceContainerHighest,RoundedCornerShape(16.dp))
        ){
            val cell=minOf(size.width/SnakeEngine.WIDTH,size.height/SnakeEngine.HEIGHT)
            val origin=Offset((size.width-cell*SnakeEngine.WIDTH)/2,(size.height-cell*SnakeEngine.HEIGHT)/2)
            state.body.forEachIndexed{index,p->
                drawRoundRect(
                    color=if(index==0)scheme.primary else scheme.tertiary,
                    topLeft=origin+Offset(p.x*cell+1f,p.y*cell+1f),
                    size=Size((cell-2f).coerceAtLeast(1f),(cell-2f).coerceAtLeast(1f)),
                    cornerRadius=androidx.compose.ui.geometry.CornerRadius(cell*.2f)
                )
            }
            drawCircle(scheme.error,cell*.38f,
                origin+Offset((state.food.x+.5f)*cell,(state.food.y+.5f)*cell))
        }
        if(state.lost||state.won)Text(
            if(state.won)"¡Ganaste!" else "Fin del juego. Reinicia para intentarlo.",
            color=scheme.primary,style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center) {
            GameButton("↑"){state=SnakeEngine.turn(state,Direction.UP)}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
            GameButton("←"){state=SnakeEngine.turn(state,Direction.LEFT)}
            GameButton("↓"){state=SnakeEngine.turn(state,Direction.DOWN)}
            GameButton("→"){state=SnakeEngine.turn(state,Direction.RIGHT)}
        }
        Text("Controles táctiles · No requiere Internet",
            style=MaterialTheme.typography.bodySmall,color=scheme.onSurfaceVariant)
    }
}

@Composable
private fun BlockGameScreen() {
    var state by remember{mutableStateOf(BlockEngine.start())}
    var paused by remember{mutableStateOf(false)}
    LaunchedEffect(paused){
        while(!paused && !state.lost){
            delay(530L)
            state=BlockEngine.step(state)
        }
    }
    val scheme=MaterialTheme.colorScheme
    val colors=listOf(
        scheme.primary,scheme.tertiary,scheme.secondary,
        scheme.error,scheme.primaryContainer,scheme.tertiaryContainer,scheme.secondaryContainer
    )
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Header("Bloques clásicos · Offline",state.score,paused,
            onPause={paused=!paused},onRestart={state=BlockEngine.start();paused=false})
        Text("Filas completas: ${state.cleared}",style=MaterialTheme.typography.bodySmall)
        Canvas(Modifier.fillMaxWidth().weight(1f).background(
            scheme.surfaceContainerHighest,RoundedCornerShape(16.dp))
        ) {
            val cell=minOf(size.width/BlockEngine.WIDTH,size.height/BlockEngine.HEIGHT)
            val origin=Offset((size.width-cell*BlockEngine.WIDTH)/2,(size.height-cell*BlockEngine.HEIGHT)/2)
            fun drawCell(p:GridPoint,color:Color) {
                if(p.x in 0 until BlockEngine.WIDTH && p.y in 0 until BlockEngine.HEIGHT)
                    drawRoundRect(color,topLeft=origin+Offset(p.x*cell+1f,p.y*cell+1f),
                        size=Size(cell-2f,cell-2f),
                        cornerRadius=androidx.compose.ui.geometry.CornerRadius(3f))
            }
            state.board.forEachIndexed{index,value->
                if(value>0)drawCell(GridPoint(index%10,index/10),colors[(value-1)%colors.size])
            }
            BlockEngine.active(state).forEach{drawCell(it,colors[state.piece.type%colors.size])}
        }
        if(state.lost)Text("Fin del juego",color=scheme.error)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
            GameButton("←"){if(!paused)state=BlockEngine.shift(state,-1)}
            GameButton("⟳"){if(!paused)state=BlockEngine.rotate(state)}
            GameButton("→"){if(!paused)state=BlockEngine.shift(state,1)}
            GameButton("↓"){if(!paused)state=BlockEngine.step(state)}
        }
        GameButton("Soltar pieza",Modifier.fillMaxWidth()){
            if(!paused)state=BlockEngine.drop(state)
        }
    }
}

@Composable
private fun ShooterGameScreen() {
    var state by remember{mutableStateOf(ArcadeEngine.start())}
    var paused by remember{mutableStateOf(false)}
    LaunchedEffect(paused){
        while(!paused && !state.lost) {
            delay(110L)
            state=ArcadeEngine.step(state)
        }
    }
    val scheme=MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Header("Nave espacial · Offline",state.score,paused,
            onPause={paused=!paused},onRestart={state=ArcadeEngine.start();paused=false})
        Text("Vidas: ${state.lives}  ·  Destruye los objetivos y esquiva los meteoritos",
            style=MaterialTheme.typography.bodySmall)
        Canvas(Modifier.fillMaxWidth().weight(1f).background(
            Color(0xFF0A1530),RoundedCornerShape(16.dp))
        ){
            val cell=minOf(size.width/ArcadeEngine.WIDTH,size.height/ArcadeEngine.HEIGHT)
            val origin=Offset((size.width-cell*ArcadeEngine.WIDTH)/2,(size.height-cell*ArcadeEngine.HEIGHT)/2)
            val shipX=origin.x+(state.ship+.5f)*cell
            val shipY=origin.y+(ArcadeEngine.HEIGHT-1.7f)*cell
            val path=androidx.compose.ui.graphics.Path().apply{
                moveTo(shipX,shipY-cell*.8f)
                lineTo(shipX-cell*.48f,shipY+cell*.35f)
                lineTo(shipX+cell*.48f,shipY+cell*.35f)
                close()
            }
            drawPath(path,Color(0xFF66DFF5))
            state.shots.forEach {
                drawLine(Color(0xFFFFE36D),
                    origin+Offset((it.x+.5f)*cell,it.y*cell),
                    origin+Offset((it.x+.5f)*cell,(it.y+.6f)*cell),
                    strokeWidth=cell*.14f)
            }
            state.enemies.forEach {
                drawCircle(Color(0xFFFF766C),radius=cell*.41f,
                    center=origin+Offset((it.x+.5f)*cell,(it.y+.5f)*cell))
            }
        }
        if(state.lost)Text("Fin de la partida",color=scheme.error)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
            GameButton("←",Modifier.weight(1f)){if(!paused)state=ArcadeEngine.move(state,-1)}
            Spacer(Modifier.weight(.07f))
            GameButton("DISPARAR",Modifier.weight(1.7f)){if(!paused)state=ArcadeEngine.shoot(state)}
            Spacer(Modifier.weight(.07f))
            GameButton("→",Modifier.weight(1f)){if(!paused)state=ArcadeEngine.move(state,1)}
        }
        Text("Juego de nave con gráficos propios y botones táctiles",
            style=MaterialTheme.typography.bodySmall,color=scheme.onSurfaceVariant)
    }
}
