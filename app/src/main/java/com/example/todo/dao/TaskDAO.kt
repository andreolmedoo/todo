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
            task.toContentValues()
        )
    }

    /** Updates a task by id and returns the number of affected rows. */
    fun update(task: Task): Int {
        require(task.id > 0) { "A tarefa precisa ter um id válido para ser atualizada." }
        validate(task)

        return dbHelper.writableDatabase.update(
            DBHelper.TABLE_TASKS,
            task.toContentValues(),
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

    /** Returns every task. */
    fun getAll(): List<Task> = list()

    /** Performs a case-insensitive title search. */
    fun searchByTitle(query: String): List<Task> = list(search = query)

    /** Returns only tasks that have not been completed. */
    fun getPending(): List<Task> = list(filter = TaskFilter.PENDING)

    /** Returns only tasks that have already been completed. */
    fun getCompleted(): List<Task> = list(filter = TaskFilter.COMPLETED)

    /**
     * Flexible list operation for screens that combine title search and the
     * status filter.
     */
    fun list(search: String? = null, filter: TaskFilter = TaskFilter.ALL): List<Task> {
        val conditions = mutableListOf<String>()
        val arguments = mutableListOf<String>()

        if (!search.isNullOrBlank()) {
            conditions += "${DBHelper.COLUMN_TITLE} LIKE ?"
            arguments += "%${search.trim()}%"
        }

        when (filter) {
            TaskFilter.PENDING -> {
                conditions += "${DBHelper.COLUMN_COMPLETED} = ?"
                arguments += "0"
            }

            TaskFilter.COMPLETED -> {
                conditions += "${DBHelper.COLUMN_COMPLETED} = ?"
                arguments += "1"
            }

            TaskFilter.ALL -> Unit
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
            ORDER_BY
        ).use { cursor ->
            val tasks = mutableListOf<Task>()
            while (cursor.moveToNext()) {
                tasks.add(cursor.toTask())
            }
            tasks
        }
    }

    /** Total number of tasks, used by the progress summary. */
    fun countAll(): Int = count(null)

    /** Number of completed tasks, used by the progress summary. */
    fun countCompleted(): Int = count("${DBHelper.COLUMN_COMPLETED} = 1")

    /** Releases the database resources owned by this DAO. */
    fun close() {
        dbHelper.close()
    }

    private fun count(selection: String?): Int {
        val where = if (selection == null) "" else " WHERE $selection"
        return dbHelper.readableDatabase
            .rawQuery("SELECT COUNT(*) FROM ${DBHelper.TABLE_TASKS}$where", null)
            .use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
    }

    private fun validate(task: Task) {
        require(task.title.isNotBlank()) { "O título da tarefa é obrigatório." }
    }

    private fun Task.toContentValues(): ContentValues = ContentValues().apply {
        put(DBHelper.COLUMN_TITLE, title.trim())
        put(DBHelper.COLUMN_DESCRIPTION, description.trim())
        put(DBHelper.COLUMN_DUE_DATE, dueDate.trim())
        put(DBHelper.COLUMN_COMPLETED, if (completed) 1 else 0)
        put(DBHelper.COLUMN_PRIORITY, Task.normalizePriority(priority))
    }

    private fun Cursor.toTask(): Task = Task(
        id = getInt(getColumnIndexOrThrow(DBHelper.COLUMN_ID)),
        title = getString(getColumnIndexOrThrow(DBHelper.COLUMN_TITLE)),
        description = getString(getColumnIndexOrThrow(DBHelper.COLUMN_DESCRIPTION)),
        dueDate = getString(getColumnIndexOrThrow(DBHelper.COLUMN_DUE_DATE)),
        completed = getInt(getColumnIndexOrThrow(DBHelper.COLUMN_COMPLETED)) == 1,
        priority = getInt(getColumnIndexOrThrow(DBHelper.COLUMN_PRIORITY))
    )

    companion object {
        private val TASK_COLUMNS = arrayOf(
            DBHelper.COLUMN_ID,
            DBHelper.COLUMN_TITLE,
            DBHelper.COLUMN_DESCRIPTION,
            DBHelper.COLUMN_DUE_DATE,
            DBHelper.COLUMN_COMPLETED,
            DBHelper.COLUMN_PRIORITY
        )

        // Pending first, then higher priority, then closest deadline
        // (tasks without a deadline last) and finally the title.
        private val ORDER_BY = """
            ${DBHelper.COLUMN_COMPLETED} ASC,
            ${DBHelper.COLUMN_PRIORITY} DESC,
            CASE WHEN ${DBHelper.COLUMN_DUE_DATE} = '' THEN 1 ELSE 0 END ASC,
            ${DBHelper.COLUMN_DUE_DATE} ASC,
            ${DBHelper.COLUMN_TITLE} COLLATE NOCASE ASC
        """.trimIndent().replace("\n", " ")
    }
}
