package com.rr.numio.launcher.data

/** Something on Sol's home grid: an app (1×1) or a widget, with its top-left cell and size. */
data class GridItem(val id: String, val col: Int, val row: Int, val w: Int, val h: Int) {
    val isWidget get() = HomeGrid.isWidget(id)
    fun covers(c: Int, r: Int) = c in col until col + w && r in row until row + h
    fun overlaps(o: GridItem) =
        col < o.col + o.w && o.col < col + w && row < o.row + o.h && o.row < row + h
}

/**
 * Sol's home: one grid, [COLS] wide, as tall as the screen allows. Apps take one cell;
 * the clock, alarm chip and calendar are widgets spanning several. Everything can be
 * dragged into free space; apps swap with apps and can move to and from the dock.
 * Pure Kotlin, so the rules are unit tested. Grid cells are Slot(HOME, row * COLS + col).
 */
object HomeGrid {
    const val COLS = 5

    const val CLOCK = "w:clock"
    const val TODAY = "w:today"
    const val MONTH = "w:month"
    /** The Workspace portal (official Numio mark); only shown when there's a work profile. */
    const val PORTAL = "w:portal"

    /** Widgets, their sizes and where they start on a fresh layout. */
    private val widgets = listOf(
        GridItem(CLOCK, 0, 0, 5, 2),
        GridItem(TODAY, 0, 2, 4, 1),
        GridItem(MONTH, 0, 3, 3, 3),
        // beside the alarm chip, in the column the chip leaves free
        GridItem(PORTAL, 4, 2, 1, 1),
    )

    fun isWidget(id: String) = id.startsWith("w:")

    fun sizeOf(id: String): Pair<Int, Int> =
        widgets.firstOrNull { it.id == id }?.let { it.w to it.h } ?: (1 to 1)

    // ── saving ──────────────────────────────────────────────────────────────

    /** One item per line: id, column, row. App ids never contain tabs. */
    fun serialize(items: List<GridItem>): String =
        items.joinToString("\n") { "${it.id}\t${it.col}\t${it.row}" }

    /** Saved text → items (sizes come from the ids). Broken lines are skipped. */
    fun parse(text: String): List<GridItem> =
        text.split('\n').mapNotNull { line ->
            val p = line.split('\t')
            if (p.size != 3 || p[0].isEmpty()) return@mapNotNull null
            val c = p[1].toIntOrNull() ?: return@mapNotNull null
            val r = p[2].toIntOrNull() ?: return@mapNotNull null
            val (w, h) = sizeOf(p[0])
            GridItem(p[0], c, r, w, h)
        }

    /**
     * Before the grid, Sol had two rows of five app cells under fixed widgets. Those apps
     * keep their columns on the bottom two rows, so nothing jumps on the first update.
     */
    fun fromLegacy(homeCells: List<String?>, rows: Int): List<GridItem> =
        widgets.filter { it.id != PORTAL } + homeCells.mapIndexedNotNull { i, key ->
            key?.let { GridItem(it, i % COLS, rows - 2 + i / COLS, 1, 1) }
        }

    // ── keeping a layout valid ──────────────────────────────────────────────

    private fun inBounds(it: GridItem, rows: Int) =
        it.col >= 0 && it.row >= 0 && it.col + it.w <= COLS && it.row + it.h <= rows

