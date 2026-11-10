package pe.todoenuno.games

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Executes on touch-down, with optional hold repeat; callbacks always see current state. */
@Composable
fun GameControl(label:String,description:String,enabled:Boolean,modifier:Modifier=Modifier,
                repeatMillis:Long?=null,onAction:()->Unit) {
    var pressed by remember{mutableStateOf(false)}
    val action by rememberUpdatedState(onAction)
    val scale by animateFloatAsState(if(pressed && enabled).94f else 1f,label="control-press")
    Surface(color=if(enabled)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor=if(enabled)MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape=RoundedCornerShape(18.dp),tonalElevation=if(pressed)0.dp else 3.dp,
        modifier=modifier.heightIn(min=58.dp).widthIn(min=64.dp).scale(scale)
            .semantics {
                role=Role.Button
                contentDescription=description
                if(!enabled)disabled()
                onClick { if(enabled){action();true}else false }
            }
            .onKeyEvent { event ->
                if(enabled && event.type==KeyEventType.KeyDown &&
                    event.key in listOf(Key.Enter,Key.Spacebar,Key.DirectionCenter)) {
                    action();true
                } else false
            }
            .focusable(enabled)
            .pointerInput(enabled,repeatMillis) {
                if(enabled)detectTapGestures(onPress={
                    coroutineScope {
                        pressed=true
                        action()
                        val repeating=repeatMillis?.let { interval -> launch {
                            delay(260)
                            while(isActive){action();delay(interval)}
                        } }
                        try {tryAwaitRelease()} finally {repeating?.cancel();pressed=false}
                    }
                })
            }) {
        Box(Modifier.padding(horizontal=12.dp,vertical=10.dp),contentAlignment=Alignment.Center) {
            Text(label,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
        }
    }
}

internal class GameUi {
    var paused by mutableStateOf(true)
    var started by mutableStateOf(false)
    var epoch by mutableIntStateOf(0)
    var sound by mutableStateOf(false)
    var effects by mutableStateOf(true)
    var best by mutableIntStateOf(0)
    var ended=false
    val active get()=started && !paused && !ended
    val flash=Animatable(0f)
    fun play(){started=true;paused=false}
    fun restart(action:()->Unit){action();epoch++;play()}
}

@Composable
internal fun rememberGameUi(id:String,score:Int,ended:Boolean,lives:Int=0):GameUi {
    val context=LocalContext.current
    val prefs=remember(context){context.getSharedPreferences("offline_games",Context.MODE_PRIVATE)}
    val ui=remember(id){GameUi().apply {
        best=prefs.getInt("best_$id",0)
        sound=prefs.getBoolean("sound",false)
        effects=prefs.getBoolean("effects",true)
    }}
    ui.ended=ended
    val owner=LocalLifecycleOwner.current
    val haptics=LocalHapticFeedback.current
    var tone by remember{mutableStateOf<ToneGenerator?>(null)}
    DisposableEffect(ui.sound) {
        val current=if(ui.sound)runCatching{ToneGenerator(AudioManager.STREAM_MUSIC,35)}.getOrNull() else null
        tone=current
        onDispose{current?.release();if(tone===current)tone=null}
    }
    DisposableEffect(owner,ui) {
        val observer=LifecycleEventObserver{_,event->
            if(event==Lifecycle.Event.ON_PAUSE){ui.paused=true;tone?.stopTone()}
        }
        owner.lifecycle.addObserver(observer)
        onDispose{owner.lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(ui.sound,ui.effects) {
        prefs.edit().putBoolean("sound",ui.sound).putBoolean("effects",ui.effects).apply()
        if(!ui.effects)ui.flash.snapTo(0f)
    }
    var previousScore by remember(id){mutableIntStateOf(score)}
    var previousLives by remember(id){mutableIntStateOf(lives)}
    LaunchedEffect(score,lives,ui.epoch) {
        val scored=score>previousScore
        val damaged=lives<previousLives
        previousScore=score;previousLives=lives
        if(score>ui.best){ui.best=score;prefs.edit().putInt("best_$id",score).apply()}
        if(scored || damaged) {
            if(ui.sound)runCatching{tone?.startTone(if(damaged)ToneGenerator.TONE_PROP_NACK else ToneGenerator.TONE_PROP_BEEP,65)}
            if(ui.effects) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                ui.flash.snapTo(.26f)
                ui.flash.animateTo(0f,tween(230))
            }
        }
    }
    return ui
}

@Composable
internal fun GameLoop(ui:GameUi,interval:Long,onTick:()->Unit) {
    val tick by rememberUpdatedState(onTick)
    LaunchedEffect(ui.active,ui.epoch,interval) {
        if(ui.active)while(isActive){delay(interval);tick()}
    }
}

@Composable
internal fun GameHeader(title:String,score:Int,ui:GameUi,metric:String="Puntos",showRecord:Boolean=true,onRestart:()->Unit) {
    Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween) {
        Column {Text("$metric $score",fontWeight=FontWeight.Bold);if(showRecord)Text("Récord ${ui.best}",style=MaterialTheme.typography.labelSmall)}
        TextButton(enabled=!ui.ended,onClick={if(ui.paused)ui.play() else ui.paused=true}) {
            Text(if(!ui.started)"Jugar" else if(ui.paused)"Continuar" else "Pausa")
        }
        TextButton(onClick={ui.restart(onRestart)}){Text("Reiniciar")}
    }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilterChip(selected=ui.sound,onClick={ui.sound=!ui.sound},label={Text("Sonido ${if(ui.sound)"sí" else "no"}")})
        FilterChip(selected=ui.effects,onClick={ui.effects=!ui.effects},label={Text("Efectos ${if(ui.effects)"sí" else "no"}")})
    }
}

