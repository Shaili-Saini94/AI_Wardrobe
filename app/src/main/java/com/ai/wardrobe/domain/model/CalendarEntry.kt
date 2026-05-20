package com.ai.wardrobe.domain.model

data class CalendarEntry(
    val id: Long? = null,
    val outfitId: Long?,
    val plannedDate: String,   // "yyyy-MM-dd"
    val note: String? = null,
    val outfit: Outfit? = null
)
