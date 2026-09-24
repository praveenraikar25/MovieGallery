package com.raikar.moviegallery.domain.model

/** One page of a paginated top-TV-shows request. */
data class TvShowPage(
    val shows: List<TvShow>,
    val page: Int,
    val totalPages: Int,
)
