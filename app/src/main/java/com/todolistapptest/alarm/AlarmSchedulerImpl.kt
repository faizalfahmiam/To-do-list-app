package com.mbexample.alarmmanager.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.todolistapptest.alarm.AlarmScheduler
import com.todolistapptest.models.Task
import com.todolistapptest.receiver.NotificationReceiver
import com.todolistapptest.utils.Constants.MESSAGE
import com.todolistapptest.utils.Constants.TASK_ID
import com.todolistapptest.utils.Constants.TITLE

class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(task: Task) {
        val minusTen = 10
        val millisTen = minusTen * 60 * 1000
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.hashCode(),
            Intent(context, NotificationReceiver::class.java).apply {
                putExtra(TITLE, "Reminding your task ${task.title} is $minusTen minutes left")
                putExtra(MESSAGE, task.description)
                putExtra(TASK_ID, task.id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.date.time.minus(millisTen),
                pendingIntent
            )
        }else{
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                task.date.time.minus(millisTen),
                pendingIntent
            )
        }
    }

    override fun cancel(task: Task) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                task.hashCode(),
                Intent(context, NotificationReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
    }

}