package com.danylo.seriesdiary.data

import com.danylo.seriesdiary.network.CreateSeriesRequest
import com.danylo.seriesdiary.network.RetrofitClient
import com.danylo.seriesdiary.network.SeriesApiService
import com.danylo.seriesdiary.network.UpdateFavoriteRequest
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.IOException

sealed class FetchResult<out T> {
    data class Success<T>(val data: T) : FetchResult<T>()
    data class Offline<T>(val cached: T) : FetchResult<T>()
    data class Error(val message: String) : FetchResult<Nothing>()
}

// open — щоб у unit-тестах можна було створити FakeSeriesRepository,
// який перевизначає createSeries() без реальної мережі/БД.
open class SeriesRepository(
    private val dao: TvSeriesDao,
    private val api: SeriesApiService = RetrofitClient.api
) {

    open fun observeCached(): Flow<List<TvSeriesEntity>> = dao.getAllSeries()

    suspend fun refreshSeries(): FetchResult<List<TvSeriesEntity>> {
        return try {
            val remote = api.getAllSeries()
            // зберігаємо локальні поля (фото, улюблене) під час оновлення з API
            val cachedById = dao.getAllSeriesOnce().associateBy { it.id }
            val entities = remote.map { dto ->
                val cached = cachedById[dto.id ?: ""]
                dto.toEntity(
                    previousFavorite = cached?.isFavorite ?: false,
                    previousPhotoPath = cached?.photoPath
                )
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
            val cached = dao.getById(id)
            val entity = dto.toEntity(
                previousFavorite = cached?.isFavorite ?: false,
                previousPhotoPath = cached?.photoPath
            )
            dao.insert(entity)
            FetchResult.Success(entity)
        } catch (e: IOException) {
            val fallback = dao.getById(id)
            if (fallback != null) FetchResult.Offline(fallback)
            else FetchResult.Error("Немає з'єднання з мережею")
        } catch (e: Exception) {
            FetchResult.Error(e.message ?: "Не вдалось отримати серіал")
        }
    }

    open suspend fun createSeries(
        title: String,
        releaseYear: Int,
        status: String,
        rating: Double,
        isFavorite: Boolean = false,
        numberOfSeasons: Int = 1,
        imdbUrl: String = "",
        comment: String = ""
    ): FetchResult<TvSeriesEntity> {
        return try {
            val dto = api.createSeries(
                CreateSeriesRequest(
                    title = title,
                    releaseYear = releaseYear,
                    status = status,
                    rating = rating,
                    isFavorite = isFavorite,
                    numberOfSeasons = numberOfSeasons,
                    imdbUrl = imdbUrl,
                    comment = comment
                )
            )
            // Use previousFavorite=isFavorite in case API doesn't echo the field back
            val entity = dto.toEntity(previousFavorite = isFavorite)
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
            // видаляємо файл фото перед видаленням запису з БД
            dao.getById(id)?.photoPath?.let { deletePhotoFile(it) }
            dao.deleteById(id)
            FetchResult.Success(Unit)
        } catch (e: IOException) {
            FetchResult.Error("Немає з'єднання з мережею")
        } catch (e: Exception) {
            FetchResult.Error(e.message ?: "Не вдалося видалити серіал")
        }
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        dao.updateFavorite(id, isFavorite)
        try {
            api.updateFavorite(id, UpdateFavoriteRequest(isFavorite))
        } catch (_: Exception) {
            //локальний стан оновлено, API — при наступному refresh
        }
    }

    suspend fun updatePhoto(id: String, newPath: String?) {
        // якщо змінюємо/видаляємо фото — старий файл прибираємо з диску
        val previous = dao.getById(id)?.photoPath
        if (previous != null && previous != newPath) {
            deletePhotoFile(previous)
        }
        dao.updatePhoto(id, newPath)
    }

    private fun deletePhotoFile(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
