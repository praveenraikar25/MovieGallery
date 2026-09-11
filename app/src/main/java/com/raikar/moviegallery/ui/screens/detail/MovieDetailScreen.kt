package com.raikar.moviegallery.ui.screens.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.ui.components.AppButton
import com.raikar.moviegallery.ui.components.AppIcons
import com.raikar.moviegallery.ui.components.ButtonVariant
import com.raikar.moviegallery.ui.components.ErrorState
import com.raikar.moviegallery.ui.components.GenreChip
import com.raikar.moviegallery.ui.components.InitialAvatar
import com.raikar.moviegallery.ui.components.LoadingState
import com.raikar.moviegallery.ui.theme.HeroTitleStyle
import com.raikar.moviegallery.ui.theme.PillShape
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors
import com.raikar.moviegallery.ui.util.MovieGradients

@Composable
fun MovieDetailScreen(
    onBack: () -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LightStatusBarIcons(light = false)

    val error = uiState.error
    val detail = uiState.movie
    when {
        uiState.isLoading && detail == null -> LoadingState()
        error != null && detail == null -> ErrorState(error = error, onRetry = viewModel::retry)
        detail != null ->
            MovieDetailContent(
                detail = detail,
                isInWatchlist = uiState.isInWatchlist,
                onBack = onBack,
                // Passing the loaded detail keeps the toggle from being a no-op that
                // silently does nothing before the request has come back.
                onToggleWatchlist = { viewModel.onToggleWatchlist(detail) },
            )
    }
}

@Composable
private fun MovieDetailContent(
    detail: MovieDetail,
    isInWatchlist: Boolean,
    onBack: () -> Unit,
    onToggleWatchlist: () -> Unit,
) {
    val movie = detail.summary
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val heroHeight = if (isLandscape) Sizes.HeroHeightLandscape else Sizes.HeroHeight

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                        .background(MovieGradients.brushFor(movie.id)),
            ) {
                if (movie.backdropUrl != null) {
                    AsyncImage(
                        model = movie.backdropUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                                ),
                            ),
                )
                Text(
                    text = movie.title,
                    style = HeroTitleStyle,
                    color = Color.White.copy(alpha = 1f),
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp),
                )
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(12.dp)
                            .size(Sizes.BackButton)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBack,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Joins only the fields TMDB returned — year, runtime and
                        // genres are all independently absent for some titles.
                        Text(
                            text = detail.metadataLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.movieColors.textMuted,
                        )
                        if (movie.hasRating) {
                            Text(
                                text = " · ★ ${movie.ratingLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                AppButton(
                    text = if (isInWatchlist) "In Watchlist" else "Add to Watchlist",
                    onClick = onToggleWatchlist,
                    variant = ButtonVariant.Primary,
                    shape = PillShape,
                    leadingIcon = {
                        Icon(
                            imageVector = if (isInWatchlist) AppIcons.BookmarkFilled else AppIcons.BookmarkOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    movie.genres.forEach { genre -> GenreChip(text = genre) }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Synopsis",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = movie.synopsis.ifBlank { "No synopsis available." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.movieColors.textMuted,
                )

                if (detail.cast.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Cast",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                detail.cast.forEachIndexed { index, member ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        InitialAvatar(
                            text = member.initial,
                            size = Sizes.AvatarMd,
                            background = MaterialTheme.movieColors.surfaceAlt,
                            contentColor = MaterialTheme.movieColors.textMuted,
                        )
                        Spacer(modifier = Modifier.padding(start = 12.dp))
                        Column {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = member.role,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.movieColors.textMuted,
                            )
                        }
                    }
                    if (index != detail.cast.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.movieColors.border)
                    }
                }
            }
        }
    }
}

@Composable
private fun LightStatusBarIcons(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(light) {
        val window = (view.context as android.app.Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = light
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}
