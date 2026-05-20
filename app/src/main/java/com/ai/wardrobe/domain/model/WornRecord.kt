package com.ai.wardrobe.domain.model

data class WornRecord(
    val id: Long? = null,
    val clothingItemId: Long,
    val wornAt: Long
)
