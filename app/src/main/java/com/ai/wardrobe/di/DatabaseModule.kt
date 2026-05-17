package com.ai.wardrobe.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.ai.wardrobe.database.WardrobeDatabase
import com.ai.wardrobe.data.repository.WardrobeRepositoryImpl
import com.ai.wardrobe.domain.repository.WardrobeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSqlDriver(@ApplicationContext context: Context): SqlDriver {
        return AndroidSqliteDriver(WardrobeDatabase.Schema, context, "wardrobe.db")
    }

    @Provides
    @Singleton
    fun provideWardrobeDatabase(driver: SqlDriver): WardrobeDatabase {
        return WardrobeDatabase(driver)
    }

    @Provides
    @Singleton
    fun provideWardrobeRepository(db: WardrobeDatabase): WardrobeRepository {
        return WardrobeRepositoryImpl(db, Dispatchers.IO)
    }
}
