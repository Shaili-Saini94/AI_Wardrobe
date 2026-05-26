package com.ai.wardrobe

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ai.wardrobe.util.NotificationHelper
import com.ai.wardrobe.workers.OutfitReminderWorker
import com.ai.wardrobe.workers.ThumbnailRetryWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class WardrobeApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val wm = WorkManager.getInstance(this)

        // Retry missing thumbnails — runs once, checks for gaps
        val thumbnailRetry = androidx.work.OneTimeWorkRequestBuilder<ThumbnailRetryWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .build()
        wm.enqueue(thumbnailRetry)

        // Daily outfit reminder — every 24 hours
        val dailyReminder = PeriodicWorkRequestBuilder<OutfitReminderWorker>(
            24, TimeUnit.HOURS
        ).build()
        wm.enqueueUniquePeriodicWork(
            "outfit_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyReminder
        )
    }
}
