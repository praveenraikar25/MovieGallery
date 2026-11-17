package com.raikar.moviegallery.appfunctions

import androidx.appfunctions.AppFunctionSerializable
import com.raikar.moviegallery.domain.model.Movie

/** A movie from The Movie Database (TMDB) catalog. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class MovieSummary(
    /** The TMDB movie ID. */
    val movieId: Int,
    /** The movie's title. */
    val title: String,
    /** The release year, or null when the movie has no release date yet. */
    val releaseYear: Int?,
    /** The average TMDB user rating from 0 to 10. 0 means the movie has no ratings yet. */
    val rating: Double,
    /** The movie's genres, such as "Drama" or "Science Fiction". */
    val genres: List<String>,
    /** A short plot summary. Empty when TMDB has none. */
    val synopsis: String,
)

/** The outcome of adding a movie to the watchlist. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AddToWatchlistResult(
    /** The movie that is now on the watchlist. */
    val movie: MovieSummary,
    /** True if the movie was already on the watchlist, so nothing changed. */
    val wasAlreadyInWatchlist: Boolean,
)

internal fun Movie.toMovieSummary() =
    MovieSummary(
        movieId = id,
        title = title,
        releaseYear = year,
        rating = rating,
        genres = genres,
        synopsis = synopsis,
    )
