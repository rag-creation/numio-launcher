package com.rr.numio.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeGridTest {

    private val rows = 8
    private fun cell(c: Int, r: Int) = Slot(Slot.HOME, r * HomeGrid.COLS + c)
    private fun dock(i: Int) = Slot(Slot.DOCK, i)
    private fun app(id: String, c: Int, r: Int) = GridItem(id, c, r, 1, 1)
    private fun base(vararg apps: GridItem) =
        HomeGrid.normalize(HomeGrid.fromLegacy(emptyList(), rows) + apps, rows, apps.map { it.id }.toSet())
    private fun List<GridItem>.find(id: String) = first { it.id == id }

    @Test fun freshLayoutHasTheThreeWidgetsStacked() {
        val g = base()
        assertEquals(GridItem(HomeGrid.CLOCK, 0, 0, 5, 2), g.find(HomeGrid.CLOCK))
        assertEquals(GridItem(HomeGrid.TODAY, 0, 2, 4, 1), g.find(HomeGrid.TODAY))
        assertEquals(GridItem(HomeGrid.MONTH, 0, 3, 3, 3), g.find(HomeGrid.MONTH))
    }

    @Test fun legacyAppsKeepTheirColumnsOnTheBottomTwoRows() {
        val g = HomeGrid.normalize(HomeGrid.fromLegacy(listOf("a", null, "b", null, null, "c"), rows), rows, setOf("a", "b", "c"))
        assertEquals(app("a", 0, 6), g.find("a"))
        assertEquals(app("b", 2, 6), g.find("b"))
        assertEquals(app("c", 0, 7), g.find("c"))
    }

    @Test fun saveAndLoadRoundTrip() {
        val g = base(app("com.x/.Main#0", 4, 5))
        assertEquals(g.toSet(), HomeGrid.parse(HomeGrid.serialize(g)).toSet())
    }

    @Test fun normalizeFixesOverlapsBoundsDuplicatesAndUninstalledApps() {
        val saved = HomeGrid.fromLegacy(emptyList(), rows) +
            listOf(app("a", 0, 0), app("b", 9, 9), app("b", 1, 7), app("gone", 4, 7))
        val g = HomeGrid.normalize(saved, rows, setOf("a", "b"))
        assertEquals(2, g.count { !it.isWidget })
        assertTrue(g.none { it.id == "gone" })
        // no two items overlap, everything in bounds
        g.forEach { a -> g.forEach { b -> if (a !== b) assertTrue(!a.overlaps(b)) } }
        assertTrue(g.all { it.col + it.w <= HomeGrid.COLS && it.row + it.h <= rows })
    }

    @Test fun appMovesIntoAnEmptyCellBesideTheCalendar() {
        val g = base(app("a", 0, 7))
        val (out, _) = HomeGrid.move(g, listOf(null), rows, cell(0, 7), cell(4, 3))!!
        assertEquals(app("a", 4, 3), out.find("a"))
    }

    @Test fun appsSwap() {
        val g = base(app("a", 0, 7), app("b", 4, 7))
        val (out, _) = HomeGrid.move(g, emptyList(), rows, cell(0, 7), cell(4, 7))!!
        assertEquals(app("a", 4, 7), out.find("a"))
        assertEquals(app("b", 0, 7), out.find("b"))
    }

    @Test fun nothingDropsOnTopOfAWidget() {
        val g = base(app("a", 0, 7))
        assertNull(HomeGrid.move(g, emptyList(), rows, cell(0, 7), cell(1, 4)))   // inside the calendar
        assertNull(HomeGrid.move(g, listOf("d"), rows, dock(0), cell(2, 0)))      // dock app onto the clock
    }

    @Test fun widgetMovesIntoFreeSpaceOnly() {
        val g = base(app("a", 4, 7))
        // calendar (3×3) down to rows 5-7, columns 0-2: free
        val (out, _) = HomeGrid.move(g, emptyList(), rows, cell(0, 3), cell(0, 5))!!
        assertEquals(GridItem(HomeGrid.MONTH, 0, 5, 3, 3), out.find(HomeGrid.MONTH))
        // calendar onto columns 2-4: would cover the app at (4,7) → not allowed
        assertNull(HomeGrid.move(g, emptyList(), rows, cell(0, 3), cell(2, 5)))
        // off the bottom → not allowed
        assertNull(HomeGrid.move(g, emptyList(), rows, cell(0, 3), cell(0, 6)))
        // widgets never go to the dock
        assertNull(HomeGrid.move(g, listOf(null), rows, cell(0, 0), dock(0)))
    }

    @Test fun clockMovesDownAndAppTakesTheTop() {
        val g = base(app("a", 4, 7))
        val (g2, _) = HomeGrid.move(g, emptyList(), rows, cell(0, 3), cell(0, 5))!!   // calendar down
        val (g3, _) = HomeGrid.move(g2, emptyList(), rows, cell(0, 2), cell(0, 4))!!  // chip down
        val (g4, _) = HomeGrid.move(g3, emptyList(), rows, cell(0, 0), cell(0, 2))!!  // clock down
        assertEquals(GridItem(HomeGrid.CLOCK, 0, 2, 5, 2), g4.find(HomeGrid.CLOCK))
        val (g5, _) = HomeGrid.move(g4, emptyList(), rows, cell(4, 7), cell(2, 0))!!  // app to the top
        assertEquals(app("a", 2, 0), g5.find("a"))
    }

    @Test fun gridAndDockTradePlaces() {
        val g = base(app("a", 0, 7))
        // grid app → empty dock cell
        val (g1, d1) = HomeGrid.move(g, listOf("x", null), rows, cell(0, 7), dock(1))!!
        assertEquals(listOf("x", "a"), d1)
        assertTrue(g1.none { it.id == "a" })
        // dock app onto a grid app: they swap
        val (g2, d2) = HomeGrid.move(base(app("a", 0, 7)), listOf("x"), rows, dock(0), cell(0, 7))!!
        assertEquals(listOf("a"), d2)
        assertEquals(app("x", 0, 7), g2.find("x"))
        // dock app into an empty grid cell
        val (g3, d3) = HomeGrid.move(base(), listOf("x"), rows, dock(0), cell(3, 4))!!
        assertEquals(listOf<String?>(null), d3)
        assertNotNull(g3.firstOrNull { it.id == "x" && it.col == 3 && it.row == 4 })
    }

    @Test fun firstFreeSkipsWidgets() {
        assertEquals(4 to 2, HomeGrid.firstFree(base(), rows))
    }

    // ── the Workspace portal ────────────────────────────────────────────────

    private fun withPortal(vararg apps: GridItem) =
        HomeGrid.normalize(HomeGrid.fromLegacy(emptyList(), rows) + apps, rows, apps.map { it.id }.toSet(), portal = true)

    @Test fun portalOnlyWhenThereIsAWorkProfile() {
        assertTrue(base().none { it.id == HomeGrid.PORTAL })
        assertEquals(GridItem(HomeGrid.PORTAL, 4, 2, 1, 1), withPortal().find(HomeGrid.PORTAL))
    }

    @Test fun portalTakesAFreeSpotWhenItsDefaultIsTaken() {
        val g = withPortal(app("a", 4, 2))
        assertEquals(app("a", 4, 2), g.find("a"))
        val p = g.find(HomeGrid.PORTAL)
        assertTrue(g.none { it !== p && it.overlaps(p) })
    }

    @Test fun portalMovesLikeAWidgetAndStaysOutOfTheDock() {
        val g = withPortal(app("a", 0, 7))
        val (moved, _) = HomeGrid.move(g, emptyList(), rows, cell(4, 2), cell(3, 7))!!
        assertEquals(GridItem(HomeGrid.PORTAL, 3, 7, 1, 1), moved.find(HomeGrid.PORTAL))
        assertNull(HomeGrid.move(g, listOf(null), rows, cell(4, 2), dock(0)))
        // apps can't be dropped on it
        assertNull(HomeGrid.move(g, emptyList(), rows, cell(0, 7), cell(4, 2)))
    }

    @Test fun portalPositionIsSaved() {
        val g = withPortal()
        val (moved, _) = HomeGrid.move(g, emptyList(), rows, cell(4, 2), cell(1, 6))!!
        val reloaded = HomeGrid.normalize(HomeGrid.parse(HomeGrid.serialize(moved)), rows, emptySet(), portal = true)
        assertEquals(GridItem(HomeGrid.PORTAL, 1, 6, 1, 1), reloaded.find(HomeGrid.PORTAL))
    }

    @Test fun hidingTheLogoFreesItsCellAndKeepsItsPlaceForLater() {
        val shown = withPortal()
        val (moved, _) = HomeGrid.move(shown, emptyList(), rows, cell(4, 2), cell(2, 7))!!   // logo to (2,7)
        // hidden: not on the visible grid, so its cell takes an app…
        val hidden = HomeGrid.normalize(moved, rows, setOf("a"), portal = false)
        assertTrue(hidden.none { it.id == HomeGrid.PORTAL })
        assertNotNull(HomeGrid.move(hidden + app("a", 0, 7), emptyList(), rows, cell(0, 7), cell(3, 7)))
        // …but saving keeps its last place, so showing it again puts it back
        val saved = HomeGrid.keepHidden(hidden, previous = moved)
        val again = HomeGrid.normalize(HomeGrid.parse(HomeGrid.serialize(saved)), rows, emptySet(), portal = true)
        assertEquals(GridItem(HomeGrid.PORTAL, 2, 7, 1, 1), again.find(HomeGrid.PORTAL))
    }

    @Test fun keepHiddenDoesNothingWhenTheLogoIsShown() {
        val shown = withPortal()
        assertEquals(shown, HomeGrid.keepHidden(shown, previous = shown))
    }
}
