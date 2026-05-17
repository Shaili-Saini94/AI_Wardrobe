package com.ai.wardrobe.database

import kotlin.Long
import kotlin.String

public data class Outfit(
  public val id: Long,
  public val name: String,
  public val dateCreated: Long,
)
