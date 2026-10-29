package com.raikar.moviegallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.model.TvShow
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors

/** What a poster grid tile displays, independent of whether it came from a [Movie] or a [TvShow]. */
data class PosterGridItem(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val subtitle: String,
    /** "8.5"-style rating, or null for unrated (0.0) titles. */
    val ratingLabel: String?,
)

/**
 * The one grid tile shared by Search, Watchlist and Top TV Shows: poster, title below it, then a
 * "year · genres" subtitle with the rating beside it. The subtitle truncates so the rating always
 * stays on one line. The tile is tappable only when [onClick] is given; [badge] is drawn in the
 * poster's corner.
 */
@Composable
fun PosterGridCard(
    item: PosterGridItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    badge: (@Composable BoxScope.() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(modifier = modifier.fillMaxWidth().then(clickModifier)) {
        PosterBox(
            movieId = item.id,
            imageUrl = item.posterUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            badge = badge,
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.movieColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (item.ratingLabel != null) {
                Text(
                    text = "★ ${item.ratingLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

fun Movie.toPosterGridItem() =
    PosterGridItem(
        id = id,
        title = title,
        posterUrl = posterUrl,
        subtitle = gridSubtitle(year, genreLabel),
        ratingLabel = ratingLabel.takeIf { hasRating },
    )

fun TvShow.toPosterGridItem() =
    PosterGridItem(
        id = id,
        title = name,
        posterUrl = posterUrl,
        subtitle = gridSubtitle(firstAirYear, genreLabel),
        ratingLabel = ratingLabel.takeIf { hasRating },
    )

/** Joins only the parts the backend actually returned, e.g. "2011 · Drama"; "TBA" when there are none. */
internal fun gridSubtitle(
    year: Int?,
    genreLabel: String,
): String =
    listOfNotNull(
        year?.toString(),
        genreLabel.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { "TBA" }

/** The saved-movie bookmark shown in a poster's corner on the Watchlist. */
@Composable
fun WatchlistBookmarkBadge() {
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
}
