package com.raikar.moviegallery.data.local

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json

/**
 * Genres are only ever displayed, never queried, so a JSON array in one column is
 * enough — a join table would add a second query per row for nothing.
 */
object Converters {
    @ColumnTypeConverter
    fun genresToJson(genres: List<String>): String = Json.encodeToString(genres)

    @ColumnTypeConverter
    fun genresFromJson(json: String): List<String> = Json.decodeFromString(json)
}
