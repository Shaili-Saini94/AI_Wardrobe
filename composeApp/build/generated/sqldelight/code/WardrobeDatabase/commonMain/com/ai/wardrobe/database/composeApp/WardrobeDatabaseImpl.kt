package com.ai.wardrobe.database.composeApp

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.ai.wardrobe.database.WardrobeDatabase
import com.ai.wardrobe.database.WardrobeDatabaseQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<WardrobeDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = WardrobeDatabaseImpl.Schema

internal fun KClass<WardrobeDatabase>.newInstance(driver: SqlDriver): WardrobeDatabase = WardrobeDatabaseImpl(driver)

private class WardrobeDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver),
    WardrobeDatabase {
  override val wardrobeDatabaseQueries: WardrobeDatabaseQueries = WardrobeDatabaseQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE ClothingItem (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT,
          |    imageUri TEXT NOT NULL,
          |    category TEXT NOT NULL,
          |    tags TEXT,
          |    dateAdded INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Outfit (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT,
          |    name TEXT NOT NULL,
          |    dateCreated INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE OutfitItem (
          |    outfitId INTEGER NOT NULL,
          |    clothingItemId INTEGER NOT NULL,
          |    PRIMARY KEY (outfitId, clothingItemId),
          |    FOREIGN KEY (outfitId) REFERENCES Outfit(id) ON DELETE CASCADE,
          |    FOREIGN KEY (clothingItemId) REFERENCES ClothingItem(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
