package com.mindvault.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "recent_searches")
private val RECENT_SEARCHES_KEY = stringSetPreferencesKey("recent_searches")
private const val MAX_RECENT = 10

@Singleton
class RecentSearchesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val recentSearches: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[RECENT_SEARCHES_KEY]?.toList()?.takeLast(MAX_RECENT)?.reversed() ?: emptyList()
    }

    suspend fun addSearch(query: String) {
        if (query.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[RECENT_SEARCHES_KEY]?.toMutableList() ?: mutableListOf()
            current.remove(query)
            current.add(query)
            if (current.size > MAX_RECENT) current.removeAt(0)
            prefs[RECENT_SEARCHES_KEY] = current.toSet()
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.remove(RECENT_SEARCHES_KEY)
        }
    }
}
