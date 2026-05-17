package com.fashionai.sdk.detection

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.fashionai.sdk.internal.FashionAILogger
import com.fashionai.sdk.model.ClothingType
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifierResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device clothing detector backed by MediaPipe Tasks (formerly TensorFlow Lite).
 *
 * Model placement: put your .tflite file in the module's assets/ folder.
 * Recommended model: EfficientNet-Lite2 fine-tuned on DeepFashion2.
 *
 * If you don't have a custom model yet, a placeholder path is used and
 * the detector will gracefully return a low-confidence UNKNOWN result
 * so the app can still function while you source or train a model.
 */
internal class TFLiteClothingDetector(
    private val context: Context,
    private val modelFileName: String = "fashion_classifier.tflite",
    private val maxResults: Int = 5,
    private val scoreThreshold: Float = 0.25f,
    private val useGpu: Boolean = true
) {

    private var classifier: ImageClassifier? = null
    private var isInitialized = false

    fun initialize(): Boolean {
        return try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath(modelFileName)

            if (useGpu) {
                baseOptionsBuilder.setDelegate(Delegate.GPU)
            }

            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(RunningMode.IMAGE)
                .setMaxResults(maxResults)
                .setScoreThreshold(scoreThreshold)
                .build()

            classifier = ImageClassifier.createFromOptions(context, options)
            isInitialized = true
            FashionAILogger.d("TFLiteClothingDetector initialized with model: $modelFileName")
            true
        } catch (e: Exception) {
            FashionAILogger.e("Failed to initialize TFLite model: ${e.message}")
            isInitialized = false
            false
        }
    }

    suspend fun detect(bitmap: Bitmap): DetectionResult = withContext(Dispatchers.Default) {
        if (!isInitialized || classifier == null) {
            FashionAILogger.w("Detector not initialized — returning fallback result")
            return@withContext DetectionResult.unknown()
        }

        val startTime = SystemClock.elapsedRealtime()

        return@withContext try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result: ImageClassifierResult = classifier!!.classify(mpImage)

            val latencyMs = SystemClock.elapsedRealtime() - startTime
            FashionAILogger.d("Inference completed in ${latencyMs}ms")

            parseResults(result)
        } catch (e: Exception) {
            FashionAILogger.e("Inference error: ${e.message}")
            DetectionResult.unknown()
        }
    }

    private fun parseResults(result: ImageClassifierResult): DetectionResult {
        val allCategories = result.classificationResult()
            .classifications()
            .flatMap { it.categories() }
            .sortedByDescending { it.score() }

        if (allCategories.isEmpty()) return DetectionResult.unknown()

        val top = allCategories.first()
        val topLabel = top.categoryName().lowercase().trim()
        val topType = FashionLabelDictionary.typeForLabel(topLabel)
        val topDisplay = FashionLabelDictionary.displayNameForLabel(topLabel)

        val alternatives = allCategories.drop(1).take(4).map { cat ->
            DetectionCandidate(
                label = cat.categoryName(),
                confidence = cat.score(),
                clothingType = FashionLabelDictionary.typeForLabel(cat.categoryName())
            )
        }

        return DetectionResult(
            clothingType = topType,
            subTypeRaw = topLabel,
            subTypeDisplay = topDisplay,
            confidence = top.score(),
            alternatives = alternatives,
            isFallback = top.score() < 0.40f || topType == ClothingType.UNKNOWN
        )
    }

    fun close() {
        classifier?.close()
        classifier = null
        isInitialized = false
    }
}
