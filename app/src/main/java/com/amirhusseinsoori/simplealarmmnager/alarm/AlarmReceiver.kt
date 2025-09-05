package com.amirhusseinsoori.simplealarmmnager.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.amirhusseinsoori.simplealarmmnager.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "alarm_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AlarmReceiver triggered")
        
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Alarm!"
        Log.d(TAG, "Alarm message: $message")
        
        // Acquire wake lock to ensure the device stays awake
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "MyApp::AlarmWakeLock"
        )
        
        try {
            wakeLock.acquire(60 * 1000L) // Hold for 60 seconds
            Log.d(TAG, "Wake lock acquired")
            
            // Create notification channel
            createNotificationChannel(context)
            
            // Create the alarm activity intent with proper flags
            val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                       Intent.FLAG_ACTIVITY_SINGLE_TOP or
                       Intent.FLAG_ACTIVITY_CLEAR_TOP or
                       Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or
                       Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                       Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("ALARM_MESSAGE", message)
            }
            
            // Create pending intent for the activity
            val activityPendingIntent = PendingIntent.getActivity(
                context,
                0,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Create high-priority notification with full-screen intent
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Alarm!")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(activityPendingIntent)
                .setFullScreenIntent(activityPendingIntent, true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setVibrate(longArrayOf(0, 1000, 500, 1000))
//                .setLights(0xFF0000FF, 1000, 1000)
                .build()
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            Log.d(TAG, "Notification created and shown")
            
            // Try to launch the activity directly - this might not work when app is destroyed
            try {
                context.startActivity(alarmIntent)
                Log.d(TAG, "Activity launched successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch activity directly: ${e.message}")
                // The notification with full-screen intent should still work
                Log.d(TAG, "Relying on full-screen intent notification")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in AlarmReceiver: ${e.message}")
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
                Log.d(TAG, "Wake lock released")
            }
        }
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
}