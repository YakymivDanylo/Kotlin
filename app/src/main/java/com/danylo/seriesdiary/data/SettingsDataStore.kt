package com.danylo.seriesdiary.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferencesKeys {
    val USER_NAME: Preferences.Key<String> = stringPreferencesKey("user_name")
    val DEFAULT_SORT_BY_RATING: Preferences.Key<Boolean> = booleanPreferencesKey("default_sort_by_rating")
    val SHOW_ENDED_SERIES: Preferences.Key<Boolean> = booleanPreferencesKey("show_ended_series")
}

class SettingsDataStore(private val context: Context) {

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.USER_NAME] ?: ""
    }

    val defaultSortByRating: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DEFAULT_SORT_BY_RATING] ?: false
    }

    val showEndedSeries: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SHOW_ENDED_SERIES] ?: true
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USER_NAME] = name
        }
    }

    suspend fun saveDefaultSortByRating(sortByRating: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEFAULT_SORT_BY_RATING] = sortByRating
        }
    }

    suspend fun saveShowEndedSeries(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SHOW_ENDED_SERIES] = show
        }
    }
}
