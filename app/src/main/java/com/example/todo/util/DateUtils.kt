package com.example.todo.util

import java.util.Calendar

/** The user types dd/MM/yyyy; the database stores yyyy-MM-dd so that text ordering is chronological. */
object DateUtils {

    /** Converts dd/MM/yyyy to yyyy-MM-dd, or null when the date is invalid. */
    fun toIsoDate(input: String): String? {
        val value = input.trim()
        if (value.length != 10) return null
        if (value[2] != '/' || value[5] != '/') return null

        val day = value.substring(0, 2).toIntOrNull() ?: return null
        val month = value.substring(3, 5).toIntOrNull() ?: return null
        val year = value.substring(6, 10).toIntOrNull() ?: return null

        if (year < 1900 || year > 2999) return null
        if (month < 1 || month > 12) return null
        if (day < 1 || day > daysInMonth(year, month)) return null

        return "%04d-%02d-%02d".format(year, month, day)
    }

    /** Converts yyyy-MM-dd to dd/MM/yyyy, returning the input when unrecognized. */
    fun toDisplayDate(isoDate: String): String {
        if (isoDate.length != 10 || isoDate[4] != '-' || isoDate[7] != '-') return isoDate
        return "${isoDate.substring(8, 10)}/${isoDate.substring(5, 7)}/${isoDate.substring(0, 4)}"
    }

    /** Current date in ISO format. */
    fun todayIso(): String {
        val calendar = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** A task is overdue when it has a deadline, is not completed and the deadline has passed. */
    fun isOverdue(isoDueDate: String, completed: Boolean): Boolean {
        if (completed || isoDueDate.isBlank()) return false
        return isoDueDate < todayIso()
    }

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if (isLeapYear(year)) 29 else 28
    }

    private fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}
