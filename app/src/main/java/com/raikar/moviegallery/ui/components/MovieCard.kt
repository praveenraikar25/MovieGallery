package com.raikar.moviegallery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors

@Composable
fun RailMovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(Sizes.PosterWidth).clickable(onClick = onClick)) {
        PosterBox(
            movieId = movie.id,
            imageUrl = movie.posterUrl,
            contentDescription = movie.title,
            modifier = Modifier.width(Sizes.PosterWidth).height(Sizes.PosterHeight),
        )
        val density = LocalDensity.current
        val lineHeightMultiplier = 1.2f
        val titleLineHeight =
            with(density) {
                MaterialTheme.typography.labelLarge.fontSize
                    .toDp() * lineHeightMultiplier
            }
        val ratingLineHeight =
            with(density) {
                MaterialTheme.typography.labelSmall.fontSize
                    .toDp() *
                    lineHeightMultiplier
            }
        val textBlockHeight = titleLineHeight * 2 + 8.dp + 3.dp + ratingLineHeight
        Box(modifier = Modifier.height(textBlockHeight)) {
            Column {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(modifier = Modifier.padding(top = 3.dp)) {
                    Text(
                        text = movie.subtitleWithRating,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.movieColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Card subtitles, built by joining only the parts TMDB actually returned — an
 * unreleased film has no year and reports a 0.0 rating, and some titles carry no
 * genres at all.
 */
private val Movie.subtitleWithRating: String
    get() =
        listOfNotNull(
            "★ $ratingLabel".takeIf { hasRating },
            genreLabel.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifBlank { yearLabel }
