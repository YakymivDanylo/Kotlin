package com.danylo.seriesdiary.data

import com.danylo.seriesdiary.network.CreateSeriesRequest
import com.danylo.seriesdiary.network.RetrofitClient
import com.danylo.seriesdiary.network.SeriesApiService
import kotlinx.coroutines.flow.Flow
import java.io.IOException

sealed class FetchResult<out T> {
    data class Success<T>(val data: T) : FetchResult<T>()
    data class Offline<T>(val cached: T) : FetchResult<T>()
    data class Error(val message: String) : FetchResult<Nothing>()
}

class SeriesRepository(
    private val dao: TvSeriesDao,
    private val api: SeriesApiService = RetrofitClient.api
) {

    fun observeCached(): Flow<List<TvSeriesEntity>> = dao.getAllSeries()

    suspend fun refreshSeries(): FetchResult<List<TvSeriesEntity>> {
        return try {
            val remote = api.getAllSeries()
            val existingFavorites = dao.getAllSeriesOnce()
                .associate { it.id to it.isFavorite }
            val entities = remote.map { dto ->
                dto.toEntity(previousFavorite = existingFavorites[dto.id ?: ""] ?: false)
            }
            dao.replaceAll(entities)
            FetchResult.Success(entities)
        } catch (e: IOException) {
            val cached = dao.getAllSeriesOnce()
            if (cached.isNotEmpty()) FetchResult.Offline(cached)
            else FetchResult.Error("Немає з'єднання з мережею")
        } catch (e: Exception) {
            FetchResult.Error(e.message ?: "Невідома помилка мережі")
        }
    }

    suspend fun fetchSeriesById(id: String): FetchResult<TvSeriesEntity> {
        return try {
            val dto = api.getSeriesById(id)
            val existingFavorite = dao.getById(id)?.isFavorite ?: false
            val entity = dto.toEntity(previousFavorite = existingFavorite)
            dao.insert(entity)
            FetchResult.Success(entity)
        } catch (e: IOException) {
            val cached = dao.getById(id)
            if (cached != null) FetchResult.Offline(cached)
            else FetchResult.Error("Немає з'єднання з мережею")
        } catch (e: Exception) {
            FetchResult.Error(e.message ?: "Не вдалось отримати серіал")
        }
    }

    suspend fun createSeries(
        title: String,
        releaseYear: Int,
        status: String,
        rating: Double,
        isFavorite: Boolean = false
    ): FetchResult<TvSeriesEntity> {
        return try {
            val dto = api.createSeries(CreateSeriesRequest(title, releaseYear, status, rating))
            val entity = dto.toEntity().copy(isFavorite = isFavorite)
            dao.insert(entity)
            FetchResult.Success(entity)
        } catch (e: IOException) {
            FetchResult.Error("Немає з'єднання з мережею")
        } catch (e: Exception) {
            FetchResult.Error(e.message ?: "Не вдалося створити серіал")
        }
    }

    suspend fun deleteSeries(id: String): FetchResult<Unit> {
        return try {
            api.deleteSeries(id)
            dao.deleteById(id)
            FetchResult.Success(Unit)
        } catch (e: IOException) {
            FetchResult.Error("Немає з'єднання з мережею")
        } catch (e: Exception) {
            FetchResult.Error(e.message ?: "Не вдалося видалити серіал")
        }
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) =
        dao.updateFavorite(id, isFavorite)
}
