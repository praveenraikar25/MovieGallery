package com.raikar.moviegallery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.ui.common.errorDetail
import com.raikar.moviegallery.ui.common.errorTitle
import com.raikar.moviegallery.ui.theme.PillShape
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FilmReelSpinner()
    }
}

@Composable
fun ErrorState(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            tint = MaterialTheme.movieColors.textMuted,
            modifier = Modifier.size(Sizes.EmptyIcon),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error.errorTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = error.errorDetail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.movieColors.textMuted,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .widthIn(max = 260.dp)
                    .padding(top = 4.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        AppButton(
            text = "Retry",
            onClick = onRetry,
            variant = ButtonVariant.Secondary,
            shape = PillShape,
            modifier = Modifier.widthIn(max = 200.dp),
        )
    }
}
