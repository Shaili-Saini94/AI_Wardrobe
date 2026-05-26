package com.fashionai.sdk.detection

import android.content.Context
import android.graphics.Bitmap
import com.fashionai.sdk.internal.FashionAILogger
import com.fashionai.sdk.model.ClothingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * On-device clothing detector using the raw TFLite Interpreter API.
 * Works with any .tflite model — no embedded metadata required.
 * Reads labels from assets/labels.txt (one label per line).
 */
internal class TFLiteClothingDetector(
    private val context: Context,
    private val modelFileName: String = "ai.tflite",
    private val labelsFileName: String = "labels.txt",
    private val maxResults: Int = 5,
    private val scoreThreshold: Float = 0.01f,
    private val useGpu: Boolean = true
) {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var labels: List<String> = emptyList()
    private var inputSize: Int = 224
    private var isQuantized: Boolean = false
    private var isInitialized = false

    fun initialize(): Boolean {
        return try {
            labels = loadLabels()
            val model = loadModelFile()

            val options = Interpreter.Options()
            if (useGpu) {
                val compatList = CompatibilityList()
                if (compatList.isDelegateSupportedOnThisDevice) {
                    gpuDelegate = GpuDelegate(compatList.bestOptionsForThisDevice)
                    options.addDelegate(gpuDelegate!!)
                    FashionAILogger.d("GPU delegate enabled")
                } else {
                    options.numThreads = 4
                }
            } else {
                options.numThreads = 4
            }

            interpreter = Interpreter(model, options)

            // Auto-detect input shape and quantization from the model
            val inputTensor = interpreter!!.getInputTensor(0)
            val inputShape = inputTensor.shape() // [1, H, W, 3]
            inputSize = inputShape[1]

            val outputTensor = interpreter!!.getOutputTensor(0)
            isQuantized = outputTensor.dataType() == DataType.UINT8

            FashionAILogger.d("Model loaded: input=${inputSize}x${inputSize}, quantized=$isQuantized, labels=${labels.size}")
            isInitialized = true
            true
        } catch (e: Exception) {
            FashionAILogger.e("Failed to initialize TFLite: ${e.message}")
            false
        }
    }

    suspend fun detect(bitmap: Bitmap): DetectionResult = withContext(Dispatchers.Default) {
        if (!isInitialized || interpreter == null) {
            FashionAILogger.w("Detector not initialized")
            return@withContext DetectionResult.unknown()
        }

        return@withContext try {
            val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
            val inputBuffer = preprocessBitmap(resized)

            val outputCount = if (labels.isNotEmpty()) labels.size else 1001
            val outputBuffer = if (isQuantized) {
                Array(1) { ByteArray(outputCount) }
            } else {
                Array(1) { FloatArray(outputCount) }
            }

            interpreter!!.run(inputBuffer, outputBuffer)

            val scores: FloatArray = if (isQuantized) {
                (outputBuffer as Array<ByteArray>)[0].map { (it.toInt() and 0xFF) / 255f }.toFloatArray()
            } else {
                (outputBuffer as Array<FloatArray>)[0]
            }

            parseScores(scores)
        } catch (e: Exception) {
            FashionAILogger.e("Inference error: ${e.message}")
            DetectionResult.unknown()
        }
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(
            if (isQuantized) 1 * inputSize * inputSize * 3
            else 1 * inputSize * inputSize * 3 * 4
        )
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            if (isQuantized) {
                byteBuffer.put(r.toByte())
                byteBuffer.put(g.toByte())
                byteBuffer.put(b.toByte())
            } else {
                // Normalize to [-1, 1]
                byteBuffer.putFloat((r - 127.5f) / 127.5f)
                byteBuffer.putFloat((g - 127.5f) / 127.5f)
                byteBuffer.putFloat((b - 127.5f) / 127.5f)
            }
        }
        return byteBuffer
    }

    private fun parseScores(scores: FloatArray): DetectionResult {
        data class Scored(val index: Int, val score: Float)

        val top = scores.mapIndexed { i, s -> Scored(i, s) }
            .filter { it.score >= scoreThreshold }
            .sortedByDescending { it.score }
            .take(maxResults)

        if (top.isEmpty()) return DetectionResult.unknown()

        val best = top.first()
        val bestLabel = labels.getOrElse(best.index) { "class_${best.index}" }
        val bestType = FashionLabelDictionary.typeForLabel(bestLabel)
        val bestDisplay = FashionLabelDictionary.displayNameForLabel(bestLabel)

        FashionAILogger.d("Top label: '$bestLabel' (${best.score}) → $bestType")

        val alternatives = top.drop(1).map { s ->
            val label = labels.getOrElse(s.index) { "class_${s.index}" }
            DetectionCandidate(
                label = label,
                confidence = s.score,
                clothingType = FashionLabelDictionary.typeForLabel(label)
            )
        }

        return DetectionResult(
            clothingType = bestType,
            subTypeRaw = bestLabel,
            subTypeDisplay = bestDisplay,
            confidence = best.score,
            alternatives = alternatives,
            isFallback = best.score < 0.40f || bestType == ClothingType.UNKNOWN
        )
    }

    private fun loadLabels(): List<String> {
        return try {
            context.assets.open(labelsFileName).use { stream ->
                BufferedReader(InputStreamReader(stream)).readLines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            }
        } catch (e: Exception) {
            FashionAILogger.w("Could not load labels file '$labelsFileName': ${e.message}")
            emptyList()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        return inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        gpuDelegate?.close()
        gpuDelegate = null
        isInitialized = false
    }
}
