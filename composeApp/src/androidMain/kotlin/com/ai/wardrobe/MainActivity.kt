package com.ai.wardrobe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.ai.wardrobe.database.DriverFactory
import com.ai.wardrobe.database.createDatabase
import com.ai.wardrobe.data.repository.WardrobeRepositoryImpl
import com.ai.wardrobe.ai.AndroidImageLabeler
import com.ai.wardrobe.ai.AndroidPlatformCamera
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        // Manual DI for CMP restructuring demo
        val driverFactory = DriverFactory(applicationContext)
        val database = createDatabase(driverFactory)
        val repository = WardrobeRepositoryImpl(database, Dispatchers.IO)
        val imageLabeler = AndroidImageLabeler(applicationContext)
        val platformCamera = AndroidPlatformCamera()
        
        setContent {
            App(
                repository = repository,
                imageLabeler = imageLabeler,
                platformCamera = platformCamera
            )
        }
    }
}
