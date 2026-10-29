package com.raikar.moviegallery.ui.screens.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.ui.components.AppButton
import com.raikar.moviegallery.ui.components.AppIcons
import com.raikar.moviegallery.ui.components.ButtonVariant
import com.raikar.moviegallery.ui.components.ErrorState
import com.raikar.moviegallery.ui.components.GenreChip
import com.raikar.moviegallery.ui.components.LoadingState
import com.raikar.moviegallery.ui.theme.HeroTitleStyle
import com.raikar.moviegallery.ui.theme.PillShape
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors
import com.raikar.moviegallery.ui.util.MovieGradients
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.roundToInt

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
    val expandedHeight = if (isLandscape) Sizes.HeroHeightLandscape else Sizes.HeroHeight
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val collapsedHeight = statusBarHeight + CollapsedBarHeight

    val density = LocalDensity.current
    val expandedPx = with(density) { expandedHeight.toPx() }
    val collapsedPx = with(density) { collapsedHeight.toPx() }
    val collapseDistancePx = (expandedPx - collapsedPx).roundToInt().coerceAtLeast(0)

    val listState = rememberLazyListState()
    // Read only from layout/draw lambdas below, so scrolling re-measures the header
    // without recomposing the screen.
    val fraction by remember(expandedPx, collapsedPx) {
        derivedStateOf {
            collapseFraction(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                expandedHeightPx = expandedPx,
                collapsedHeightPx = collapsedPx,
            )
        }
    }

    // A short page can't scroll far enough to finish collapsing, so pad the end of the
    // list by the shortfall. Only measurable while every item is laid out.
    var viewportPx by remember { mutableIntStateOf(0) }
    var extraBottomPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(listState, collapseDistancePx) {
        snapshotFlow {
            val info = listState.layoutInfo
            val items = info.visibleItemsInfo
            val allLaidOut =
                items.isNotEmpty() && items.first().index == 0 && items.last().index == info.totalItemsCount - 1
            if (allLaidOut && viewportPx > 0) {
                extraBottomPaddingPx(viewportPx, items.sumOf { it.size }, collapseDistancePx)
            } else {
                null
            }
        }.filterNotNull().collect { extraBottomPx = it }
    }

    Box(modifier = Modifier.fillMaxSize().onSizeChanged { viewportPx = it.height }) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = with(density) { extraBottomPx.toDp() }),
        ) {
            // The header is drawn over the list; this spacer holds its place so the content
            // starts below the image and then scrolls up behind the pinned strip.
            item { Spacer(modifier = Modifier.height(expandedHeight)) }

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
                }
            }

            if (detail.cast.isNotEmpty()) {
                item { CastSection(cast = detail.cast, modifier = Modifier.padding(bottom = 20.dp)) }
            }
        }

        CollapsingHeader(
            movie = movie,
            expandedHeight = expandedHeight,
            collapsedHeight = collapsedHeight,
            statusBarHeight = statusBarHeight,
            fraction = { fraction },
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

/**
 * The backdrop, title and back button. Its height, scrim and title placement are all driven by
 * [fraction] (0 = expanded, 1 = pinned strip) and read only in layout/draw, never in composition.
 * The text and icons stay white throughout, so the status bar icons need no scroll-driven change.
 */
@Composable
private fun CollapsingHeader(
    movie: Movie,
    expandedHeight: Dp,
    collapsedHeight: Dp,
    statusBarHeight: Dp,
    fraction: () -> Float,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val height = lerp(expandedHeight.toPx(), collapsedHeight.toPx(), fraction()).roundToInt()
                    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                    layout(placeable.width, height) { placeable.place(0, 0) }
                }.clipToBounds()
                .background(MovieGradients.brushFor(movie.id)),
    ) {
        if (movie.backdropUrl != null) {
            // Keeps its expanded size and is cropped by the shrinking header, centered, so
            // the image doesn't squash as it collapses.
            AsyncImage(
                model = movie.backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .requiredHeight(expandedHeight)
                        .align(Alignment.Center),
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
                    ).drawBehind { drawRect(Color.Black.copy(alpha = lerp(0f, COLLAPSED_SCRIM_ALPHA, fraction()))) },
        )
        Text(
            text = movie.title,
            style = HeroTitleStyle,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier.layout { measurable, constraints ->
                    val f = fraction()
                    val scale = lerp(1f, COLLAPSED_TITLE_SCALE, f)
                    val startPx = lerp(TitleStartExpanded.toPx(), TitleStartCollapsed.toPx(), f)
                    // Laid out at full size and scaled, so the width it may take is divided by
                    // the scale to make the wrapped text fit the room left beside the back button.
                    val maxWidth = ((constraints.maxWidth - startPx - TitleEnd.toPx()) / scale).roundToInt()
                    val placeable = measurable.measure(Constraints(maxWidth = maxWidth.coerceAtLeast(0)))
                    val scaledHeight = placeable.height * scale
                    val statusPx = statusBarHeight.toPx()
                    val expandedY = constraints.maxHeight - TitleBottomPadding.toPx() - scaledHeight
                    val collapsedY = statusPx + (collapsedHeight.toPx() - statusPx - scaledHeight) / 2
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeWithLayer(startPx.roundToInt(), lerp(expandedY, collapsedY, f).roundToInt()) {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                    }
                },
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

/** Height of the pinned strip below the status bar. */
private val CollapsedBarHeight = 56.dp

/** Extra darkening layered on the backdrop once collapsed, so the white title stays legible on the thin crop. */
private const val COLLAPSED_SCRIM_ALPHA = 0.35f

/** HeroTitleStyle is 28sp; this lands the collapsed title at about 18sp. */
private const val COLLAPSED_TITLE_SCALE = 0.64f

private val TitleStartExpanded = 20.dp
private val TitleEnd = 20.dp
private val TitleBottomPadding = 20.dp

/** Clears the 36dp back button (12dp inset on each side) plus a gap. */
private val TitleStartCollapsed = 60.dp

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
