package com.fashionai.sdk

import com.fashionai.sdk.model.FashionAILogLevel
import com.fashionai.sdk.model.FashionAIMode

/**
 * Configuration for the FashionAI SDK.
 * Build using [FashionAI.Builder].
 */
data class FashionAIConfig(
    /** Processing mode: on-device, hybrid, or cloud */
    val mode: FashionAIMode = FashionAIMode.ON_DEVICE,

    /** API key for cloud/hybrid LLM features */
    val apiKey: String? = null,

    /**
     * Name of the TFLite model file placed in your assets/ folder.
     * Default: "fashion_classifier.tflite"
     * Download a pre-trained model from the SDK documentation.
     */
    val modelFileName: String = "fashion_classifier.tflite",

    /** Use GPU delegate for faster inference (recommended: true) */
    val useGpuAcceleration: Boolean = true,

    /** Minimum confidence to accept a detection result (0.0–1.0) */
    val detectionConfidenceThreshold: Float = 0.25f,

    /** Max outfit suggestions to generate per call */
    val maxOutfitResults: Int = 5,

    /** SDK log verbosity */
    val logLevel: FashionAILogLevel = FashionAILogLevel.WARN
)
