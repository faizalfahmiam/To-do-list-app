package com.todolistapptest.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.todolistapptest.MainActivity
import com.todolistapptest.R
import com.todolistapptest.utils.Constants.MESSAGE
import com.todolistapptest.utils.Constants.TASK_CHANNEL_ID
import com.todolistapptest.utils.Constants.TASK_CHANNEL_NAME
import com.todolistapptest.utils.Constants.TASK_ID
import com.todolistapptest.utils.Constants.TITLE

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val title = intent?.getStringExtra(TITLE) ?: return
        val message = intent.getStringExtra(MESSAGE)
        val taskId = intent.getIntExtra(TASK_ID, 1)
        val channelId = TASK_CHANNEL_ID.toString()
        val channelName = TASK_CHANNEL_NAME
        val notificationBuilder = NotificationCompat.Builder(context!!, channelId)
        val intents = Intent(context, MainActivity::class.java)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intents,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder.setSmallIcon(getNotificationIcon(notificationBuilder))
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)

        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationBuilder.setChannelId(channelId)
            mNotificationManager.createNotificationChannel(channel)
        }
        val notifications = notificationBuilder.build()
        mNotificationManager.notify(taskId, notifications)
    }
    private fun getNotificationIcon(notificationBuilder: NotificationCompat.Builder): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val color = 0x008000
            notificationBuilder.color = color
            return R.drawable.ic_launcher_foreground
        }
        return R.drawable.ic_launcher_foreground
    }
}