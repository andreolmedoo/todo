package com.example.todo.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Creates and versions the local SQLite database used by the application.
 *
 * Activities should access task data through TaskDAO instead of issuing SQL
 * directly against this helper.
 */
class DBHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TASKS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 2 added the priority column, keeping the existing rows.
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE $TABLE_TASKS ADD COLUMN $COLUMN_PRIORITY " +
                    "INTEGER NOT NULL DEFAULT 2"
            )
        }
    }

    companion object {
        const val DATABASE_NAME = "todo.db"
        const val DATABASE_VERSION = 2

        const val TABLE_TASKS = "tasks"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_DUE_DATE = "due_date"
        const val COLUMN_COMPLETED = "completed"
        const val COLUMN_PRIORITY = "priority"

        private const val CREATE_TASKS_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_TASKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL CHECK(length(trim($COLUMN_TITLE)) > 0),
                $COLUMN_DESCRIPTION TEXT NOT NULL DEFAULT '',
                $COLUMN_DUE_DATE TEXT NOT NULL DEFAULT '',
                $COLUMN_COMPLETED INTEGER NOT NULL DEFAULT 0
                    CHECK($COLUMN_COMPLETED IN (0, 1)),
                $COLUMN_PRIORITY INTEGER NOT NULL DEFAULT 2
                    CHECK($COLUMN_PRIORITY IN (1, 2, 3))
            )
        """
    }
}
