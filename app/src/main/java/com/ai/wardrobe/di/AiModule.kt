package com.ai.wardrobe.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// FashionAI SDK replaced by Gemini Vision AI.
// GeminiClothingAnalyzer and ImageLabeler are @Singleton @Inject — no manual provides needed.
@Module
@InstallIn(SingletonComponent::class)
object AiModule