    /**
     * Makes any saved layout valid for this many [rows]: every widget present, nothing out
     * of bounds, nothing overlapping, no app twice, only [installed] apps, and the Workspace
     * portal only when [portal] is true (there's a work profile). Anything that
     * doesn't fit where it was goes to the first free spot; if there is none, it's left out.
     */
    fun normalize(saved: List<GridItem>, rows: Int, installed: Set<String>, portal: Boolean = false): List<GridItem> {
        val placed = ArrayList<GridItem>()
        val seen = HashSet<String>()
        // What's already placed wins; a widget that's new to this layout (like the portal when a
        // work profile first appears) takes its default spot only if free, after everything else.
        val enabled = widgets.filter { portal || it.id != PORTAL }
        val savedWidgets = enabled.mapNotNull { w -> saved.firstOrNull { it.id == w.id } }
        val newWidgets = enabled.filter { w -> saved.none { it.id == w.id } }
        val wanted = savedWidgets + saved.filter { !it.isWidget && it.id in installed } + newWidgets
        for (item in wanted) {
            if (!seen.add(item.id)) continue
            val ok = inBounds(item, rows) && placed.none { it.overlaps(item) }
            val spot = if (ok) item else firstFree(placed, rows, item.w, item.h)?.let { (c, r) -> item.copy(col = c, row = r) }
            if (spot != null) placed += spot
        }
        return placed
    }

    /** The first top-left cell (reading order) where a w×h item fits, or null. */
    fun firstFree(items: List<GridItem>, rows: Int, w: Int = 1, h: Int = 1): Pair<Int, Int>? {
        for (r in 0..rows - h) for (c in 0..COLS - w) {
            val probe = GridItem("", c, r, w, h)
            if (items.none { it.overlaps(probe) }) return c to r
        }
        return null
    }

    fun itemAt(items: List<GridItem>, c: Int, r: Int): GridItem? = items.firstOrNull { it.covers(c, r) }

    // ── moving ──────────────────────────────────────────────────────────────

    /**
     * Drag [from] onto [to] (grid cells are Slot.HOME, dock cells Slot.DOCK).
     *  - a widget moves into free space on the grid (never into the dock)
     *  - an app moves into an empty grid or dock cell, or swaps with the app there
     *  - nothing can be dropped on top of a widget
     * Returns the new grid and dock, or null if the move isn't allowed.
     */
    fun move(
        items: List<GridItem>, dock: List<String?>, rows: Int, from: Slot, to: Slot,
    ): Pair<List<GridItem>, List<String?>>? {
        if (from == to) return null
        val grid = items.toMutableList()
        val d = dock.toMutableList()

        // what's being dragged
        val source: GridItem? = if (from.area == Slot.HOME) {
            grid.firstOrNull { it.col == from.index % COLS && it.row == from.index / COLS } ?: return null
        } else null
        val dockKey: String? = if (from.area == Slot.DOCK) d.getOrNull(from.index) ?: return null else null

        if (to.area == Slot.DOCK) {
            if (to.index !in d.indices) return null
            val there = d[to.index]
            if (source != null) {
                if (source.isWidget) return null
                // grid app → dock: whatever was in that dock cell takes the app's grid cell
                grid.remove(source)
                if (there != null) grid += source.copy(id = there)
                d[to.index] = source.id
            } else {
                d[from.index] = there
                d[to.index] = dockKey
            }
            return grid to d
        }

        // onto the grid
        val tc = to.index % COLS
        val tr = to.index / COLS
        if (source != null && source.isWidget) {
            val moved = source.copy(col = tc, row = tr)
            if (!inBounds(moved, rows)) return null
            if (grid.any { it !== source && it.overlaps(moved) }) return null
            grid[grid.indexOf(source)] = moved
            return grid to d
        }
        if (tc !in 0 until COLS || tr !in 0 until rows) return null
        val occupant = itemAt(grid, tc, tr)
        if (occupant != null && occupant.isWidget) return null
        if (source != null) {
            // grid app → grid cell: move, or swap with the app there
            grid.remove(source)
            if (occupant != null) {
                grid.remove(occupant)
                grid += occupant.copy(col = source.col, row = source.row)
            }
            grid += source.copy(col = tc, row = tr)
        } else {
            // dock app → grid cell: the app there (if any) goes into its dock cell
            if (occupant != null) grid.remove(occupant)
            if (grid.any { it.id == dockKey }) return null  // already on the grid elsewhere
            grid += GridItem(dockKey!!, tc, tr, 1, 1)
            d[from.index] = occupant?.id
        }
        return grid to d
    }
}
