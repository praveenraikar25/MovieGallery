package com.raikar.moviegallery.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.domain.model.CastMember
import com.raikar.moviegallery.ui.components.InitialAvatar
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors

private val TileWidth = 88.dp

/** The "Cast" heading and its horizontally scrolling row of tiles. Call only for a non-empty [cast]. */
@Composable
internal fun CastSection(
    cast: List<CastMember>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Cast",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(cast) { member -> CastTile(member) }
        }
    }
}

@Composable
private fun CastTile(member: CastMember) {
    Column(
        modifier = Modifier.width(TileWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InitialAvatar(
            text = member.initial,
            size = Sizes.AvatarLg,
            background = MaterialTheme.movieColors.surfaceAlt,
            contentColor = MaterialTheme.movieColors.textMuted,
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Fixed line counts (reserved with minLines) keep every tile the same height
        // however long a name or role is.
        Text(
            text = member.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = member.role,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.movieColors.textMuted,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
