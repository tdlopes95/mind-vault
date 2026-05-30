package com.mindvault.app.data.repository

import com.mindvault.app.data.local.dao.CategoryDao
import com.mindvault.app.data.local.entity.CategoryEntity
import com.mindvault.app.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(private val dao: CategoryDao) : CategoryRepositoryInterface {

    override fun getAllCategories(): Flow<List<Category>> =
        dao.getAllCategories().map { it.map(CategoryEntity::toDomain) }

    override fun getCategoryById(id: Long): Flow<Category?> =
        dao.getCategoryById(id).map { it?.toDomain() }

    override fun getNotesCountInCategory(categoryId: Long): Flow<Int> =
        dao.getNotesCountInCategory(categoryId)

    override suspend fun insertCategory(name: String, color: Int, icon: String?): Long =
        dao.insertCategory(CategoryEntity(name = name, color = color, icon = icon))

    override suspend fun updateCategory(category: Category) =
        dao.updateCategory(category.toEntity())

    override suspend fun deleteCategory(category: Category) =
        dao.deleteCategory(category.toEntity())
}

private fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    color = color,
    icon = icon,
    sortOrder = sortOrder,
    createdAt = createdAt,
)

private fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    color = color,
    icon = icon,
    sortOrder = sortOrder,
    createdAt = createdAt,
)
