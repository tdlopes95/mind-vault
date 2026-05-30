package com.mindvault.app.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.app.data.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                backupManager.exportToJson(uri)
                _uiState.update { it.copy(isLoading = false, message = "Export successful") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "Export failed: ${e.message}") }
            }
        }
    }

    fun importJson(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val result = backupManager.importFromJson(uri)
                _uiState.update { it.copy(isLoading = false, message = result.summary) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "Import failed: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
