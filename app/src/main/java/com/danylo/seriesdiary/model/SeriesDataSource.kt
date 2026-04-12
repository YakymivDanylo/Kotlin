package com.danylo.seriesdiary.model


object SeriesDataSource {
    val seriesList: List<TvSeries> = listOf(
        TvSeries("Breaking Bad", 2008, SeriesStatus.ENDED, 9.5),
        TvSeries("Fallout", 2024, SeriesStatus.CONTINUING, 8.3),
        TvSeries("The Boys", 2019, SeriesStatus.CONTINUING, 8.7),
        TvSeries("Game of Thrones", 2011, SeriesStatus.ENDED, 9.2),
        TvSeries("Chernobyl", 2019, SeriesStatus.ENDED, 9.4)
    )

    val episodesSet: Set<Episode> = setOf(
        Episode("Breaking Bad", 1, 1),
        Episode("Fallout", 1, 1),
        Episode("The Boys", 1, 1),
        Episode("Chernobyl", 1, 1)
    )

    val seriesMap: Map<String, List<TvSeries>> = mapOf(
        "Драма" to listOf(seriesList[0], seriesList[3], seriesList[4]),
        "Фантастика" to listOf(seriesList[1], seriesList[2])
    )

    val topRatedSeries = seriesList
        .filter { it.rating > 8.5 }
        .sortedByDescending { it.rating }

    val seriesByStatus = seriesList.groupBy { it.status }
}