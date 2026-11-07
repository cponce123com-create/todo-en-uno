package pe.todoenuno.timers

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun BackgroundTimerScreen(mode:String) {
    require(mode=="countdown" || mode=="pomodoro")
    val context=LocalContext.current
    val isPomodoro=mode=="pomodoro"
    var input by rememberSaveable(mode){mutableStateOf("5")}
    var focus by rememberSaveable(mode){mutableStateOf(true)}
    val default=if(isPomodoro)25*60_000L else 5*60_000L
    var deadline by remember(mode){mutableLongStateOf(TimerScheduler.deadline(context,mode))}
    var remaining by remember(mode){mutableLongStateOf(TimerScheduler.remaining(context,mode,default))}
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var info by remember {mutableStateOf<String?>(null)}
    var notifyRequested by remember {mutableStateOf(false)}
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){
        notifyRequested=true
        if(!it)info="Sin permiso de notificaciones no aparecerá el aviso de finalización."
    }
    LaunchedEffect(deadline) {
        while(deadline>0) {
            currentTime=System.currentTimeMillis()
            val left=(deadline-currentTime).coerceAtLeast(0)
            remaining=left
            if(left==0L) {
                deadline=0L
                info="Finalizó el tiempo. Puedes iniciar otra sesión."
                break
            }
            delay(250L)
        }
    }
    val shown=(if(deadline>0)(deadline-currentTime).coerceAtLeast(0) else remaining)
    val seconds=(shown+999L)/1000L
    val canExact=TimerScheduler.exactAllowed(context)
    fun startTimer() {
        val interval=if(isPomodoro) if(focus)25*60_000L else 5*60_000L
            else ((input.toLongOrNull()?:0L).coerceIn(1,999)*60_000L)
        val length=remaining.takeIf{it>0} ?: interval
        val exact=TimerScheduler.schedule(context,mode,length)
        deadline=TimerScheduler.deadline(context,mode)
        currentTime=System.currentTimeMillis()
        info=if(exact)"Alarma programada para la hora prevista."
            else "Alarma programada; Android puede demorarla para ahorrar batería."
    }
    Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.Center) {
        Text(if(isPomodoro)"Pomodoro" else "Temporizador",
            style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        if(isPomodoro)Text(if(focus)"Enfoque de 25 minutos" else "Descanso de 5 minutos",
            modifier=Modifier.padding(10.dp))
        Text(String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60),
            fontSize=54.sp,modifier=Modifier.padding(vertical=24.dp))
        if(!isPomodoro) {
            OutlinedTextField(value=input,onValueChange={
                input=it.filter(Char::isDigit).take(3)
                if(deadline==0L)remaining=(input.toLongOrNull()?:0L).coerceIn(0,999)*60_000L
            },modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=deadline==0L,
                label={Text("Minutos (1 a 999)")},
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),
            modifier=Modifier.padding(top=16.dp)) {
            Button(onClick={
                if(Build.VERSION.SDK_INT>=33 && !notifyRequested)
                    request.launch(Manifest.permission.POST_NOTIFICATIONS)
                if(deadline>0L) {
                    val left=(deadline-System.currentTimeMillis()).coerceAtLeast(0L)
                    TimerScheduler.pause(context,mode,left)
                    remaining=left;deadline=0
                    info="Pausado. No se enviará el aviso."
                } else startTimer()
            }){Text(if(deadline>0)"Pausar" else "Iniciar")}
            OutlinedButton(onClick={
                TimerScheduler.clear(context,mode)
                deadline=0
                remaining=if(isPomodoro) if(focus)25*60_000L else 5*60_000L
                    else ((input.toLongOrNull()?:0L).coerceIn(0,999)*60_000L)
                info="Temporizador reiniciado."
            }){Text("Reiniciar")}
        }
        if(isPomodoro) OutlinedButton(onClick={
            TimerScheduler.clear(context,mode)
            deadline=0;focus=!focus
            remaining=if(focus)25*60_000L else 5*60_000L
        },modifier=Modifier.padding(top=10.dp)) {Text("Cambiar a ${if(focus)"descanso" else "enfoque"}")}
        if(Build.VERSION.SDK_INT>=31 && !canExact) OutlinedButton(onClick={
            runCatching {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure {info="Configura alarmas y recordatorios desde ajustes."}
        },modifier=Modifier.padding(top=10.dp)) {Text("Permitir alarmas puntuales")}
        info?.let {Text(it,color=MaterialTheme.colorScheme.primary,
            style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=14.dp))}
        Text("La alarma puede avisar con la aplicación cerrada. Android requiere permiso para notificaciones y, para puntualidad exacta, acceso a alarmas. Tras reiniciar el teléfono necesitarás iniciar otra cuenta.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=18.dp))
    }
}
