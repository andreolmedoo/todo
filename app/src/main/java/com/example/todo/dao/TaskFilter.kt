package com.example.todo.dao

/** Status filters offered on the main screen; the order matches R.array.task_filters. */
enum class TaskFilter {
    ALL,
    PENDING,
    COMPLETED;

    companion object {
        fun fromPosition(position: Int): TaskFilter =
            values().getOrElse(position) { ALL }
    }
}
