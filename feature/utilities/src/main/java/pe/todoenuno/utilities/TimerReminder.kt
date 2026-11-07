package pe.todoenuno.utilities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Reminder uses WorkManager best-effort. Doze, battery optimisation and system policy
 * can delay delivery. This deliberately does NOT claim exact alarm timing.
 */
class TimerReminderWorker(context:Context,params:WorkerParameters):Worker(context,params) {
    override fun doWork():Result {
        val manager=applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel="timer_reminders"
        if(Build.VERSION.SDK_INT>=26)
            manager.createNotificationChannel(NotificationChannel(channel,"Avisos de temporizador",NotificationManager.IMPORTANCE_HIGH))
        if(Build.VERSION.SDK_INT>=33 &&
            ContextCompat.checkSelfPermission(applicationContext,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            return Result.success()
        val intent=applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val pending=intent?.let{
            PendingIntent.getActivity(applicationContext,0,it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val notification=NotificationCompat.Builder(applicationContext,channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Todo en Uno")
            .setContentText("El temporizador ha terminado.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply { if(pending!=null)setContentIntent(pending) }
            .build()
        manager.notify(9401,notification)
        return Result.success()
    }
}

object TimerReminders {
    private const val ID="todo_timer_notice"
    fun schedule(context:Context,remainingMillis:Long) {
        require(remainingMillis>0)
        val request=OneTimeWorkRequestBuilder<TimerReminderWorker>()
            .setInitialDelay(remainingMillis,TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ID,ExistingWorkPolicy.REPLACE,request)
    }
    fun cancel(context:Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ID)
    }
}
