# Consumer ProGuard rules — applied to apps that depend on this SDK
-keep class com.fashionai.sdk.** { *; }
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
-dontwarn com.google.mediapipe.**
-dontwarn com.google.mlkit.**
