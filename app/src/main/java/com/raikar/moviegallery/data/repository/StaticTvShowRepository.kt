package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.domain.model.TvShow
import com.raikar.moviegallery.domain.repository.TvShowRepository
import javax.inject.Inject

/**
 * Placeholder data pending a TMDB `/tv/top_rated` call — swapping in the network
 * source should mean replacing this class alone, with a DTO and mapper alongside
 * the movie ones in `data/remote`.
 */
class StaticTvShowRepository
    @Inject
    constructor() : TvShowRepository {
        override suspend fun topTvShows(): List<TvShow> = topShows

        private companion object {
            // Ordered best-first; ids are positional placeholders, not TMDB ids.
            val topShows =
                listOf(
                    TvShow(1, "Breaking Bad", 2008, 8.9, listOf("Drama", "Crime")),
                    TvShow(2, "The Sopranos", 1999, 8.7, listOf("Drama", "Crime")),
                    TvShow(3, "Chernobyl", 2019, 8.7, listOf("Drama", "History")),
                    TvShow(4, "The Wire", 2002, 8.6, listOf("Drama", "Crime")),
                    TvShow(5, "Arcane", 2021, 8.6, listOf("Animation", "Action")),
                    TvShow(6, "Band of Brothers", 2001, 8.5, listOf("Drama", "War")),
                    TvShow(7, "Better Call Saul", 2015, 8.5, listOf("Drama", "Crime")),
                    TvShow(8, "The Last of Us", 2023, 8.5, listOf("Drama", "Sci-Fi")),
                    TvShow(9, "Severance", 2022, 8.4, listOf("Drama", "Sci-Fi")),
                    TvShow(10, "Succession", 2018, 8.3, listOf("Drama")),
                )
        }
    }
