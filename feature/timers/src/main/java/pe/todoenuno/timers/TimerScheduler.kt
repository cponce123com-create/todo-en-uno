package pe.todoenuno.timers

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Programación persistente mediante AlarmManager, sin un servicio perpetuo. */
object TimerScheduler {
    private const val PREFS="todo_timer_state"
    private const val ACTION="pe.todoenuno.TIMER_COMPLETE"
    internal const val EXTRA_MODE="timer_mode"
    private const val CHANNEL_ID="todo_timer_notifications"
    fun preferences(context:Context)=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
    fun deadline(context:Context,mode:String):Long=
        preferences(context).getLong("end_$mode",0L)
    fun remaining(context:Context,mode:String,default:Long):Long=
        preferences(context).getLong("remaining_$mode",default)

    private fun intent(context:Context,mode:String):PendingIntent {
        val code=if(mode=="pomodoro")2002 else 2001
        val message=Intent(context,TimerAlarmReceiver::class.java).apply {
            action=ACTION
            putExtra(EXTRA_MODE,mode)
        }
        return PendingIntent.getBroadcast(context,code,message,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun exactAllowed(context:Context):Boolean {
        if(Build.VERSION.SDK_INT<31)return true
        val alarm=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarm.canScheduleExactAlarms()
    }

    fun schedule(context:Context,mode:String,millis:Long):Boolean {
        require(mode in listOf("countdown","pomodoro") && millis>0)
        val alarm=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val end=System.currentTimeMillis()+millis
        val request=intent(context,mode)
        val exact=exactAllowed(context)
        if(exact) {
            try {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,end,request)
            } catch(e:SecurityException) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,end,request)
            }
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,end,request)
        }
        preferences(context).edit().putLong("end_$mode",end)
            .putLong("remaining_$mode",millis).apply()
        return exact
    }

    fun pause(context:Context,mode:String,remaining:Long){
        cancelAlarm(context,mode)
        preferences(context).edit().putLong("end_$mode",0)
            .putLong("remaining_$mode",remaining.coerceAtLeast(0)).apply()
    }

    fun clear(context:Context,mode:String){
        cancelAlarm(context,mode)
        preferences(context).edit().remove("end_$mode").remove("remaining_$mode").apply()
    }

    private fun cancelAlarm(context:Context,mode:String) {
        val alarm=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(intent(context,mode))
    }

    internal fun complete(context:Context,mode:String) {
        // Si hubo cancelación antes de recibir el evento, no emitir una alerta obsoleta.
        val end=deadline(context,mode)
        if(end<=0)return
        preferences(context).edit().remove("end_$mode").remove("remaining_$mode").apply()
        val notificationManager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID,"Temporizadores",NotificationManager.IMPORTANCE_HIGH
        ))
        if(Build.VERSION.SDK_INT>=33 &&
            ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)
                !=PackageManager.PERMISSION_GRANTED) return
        val open=context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending=open?.let {
            PendingIntent.getActivity(context,0,it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val title=if(mode=="pomodoro")"Fin del bloque Pomodoro" else "Temporizador terminado"
        val note=Notification.Builder(context,CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Todo en Uno: ya terminó la cuenta atrás.")
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(pending)
            .build()
        notificationManager.notify(if(mode=="pomodoro")2002 else 2001,note)
    }
}

class TimerAlarmReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent) {
        val mode=intent.getStringExtra(TimerScheduler.EXTRA_MODE) ?: return
        if(mode in listOf("countdown","pomodoro")) TimerScheduler.complete(context,mode)
    }
}
