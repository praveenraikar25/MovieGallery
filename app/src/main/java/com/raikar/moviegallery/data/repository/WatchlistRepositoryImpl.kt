package com.raikar.moviegallery.data.repository

import com.raikar.moviegallery.data.local.WatchlistDao
import com.raikar.moviegallery.data.local.toDomain
import com.raikar.moviegallery.data.local.toWatchlistEntity
import com.raikar.moviegallery.domain.model.Movie
import com.raikar.moviegallery.domain.repository.AuthRepository
import com.raikar.moviegallery.domain.repository.WatchlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room is the single source of truth: screens only ever read the signed-in account's rows,
 * and switching accounts switches every open screen to the new account's rows. With no
 * signed-in account the watchlist is empty and toggling writes nothing, so no row is ever
 * saved without an owner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class WatchlistRepositoryImpl
    @Inject
    constructor(
        private val dao: WatchlistDao,
        private val authRepository: AuthRepository,
        private val clock: Clock,
    ) : WatchlistRepository {
        override val watchlist: Flow<List<Movie>> =
            authRepository.currentUserId.flatMapLatest { uid ->
                if (uid == null) {
                    flowOf(emptyList())
                } else {
                    dao.observe(uid).map { rows -> rows.map { it.toDomain() } }
                }
            }

        override fun isInWatchlist(id: Int): Flow<Boolean> =
            authRepository.currentUserId
                .flatMapLatest { uid -> if (uid == null) flowOf(false) else dao.observeContains(uid, id) }
                .distinctUntilChanged()

        override suspend fun toggle(movie: Movie) {
            val uid = authRepository.currentUserId.first() ?: return
            dao.toggle(movie.toWatchlistEntity(ownerUid = uid, addedAt = clock.millis()))
        }
    }
