package com.ai.wardrobe.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Pre-validates a photo before it enters the clothing classifier pipeline.
 *
 * Two rejection scenarios:
 *  1. **Phone screenshot**: Aspect ratio matches a phone screen (≥ 1.88 or ≤ 0.53).
 *  2. **Multiple people**: ML Kit Face Detection finds 2 or more faces.
 */
@Singleton
class ImagePreValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    sealed class Result {
        object Valid : Result()
        data class Rejected(val reason: String) : Result()
    }

    /**
     * On-device face detector tuned for whole-frame photos.
     * - FAST mode: optimized for speed; we just need a count, not landmarks
     * - PERFORMANCE_MODE_FAST avoids classification work we don't use
     */
    private val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.10f)   // ignore tiny background faces (< 10% of frame width)
                .build()
        )
    }

    /**
     * Runs all pre-validation checks.  Returns [Result.Valid] when the image should
     * proceed to classification, or [Result.Rejected] with a user-readable message.
     */
    suspend fun validate(uri: Uri): Result {
        // ── 1. Phone screenshot check (aspect ratio of the original file) ─────
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val origW = bounds.outWidth.toFloat()
        val origH = bounds.outHeight.toFloat()
        val origAr = if (origW > 0f) origH / origW else 1f

        Log.w("PreValidator", "ar=${"%.2f".format(origAr)} origW=${origW.toInt()} origH=${origH.toInt()}")

        if (origAr > 1.88f || origAr < 0.53f) {
            return Result.Rejected(
                "This looks like a phone screenshot — please use the camera or pick an actual clothing photo from your gallery"
            )
        }

        // ── 2. Multi-person check (ML Kit Face Detection, on-device) ──────────
        val bitmap = loadScaled(uri, targetSize = 512) ?: return Result.Valid
        val faceCount = detectFaceCount(bitmap)

        Log.w("PreValidator", "faces=$faceCount")

        if (faceCount >= 2) {
            return Result.Rejected(
                "Multiple people detected — please photograph one person at a time, or take a flat-lay photo of the garment alone"
            )
        }

        return Result.Valid
    }

    /**
     * Returns the number of human faces detected in [bitmap] (asynchronously).
     * Returns 0 on any detection failure so a buggy detector cannot block the user.
     */
    private suspend fun detectFaceCount(bitmap: Bitmap): Int =
        suspendCancellableCoroutine { cont ->
            val input = InputImage.fromBitmap(bitmap, 0)
            faceDetector.process(input)
                .addOnSuccessListener { faces ->
                    if (cont.isActive) cont.resume(faces.size)
                }
                .addOnFailureListener { e ->
                    Log.e("PreValidator", "Face detection failed: ${e.message}")
                    if (cont.isActive) cont.resume(0)
                }
        }

    // ── Image loader ─────────────────────────────────────────────────────────

    private fun loadScaled(uri: Uri, targetSize: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sampleSize = maxOf(1, minOf(bounds.outWidth, bounds.outHeight) / targetSize)
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (_: Exception) { null }
}
