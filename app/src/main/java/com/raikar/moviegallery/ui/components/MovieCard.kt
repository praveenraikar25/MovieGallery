package com.raikar.moviegallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@Composable
fun GridMovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        PosterBox(
            movieId = movie.id,
            imageUrl = movie.posterUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
        )
        Text(
            text = movie.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp),
        )
        Text(
            text = movie.subtitleWithYear,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.movieColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
fun WatchlistMovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        PosterBox(
            movieId = movie.id,
            imageUrl = movie.posterUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            overlayTitle = movie.title,
            badge = {
                Box(
                    modifier =
                        Modifier
                            .size(Sizes.BookmarkBadge)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.BookmarkFilled,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = movie.subtitleWithYear,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.movieColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (movie.hasRating) {
                Text(
                    text = "★ ${movie.ratingLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
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

private val Movie.subtitleWithYear: String
    get() =
        listOfNotNull(
            year?.toString(),
            genreLabel.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifBlank { "TBA" }
