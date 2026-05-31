package com.mindvault.app.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mindvault.app.data.local.MindVaultDatabase
import com.mindvault.app.data.local.entity.CategoryEntity
import com.mindvault.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var db: MindVaultDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var noteDao: NoteDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, MindVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = db.categoryDao()
        noteDao = db.noteDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun insertAndGetAllCategories() = runTest {
        categoryDao.insertCategory(CategoryEntity(name = "Work", createdAt = 0))
        categoryDao.insertCategory(CategoryEntity(name = "Personal", createdAt = 0))

        val cats = categoryDao.getAllCategories().first()
        assertEquals(2, cats.size)
    }

    @Test
    fun insertDuplicate_ignoredDueToUniqueIndex() = runTest {
        categoryDao.insertCategory(CategoryEntity(name = "Work", createdAt = 0))
        categoryDao.insertCategory(CategoryEntity(name = "Work", createdAt = 0)) // ignored

        assertEquals(1, categoryDao.getAllCategories().first().size)
    }

    @Test
    fun getCategoryById_returnsCorrectCategory() = runTest {
        val id = categoryDao.insertCategory(CategoryEntity(name = "Work", color = 0xFF0000, createdAt = 0))

        val cat = categoryDao.getCategoryById(id).first()
        assertNotNull(cat)
        assertEquals("Work", cat!!.name)
        assertEquals(0xFF0000, cat.color)
    }

    @Test
    fun getCategoryById_unknownId_returnsNull() = runTest {
        val cat = categoryDao.getCategoryById(999L).first()
        assertNull(cat)
    }

    @Test
    fun updateCategory_persistsChanges() = runTest {
        val id = categoryDao.insertCategory(CategoryEntity(name = "Work", createdAt = 0))
        val original = categoryDao.getCategoryById(id).first()!!

        categoryDao.updateCategory(original.copy(name = "Work Updated", color = 0x00FF00))

        val updated = categoryDao.getCategoryById(id).first()
        assertEquals("Work Updated", updated!!.name)
        assertEquals(0x00FF00, updated.color)
    }

    @Test
    fun deleteCategory_removesFromList() = runTest {
        val id = categoryDao.insertCategory(CategoryEntity(name = "Work", createdAt = 0))
        val cat = categoryDao.getCategoryById(id).first()!!

        categoryDao.deleteCategory(cat)

        assertTrue(categoryDao.getAllCategories().first().isEmpty())
    }

    @Test
    fun getNotesCountInCategory_countsActiveNotes() = runTest {
        val catId = categoryDao.insertCategory(CategoryEntity(name = "Work", createdAt = 0))
        noteDao.insertNote(NoteEntity(title = "Note 1", content = "", categoryId = catId))
        noteDao.insertNote(NoteEntity(title = "Note 2", content = "", categoryId = catId))
        val deletedId = noteDao.insertNote(NoteEntity(title = "Deleted", content = "", categoryId = catId))
        noteDao.softDeleteNote(deletedId)

        val count = categoryDao.getNotesCountInCategory(catId).first()
        assertEquals(2, count)
    }

    @Test
    fun getNotesInCategory_returnsOnlyActiveNotesInCategory() = runTest {
        val catId = categoryDao.insertCategory(CategoryEntity(name = "Work", createdAt = 0))
        noteDao.insertNote(NoteEntity(title = "Work Note", content = "", categoryId = catId))
        noteDao.insertNote(NoteEntity(title = "Other Note", content = ""))

        val notes = categoryDao.getNotesInCategory(catId).first()
        assertEquals(1, notes.size)
        assertEquals("Work Note", notes[0].title)
    }

    @Test
    fun getUncategorizedNotes_excludesCategorized() = runTest {
        val catId = categoryDao.insertCategory(CategoryEntity(name = "Work", createdAt = 0))
        noteDao.insertNote(NoteEntity(title = "Uncategorized", content = ""))
        noteDao.insertNote(NoteEntity(title = "Categorized", content = "", categoryId = catId))

        val notes = categoryDao.getUncategorizedNotes().first()
        assertEquals(1, notes.size)
        assertEquals("Uncategorized", notes[0].title)
    }
}
