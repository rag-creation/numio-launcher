package com.rr.numio.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class MonthGridTest {

    @Test fun october2026StartsOnThursday_mondayFirst() {
        val weeks = monthGrid(2026, Calendar.OCTOBER, Calendar.MONDAY)
        assertEquals(listOf(0, 0, 0, 1, 2, 3, 4), weeks.first())
        assertEquals(5, weeks.size)
        assertEquals(31, weeks.flatten().maxOrNull())
    }

    @Test fun october2026_sundayFirst() {
        val weeks = monthGrid(2026, Calendar.OCTOBER, Calendar.SUNDAY)
        assertEquals(listOf(0, 0, 0, 0, 1, 2, 3), weeks.first())
    }

    @Test fun february2026_saturdayFirst() {
        // 1 Feb 2026 is a Sunday; with Saturday first it sits in the second column
        val weeks = monthGrid(2026, Calendar.FEBRUARY, Calendar.SATURDAY)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), weeks.first())
        assertEquals(28, weeks.flatten().maxOrNull())
        weeks.forEach { assertEquals(7, it.size) }
    }
}
