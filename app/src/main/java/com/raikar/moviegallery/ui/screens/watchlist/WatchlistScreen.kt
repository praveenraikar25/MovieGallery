package com.raikar.moviegallery.ui.screens.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raikar.moviegallery.ui.components.AppIcons
import com.raikar.moviegallery.ui.components.WatchlistMovieCard
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors

@Composable
fun WatchlistScreen(
    onMovieClick: (Int) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isEmpty) {
        WatchlistEmptyState()
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "Watchlist",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${uiState.movies.size} saved",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.movieColors.textMuted,
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 90.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.movies, key = { it.id }) { movie ->
                    WatchlistMovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
                }
            }
        }
    }
}

@Composable
private fun WatchlistEmptyState() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = AppIcons.BookmarkOutline,
            contentDescription = null,
            tint = MaterialTheme.movieColors.textMuted,
            modifier = Modifier.size(Sizes.EmptyIcon),
        )
        androidx.compose.foundation.layout
            .Spacer(modifier = Modifier.size(0.dp, 16.dp))
        Text(
            text = "Your watchlist is empty",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Movies you save will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.movieColors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp).padding(top = 4.dp),
        )
    }
}
