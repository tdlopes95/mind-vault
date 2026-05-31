package com.mindvault.app.data.repository

import com.mindvault.app.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeCategoryRepository : CategoryRepositoryInterface {

    private val categories = mutableListOf<Category>()
    private var nextId = 1L

    fun seed(category: Category): Category {
        val stored = category.copy(id = nextId++)
        categories.add(stored)
        return stored
    }

    override fun getAllCategories(): Flow<List<Category>> = flow { emit(categories.toList()) }

    override fun getCategoryById(id: Long): Flow<Category?> = flow {
        emit(categories.find { it.id == id })
    }

    override fun getNotesCountInCategory(categoryId: Long): Flow<Int> = flow { emit(0) }

    override suspend fun insertCategory(name: String, color: Int, icon: String?): Long {
        val id = nextId++
        categories.add(Category(id = id, name = name, color = color))
        return id
    }

    override suspend fun updateCategory(category: Category) {
        val idx = categories.indexOfFirst { it.id == category.id }
        if (idx >= 0) categories[idx] = category
    }

    override suspend fun deleteCategory(category: Category) {
        categories.removeAll { it.id == category.id }
    }
}
