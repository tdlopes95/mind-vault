package com.mindvault.app.data.repository

import com.mindvault.app.data.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepositoryInterface {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoryById(id: Long): Flow<Category?>
    fun getNotesCountInCategory(categoryId: Long): Flow<Int>
    suspend fun insertCategory(name: String, color: Int = 0, icon: String? = null): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
}
