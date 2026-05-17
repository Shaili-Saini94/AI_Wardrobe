package com.ai.wardrobe.database

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.ai.wardrobe.database.composeApp.newInstance
import com.ai.wardrobe.database.composeApp.schema
import kotlin.Unit

public interface WardrobeDatabase : Transacter {
  public val wardrobeDatabaseQueries: WardrobeDatabaseQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = WardrobeDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): WardrobeDatabase = WardrobeDatabase::class.newInstance(driver)
  }
}
