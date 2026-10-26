package com.example.todo.model

data class Task(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val dueDate: String = "",
    val completed: Boolean = false
)
