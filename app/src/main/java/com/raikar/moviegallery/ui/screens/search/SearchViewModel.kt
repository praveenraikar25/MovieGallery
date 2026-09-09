package com.raikar.moviegallery.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.usecase.SearchMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

private const val QUERY_DEBOUNCE_MS = 500L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val searchMovies: SearchMoviesUseCase,
    ) : ViewModel() {
        private val query = MutableStateFlow("")

        /** Bumped to re-run the current query after a failure. */
        private val retryTrigger = MutableStateFlow(0)

        /**
         * Debounced so typing doesn't fire a request per keystroke; `transformLatest`
         * both cancels the in-flight request when the query changes again and lets us
         * emit the loading state before awaiting the new one.
         */
        private val results: Flow<Results> =
            // query is a StateFlow, which already drops duplicate values, so re-typing
            // the same text does not refetch.
            combine(query.debounce(QUERY_DEBOUNCE_MS), retryTrigger) { q, _ -> q }
                .transformLatest { q ->
                    emit(Results(isLoading = true))
                    emit(
                        when (val result = searchMovies(q)) {
                            is DataResult.Success -> Results(isLoading = false, movies = result.data)
                            is DataResult.Failure -> Results(isLoading = false, error = result.error)
                        },
                    )
                }

        /**
         * The raw query is combined in undebounced so the text field echoes keystrokes
         * immediately while the request for them is still pending.
         */
        val uiState: StateFlow<SearchUiState> =
            combine(query, results) { q, r ->
                SearchUiState(
                    query = q,
                    isLoading = r.isLoading,
                    results = r.movies,
                    error = r.error,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

        fun onQueryChange(newQuery: String) {
            query.value = newQuery
        }

        fun retry() {
            retryTrigger.value++
        }

        private data class Results(
            val isLoading: Boolean,
            val movies: List<Movie> = emptyList(),
            val error: AppError? = null,
        )
    }
