package com.danylo.seriesdiary

import com.danylo.seriesdiary.data.FetchResult
import com.danylo.seriesdiary.data.SeriesRepository
import com.danylo.seriesdiary.data.TvSeriesDao
import com.danylo.seriesdiary.data.TvSeriesEntity
import com.danylo.seriesdiary.network.CreateSeriesRequest
import com.danylo.seriesdiary.network.SeriesApiService
import com.danylo.seriesdiary.network.SeriesDto
import com.danylo.seriesdiary.network.UpdateFavoriteRequest
import com.danylo.seriesdiary.viewmodel.AddSeriesUiState
import com.danylo.seriesdiary.viewmodel.AddSeriesViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Завдання 1 — Модульне тестування ViewModel з CRUD-операціями.
 *
 * Покриває AddSeriesViewModel (ЛР №9): додавання серіалу через мережевий шар
 * SeriesRepository. Структура — AAA (Arrange — Act — Assert).
 *
 * Покриті сценарії:
 *   - позитивний: успішне створення -> стан Saved;
 *   - негативний: помилка мережі -> стан Error з повідомленням;
 *   - edge case: повторний save() під час Saving ігнорується.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddSeriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `позитивний - save повертає Saved при успіху API`() = runTest {
        // Arrange
        val created = sampleEntity(id = "new-1", title = "Severance")
        val repository = FakeSeriesRepository { FetchResult.Success(created) }
        val viewModel = AddSeriesViewModel(repository)

        // Act
        viewModel.save(
            title = "Severance",
            releaseYear = 2022,
            status = "CONTINUING",
            rating = 8.7,
            isFavorite = true,
            numberOfSeasons = 2,
            imdbUrl = "https://www.imdb.com/title/tt11280740/",
            comment = "Must watch"
        )
        advanceUntilIdle()

        // Assert
        assertEquals(AddSeriesUiState.Saved, viewModel.uiState.value)
        assertEquals(1, repository.createCallCount)
        assertEquals("Severance", repository.lastTitle)
        assertEquals(2022, repository.lastYear)
    }

    @Test
    fun `негативний - save повертає Error з повідомленням при помилці мережі`() = runTest {
        // Arrange
        val repository = FakeSeriesRepository { FetchResult.Error("Немає з'єднання з мережею") }
        val viewModel = AddSeriesViewModel(repository)

        // Act
        viewModel.save("Foundation", 2021, "CONTINUING", 8.4)
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue("Очікувався Error, отримано $state", state is AddSeriesUiState.Error)
        assertEquals("Немає з'єднання з мережею", (state as AddSeriesUiState.Error).message)
    }

    @Test
    fun `edge case - повторний save під час Saving ігнорується`() = runTest {
        // Arrange: перший виклик "зависає" на gate, імітуючи стан Saving
        val gate = CompletableDeferred<FetchResult<TvSeriesEntity>>()
        val repository = FakeSeriesRepository { gate.await() }
        val viewModel = AddSeriesViewModel(repository)

        // Act: швидко натиснули двічі
        viewModel.save("Lost", 2004, "ENDED", 8.1)   // стартує, зависає на gate -> Saving
        viewModel.save("Lost", 2004, "ENDED", 8.1)   // має бути проігнорований

        // Assert: createSeries викликано лише один раз
        assertEquals(AddSeriesUiState.Saving, viewModel.uiState.value)
        assertEquals(1, repository.createCallCount)

        // прибираємо за собою
        gate.complete(FetchResult.Success(sampleEntity()))
        advanceUntilIdle()
        assertEquals(AddSeriesUiState.Saved, viewModel.uiState.value)
    }

    @Test
    fun `edge case - reset повертає Idle після помилки`() = runTest {
        // Arrange
        val repository = FakeSeriesRepository { FetchResult.Error("boom") }
        val viewModel = AddSeriesViewModel(repository)
        viewModel.save("X", 2020, "CONTINUING", 5.0)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AddSeriesUiState.Error)

        // Act
        viewModel.reset()

        // Assert
        assertEquals(AddSeriesUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `негативний - офлайн режим повертає спеціалізоване повідомлення Error`() = runTest {
        // Arrange
        val repository = FakeSeriesRepository { FetchResult.Offline(sampleEntity()) }
        val viewModel = AddSeriesViewModel(repository)

        // Act
        viewModel.save("Offline Show", 2019, "ENDED", 6.0)
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is AddSeriesUiState.Error)
        assertEquals(
            "Офлайн-режим: збереження недоступне",
            (state as AddSeriesUiState.Error).message
        )
    }

    @Test
    fun `позитивний - початковий стан Idle, після успіху Saved`() = runTest {
        // Arrange
        val repository = FakeSeriesRepository { FetchResult.Success(sampleEntity()) }
        val viewModel = AddSeriesViewModel(repository)

        // Початковий стан
        assertEquals(AddSeriesUiState.Idle, viewModel.uiState.value)

        // Act
        viewModel.save("Show", 2023, "CONTINUING", 7.5)
        advanceUntilIdle()

        // Assert
        assertEquals(AddSeriesUiState.Saved, viewModel.uiState.value)
    }

    // --- Helpers ---

    private fun sampleEntity(
        id: String = "1",
        title: String = "Sample"
    ): TvSeriesEntity = TvSeriesEntity(
        id = id,
        title = title,
        releaseYear = 2020,
        status = "CONTINUING",
        rating = 7.0,
        isFavorite = false,
        numberOfSeasons = 1,
        imdbUrl = "",
        comment = "",
        photoPath = null
    )
}

