package com.danylo.seriesdiary.model

class TvSeries(
    title: String,
    releaseYear: Int,
    val status: SeriesStatus = SeriesStatus.CONTINUING,
    var rating: Double = 0.0
) : MediaItem(title, releaseYear) {

    var watchedEpisodes: Int = 0

    var totalEpisodes: Int? = null

    companion object {
        fun createTrendingSeries() : TvSeries{
            return TvSeries("Новий Хіт від Netflix", 2026, SeriesStatus.UPCOMING, 9.5)
        }
    }

    fun getProgress(): String{
        val total = totalEpisodes ?: "Невідомо"
        return "Переглянуто $watchedEpisodes / $total"
    }

}