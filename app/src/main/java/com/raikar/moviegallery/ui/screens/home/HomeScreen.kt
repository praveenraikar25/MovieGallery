package com.raikar.moviegallery.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raikar.moviegallery.domain.model.MovieSection
import com.raikar.moviegallery.ui.components.ErrorState
import com.raikar.moviegallery.ui.components.InitialAvatar
import com.raikar.moviegallery.ui.components.LoadingState
import com.raikar.moviegallery.ui.components.RailMovieCard
import com.raikar.moviegallery.ui.components.SectionHeader
import com.raikar.moviegallery.ui.theme.Sizes

@Composable
fun HomeScreen(
    onMovieClick: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Marquee",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            InitialAvatar(text = "A", size = Sizes.AvatarSm)
        }

        val error = uiState.error
        when {
            uiState.isLoading -> LoadingState()
            error != null -> ErrorState(error = error, onRetry = viewModel::retry)
            else ->
                HomeSections(
                    sections = uiState.sections,
                    onMovieClick = onMovieClick,
                )
        }
    }
}

@Composable
private fun HomeSections(
    sections: List<MovieSection>,
    onMovieClick: (Int) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sections, key = { it.title }) { section ->
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(bottom = 26.dp)) {
                SectionHeader(title = section.title)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(section.movies, key = { it.id }) { movie ->
                        RailMovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
                    }
                }
            }
        }

        item {
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(90.dp),
            )
        }
    }
}
