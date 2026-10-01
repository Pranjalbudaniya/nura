package com.nura.messaging.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nura.messaging.domain.repositories.notification.NotificationService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NuraNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationService {

    companion object {
        private const val TAG = "NuraNotificationManager"
        private const val CHANNEL_ID = "nura_messages"
        private const val CHANNEL_NAME = "Messages"
        private const val CHANNEL_DESC = "Notifications for incoming messages"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    @Volatile
    private var currentActiveConversationId: String? = null

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun showMessageNotification(
        title: String,
        content: String,
        conversationId: String,
        senderId: String
    ) {
        if (currentActiveConversationId == conversationId) {
            Log.d(TAG, "Suppressed notification: chat $conversationId is currently active in foreground")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Cannot show notification: POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        val launchIntent = try {
            val intentClass = Class.forName("com.nura.messaging.MainActivity")
            Intent(context, intentClass).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve MainActivity for notification intent", e)
            null
        }

        val pendingIntent = launchIntent?.let { intent ->
            PendingIntent.getActivity(
                context,
                conversationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .apply {
                if (pendingIntent != null) {
                    setContentIntent(pendingIntent)
                }
            }
            .build()

        notificationManager?.notify(conversationId.hashCode(), notification)
        Log.d(TAG, "Dispatched notification for conversation $conversationId from $title")
    }

    override fun setActiveConversation(conversationId: String?) {
        currentActiveConversationId = conversationId
        if (conversationId != null) {
            cancelConversationNotifications(conversationId)
        }
    }

    override fun cancelConversationNotifications(conversationId: String) {
        notificationManager?.cancel(conversationId.hashCode())
    }
}
