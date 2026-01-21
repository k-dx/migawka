package xyz.jdubiel.migawka.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.flow
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.R
import xyz.jdubiel.migawka.UploadMetadata
import xyz.jdubiel.migawka.UploadRequest
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class SyncWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val grpcProvider = (context as MigawkaApplication).grpcProvider
    private val stub = grpcProvider.getMigawkaServiceStub()
    private val imageRepository = (context as MigawkaApplication).imageRepository

    override suspend fun doWork(): Result {
        Log.d(TAG, "starting worker")
        val photoUris: List<LocalImage> = imageRepository.getLocalOnlyEntries()
        return try {
            val uploadFlow = flow {
                photoUris.forEach { localImage ->
                    Log.d(TAG, "uploading image ${localImage.hash}")
                    val uri = localImage.contentUri
                    // Send Metadata for the new file
                    val rfc3339: String = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        .format(localImage.date.atOffset(ZoneOffset.UTC))

                    val metadata = UploadMetadata.newBuilder()
                        .setId(localImage.hash.toString())
                        .setCreationTime(rfc3339)
                        .setFilename(uri.lastPathSegment).build()
                    emit(
                        UploadRequest.newBuilder()
                        .setMetadata(metadata)
                        .build())

                    // Stream the file in chunks
                    applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val buffer = ByteArray(32 * 1024) // 32KB chunks
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            emit(UploadRequest.newBuilder()
                                .setChunk(ByteString.copyFrom(buffer, 0, bytesRead))
                                .build())
                        }
                    }
                }
            }

            // Execute the gRPC call
            val response = stub.uploadPhotos(uploadFlow)

            if (response.status.code == 200) {
                Log.d(TAG, "sync complete")
                sendNotification(
                    context.getString(R.string.sync_complete),
                    context.getString(R.string.uploaded_photos, photoUris.size))
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        } finally {
//            channel.shutdown() // not needed as it will happen in onTerminate()
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "daily_sync_channel"
        val notificationId = 1

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the Channel (Required for API 26+)
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

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Removes notification when tapped

        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        const val TAG = "SyncWorker"
    }
}