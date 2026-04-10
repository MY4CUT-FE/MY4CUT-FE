package com.umc.mobile.my4cut.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.ui.notification.NotificationActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val PREFS_NAME = "my4cut_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val TAG = "FCM_PUSH"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(TAG, "onNewToken: $token")

        saveFcmToken(token)
    }

    private fun saveFcmToken(token: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "onMessageReceived called")
        Log.d(TAG, "message.data=${message.data}")
        Log.d(TAG, "message.notification?.title=${message.notification?.title}")
        Log.d(TAG, "message.notification?.body=${message.notification?.body}")

        val data = message.data

        val title = data["title"]
            ?: message.notification?.title
            ?: "MY4CUT"
        val body = data["body"]
            ?: message.notification?.body
            ?: "새 알림이 도착했습니다."

        Log.d(TAG, "resolved title=$title")
        Log.d(TAG, "resolved body=$body")

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        Log.d(TAG, "showNotification: start")

        val channelId = "my4cut_push"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MY4CUT 알림",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created/updated: $channelId")
        }

        val intent = Intent(this, NotificationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        Log.d(TAG, "PendingIntent created for NotificationActivity")

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        try {
            NotificationManagerCompat.from(this)
                .notify(System.currentTimeMillis().toInt(), notification)
            Log.d(TAG, "Notification displayed")
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification display failed", e)
        }
    }
}