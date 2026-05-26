package com.ai.wardrobe.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ai.wardrobe.ai.GeminiClothingAnalyzer
import com.ai.wardrobe.domain.repository.WardrobeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream

@HiltWorker
class ThumbnailRetryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: WardrobeRepository,
    private val gemini: GeminiClothingAnalyzer
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val items = repository.getAllClothingItems().first()
        val missing = items.filter { it.thumbnailUri == null || it.thumbnailBackUri == null }

        if (missing.isEmpty()) return Result.success()

        var anyFailed = false

        missing.forEach { item ->
            try {
                val base64 = uriToBase64(item.imageUri) ?: return@forEach

                if (item.thumbnailUri == null) {
                    val frontPath = gemini.generateFrontThumbnail(
                        base64Image = base64,
                        category = item.category,
                        tags = item.tags
                    )
                    if (frontPath != null) {
                        repository.updateThumbnailUri(item.id!!, frontPath)
                    }
                }

                if (item.thumbnailBackUri == null) {
                    val backPath = gemini.generateBackThumbnail(
                        base64Image = base64,
                        category = item.category,
                        tags = item.tags
                    )
                    if (backPath != null) {
                        repository.updateThumbnailBackUri(item.id!!, backPath)
                    }
                }
            } catch (e: Exception) {
                anyFailed = true
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }

    private fun uriToBase64(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val stream = applicationContext.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }
}
