package com.danylo.seriesdiary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TvSeriesDao {

    @Query("SELECT * FROM tv_series")
    fun getAllSeries(): Flow<List<TvSeriesEntity>>

    @Query("SELECT * FROM tv_series")
    suspend fun getAllSeriesOnce(): List<TvSeriesEntity>

    @Query("SELECT * FROM tv_series WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TvSeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(series: List<TvSeriesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(series: TvSeriesEntity)

    @Delete
    suspend fun delete(series: TvSeriesEntity)

    @Query("DELETE FROM tv_series WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tv_series")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(series: List<TvSeriesEntity>) {
        clear()
        insertAll(series)
    }

    @Query("UPDATE tv_series SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM tv_series")
    suspend fun count(): Int
}
