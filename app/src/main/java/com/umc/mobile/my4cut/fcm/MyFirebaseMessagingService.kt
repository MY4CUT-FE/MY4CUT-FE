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
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
import com.umc.mobile.my4cut.ui.home.HomeFragment

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

        // 푸시가 도착했음을 HomeFragment에 전달
        // 알림창에 시스템 알림이 남아있는지와 상관없이,
        // HomeFragment는 getUnreadStatus()로 서버의 읽음 상태를 다시 확인해서 on/off를 결정한다.
        sendBroadcast(
            Intent(HomeFragment.ACTION_NOTIFICATION_RECEIVED)
                .setPackage(packageName)
        )

        showNotification(title, body, data)
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        Log.d(TAG, "showNotification: start")
        Log.d(TAG, "Preparing notification intent")

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

        val intent = createNotificationIntent(data).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        Log.d(TAG, "PendingIntent target=${intent.component?.className}")
        Log.d(TAG, "Intent flags=${intent.flags}")

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

    private fun createNotificationIntent(
        data: Map<String, String>
    ): Intent {
        val type = data["type"]

        return when (type) {

            // 댓글 알림 → 해당 스페이스의 해당 사진
            "MEDIA_COMMENT" -> {
                val workspaceId = data["workspaceId"]?.toLongOrNull() ?: -1L
                val mediaId = data["mediaId"]?.toLongOrNull() ?: -1L

                Intent(this, MainActivity::class.java).apply {
                    putExtra("OPEN_SPACE_ID", workspaceId)
                    putExtra("OPEN_PHOTO_ID", mediaId)
                }
            }

            // 미디어 업로드 알림 → 해당 스페이스의 해당 사진
            "MEDIA_UPLOADED" -> {
                val workspaceId = data["workspaceId"]?.toLongOrNull() ?: -1L
                val mediaId = data["mediaId"]?.toLongOrNull() ?: -1L

                Intent(this, MainActivity::class.java).apply {
                    putExtra("OPEN_SPACE_ID", workspaceId)
                    putExtra("OPEN_PHOTO_ID", mediaId)
                }
            }

            // 워크스페이스 초대 수락 → 해당 스페이스
            "WORKSPACE_ACCEPTED" -> {
                val workspaceId = data["workspaceId"]?.toLongOrNull() ?: -1L

                Intent(this, MainActivity::class.java).apply {
                    putExtra("OPEN_SPACE_ID", workspaceId)
                }
            }

            // 친구 수락 → 리터치 스페이스 탭
            "FRIEND_ACCEPTED" -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra("NAVIGATE_TO_TAB", R.id.menu_retouch)
                }
            }

            // 수락/거절이 필요한 알림은 알림 목록으로
            "WORKSPACE_INVITE",
            "FRIEND_REQUEST" -> {
                Intent(this, NotificationActivity::class.java)
            }

            else -> {
                Intent(this, NotificationActivity::class.java)
            }
        }.apply {
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
    }
}