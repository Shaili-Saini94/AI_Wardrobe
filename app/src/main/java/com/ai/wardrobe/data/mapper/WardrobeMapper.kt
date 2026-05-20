package com.ai.wardrobe.data.mapper

import com.ai.wardrobe.domain.model.CalendarEntry
import com.ai.wardrobe.domain.model.ClothingItem
import com.ai.wardrobe.domain.model.Outfit
import com.ai.wardrobe.domain.model.WornRecord
import com.ai.wardrobe.database.ClothingItem as ClothingItemEntity
import com.ai.wardrobe.database.Outfit as OutfitEntity
import com.ai.wardrobe.database.WornHistory as WornHistoryEntity
import com.ai.wardrobe.database.OutfitCalendar as OutfitCalendarEntity

fun ClothingItemEntity.toClothingItem(): ClothingItem = ClothingItem(
    id               = id,
    imageUri         = imageUri,
    thumbnailUri     = thumbnailUri,
    thumbnailBackUri = thumbnailBackUri,
    category         = category,
    tags             = tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    occasions        = occasions?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    seasons          = seasons?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    styleTypes       = styleTypes?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    mood             = mood,
    weather          = weather,
    dominantColor    = dominantColor,
    dateAdded        = dateAdded
)

fun OutfitEntity.toOutfit(items: List<ClothingItem>): Outfit = Outfit(
    id           = id,
    name         = name,
    styleType    = styleType    ?: "",
    occasionType = occasionType ?: "",
    items        = items,
    styleNote    = styleNote    ?: "",
    weatherNote  = weatherNote  ?: "",
    colorStory   = colorStory   ?: "",
    whyItWorks   = whyItWorks   ?: "",
    dateCreated  = dateCreated
)

fun WornHistoryEntity.toWornRecord(): WornRecord = WornRecord(
    id             = id,
    clothingItemId = clothingItemId,
    wornAt         = wornAt
)

fun OutfitCalendarEntity.toCalendarEntry(outfit: Outfit? = null): CalendarEntry = CalendarEntry(
    id          = id,
    outfitId    = outfitId,
    plannedDate = plannedDate,
    note        = note,
    outfit      = outfit
)
