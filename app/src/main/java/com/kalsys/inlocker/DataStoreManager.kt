package com.kalsys.inlocker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore // Correct import
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Use property delegate to create the DataStore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        private val Context.dataStore by preferencesDataStore(name = "app_preferences")
        val DIRECTORY_URI_KEY = stringPreferencesKey("directory_uri")
    }

    suspend fun saveFolderUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[DIRECTORY_URI_KEY] = uri
        }
    }

    fun getFolderUri(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[DIRECTORY_URI_KEY]
        }
    }
}
