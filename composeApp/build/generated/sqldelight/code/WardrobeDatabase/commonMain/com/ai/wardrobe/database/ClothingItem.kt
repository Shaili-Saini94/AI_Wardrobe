package com.ai.wardrobe.database

import kotlin.Long
import kotlin.String

public data class ClothingItem(
  public val id: Long,
  public val imageUri: String,
  public val category: String,
  public val tags: String?,
  public val dateAdded: Long,
)
