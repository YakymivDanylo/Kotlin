package com.danylo.seriesdiary.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Фіксована точка-орієнтир для розрахунку відстані.
 * Координати HBO HQ, 1100 Avenue of the Americas, New York, NY.
 */
object ReferencePoint {
    const val NAME = "HBO HQ, Нью-Йорк"
    const val LATITUDE = 40.7575
    const val LONGITUDE = -73.9850
}

sealed interface LocationUiState {
    object Idle : LocationUiState
    object Loading : LocationUiState
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float,
        val timestampMillis: Long,
        val distanceToReferenceMeters: Float
    ) : LocationUiState
    data class Error(val message: String) : LocationUiState
}

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val fused = LocationServices.getFusedLocationProviderClient(application)
    private var cancellationSource: CancellationTokenSource? = null

    private val _uiState = MutableStateFlow<LocationUiState>(LocationUiState.Idle)
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    /**
     * Запитує свіжу локацію. Викликається лише після надання дозволу через PermissionGate,
     * але додатково страхуємось перевіркою на checkSelfPermission.
     */
    @SuppressLint("MissingPermission")
    fun refresh() {
        val app = getApplication<Application>()
        val fineGranted = ContextCompat.checkSelfPermission(
            app, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            app, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            _uiState.value = LocationUiState.Error("Немає дозволу на геолокацію")
            return
        }

        // скасовуємо попередній виклик, якщо він ще активний
        cancellationSource?.cancel()
        val source = CancellationTokenSource().also { cancellationSource = it }

        _uiState.value = LocationUiState.Loading
        viewModelScope.launch {
            try {
                val priority = if (fineGranted) Priority.PRIORITY_HIGH_ACCURACY
                else Priority.PRIORITY_BALANCED_POWER_ACCURACY
                val request = CurrentLocationRequest.Builder()
                    .setPriority(priority)
                    .setMaxUpdateAgeMillis(0L) // саме свіжа локація, не кеш
                    .build()

                val location = suspendCancellableCoroutine<Location?> { cont ->
                    fused.getCurrentLocation(request, source.token)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                    cont.invokeOnCancellation { source.cancel() }
                }

                if (location == null) {
                    _uiState.value = LocationUiState.Error(
                        "Не вдалося отримати координати. Перевірте, чи увімкнено GPS."
                    )
                } else {
                    val distance = FloatArray(1)
                    Location.distanceBetween(
                        location.latitude, location.longitude,
                        ReferencePoint.LATITUDE, ReferencePoint.LONGITUDE,
                        distance
                    )
                    _uiState.value = LocationUiState.Success(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = if (location.hasAccuracy()) location.accuracy else 0f,
                        timestampMillis = location.time,
                        distanceToReferenceMeters = distance[0]
                    )
                }
            } catch (e: SecurityException) {
                _uiState.value = LocationUiState.Error("Дозвіл на геолокацію відкликано")
            } catch (e: Exception) {
                _uiState.value = LocationUiState.Error(e.message ?: "Помилка геолокації")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancellationSource?.cancel()
    }
}
