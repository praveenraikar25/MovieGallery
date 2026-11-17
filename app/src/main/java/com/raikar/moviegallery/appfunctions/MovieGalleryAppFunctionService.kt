package com.raikar.moviegallery.appfunctions

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionPermissionRequiredException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.raikar.moviegallery.MainActivity
import com.raikar.moviegallery.domain.model.AppError
import com.raikar.moviegallery.domain.model.DataResult
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.usecase.GetMovieUseCase
import com.raikar.moviegallery.domain.usecase.IsSignedInUseCase
import com.raikar.moviegallery.domain.usecase.ObserveIsInWatchlistUseCase
import com.raikar.moviegallery.domain.usecase.ObserveWatchlistMoviesUseCase
import com.raikar.moviegallery.domain.usecase.SearchMoviesUseCase
import com.raikar.moviegallery.domain.usecase.ToggleWatchlistUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

private const val MAX_SEARCH_RESULTS = 10

/**
 * Exposes movie search, the watchlist and the Movie Detail screen to on-device agents.
 * KSP generates the concrete `MovieGalleryAppFunctionService` (registered in the manifest)
 * and its schema, `movie_gallery_app_functions.xml`.
 *
 * Every function runs the same use cases the screens do, so an agent sees exactly what the
 * UI would: the watchlist is the signed-in account's, and nothing here removes from it.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "MovieGalleryAppFunctionService",
    appFunctionXmlFileName = "movie_gallery_app_functions",
)
abstract class BaseMovieGalleryAppFunctionService : AppFunctionService() {
    private val searchMoviesUseCase: SearchMoviesUseCase by inject()
    private val getMovie: GetMovieUseCase by inject()
    private val isSignedIn: IsSignedInUseCase by inject()
    private val observeWatchlist: ObserveWatchlistMoviesUseCase by inject()
    private val observeIsInWatchlist: ObserveIsInWatchlistUseCase by inject()
    private val toggleWatchlist: ToggleWatchlistUseCase by inject()

    /**
     * Searches The Movie Database (TMDB) for movies whose title matches `query`.
     *
     * @param query Words from the movie's title, such as "dune" or "the godfather". If blank,
     * returns currently popular movies.
     * @return Up to 10 matching movies, best match first. Empty if nothing matches.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun searchMovies(query: String): List<MovieSummary> =
        runAppFunction {
            searchMoviesUseCase(query.trim())
                .orThrow()
                .take(MAX_SEARCH_RESULTS)
                .map { it.toMovieSummary() }
        }

    /**
     * Lists the movies on the signed-in user's MovieGallery watchlist.
     *
     * @return The watchlist's movies, oldest added first. Empty if the watchlist is empty.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getWatchlist(): List<MovieSummary> =
        runAppFunction {
            requireSignedIn()
            observeWatchlist().first().map { it.toMovieSummary() }
        }

    /**
     * Adds a movie to the signed-in user's MovieGallery watchlist. If the movie is already on
     * the watchlist, leaves it unchanged.
     *
     * @param movieId The TMDB movie ID, as returned in `movieId` by a movie search.
     * @return The added movie, and whether it was already on the watchlist.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun addToWatchlist(movieId: Int): AddToWatchlistResult =
        runAppFunction {
            requireSignedIn()
            val movie = loadMovie(movieId)
            val alreadyAdded = observeIsInWatchlist(movieId).first()
            // Toggle is the only write the watchlist offers, so only call it when it adds.
            if (!alreadyAdded) toggleWatchlist(movie)
            AddToWatchlistResult(movie = movie.toMovieSummary(), wasAlreadyInWatchlist = alreadyAdded)
        }

    /**
     * Opens a movie's details screen in MovieGallery, showing its synopsis, cast, runtime and
     * a watchlist button.
     *
     * @param movieId The TMDB movie ID, as returned in `movieId` by a movie search.
     * @return An intent that opens the movie's details screen.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun openMovieDetails(movieId: Int): PendingIntent =
        runAppFunction {
            // Validated first, so an agent never opens a screen that can only show an error.
            loadMovie(movieId)
            val intent =
                Intent(applicationContext, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_MOVIE_ID, movieId)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
            PendingIntent.getActivity(
                applicationContext,
                // One request code per movie, so intents for different movies never replace each other.
                movieId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

    private fun requireSignedIn() {
        if (!isSignedIn()) {
            throw AppFunctionPermissionRequiredException("The user must sign in to MovieGallery to use the watchlist.")
        }
    }

    private suspend fun loadMovie(movieId: Int): Movie {
        if (movieId <= 0) throw AppFunctionInvalidArgumentException("movieId must be a positive TMDB movie ID.")
        return when (val result = getMovie(movieId)) {
            is DataResult.Success -> result.data.summary
            is DataResult.Failure ->
                if (result.error == AppError.NotFound) {
                    throw AppFunctionElementNotFoundException("No movie has TMDB ID $movieId.")
                } else {
                    throw result.error.toAppFunctionException()
                }
        }
    }

    /** Runs [block] off the main thread and lets only [AppFunctionException]s escape. */
    private suspend fun <T> runAppFunction(block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: AppFunctionException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw AppFunctionAppUnknownException("MovieGallery failed unexpectedly: ${e.javaClass.simpleName}.")
            }
        }

    private fun <T> DataResult<T>.orThrow(): T =
        when (this) {
            is DataResult.Success -> data
            is DataResult.Failure -> throw error.toAppFunctionException()
        }

    private fun AppError.toAppFunctionException(): AppFunctionException =
        when (this) {
            AppError.NotFound -> AppFunctionElementNotFoundException("TMDB has no such movie.")
            AppError.Network -> AppFunctionAppUnknownException("Couldn't reach TMDB. The device may be offline.")
            AppError.Unauthorized, AppError.AuthRequired ->
                AppFunctionAppUnknownException("TMDB rejected MovieGallery's credentials.")
            is AppError.Http -> AppFunctionAppUnknownException("TMDB returned HTTP $code.")
            AppError.Serialization, is AppError.Unknown ->
                AppFunctionAppUnknownException("MovieGallery couldn't read TMDB's response.")
        }
}
