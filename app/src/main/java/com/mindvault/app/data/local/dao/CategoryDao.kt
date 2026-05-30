package com.mindvault.app.data.local.dao

import androidx.room.*
import com.mindvault.app.data.local.entity.CategoryEntity
import com.mindvault.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM CategoryEntity ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM CategoryEntity WHERE id = :id")
    fun getCategoryById(id: Long): Flow<CategoryEntity?>

    @Query("SELECT COUNT(*) FROM notes WHERE categoryId = :categoryId AND isDeleted = 0 AND isArchived = 0")
    fun getNotesCountInCategory(categoryId: Long): Flow<Int>

    @Query("SELECT * FROM notes WHERE categoryId = :categoryId AND isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getNotesInCategory(categoryId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE categoryId IS NULL AND isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getUncategorizedNotes(): Flow<List<NoteEntity>>
}
