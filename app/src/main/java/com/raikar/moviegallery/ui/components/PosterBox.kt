package com.raikar.moviegallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.raikar.moviegallery.ui.util.MovieGradients

/**
 * The single poster primitive, shared by every card and the detail hero.
 *
 * [imageUrl] is drawn on top of the per-movie gradient, so the gradient doubles as
 * the placeholder while loading and as the fallback for titles TMDB has no artwork
 * for.
 */
@Composable
fun PosterBox(
    movieId: Int,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    contentDescription: String? = null,
    badge: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MovieGradients.brushFor(movieId)),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (badge != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                badge()
            }
        }
    }
}
