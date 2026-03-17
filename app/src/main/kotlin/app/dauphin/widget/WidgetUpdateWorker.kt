package app.dauphin.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        CourseCardWidget().updateAll(context)
        scheduleNextUpdate(context)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "WidgetUpdateWorker"

        fun scheduleNextUpdate(context: Context) {
            val delay = calculateDelayUntilNextUpdate()
            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        private fun calculateDelayUntilNextUpdate(): Long {
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            val minute = calendar.get(Calendar.MINUTE)

            // Update at XX:10 and XX:40
            val targetMinute = when {
                minute < 10 -> 10
                minute < 40 -> 40
                else -> 10
            }

            calendar.set(Calendar.MINUTE, targetMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (calendar.timeInMillis <= now) {
                // If it's already past the target time in this hour, schedule for next hour
                calendar.add(Calendar.HOUR_OF_DAY, 1)
            }

            return calendar.timeInMillis - now
        }
    }
}
