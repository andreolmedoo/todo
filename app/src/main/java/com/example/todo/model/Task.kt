package com.example.todo.model

data class Task(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    // ISO format (yyyy-MM-dd), empty when the task has no deadline.
    val dueDate: String = "",
    val completed: Boolean = false,
    val priority: Int = PRIORITY_MEDIUM
) {
    companion object {
        const val PRIORITY_LOW = 1
        const val PRIORITY_MEDIUM = 2
        const val PRIORITY_HIGH = 3

        fun normalizePriority(value: Int): Int = when (value) {
            PRIORITY_LOW, PRIORITY_MEDIUM, PRIORITY_HIGH -> value
            else -> PRIORITY_MEDIUM
        }
    }
}
