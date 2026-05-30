package com.mindvault.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dashboardDataStore: DataStore<Preferences> by preferencesDataStore(name = "dashboard_prefs")
private val IS_DASHBOARD_MODE_KEY = booleanPreferencesKey("is_dashboard_mode")

@Singleton
class DashboardPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val isDashboardMode: Flow<Boolean> = context.dashboardDataStore.data.map { prefs ->
        prefs[IS_DASHBOARD_MODE_KEY] ?: true
    }

    suspend fun setDashboardMode(enabled: Boolean) {
        context.dashboardDataStore.edit { prefs ->
            prefs[IS_DASHBOARD_MODE_KEY] = enabled
        }
    }
}
