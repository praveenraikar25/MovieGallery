package com.raikar.moviegallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.raikar.moviegallery.ui.navigation.AppNavHost
import com.raikar.moviegallery.ui.theme.MovieGalleryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MovieGalleryTheme {
                AppNavHost()
            }
        }
    }
}
