package com.raikar.moviegallery.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WatchlistDao {
    // rowid breaks ties between two saves in the same millisecond, so insertion order
    // is stable even when the clock isn't fine-grained enough to.
    @Query("SELECT * FROM watchlist WHERE owner_uid = :ownerUid ORDER BY added_at ASC, rowid ASC")
    abstract fun observe(ownerUid: String): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE owner_uid = :ownerUid AND movie_id = :movieId)")
    abstract fun observeContains(
        ownerUid: String,
        movieId: Int,
    ): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE owner_uid = :ownerUid AND movie_id = :movieId)")
    protected abstract suspend fun contains(
        ownerUid: String,
        movieId: Int,
    ): Boolean

    @Insert
    protected abstract suspend fun insert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE owner_uid = :ownerUid AND movie_id = :movieId")
    protected abstract suspend fun delete(
        ownerUid: String,
        movieId: Int,
    )

    /**
     * The check and the write run in one transaction. Room serializes write transactions,
     * so rapid taps each see the previous tap's result and can never insert a duplicate.
     */
    @Transaction
    open suspend fun toggle(entity: WatchlistEntity) {
        if (contains(entity.ownerUid, entity.movieId)) {
            delete(entity.ownerUid, entity.movieId)
        } else {
            insert(entity)
        }
    }
}
