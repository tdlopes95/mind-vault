package com.mindvault.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.app.data.model.Category
import com.mindvault.app.data.repository.CategoryRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: CategoryRepositoryInterface,
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = repository.getAllCategories()
        .map { CategoriesUiState(categories = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoriesUiState(),
        )

    fun createCategory(name: String, color: Int = 0) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.insertCategory(name.trim(), color) }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch { repository.updateCategory(category) }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }
}
