package com.raikar.moviegallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.domain.model.ProductionCountry
import com.raikar.moviegallery.ui.theme.PillShape
import com.raikar.moviegallery.ui.theme.movieColors

/**
 * A production country that opens its details. Filled and ending in a chevron, unlike the
 * outlined, inert [GenreChip], so it reads as tappable.
 */
@Composable
fun CountryChip(
    country: ProductionCountry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .height(36.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.movieColors.border, PillShape)
                .clickable(onClickLabel = "Show country details", role = Role.Button, onClick = onClick)
                .padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = countryChipLabel(country),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            // TalkBack would otherwise spell out the flag's two regional-indicator letters.
            modifier = Modifier.clearAndSetSemantics { contentDescription = country.name },
        )
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.movieColors.textMuted,
            modifier = Modifier.size(12.dp),
        )
    }
}

/** "🇰🇷 South Korea", or just the name when the code yields no flag. */
internal fun countryChipLabel(country: ProductionCountry): String =
    country.flag?.let { "$it ${country.name}" } ?: country.name
