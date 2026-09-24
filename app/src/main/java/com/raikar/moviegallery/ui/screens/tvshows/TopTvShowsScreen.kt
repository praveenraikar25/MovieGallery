package com.raikar.moviegallery.ui.screens.tvshows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raikar.moviegallery.ui.components.ErrorState
import com.raikar.moviegallery.ui.components.GridTvShowCard
import com.raikar.moviegallery.ui.components.LoadingState
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors
import kotlinx.coroutines.flow.distinctUntilChanged

/** Trigger the next page this many items before the end of what's loaded. */
private const val PREFETCH_THRESHOLD = 6

@Composable
fun TopTvShowsScreen(
    onBack: () -> Unit,
    viewModel: TopTvShowsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow -1
            layoutInfo.totalItemsCount - lastVisible
        }.distinctUntilChanged()
            .collect { remaining ->
                if (remaining in 0 until PREFETCH_THRESHOLD) {
                    viewModel.loadNextPage()
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopTvShowsHeader(onBack = onBack)

        val error = uiState.error
        when {
            uiState.isLoading -> LoadingState()
            error != null && uiState.isEmpty -> ErrorState(error = error, onRetry = viewModel::loadNextPage)
            else ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = Sizes.GridPosterMinWidth),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                ) {
                    items(uiState.shows, key = { it.id }) { show ->
                        GridTvShowCard(show = show)
                    }
                    if (uiState.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun TopTvShowsHeader(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column {
            Text(
                text = "Top TV Shows",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "The highest rated series",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.movieColors.textMuted,
            )
        }
    }
}
