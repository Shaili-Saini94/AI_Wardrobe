package com.ai.wardrobe.database

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): WardrobeDatabase {
    val driver = driverFactory.createDriver()
    return WardrobeDatabase(driver)
}
