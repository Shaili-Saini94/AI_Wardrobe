# FashionAI SDK ProGuard Rules

# Keep all public SDK classes
-keep class com.fashionai.sdk.FashionAI { *; }
-keep class com.fashionai.sdk.FashionAIConfig { *; }
-keep class com.fashionai.sdk.FashionAIMode { *; }

# Keep all public model classes
-keep class com.fashionai.sdk.model.** { *; }
-keep class com.fashionai.sdk.detection.DetectionResult { *; }
-keep class com.fashionai.sdk.categorization.CategoryResult { *; }
-keep class com.fashionai.sdk.recommendation.OutfitSuggestion { *; }
-keep class com.fashionai.sdk.recommendation.OutfitContext { *; }
-keep class com.fashionai.sdk.color.DetectedColor { *; }
-keep class com.fashionai.sdk.color.CompatibilityResult { *; }
-keep class com.fashionai.sdk.weather.WeatherContext { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-dontwarn org.tensorflow.lite.**

# MediaPipe
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
