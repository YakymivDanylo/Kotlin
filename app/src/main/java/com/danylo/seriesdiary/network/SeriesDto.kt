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
    @SerialName("rating") val rating: Double? = null,
    @SerialName("numberOfSeasons") val numberOfSeasons: Int? = null,
    @SerialName("imdbUrl") val imdbUrl: String? = null,
    @SerialName("comment") val comment: String? = null
) {
    fun toEntity(previousFavorite: Boolean = false): TvSeriesEntity = TvSeriesEntity(
        id = id ?: title,
        title = title,
        releaseYear = releaseYear,
        status = status ?: "UNKNOWN",
        rating = rating ?: 0.0,
        isFavorite = previousFavorite,
        numberOfSeasons = numberOfSeasons ?: 1,
        imdbUrl = imdbUrl ?: "",
        comment = comment ?: ""
    )
}

@Serializable
data class CreateSeriesRequest(
    @SerialName("title") val title: String,
    @SerialName("releaseYear") val releaseYear: Int,
    @SerialName("status") val status: String,
    @SerialName("rating") val rating: Double,
    @SerialName("numberOfSeasons") val numberOfSeasons: Int,
    @SerialName("imdbUrl") val imdbUrl: String,
    @SerialName("comment") val comment: String
)
