package com.raikar.moviegallery.ui.screens.detail

import app.cash.turbine.test
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.CastMember
import com.raikar.moviegallery.domain.model.CountryDetails
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.model.MovieDetail
import com.raikar.moviegallery.domain.model.ProductionCountry
import com.raikar.moviegallery.domain.usecase.GetCountryDetailsUseCase
import com.raikar.moviegallery.domain.usecase.GetMovieUseCase
import com.raikar.moviegallery.domain.usecase.ObserveIsInWatchlistUseCase
import com.raikar.moviegallery.domain.usecase.ToggleWatchlistUseCase
import com.raikar.moviegallery.testutil.FakeCountryRepository
import com.raikar.moviegallery.testutil.FakeMovieRepository
import com.raikar.moviegallery.testutil.FakeWatchlistRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    // viewModelScope runs on Dispatchers.Main, which tests replace with a test dispatcher.
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private val movie =
        Movie(
            id = 42,
            title = "Movie 42",
            year = 2024,
            rating = 7.0,
            genres = listOf("Drama"),
            synopsis = "",
            posterUrl = null,
            backdropUrl = null,
        )

    private val movieDetail =
        MovieDetail(
            summary = movie,
            runtimeMinutes = 118,
            tagline = "A tagline",
            cast = listOf(CastMember(name = "Someone", role = "Someone Else")),
        )

    private fun viewModel(
        movieRepository: FakeMovieRepository = FakeMovieRepository(movieDetailResult = DataResult.Success(movieDetail)),
        watchlistRepository: FakeWatchlistRepository = FakeWatchlistRepository(),
        countryRepository: FakeCountryRepository = FakeCountryRepository(),
    ) = MovieDetailViewModel(
        movieId = movie.id,
        getMovie = GetMovieUseCase(movieRepository),
        observeIsInWatchlist = ObserveIsInWatchlistUseCase(watchlistRepository),
        toggleWatchlist = ToggleWatchlistUseCase(watchlistRepository),
        getCountryDetails = GetCountryDetailsUseCase(countryRepository),
    )

    private val japan = ProductionCountry(code = "JP", name = "Japan")
    private val korea = ProductionCountry(code = "KR", name = "South Korea")
    private val sovietUnion = ProductionCountry(code = "SU", name = "Soviet Union")

    private val japanDetails =
        CountryDetails(
            code = "JP",
            name = "Japan",
            nativeName = "日本",
            flag = "🇯🇵",
            capital = "Tokyo",
            continent = "Asia",
            languages = listOf("Japanese"),
            currencies = listOf("JPY"),
        )

    private val koreaDetails = japanDetails.copy(code = "KR", name = "South Korea", nativeName = "대한민국")

    @Test
    fun `loads the movie detail on init`() =
        runTest {
            val vm = viewModel()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals(movieDetail, state.movie)
            assertNull(state.error)
        }

    @Test
    fun `emits error state when loading fails`() =
        runTest {
            val vm =
                viewModel(
                    movieRepository = FakeMovieRepository(movieDetailResult = DataResult.Failure(AppError.NotFound)),
                )

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.movie)
            assertEquals(AppError.NotFound, state.error)
        }

    @Test
    fun `retry reloads the movie detail and clears the previous error`() =
        runTest {
            val movieRepository = FakeMovieRepository(movieDetailResult = DataResult.Failure(AppError.NotFound))
            val vm = viewModel(movieRepository = movieRepository)
            assertEquals(AppError.NotFound, vm.uiState.value.error)

            movieRepository.movieDetailResult = DataResult.Success(movieDetail)
            vm.retry()

            val state = vm.uiState.value
            assertNull(state.error)
            assertEquals(movieDetail, state.movie)
        }

    @Test
    fun `reflects watchlist membership and updates when it changes`() =
        runTest {
            val watchlistRepository = FakeWatchlistRepository()
            val vm = viewModel(watchlistRepository = watchlistRepository)

            vm.uiState.test {
                assertFalse(awaitItem().isInWatchlist)

                watchlistRepository.toggle(movie)
                assertTrue(awaitItem().isInWatchlist)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onToggleWatchlist toggles the current movie in the watchlist repository`() =
        runTest {
            val watchlistRepository = FakeWatchlistRepository()
            val vm = viewModel(watchlistRepository = watchlistRepository)

            vm.onToggleWatchlist(movieDetail)

            assertEquals(listOf(movie), watchlistRepository.toggleCalls)
        }

    @Test
    fun `tapping a country opens its sheet loading and then shows its details`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val countries =
                FakeCountryRepository(
                    results = mutableMapOf("JP" to DataResult.Success(japanDetails)),
                    gates = mutableMapOf("JP" to gate),
                )
            val vm = viewModel(countryRepository = countries)

            vm.onCountryClick(japan)
            assertEquals(CountrySheetState(country = japan, isLoading = true), vm.uiState.value.countrySheet)

            gate.complete(Unit)
            val sheet = vm.uiState.value.countrySheet!!
            assertFalse(sheet.isLoading)
            assertEquals(japanDetails, sheet.details)
            assertFalse(sheet.notFound)
            assertNull(sheet.error)
        }

    @Test
    fun `a country the source does not know shows not found without an error`() =
        runTest {
            val vm = viewModel(countryRepository = FakeCountryRepository(defaultResult = DataResult.Success(null)))

            vm.onCountryClick(sovietUnion)

            val sheet = vm.uiState.value.countrySheet!!
            assertTrue(sheet.notFound)
            assertNull(sheet.error)
            assertNull(sheet.details)
            assertEquals(sovietUnion, sheet.country)
        }

    @Test
    fun `a failed lookup stays inside the sheet and leaves the movie untouched`() =
        runTest {
            val vm =
                viewModel(
                    countryRepository = FakeCountryRepository(defaultResult = DataResult.Failure(AppError.Network)),
                )

            vm.onCountryClick(japan)

            val state = vm.uiState.value
            assertEquals(AppError.Network, state.countrySheet?.error)
            assertFalse(state.countrySheet!!.isLoading)
            assertEquals(movieDetail, state.movie)
            assertNull(state.error)
        }

    @Test
    fun `retrying a failed country reloads it`() =
        runTest {
            val countries = FakeCountryRepository(defaultResult = DataResult.Failure(AppError.Network))
            val vm = viewModel(countryRepository = countries)
            vm.onCountryClick(japan)

            countries.defaultResult = DataResult.Success(japanDetails)
            val gate = CompletableDeferred<Unit>()
            countries.gates["JP"] = gate
            vm.retryCountry()
            assertTrue(
                vm.uiState.value.countrySheet!!
                    .isLoading,
            )

            gate.complete(Unit)
            assertEquals(
                japanDetails,
                vm.uiState.value.countrySheet
                    ?.details,
            )
            assertEquals(listOf("JP", "JP"), countries.requestedCodes)
        }

    @Test
    fun `dismissing closes the sheet and keeps the movie`() =
        runTest {
            val vm =
                viewModel(countryRepository = FakeCountryRepository(defaultResult = DataResult.Success(japanDetails)))
            vm.onCountryClick(japan)

            vm.onCountrySheetDismiss()

            assertNull(vm.uiState.value.countrySheet)
            assertEquals(movieDetail, vm.uiState.value.movie)
        }

    @Test
    fun `dismissing while loading drops the late result`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val countries =
                FakeCountryRepository(
                    defaultResult = DataResult.Success(japanDetails),
                    gates = mutableMapOf("JP" to gate),
                )
            val vm = viewModel(countryRepository = countries)
            vm.onCountryClick(japan)

            vm.onCountrySheetDismiss()
            gate.complete(Unit)

            assertNull(vm.uiState.value.countrySheet)
        }

    @Test
    fun `tapping a second country while the first loads shows only the second`() =
        runTest {
            val japanGate = CompletableDeferred<Unit>()
            val countries =
                FakeCountryRepository(
                    results =
                        mutableMapOf(
                            "JP" to DataResult.Success(japanDetails),
                            "KR" to DataResult.Success(koreaDetails),
                        ),
                    gates = mutableMapOf("JP" to japanGate),
                )
            val vm = viewModel(countryRepository = countries)

            vm.onCountryClick(japan)
            vm.onCountryClick(korea)
            japanGate.complete(Unit)

            val sheet = vm.uiState.value.countrySheet!!
            assertEquals(korea, sheet.country)
            assertEquals(koreaDetails, sheet.details)
        }
}
