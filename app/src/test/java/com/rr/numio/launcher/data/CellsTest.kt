package com.rr.numio.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CellsTest {

    private val H = Slot.HOME
    private val D = Slot.DOCK

    @Test fun oldPackedSavesBecomeLeadingCells() {
        assertEquals(listOf("a", "b", null, null, null), Cells.pad(listOf("a", "b"), 5))
        assertEquals(listOf(null, "b", null), Cells.pad(listOf("", "b"), 3))
        assertEquals(listOf("a", "b"), Cells.pad(listOf("a", "b", "c"), 2))
    }

    @Test fun moveIntoAnEmptyHomeCell() {
        val (home, dock) = Cells.move(listOf("a", "b", null, null), listOf("x"), Slot(H, 1), Slot(H, 3))
        assertEquals(listOf("a", null, null, "b"), home)
        assertEquals(listOf("x"), dock)
    }

    @Test fun dropOnAnotherAppSwaps() {
        val (home, _) = Cells.move(listOf("a", "b", "c"), listOf(), Slot(H, 0), Slot(H, 2))
        assertEquals(listOf("c", "b", "a"), home)
    }

    @Test fun homeToAnEmptyDockCell() {
        val (home, dock) = Cells.move(listOf("a", "b"), listOf("x", null), Slot(H, 0), Slot(D, 1))
        assertEquals(listOf(null, "b"), home)
        assertEquals(listOf("x", "a"), dock)
    }

    @Test fun dockOntoAHomeAppSwapsAcross() {
        val (home, dock) = Cells.move(listOf("a", "b"), listOf("x", "y"), Slot(D, 1), Slot(H, 0))
        assertEquals(listOf("y", "b"), home)
        assertEquals(listOf("x", "a"), dock)
    }

    @Test fun draggingAnEmptyCellOrOntoItselfChangesNothing() {
        val home = listOf("a", null)
        val dock = listOf<String?>("x")
        assertEquals(home to dock, Cells.move(home, dock, Slot(H, 1), Slot(H, 0)))
        assertEquals(home to dock, Cells.move(home, dock, Slot(H, 0), Slot(H, 0)))
        assertEquals(home to dock, Cells.move(home, dock, Slot(H, 0), Slot(D, 7)))
    }

    @Test fun firstEmpty() {
        assertEquals(1, Cells.firstEmpty(listOf("a", null, null)))
        assertEquals(-1, Cells.firstEmpty(listOf("a", "b")))
    }
}
