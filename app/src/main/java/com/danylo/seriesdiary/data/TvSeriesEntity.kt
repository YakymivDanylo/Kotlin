package com.danylo.seriesdiary.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.danylo.seriesdiary.model.SeriesStatus

@Entity(tableName = "tv_series")
data class TvSeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val releaseYear: Int,
    val status: String,
    val rating: Double,
    val isFavorite: Boolean = false
) {
    fun toSeriesStatus(): SeriesStatus =
        SeriesStatus.entries.find { it.name == status } ?: SeriesStatus.UNKNOWN
}
