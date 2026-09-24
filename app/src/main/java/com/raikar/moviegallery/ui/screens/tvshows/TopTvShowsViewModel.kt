package com.raikar.moviegallery.ui.screens.tvshows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raikar.moviegallery.domain.model.DataResult
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

        /**
         * Guards re-entrancy. Distinct from the UI-facing [TopTvShowsUiState.isLoading]
         * flag, which starts `true` at rest (nothing has loaded yet) rather than
         * meaning "a fetch is in flight".
         */
        private var isFetching = false

        init {
            loadNextPage()
        }

        /**
         * Fetches the page after the last one loaded. A no-op while a fetch is
         * already in flight or once every page has been loaded, so it's safe to
         * call both on scroll and as the retry action.
         */
        fun loadNextPage() {
            val state = _uiState.value
            if (isFetching || !state.canLoadMore) return
            isFetching = true

            val nextPage = state.page + 1
            viewModelScope.launch {
                _uiState.update {
                    if (it.page ==
                        0
                    ) {
                        it.copy(isLoading = true, error = null)
                    } else {
                        it.copy(isLoadingMore = true, error = null)
                    }
                }

                when (val result = getTopTvShows(nextPage)) {
                    is DataResult.Success ->
                        _uiState.update {
                            it.copy(
                                shows = it.shows + result.data.shows,
                                page = result.data.page,
                                totalPages = result.data.totalPages,
                                isLoading = false,
                                isLoadingMore = false,
                            )
                        }

                    is DataResult.Failure ->
                        _uiState.update {
                            it.copy(isLoading = false, isLoadingMore = false, error = result.error)
                        }
                }
                isFetching = false
            }
        }
    }
