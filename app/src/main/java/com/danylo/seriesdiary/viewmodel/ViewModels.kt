package com.danylo.seriesdiary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danylo.seriesdiary.data.SeriesRepository
import com.danylo.seriesdiary.model.TvSeries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


sealed interface ListUiState {
    object Loading : ListUiState
    data class Success(val series: List<TvSeries>) : ListUiState
    data class Error(val message: String) : ListUiState
}

class ListViewModel(private val repository: SeriesRepository = SeriesRepository()) : ViewModel() {
    private val _uiState = MutableStateFlow<ListUiState>(ListUiState.Loading)
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites.asStateFlow()

    private val favorites = listOf("Breaking Bad", "The Boys")
    private var allSeries: List<TvSeries> = emptyList()

    init {
        loadSeries()
    }

    private fun loadSeries() {
        viewModelScope.launch {
            _uiState.value = ListUiState.Loading
            allSeries = repository.getSeriesList()
            updateList()
        }
    }

    fun toggleFavorites(show: Boolean) {
        _showOnlyFavorites.value = show
        updateList()
    }

    private fun updateList() {
        val filtered = if (_showOnlyFavorites.value) {
            allSeries.filter { favorites.contains(it.title) }
        } else {
            allSeries
        }
        _uiState.value = ListUiState.Success(filtered)
    }
}

sealed interface DetailsUiState {
    object Loading : DetailsUiState
    data class Success(val series: TvSeries, val extraInfo: String) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

class DetailsViewModel(
    private val seriesTitle: String,
    private val repository: SeriesRepository = SeriesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading
            val series = repository.getSeriesByTitle(seriesTitle)
            if (series != null) {
                val extraInfo = if (series.rating > 8.5) "Справжній хіт!" else "Гарний вибір."
                _uiState.value = DetailsUiState.Success(series, extraInfo)
            } else {
                _uiState.value = DetailsUiState.Error("Серіал не знайдено")
            }
        }
    }

    class Factory(private val seriesTitle: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DetailsViewModel::class.java)) {
                return DetailsViewModel(seriesTitle) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

class GridViewModel(private val repository: SeriesRepository = SeriesRepository()) : ViewModel() {
    private val _series = MutableStateFlow<List<TvSeries>>(emptyList())
    val series: StateFlow<List<TvSeries>> = _series.asStateFlow()

    private val _sortByRating = MutableStateFlow(false)
    val sortByRating: StateFlow<Boolean> = _sortByRating.asStateFlow()

    private var allSeries: List<TvSeries> = emptyList()

    init {
        loadSeries()
    }

    private fun loadSeries() {
        viewModelScope.launch {
            allSeries = repository.getSeriesList()
            updateSort()
        }
    }

    fun setSortByRating(byRating: Boolean) {
        _sortByRating.value = byRating
        updateSort()
    }

    private fun updateSort() {
        val sorted = if (_sortByRating.value) {
            allSeries.sortedByDescending { it.rating }
        } else {
            allSeries.sortedBy { it.title }
        }
        _series.value = sorted
    }
}

class ProfileViewModel(initialName: String) : ViewModel() {
    private val _userName = MutableStateFlow(initialName)
    val userName: StateFlow<String> = _userName.asStateFlow()

    fun updateName(newName: String) {
        _userName.value = newName
    }

    class Factory(private val initialName: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(initialName) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}