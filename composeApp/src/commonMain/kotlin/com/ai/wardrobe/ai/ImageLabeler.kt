package com.ai.wardrobe.ai

interface ImageLabeler {
    suspend fun labelImage(imageUri: String): List<String>
}
