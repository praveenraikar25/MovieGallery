package com.raikar.moviegallery.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.ui.theme.PillShape
import com.raikar.moviegallery.ui.theme.movieColors

@Composable
fun GenreChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.movieColors.textMuted,
        modifier =
            modifier
                .border(1.dp, MaterialTheme.movieColors.border, PillShape)
                .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
