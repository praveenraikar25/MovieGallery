package com.raikar.moviegallery

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.raikar.moviegallery.ui.navigation.AppNavHost
import com.raikar.moviegallery.ui.theme.MovieGalleryTheme

class MainActivity : ComponentActivity() {
    /** A Movie Detail request from an AppFunction intent, cleared once the nav graph has opened it. */
    private var requestedMovieId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only a fresh launch carries a new request; after recreation the back stack already has it.
        if (savedInstanceState == null) requestedMovieId = intent.movieIdExtra()
        setContent {
            MovieGalleryTheme {
                AppNavHost(
                    requestedMovieId = requestedMovieId,
                    onMovieRequestHandled = { requestedMovieId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.movieIdExtra()?.let { requestedMovieId = it }
    }

    private fun Intent.movieIdExtra(): Int? = getIntExtra(EXTRA_MOVIE_ID, 0).takeIf { it > 0 }

    companion object {
        /** TMDB movie id to open on Movie Detail; set by `openMovieDetails`. */
        const val EXTRA_MOVIE_ID = "com.raikar.moviegallery.extra.MOVIE_ID"
    }
}
