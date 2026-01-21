package xyz.jdubiel.migawka.data

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SyncManager(private val context: Context) {
    fun scheduleDailyWork(targetTime: LocalTime, unmeteredConnectionOnly: Boolean, chargingOnly: Boolean) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetTime.hour)
            set(Calendar.MINUTE, targetTime.minute)
            set(Calendar.SECOND, 0)
            if (before(currentDate)) {
                add(Calendar.HOUR_OF_DAY, 24) // Schedule for tomorrow if time already passed
            }
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val constraints = Constraints.Builder()
        if (unmeteredConnectionOnly) {
            constraints.setRequiredNetworkType(NetworkType.UNMETERED)
        } else {
            constraints.setRequiredNetworkType(NetworkType.CONNECTED)
        }
        if (chargingOnly) {
            constraints.setRequiresCharging(true)
        } else {
            constraints.setRequiresBatteryNotLow(true)
        }

        val dailyWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS) // Comment this for testing
            .setConstraints(constraints.build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )

        Log.i(TAG, "Daily sync scheduled\n\ttarget time = ${targetTime} (${formatDate(dueDate)})\n\tUnmetered: $unmeteredConnectionOnly\n\tCharging only: $chargingOnly")
    }

    fun cancelDailyWork() {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
        Log.i(TAG, "Daily sync canceled")
    }

    private fun formatDate(dueDate: Calendar): String {
        val currentDate = Calendar.getInstance()
        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis
        val nextRunTime = System.currentTimeMillis() + initialDelay

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val readableDate = formatter.format(Date(nextRunTime))

        return readableDate
    }

    companion object {
        const val uniqueWorkName = "MigawkaUploadMedia"
        const val TAG = "SyncManager"
    }
}