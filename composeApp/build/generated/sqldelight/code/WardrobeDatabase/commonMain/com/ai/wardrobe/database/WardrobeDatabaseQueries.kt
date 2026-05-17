package com.ai.wardrobe.database

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class WardrobeDatabaseQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllClothingItems(mapper: (
    id: Long,
    imageUri: String,
    category: String,
    tags: String?,
    dateAdded: Long,
  ) -> T): Query<T> = Query(663_046_846, arrayOf("ClothingItem"), driver, "WardrobeDatabase.sq", "selectAllClothingItems", "SELECT ClothingItem.id, ClothingItem.imageUri, ClothingItem.category, ClothingItem.tags, ClothingItem.dateAdded FROM ClothingItem") { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!
    )
  }

  public fun selectAllClothingItems(): Query<ClothingItem> = selectAllClothingItems(::ClothingItem)

  public fun <T : Any> selectClothingItemById(id: Long, mapper: (
    id: Long,
    imageUri: String,
    category: String,
    tags: String?,
    dateAdded: Long,
  ) -> T): Query<T> = SelectClothingItemByIdQuery(id) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!
    )
  }

  public fun selectClothingItemById(id: Long): Query<ClothingItem> = selectClothingItemById(id, ::ClothingItem)

  public fun lastInsertedOutfitId(): ExecutableQuery<Long> = Query(2_054_976_983, driver, "WardrobeDatabase.sq", "lastInsertedOutfitId", "SELECT last_insert_rowid()") { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> selectAllOutfits(mapper: (
    id: Long,
    name: String,
    dateCreated: Long,
  ) -> T): Query<T> = Query(-790_483_722, arrayOf("Outfit"), driver, "WardrobeDatabase.sq", "selectAllOutfits", "SELECT Outfit.id, Outfit.name, Outfit.dateCreated FROM Outfit") { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!
    )
  }

  public fun selectAllOutfits(): Query<Outfit> = selectAllOutfits(::Outfit)

  public fun <T : Any> selectItemsForOutfit(outfitId: Long, mapper: (
    id: Long,
    imageUri: String,
    category: String,
    tags: String?,
    dateAdded: Long,
  ) -> T): Query<T> = SelectItemsForOutfitQuery(outfitId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!
    )
  }

  public fun selectItemsForOutfit(outfitId: Long): Query<ClothingItem> = selectItemsForOutfit(outfitId, ::ClothingItem)

  /**
   * @return The number of rows updated.
   */
  public fun insertClothingItem(
    imageUri: String,
    category: String,
    tags: String?,
    dateAdded: Long,
  ): QueryResult<Long> {
    val result = driver.execute(-1_953_263_041, """
        |INSERT INTO ClothingItem (imageUri, category, tags, dateAdded)
        |VALUES (?, ?, ?, ?)
        """.trimMargin(), 4) {
          var parameterIndex = 0
          bindString(parameterIndex++, imageUri)
          bindString(parameterIndex++, category)
          bindString(parameterIndex++, tags)
          bindLong(parameterIndex++, dateAdded)
        }
    notifyQueries(-1_953_263_041) { emit ->
      emit("ClothingItem")
    }
    return result
  }

  /**
   * @return The number of rows updated.
   */
  public fun deleteClothingItem(id: Long): QueryResult<Long> {
    val result = driver.execute(2_110_945_841, """DELETE FROM ClothingItem WHERE id = ?""", 1) {
          var parameterIndex = 0
          bindLong(parameterIndex++, id)
        }
    notifyQueries(2_110_945_841) { emit ->
      emit("ClothingItem")
      emit("OutfitItem")
    }
    return result
  }

  /**
   * @return The number of rows updated.
   */
  public fun updateClothingItem(
    category: String,
    tags: String?,
    id: Long,
  ): QueryResult<Long> {
    val result = driver.execute(-1_898_811_313, """UPDATE ClothingItem SET category = ?, tags = ? WHERE id = ?""", 3) {
          var parameterIndex = 0
          bindString(parameterIndex++, category)
          bindString(parameterIndex++, tags)
          bindLong(parameterIndex++, id)
        }
    notifyQueries(-1_898_811_313) { emit ->
      emit("ClothingItem")
    }
    return result
  }

  /**
   * @return The number of rows updated.
   */
  public fun insertOutfit(name: String, dateCreated: Long): QueryResult<Long> {
    val result = driver.execute(-867_450_745, """
        |INSERT INTO Outfit (name, dateCreated)
        |VALUES (?, ?)
        """.trimMargin(), 2) {
          var parameterIndex = 0
          bindString(parameterIndex++, name)
          bindLong(parameterIndex++, dateCreated)
        }
    notifyQueries(-867_450_745) { emit ->
      emit("Outfit")
    }
    return result
  }

  /**
   * @return The number of rows updated.
   */
  public fun deleteOutfit(id: Long): QueryResult<Long> {
    val result = driver.execute(1_913_390_585, """DELETE FROM Outfit WHERE id = ?""", 1) {
          var parameterIndex = 0
          bindLong(parameterIndex++, id)
        }
    notifyQueries(1_913_390_585) { emit ->
      emit("Outfit")
      emit("OutfitItem")
    }
    return result
  }

  /**
   * @return The number of rows updated.
   */
  public fun insertOutfitItem(outfitId: Long, clothingItemId: Long): QueryResult<Long> {
    val result = driver.execute(1_207_768_122, """
        |INSERT INTO OutfitItem (outfitId, clothingItemId)
        |VALUES (?, ?)
        """.trimMargin(), 2) {
          var parameterIndex = 0
          bindLong(parameterIndex++, outfitId)
          bindLong(parameterIndex++, clothingItemId)
        }
    notifyQueries(1_207_768_122) { emit ->
      emit("OutfitItem")
    }
    return result
  }

  private inner class SelectClothingItemByIdQuery<out T : Any>(
    public val id: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ClothingItem", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ClothingItem", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(-1_930_316_044, """SELECT ClothingItem.id, ClothingItem.imageUri, ClothingItem.category, ClothingItem.tags, ClothingItem.dateAdded FROM ClothingItem WHERE id = ?""", mapper, 1) {
      var parameterIndex = 0
      bindLong(parameterIndex++, id)
    }

    override fun toString(): String = "WardrobeDatabase.sq:selectClothingItemById"
  }

  private inner class SelectItemsForOutfitQuery<out T : Any>(
    public val outfitId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ClothingItem", "OutfitItem", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ClothingItem", "OutfitItem", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(-2_134_791_213, """
    |SELECT ci.id, ci.imageUri, ci.category, ci.tags, ci.dateAdded FROM ClothingItem ci
    |JOIN OutfitItem oi ON ci.id = oi.clothingItemId
    |WHERE oi.outfitId = ?
    """.trimMargin(), mapper, 1) {
      var parameterIndex = 0
      bindLong(parameterIndex++, outfitId)
    }

    override fun toString(): String = "WardrobeDatabase.sq:selectItemsForOutfit"
  }
}
