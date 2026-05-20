package com.ai.wardrobe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ai.wardrobe.ui.MainScreen
import com.ai.wardrobe.ui.theme.AIWardrobeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable Edge-to-Edge display
        enableEdgeToEdge()

        setContent {
            AIWardrobeTheme {
                MainScreen()
            }
        }
    }
}
