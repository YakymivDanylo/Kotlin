@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.danylo.seriesdiary.network

import com.danylo.seriesdiary.data.TvSeriesEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeriesDto(
    @SerialName("id") val id: String? = null,
    @SerialName("title") val title: String,
    @SerialName("releaseYear") val releaseYear: Int,
    @SerialName("status") val status: String? = null,
    @SerialName("rating") val rating: Double? = null
) {
    fun toEntity(previousFavorite: Boolean = false): TvSeriesEntity = TvSeriesEntity(
        id = id ?: title,
        title = title,
        releaseYear = releaseYear,
        status = status ?: "UNKNOWN",
        rating = rating ?: 0.0,
        isFavorite = previousFavorite
    )
}

@Serializable
data class CreateSeriesRequest(
    @SerialName("title") val title: String,
    @SerialName("releaseYear") val releaseYear: Int,
    @SerialName("status") val status: String,
    @SerialName("rating") val rating: Double
)
