package com.raikar.moviegallery.ui.screens.tvshows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raikar.moviegallery.domain.usecase.GetTopTvShowsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopTvShowsViewModel
    @Inject
    constructor(
        private val getTopTvShows: GetTopTvShowsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TopTvShowsUiState())
        val uiState: StateFlow<TopTvShowsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                // Fetched outside update {}: that lambda can be re-run on write
                // contention, which would re-issue the request.
                val shows = getTopTvShows()
                _uiState.update { it.copy(shows = shows, isLoading = false) }
            }
        }
    }
