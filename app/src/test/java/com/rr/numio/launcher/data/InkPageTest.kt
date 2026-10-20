package com.rr.numio.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InkPageTest {

    private val rows = 4
    private fun cell(c: Int, r: Int) = Slot(Slot.HOME, r * InkPage.COLS + c)
    private fun dock(i: Int) = Slot(Slot.DOCK, i)
    private fun app(id: String, c: Int, r: Int) = GridItem(id, c, r, 1, 1)
    private fun List<GridItem>.find(id: String) = first { it.id == id }
    private val all = setOf("a", "b", "c", "d", "e", "x", "y")

    @Test fun savingAndReadingBackKeepsEveryPlace() {
        val items = listOf(app("a", 0, 0), app("com.x/.Main|0", 3, 2))
        assertEquals(items, InkPage.parse(InkPage.serialize(items)))
    }

    @Test fun nothingSavedMeansAnEmptyPage() {
        assertTrue(InkPage.parse(null).isEmpty())
        assertTrue(InkPage.layout(InkPage.parse(null), rows, all).isEmpty())
    }

    @Test fun brokenLinesAreSkipped() {
        assertEquals(listOf(app("a", 1, 1)), InkPage.parse("a\t1\t1\nbroken\nb\tx\t2\n\t1\t1"))
    }

    @Test fun layoutKeepsAppsWhereTheyWere() {
        val saved = listOf(app("a", 3, 3), app("b", 1, 0))
        assertEquals(saved, InkPage.layout(saved, rows, all))
    }

    @Test fun layoutDropsUninstalledAndRepeatedApps() {
        val g = InkPage.layout(listOf(app("a", 0, 0), app("gone", 1, 0), app("a", 2, 0)), rows, all)
        assertEquals(listOf(app("a", 0, 0)), g)
    }

    @Test fun anAppBelowTheLastRowShowsInTheFirstFreeCellWithoutMovingOthers() {
        val g = InkPage.layout(listOf(app("a", 2, 6), app("b", 0, 0)), rows, all)
        assertEquals(app("b", 0, 0), g.find("b"))
        assertEquals(app("a", 1, 0), g.find("a"))
    }

    @Test fun twoAppsInOneCellNeverOverlap() {
        val g = InkPage.layout(listOf(app("a", 0, 0), app("b", 0, 0)), rows, all)
        assertEquals(app("a", 0, 0), g.find("a"))
        assertEquals(app("b", 1, 0), g.find("b"))
    }

    @Test fun appsThatDontFitAreKeptWhenSaving() {
        // a one-row page that is full: e (saved on row 3) has no room on this screen
        val saved = listOf(app("a", 0, 0), app("b", 1, 0), app("c", 2, 0), app("d", 3, 0), app("e", 0, 3))
        val shown = InkPage.layout(saved, 1, all)
        assertTrue(shown.none { it.id == "e" })
        // swapping a and b must not lose e
        val (next, _) = InkPage.move(shown, emptyList(), 1, cell(0, 0), cell(1, 0))!!
        val kept = InkPage.keepUnshown(next, saved, shown)
        assertEquals(app("e", 0, 3), kept.find("e"))
        assertEquals(app("a", 1, 0), kept.find("a"))
        assertEquals(5, kept.size)
    }

    @Test fun removingAnAppIsNotUndoneBySaving() {
        val saved = listOf(app("a", 0, 0), app("b", 1, 0))
        val shown = InkPage.layout(saved, rows, all)
        assertEquals(listOf(app("b", 1, 0)), InkPage.keepUnshown(shown.filter { it.id != "a" }, saved, shown))
    }

    // ── moving on the page ──────────────────────────────────────────────────

    @Test fun dragAnAppToAnEmptyCell() {
        val (g, d) = InkPage.move(listOf(app("a", 0, 0)), listOf("x", null), rows, cell(0, 0), cell(3, 2))!!
        assertEquals(listOf(app("a", 3, 2)), g)
        assertEquals(listOf("x", null), d)
    }

    @Test fun dragAnAppOntoAnotherSwapsThem() {
        val (g, _) = InkPage.move(listOf(app("a", 0, 0), app("b", 2, 1)), emptyList(), rows, cell(0, 0), cell(2, 1))!!
        assertEquals(app("a", 2, 1), g.find("a"))
        assertEquals(app("b", 0, 0), g.find("b"))
    }

    @Test fun cannotDropOffThePage() {
        val g = listOf(app("a", 0, 0))
        assertNull(InkPage.move(g, emptyList(), rows, cell(0, 0), cell(0, rows)))
        assertNull(InkPage.move(g, emptyList(), rows, cell(0, 0), Slot(Slot.HOME, -1)))
        assertNull(InkPage.move(g, emptyList(), rows, cell(0, 0), Slot(Slot.SUN, 0)))
    }

    @Test fun draggingAnEmptyCellOrOntoItselfDoesNothing() {
        val g = listOf(app("a", 0, 0))
        assertNull(InkPage.move(g, emptyList(), rows, cell(1, 1), cell(2, 2)))
        assertNull(InkPage.move(g, emptyList(), rows, cell(0, 0), cell(0, 0)))
    }

    // ── page ↔ dock ─────────────────────────────────────────────────────────

    @Test fun pageAppIntoAFreeDockCell() {
        val (g, d) = InkPage.move(listOf(app("a", 1, 1)), listOf("x", null, null), rows, cell(1, 1), dock(1))!!
        assertTrue(g.isEmpty())
        assertEquals(listOf("x", "a", null), d)
    }

    @Test fun pageAppOntoADockAppSwapsThem() {
        val (g, d) = InkPage.move(listOf(app("a", 1, 1)), listOf("x", "y"), rows, cell(1, 1), dock(0))!!
        assertEquals(listOf(app("x", 1, 1)), g)
        assertEquals(listOf("a", "y"), d)
    }

    @Test fun dockAppOntoAnEmptyCell() {
        val (g, d) = InkPage.move(emptyList(), listOf("x", "y"), rows, dock(1), cell(2, 3))!!
        assertEquals(listOf(app("y", 2, 3)), g)
        assertEquals(listOf("x", null), d)
    }

    @Test fun dockAppOntoAPageAppSwapsThem() {
        val (g, d) = InkPage.move(listOf(app("a", 0, 0)), listOf("x"), rows, dock(0), cell(0, 0))!!
        assertEquals(listOf(app("x", 0, 0)), g)
        assertEquals(listOf("a"), d)
    }

    @Test fun reorderTheDock() {
        val (_, d) = InkPage.move(emptyList(), listOf("x", "y", null), rows, dock(0), dock(1))!!
        assertEquals(listOf("y", "x", null), d)
    }

    @Test fun neverShowsAnAppTwice() {
        // x is in the dock and already on the page
        assertNull(InkPage.move(listOf(app("x", 0, 0)), listOf("x"), rows, dock(0), cell(1, 0)))
        // a is on the page and already in the dock
        assertNull(InkPage.move(listOf(app("a", 0, 0)), listOf("a", null), rows, cell(0, 0), dock(1)))
        // swapping would put y on the page twice
        assertNull(InkPage.move(listOf(app("a", 0, 0), app("y", 1, 0)), listOf("y"), rows, cell(0, 0), dock(0)))
    }

    @Test fun firstFreeIsInReadingOrder() {
        assertEquals(0 to 0, InkPage.firstFree(emptyList(), rows))
        assertEquals(1 to 0, InkPage.firstFree(listOf(app("a", 0, 0)), rows))
        val full = (0 until rows).flatMap { r -> (0 until InkPage.COLS).map { c -> app("$c$r", c, r) } }
        assertNull(InkPage.firstFree(full, rows))
        assertNotNull(InkPage.firstFree(full.drop(1), rows))
    }
}
