package com.danylo.seriesdiary.data

import com.danylo.seriesdiary.model.SeriesStatus

object SeedData {
    val initialSeries = listOf(
        TvSeriesEntity(
            title = "Breaking Bad",
            releaseYear = 2008,
            status = SeriesStatus.ENDED.name,
            rating = 9.5
        ),
        TvSeriesEntity(
            title = "Fallout",
            releaseYear = 2024,
            status = SeriesStatus.CONTINUING.name,
            rating = 8.3
        ),
        TvSeriesEntity(
            title = "The Boys",
            releaseYear = 2019,
            status = SeriesStatus.CONTINUING.name,
            rating = 8.7
        ),
        TvSeriesEntity(
            title = "Game of Thrones",
            releaseYear = 2011,
            status = SeriesStatus.ENDED.name,
            rating = 9.2
        ),
        TvSeriesEntity(
            title = "Chernobyl",
            releaseYear = 2019,
            status = SeriesStatus.ENDED.name,
            rating = 9.4
        )
    )
}
