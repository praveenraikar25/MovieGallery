package com.raikar.moviegallery.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raikar.moviegallery.ui.components.AppInput
import com.raikar.moviegallery.ui.components.ErrorState
import com.raikar.moviegallery.ui.components.GridMovieCard
import com.raikar.moviegallery.ui.theme.Sizes
import com.raikar.moviegallery.ui.theme.movieColors

@Composable
fun SearchScreen(
    onMovieClick: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 14.dp),
        )
        AppInput(
            value = uiState.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "Search movies…",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.movieColors.textMuted,
                    modifier = Modifier.size(16.dp),
                )
            },
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            text = uiState.headerLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.movieColors.textMuted,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        val error = uiState.error
        if (error != null) {
            ErrorState(error = error, onRetry = viewModel::retry)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = Sizes.GridPosterMinWidth),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 90.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.results, key = { it.id }) { movie ->
                    GridMovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
                }
            }
        }
    }
}
