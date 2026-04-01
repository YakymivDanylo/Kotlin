package com.danylo.seriesdiary.data

import com.danylo.seriesdiary.model.SeriesDataSource
import com.danylo.seriesdiary.model.TvSeries
import kotlinx.coroutines.delay

class SeriesRepository {
    suspend fun getSeriesList(): List<TvSeries> {
        delay(1000)
        return SeriesDataSource.seriesList
    }

    suspend fun getSeriesByTitle(title: String): TvSeries? {
        delay(500)
        return SeriesDataSource.seriesList.find { it.title == title }
    }
}