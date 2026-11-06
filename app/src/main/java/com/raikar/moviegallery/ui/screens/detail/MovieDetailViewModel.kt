package com.raikar.moviegallery.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.domain.model.ProductionCountry
import com.raikar.moviegallery.domain.usecase.GetCountryDetailsUseCase
import com.raikar.moviegallery.domain.usecase.GetMovieUseCase
import com.raikar.moviegallery.domain.usecase.ObserveIsInWatchlistUseCase
import com.raikar.moviegallery.domain.usecase.ToggleWatchlistUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MovieDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val getMovie: GetMovieUseCase,
    observeIsInWatchlist: ObserveIsInWatchlistUseCase,
    private val toggleWatchlist: ToggleWatchlistUseCase,
    private val getCountryDetails: GetCountryDetailsUseCase,
) : ViewModel() {
    private val movieId: Int = checkNotNull(savedStateHandle["movieId"])

    private val _uiState = MutableStateFlow(MovieDetailUiState())

    /** The in-flight country lookup, cancelled when the sheet closes or another country opens. */
    private var countryJob: Job? = null
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

    fun onCountryClick(country: ProductionCountry) = loadCountry(country)

    fun retryCountry() {
        _uiState.value.countrySheet?.let { loadCountry(it.country) }
    }

    fun onCountrySheetDismiss() {
        countryJob?.cancel()
        _uiState.update { it.copy(countrySheet = null) }
    }

    /** Failures stay inside the sheet; the screen-level [MovieDetailUiState.error] is never touched. */
    private fun loadCountry(country: ProductionCountry) {
        countryJob?.cancel()
        _uiState.update { it.copy(countrySheet = CountrySheetState(country = country)) }
        countryJob =
            viewModelScope.launch {
                val result = getCountryDetails(country.code)
                val sheet =
                    when (result) {
                        is DataResult.Success ->
                            CountrySheetState(
                                country = country,
                                isLoading = false,
                                details = result.data,
                                notFound = result.data == null,
                            )
                        is DataResult.Failure ->
                            CountrySheetState(country = country, isLoading = false, error = result.error)
                    }
                _uiState.update { it.copy(countrySheet = sheet) }
            }
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
