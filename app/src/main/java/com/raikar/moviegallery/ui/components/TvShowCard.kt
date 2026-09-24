package com.raikar.moviegallery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.domain.model.TvShow
import com.raikar.moviegallery.ui.theme.movieColors

@Composable
fun GridTvShowCard(
    show: TvShow,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PosterBox(
            movieId = show.id,
            imageUrl = show.posterUrl,
            contentDescription = show.name,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
        )
        Text(
            text = show.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(
                text = show.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.movieColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (show.hasRating) {
                Text(
                    text = "★ ${show.ratingLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Joins only the parts the BFF actually returned, e.g. "2011 · Drama". */
private val TvShow.subtitle: String
    get() =
        listOfNotNull(
            firstAirYear?.toString(),
            genreLabel.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifBlank { "TBA" }