/**
 * Ручний fake репозиторію. Наслідує open class SeriesRepository та перевизначає
 * лише createSeries(). Передає у конструктор no-op DAO/API — вони не викликаються,
 * бо createSeries() override-нуто повністю.
 */
private class FakeSeriesRepository(
    private val onCreate: suspend () -> FetchResult<TvSeriesEntity>
) : SeriesRepository(NoopDao, NoopApi) {

    var createCallCount = 0
        private set
    var lastTitle: String? = null
        private set
    var lastYear: Int? = null
        private set

    override suspend fun createSeries(
        title: String,
        releaseYear: Int,
        status: String,
        rating: Double,
        isFavorite: Boolean,
        numberOfSeasons: Int,
        imdbUrl: String,
        comment: String
    ): FetchResult<TvSeriesEntity> {
        createCallCount++
        lastTitle = title
        lastYear = releaseYear
        return onCreate()
    }
}

/** No-op DAO: методи не викликаються у тестах createSeries, тож кидають. */
private object NoopDao : TvSeriesDao {
    override fun getAllSeries(): Flow<List<TvSeriesEntity>> = flowOf(emptyList())
    override suspend fun getAllSeriesOnce(): List<TvSeriesEntity> = emptyList()
    override suspend fun getById(id: String): TvSeriesEntity? = null
    override suspend fun insertAll(series: List<TvSeriesEntity>) = Unit
    override suspend fun insert(series: TvSeriesEntity) = Unit
    override suspend fun delete(series: TvSeriesEntity) = Unit
    override suspend fun deleteById(id: String) = Unit
    override suspend fun clear() = Unit
    override suspend fun updateFavorite(id: String, isFavorite: Boolean) = Unit
    override suspend fun updatePhoto(id: String, path: String?) = Unit
    override suspend fun count(): Int = 0
}

/** No-op API: не викликається у тестах createSeries. */
private object NoopApi : SeriesApiService {
    override suspend fun getAllSeries(): List<SeriesDto> = emptyList()
    override suspend fun getSeriesById(id: String): SeriesDto =
        throw NotImplementedError("not used in tests")
    override suspend fun createSeries(body: CreateSeriesRequest): SeriesDto =
        throw NotImplementedError("not used in tests")
    override suspend fun updateFavorite(id: String, body: UpdateFavoriteRequest): SeriesDto =
        throw NotImplementedError("not used in tests")
    override suspend fun deleteSeries(id: String): SeriesDto =
        throw NotImplementedError("not used in tests")
}
