package com.raikar.moviegallery.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.domain.usecase.GetMovieUseCase
import com.raikar.moviegallery.domain.usecase.ObserveIsInWatchlistUseCase
import com.raikar.moviegallery.domain.usecase.ToggleWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getMovie: GetMovieUseCase,
        observeIsInWatchlist: ObserveIsInWatchlistUseCase,
        private val toggleWatchlist: ToggleWatchlistUseCase,
    ) : ViewModel() {
        private val movieId: Int = checkNotNull(savedStateHandle["movieId"])

        private val _uiState = MutableStateFlow(MovieDetailUiState())
        val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

        init {
            load()
            // Watchlist membership is independent of the detail request, so it folds
            // into state on its own rather than being re-fetched alongside it.
            viewModelScope.launch {
                observeIsInWatchlist(movieId).collect { isInWatchlist ->
                    _uiState.update { it.copy(isInWatchlist = isInWatchlist) }
                }
            }
        }

        fun retry() = load()

        /** [movie] comes from the screen's loaded branch, so it is never stale or null. */
        fun onToggleWatchlist(movie: MovieDetail) {
            viewModelScope.launch { toggleWatchlist(movie.summary) }
        }

        private fun load() {
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                // Fetched outside update {}: that lambda can be re-run on write
                // contention, which would re-issue the request.
                val result = getMovie(movieId)
                _uiState.update { current ->
                    when (result) {
                        is DataResult.Success ->
                            current.copy(isLoading = false, movie = result.data, error = null)
                        is DataResult.Failure ->
                            current.copy(isLoading = false, error = result.error)
                    }
                }
            }
        }
    }
