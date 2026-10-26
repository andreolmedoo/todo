package com.example.todo.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.todo.db.DBHelper
import com.example.todo.model.Task

/**
 * Data access object for tasks stored in SQLite.
 *
 * The UI layer can instantiate this class with a Context and use these methods
 * without knowing table or column names.
 */
class TaskDAO(context: Context) {
    private val dbHelper = DBHelper(context.applicationContext)

    /** Inserts a task and returns its generated database id. */
    fun insert(task: Task): Long {
        validate(task)
        return dbHelper.writableDatabase.insertOrThrow(
            DBHelper.TABLE_TASKS,
            null,
            task.toContentValues(includeId = false)
        )
    }

    /** Updates a task by id and returns the number of affected rows. */
    fun update(task: Task): Int {
        require(task.id > 0) { "A tarefa precisa ter um id válido para ser atualizada." }
        validate(task)

        return dbHelper.writableDatabase.update(
            DBHelper.TABLE_TASKS,
            task.toContentValues(includeId = false),
            "${DBHelper.COLUMN_ID} = ?",
            arrayOf(task.id.toString())
        )
    }

    /** Deletes a task by id and returns the number of affected rows. */
    fun delete(id: Int): Int {
        require(id > 0) { "A tarefa precisa ter um id válido para ser excluída." }
        return dbHelper.writableDatabase.delete(
            DBHelper.TABLE_TASKS,
            "${DBHelper.COLUMN_ID} = ?",
            arrayOf(id.toString())
        )
    }

    /** Returns a task by id, or null when it does not exist. */
    fun getById(id: Int): Task? {
        require(id > 0) { "A tarefa precisa ter um id válido para ser consultada." }

        return dbHelper.readableDatabase.query(
            DBHelper.TABLE_TASKS,
            TASK_COLUMNS,
            "${DBHelper.COLUMN_ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toTask() else null
        }
    }

    /** Returns every task, with the most recently inserted task first. */
    fun getAll(): List<Task> = queryTasks()

    /** Performs a case-insensitive title search. */
    fun searchByTitle(query: String): List<Task> = queryTasks(search = query)

    /** Returns only tasks that have not been completed. */
    fun getPending(): List<Task> = queryTasks(pendingOnly = true)

    /**
     * Flexible list operation for screens that combine title search and the
     * pending-only filter.
     */
    fun list(search: String? = null, pendingOnly: Boolean = false): List<Task> =
        queryTasks(search, pendingOnly)

    /** Releases the database resources owned by this DAO. */
    fun close() {
        dbHelper.close()
    }

    private fun queryTasks(
        search: String? = null,
        pendingOnly: Boolean = false
    ): List<Task> {
        val conditions = mutableListOf<String>()
        val arguments = mutableListOf<String>()

        if (!search.isNullOrBlank()) {
            conditions += "${DBHelper.COLUMN_TITLE} LIKE ?"
            arguments += "%${search.trim()}%"
        }

        if (pendingOnly) {
            conditions += "${DBHelper.COLUMN_COMPLETED} = ?"
            arguments += "0"
        }

        val selection = conditions.takeIf { it.isNotEmpty() }?.joinToString(" AND ")
        val selectionArgs = arguments.takeIf { it.isNotEmpty() }?.toTypedArray()

        return dbHelper.readableDatabase.query(
            DBHelper.TABLE_TASKS,
            TASK_COLUMNS,
            selection,
            selectionArgs,
            null,
            null,
            "${DBHelper.COLUMN_ID} DESC"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toTask())
                }
            }
        }
    }

    private fun validate(task: Task) {
        require(task.title.isNotBlank()) { "O título da tarefa é obrigatório." }
    }

    private fun Task.toContentValues(includeId: Boolean): ContentValues = ContentValues().apply {
        if (includeId) put(DBHelper.COLUMN_ID, id)
        put(DBHelper.COLUMN_TITLE, title.trim())
        put(DBHelper.COLUMN_DESCRIPTION, description)
        put(DBHelper.COLUMN_DUE_DATE, dueDate)
        put(DBHelper.COLUMN_COMPLETED, if (completed) 1 else 0)
    }

    private fun Cursor.toTask(): Task = Task(
        id = getInt(getColumnIndexOrThrow(DBHelper.COLUMN_ID)),
        title = getString(getColumnIndexOrThrow(DBHelper.COLUMN_TITLE)),
        description = getString(getColumnIndexOrThrow(DBHelper.COLUMN_DESCRIPTION)),
        dueDate = getString(getColumnIndexOrThrow(DBHelper.COLUMN_DUE_DATE)),
        completed = getInt(getColumnIndexOrThrow(DBHelper.COLUMN_COMPLETED)) == 1
    )

    companion object {
        private val TASK_COLUMNS = arrayOf(
            DBHelper.COLUMN_ID,
            DBHelper.COLUMN_TITLE,
            DBHelper.COLUMN_DESCRIPTION,
            DBHelper.COLUMN_DUE_DATE,
            DBHelper.COLUMN_COMPLETED
        )
    }
}