@Composable
internal fun GameOverlay(ui:GameUi,won:Boolean=false,onRestart:()->Unit) {
    if(!ui.active) {
        Surface(color=Color(0xE6101930),shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(if(ui.ended){if(won)"¡Lo lograste!" else "Fin de la partida"}
                    else if(ui.started)"En pausa" else "¿Listo para jugar?",color=Color.White,
                    style=MaterialTheme.typography.titleLarge)
                Button(onClick={if(ui.ended)ui.restart(onRestart) else ui.play()}) {
                    Text(if(ui.ended)"Jugar otra vez" else if(ui.started)"Continuar partida" else "Empezar")
                }
            }
        }
    }
}

/** Keeps the board between thumb controls in landscape and retains a portrait fallback. */
@Composable
internal fun GameFrame(
    title:String, score:Int, ui:GameUi, onRestart:()->Unit,
    metric:String="Puntos", showRecord:Boolean=true,
    left:@Composable ColumnScope.()->Unit, right:@Composable ColumnScope.()->Unit,
    board:@Composable (Modifier)->Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
        if (maxWidth > maxHeight && maxWidth >= 500.dp) {
            Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(6.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.width(200.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally,content=left)
                board(Modifier.weight(1f).fillMaxHeight().testTag("game_board"))
                Column(Modifier.width(142.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    right()
                    Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    Text("$metric $score" + if(showRecord) " · Récord ${ui.best}" else "",style=MaterialTheme.typography.labelSmall)
                    TextButton(enabled=!ui.ended,onClick={if(ui.paused)ui.play() else ui.paused=true}) {
                        Text(if(!ui.started)"Jugar" else if(ui.paused)"Continuar" else "Pausa")
                    }
                    TextButton(onClick={ui.restart(onRestart)}){Text("Reiniciar")}
                    TextButton(onClick={ui.sound=!ui.sound}){Text("Sonido ${if(ui.sound) "sí" else "no"}")}
                    TextButton(onClick={ui.effects=!ui.effects}){Text("Efectos ${if(ui.effects) "sí" else "no"}")}
                }
            }
        } else {
            Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                GameHeader(title,score,ui,metric=metric,showRecord=showRecord,onRestart=onRestart)
                board(Modifier.fillMaxWidth().weight(1f).testTag("game_board"))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,
                    verticalAlignment=Alignment.CenterVertically) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,content=left)
                    Column(horizontalAlignment=Alignment.CenterHorizontally,content=right)
                }
            }
        }
    }
}

@Composable
internal fun DirectionPad(enabled:Boolean,game:String,onDirection:(Direction)->Unit) {
    GameControl("↑","$game: subir",enabled){onDirection(Direction.UP)}
    Row(horizontalArrangement=Arrangement.spacedBy(2.dp)) {
        GameControl("←","$game: izquierda",enabled){onDirection(Direction.LEFT)}
        GameControl("↓","$game: bajar",enabled){onDirection(Direction.DOWN)}
        GameControl("→","$game: derecha",enabled){onDirection(Direction.RIGHT)}
    }
}
