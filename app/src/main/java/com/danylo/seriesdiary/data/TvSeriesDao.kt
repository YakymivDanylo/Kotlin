package com.danylo.seriesdiary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TvSeriesDao {

    @Query("SELECT * FROM tv_series")
    fun getAllSeries(): Flow<List<TvSeriesEntity>>

    @Query("SELECT * FROM tv_series WHERE isFavorite = 1")
    fun getFavoriteSeries(): Flow<List<TvSeriesEntity>>

    @Query("SELECT * FROM tv_series WHERE title = :title LIMIT 1")
    suspend fun getByTitle(title: String): TvSeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(series: List<TvSeriesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(series: TvSeriesEntity)

    @Delete
    suspend fun delete(series: TvSeriesEntity)

    @Query("UPDATE tv_series SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM tv_series")
    suspend fun count(): Int
}
