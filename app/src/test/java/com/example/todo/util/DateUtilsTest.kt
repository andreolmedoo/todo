package com.example.todo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    @Test
    fun convertsValidDateToIso() {
        assertEquals("2026-10-30", DateUtils.toIsoDate("30/10/2026"))
        assertEquals("2024-02-29", DateUtils.toIsoDate("29/02/2024"))
    }

    @Test
    fun rejectsMalformedOrImpossibleDates() {
        assertNull(DateUtils.toIsoDate(""))
        assertNull(DateUtils.toIsoDate("30-10-2026"))
        assertNull(DateUtils.toIsoDate("1/1/2026"))
        assertNull(DateUtils.toIsoDate("32/10/2026"))
        assertNull(DateUtils.toIsoDate("10/13/2026"))
        assertNull(DateUtils.toIsoDate("29/02/2025"))
        assertNull(DateUtils.toIsoDate("ab/cd/efgh"))
    }

    @Test
    fun formatsIsoDateForDisplay() {
        assertEquals("30/10/2026", DateUtils.toDisplayDate("2026-10-30"))
    }

    @Test
    fun overdueOnlyAppliesToUnfinishedTasksWithPastDeadline() {
        assertTrue(DateUtils.isOverdue("2000-01-01", completed = false))
        assertFalse(DateUtils.isOverdue("2000-01-01", completed = true))
        assertFalse(DateUtils.isOverdue("2999-12-31", completed = false))
        assertFalse(DateUtils.isOverdue("", completed = false))
    }
}
