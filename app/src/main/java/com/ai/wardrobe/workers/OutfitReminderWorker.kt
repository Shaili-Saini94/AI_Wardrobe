package com.ai.wardrobe.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ai.wardrobe.domain.repository.WardrobeRepository
import com.ai.wardrobe.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class OutfitReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: WardrobeRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val allItems = repository.getAllClothingItems().first()
        if (allItems.isEmpty()) return Result.success()

        // Find items not worn in the last 30 days
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val neglected = repository.getItemsNotWornSince(cutoff).first()

        when {
            neglected.size >= 3 -> {
                val names = neglected.take(3).joinToString(", ") { it.category }
                NotificationHelper.showNotification(
                    context = applicationContext,
                    id = 1001,
                    title = "Forgotten pieces in your wardrobe ✨",
                    body = "You haven't worn your $names in a while. Time to style them?"
                )
            }
            allItems.size >= 5 -> {
                // Generic outfit nudge
                val categories = allItems.map { it.category }.distinct().take(3)
                NotificationHelper.showNotification(
                    context = applicationContext,
                    id = 1002,
                    title = "Today's outfit idea 👗",
                    body = "Mix your ${categories.joinToString(" + ")} for a fresh look today!"
                )
            }
        }

        return Result.success()
    }
}
