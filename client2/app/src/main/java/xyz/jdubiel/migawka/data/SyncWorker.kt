package xyz.jdubiel.migawka.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import xyz.jdubiel.migawka.GetMediaItemRequest
import xyz.jdubiel.migawka.MigawkaApplication

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val grpcProvider = (context as MigawkaApplication).grpcProvider
    private val grpcStub = grpcProvider.getMigawkaServiceStub()

    override suspend fun doWork(): Result {
        val request = GetMediaItemRequest.newBuilder()
            .setId("lalala")
            .build()

        val response = grpcStub.getThumbnail(request)

        sendNotification("Daily Sync Complete", "OK")
        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "daily_sync_channel"
        val notificationId = 1

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Create the Channel (Required for API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Sync Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for scheduled daily tasks"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Build the Notification
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Removes notification when tapped

        // 3. Show it
        notificationManager.notify(notificationId, builder.build())
    }
}