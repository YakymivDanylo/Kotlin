package com.danylo.seriesdiary.data

import kotlinx.coroutines.flow.Flow

class SeriesRepository(private val dao: TvSeriesDao) {

    fun getAllSeries(): Flow<List<TvSeriesEntity>> = dao.getAllSeries()

    fun getFavoriteSeries(): Flow<List<TvSeriesEntity>> = dao.getFavoriteSeries()

    suspend fun getByTitle(title: String): TvSeriesEntity? = dao.getByTitle(title)

    suspend fun insert(series: TvSeriesEntity) = dao.insert(series)

    suspend fun delete(series: TvSeriesEntity) = dao.delete(series)

    suspend fun toggleFavorite(id: Int, isFavorite: Boolean) = dao.updateFavorite(id, isFavorite)

    suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.insertAll(SeedData.initialSeries)
        }
    }
}
