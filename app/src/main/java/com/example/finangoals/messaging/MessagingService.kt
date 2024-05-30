package com.example.finangoals.messaging


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.finangoals.MainActivity
import com.example.finangoals.R
import com.example.finangoals.R.string.default_notification_channel_id
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MessagingService: FirebaseMessagingService(){
    private val random = Random

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        message.notification?.let { message ->
            Log.i("FCM Title","${message.title}" )
            Log.i("FCM Body","${message.body}" )
            sendNotification(message)
        }
    }

    private fun  sendNotification(message: RemoteMessage.Notification) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, FLAG_IMMUTABLE
        )
        val channelId = this.getString(default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_app_notif)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        manager.notify(random.nextInt(), notificationBuilder.build())
    }

    companion object {
        const val CHANNEL_NAME = "FCM notification channel"
    }

    // Funcionalidad no implementada
    override fun onNewToken(token: String) {
        Log.d("FCM TOKEN",  ": $token")
    }
}