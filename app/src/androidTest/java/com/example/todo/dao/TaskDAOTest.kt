package com.example.todo.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todo.db.DBHelper
import com.example.todo.model.Task
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDAOTest {
    private lateinit var context: Context
    private lateinit var dao: TaskDAO

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DBHelper.DATABASE_NAME)
        dao = TaskDAO(context)
    }

    @After
    fun tearDown() {
        dao.close()
        context.deleteDatabase(DBHelper.DATABASE_NAME)
    }

    @Test
    fun insertAndGetByIdPersistEveryField() {
        val id = dao.insert(
            Task(
                title = "Entregar trabalho",
                description = "Finalizar o aplicativo",
                dueDate = "2026-10-30",
                completed = false
            )
        )

        val saved = dao.getById(id.toInt())

        assertEquals(id.toInt(), saved?.id)
        assertEquals("Entregar trabalho", saved?.title)
        assertEquals("Finalizar o aplicativo", saved?.description)
        assertEquals("2026-10-30", saved?.dueDate)
        assertFalse(saved!!.completed)
    }

    @Test
    fun updateChangesExistingTask() {
        val id = dao.insert(Task(title = "Estudar"))

        val affectedRows = dao.update(
            Task(
                id = id.toInt(),
                title = "Estudar SQLite",
                description = "Revisar o CRUD",
                completed = true
            )
        )

        val updated = dao.getById(id.toInt())
        assertEquals(1, affectedRows)
        assertEquals("Estudar SQLite", updated?.title)
        assertEquals("Revisar o CRUD", updated?.description)
        assertTrue(updated!!.completed)
    }

    @Test
    fun deleteRemovesTask() {
        val id = dao.insert(Task(title = "Tarefa temporária"))

        assertEquals(1, dao.delete(id.toInt()))
        assertNull(dao.getById(id.toInt()))
    }

    @Test
    fun listSupportsTitleSearchAndPendingFilter() {
        dao.insert(Task(title = "Comprar pão", completed = false))
        dao.insert(Task(title = "Comprar café", completed = true))
        dao.insert(Task(title = "Estudar Kotlin", completed = false))

        val result = dao.list(search = "comprar", pendingOnly = true)

        assertEquals(1, result.size)
        assertEquals("Comprar pão", result.single().title)
    }
}
