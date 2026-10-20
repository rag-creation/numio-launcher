package com.rr.numio.launcher.data

/**
 * Ink's home page: apps placed anywhere on a grid drawn over the sketchbook page, [COLS]
 * wide and as many rows as the open space allows. Apps only (the clock, quote and drawing
 * stay where they are). Saved separately from Sol's grid, so the two themes never move
 * each other's apps; the dock is shared, as it always was.
 * Pure Kotlin, so the rules are unit tested. Cells are Slot(HOME, row * COLS + col).
 */
object InkPage {
    const val COLS = 4

    fun serialize(items: List<GridItem>): String =
        items.joinToString("\n") { "${it.id}\t${it.col}\t${it.row}" }

    /** Saved text → apps. Broken lines are skipped. */
    fun parse(text: String?): List<GridItem> =
        text.orEmpty().split('\n').mapNotNull { line ->
            val p = line.split('\t')
            if (p.size != 3 || p[0].isEmpty()) return@mapNotNull null
            val c = p[1].toIntOrNull() ?: return@mapNotNull null
            val r = p[2].toIntOrNull() ?: return@mapNotNull null
            GridItem(p[0], c, r, 1, 1)
        }

    private fun inBounds(it: GridItem, rows: Int) = it.col in 0 until COLS && it.row in 0 until rows

    /**
     * What the page shows for this many [rows]: only [installed] apps, each once, nothing
     * overlapping. An app whose cell isn't on screen (fewer rows than when it was placed)
     * shows in the first free cell; if the page is full it isn't shown, but stays saved.
     */
    fun layout(saved: List<GridItem>, rows: Int, installed: Set<String>): List<GridItem> {
        val placed = ArrayList<GridItem>()
        val seen = HashSet<String>()
        val wanted = saved.filter { it.id in installed && seen.add(it.id) }
        // apps in their own cells first, so a moved-up app never takes someone's place
        val (fits, rest) = wanted.partition { inBounds(it, rows) }
        for (item in fits) if (placed.none { it.covers(item.col, item.row) }) placed += item
        for (item in rest + fits.filter { it !in placed }) {
            val spot = firstFree(placed, rows) ?: break
            placed += item.copy(col = spot.first, row = spot.second)
        }
        return placed
    }

    /**
     * Saving after a change: [next] is the new page; apps that were saved but not [shown]
     * (no room on this screen) keep their saved place, so nothing is lost.
     */
    fun keepUnshown(next: List<GridItem>, saved: List<GridItem>, shown: List<GridItem>): List<GridItem> =
        next + saved.filter { s -> shown.none { it.id == s.id } && next.none { it.id == s.id } }

    /** The first free cell in reading order, or null when the page is full. */
    fun firstFree(items: List<GridItem>, rows: Int): Pair<Int, Int>? {
        for (r in 0 until rows) for (c in 0 until COLS) {
            if (items.none { it.covers(c, r) }) return c to r
        }
        return null
    }

    /**
     * Drag [from] onto [to] (page cells are Slot.HOME, dock cells Slot.DOCK). An app moves
     * into an empty cell or swaps with the app there, on the page, in the dock or between
     * them. Never makes an app appear twice. Returns the new page and dock, or null.
     */
    fun move(
        items: List<GridItem>, dock: List<String?>, rows: Int, from: Slot, to: Slot,
    ): Pair<List<GridItem>, List<String?>>? {
        if (from == to) return null
        if (from.area !in setOf(Slot.HOME, Slot.DOCK) || to.area !in setOf(Slot.HOME, Slot.DOCK)) return null
        val grid = items.toMutableList()
        val d = dock.toMutableList()
        fun at(s: Slot) = grid.firstOrNull { it.col == s.index % COLS && it.row == s.index / COLS }
        fun onPage(id: String) = grid.any { it.id == id }

        if (from.area == Slot.HOME) {
            val source = at(from) ?: return null
            if (to.area == Slot.HOME) {
                val tc = to.index % COLS
                val tr = to.index / COLS
                if (tc !in 0 until COLS || tr !in 0 until rows || to.index < 0) return null
                val occupant = at(to)
                grid.remove(source)
                if (occupant != null) {
                    grid.remove(occupant)
                    grid += occupant.copy(col = source.col, row = source.row)
                }
                grid += source.copy(col = tc, row = tr)
            } else {
                if (to.index !in d.indices) return null
                if (source.id in d) return null
                val there = d[to.index]
                if (there != null && onPage(there)) return null
                grid.remove(source)
                if (there != null) grid += source.copy(id = there)
                d[to.index] = source.id
            }
            return grid to d
        }

        // from the dock
        val key = d.getOrNull(from.index) ?: return null
        if (to.area == Slot.DOCK) {
            if (to.index !in d.indices) return null
            d[from.index] = d[to.index]
            d[to.index] = key
            return grid to d
        }
        val tc = to.index % COLS
        val tr = to.index / COLS
        if (tc !in 0 until COLS || tr !in 0 until rows || to.index < 0) return null
        if (onPage(key)) return null
        val occupant = at(to)
        if (occupant != null) {
            if (occupant.id in d) return null
            grid.remove(occupant)
        }
        grid += GridItem(key, tc, tr, 1, 1)
        d[from.index] = occupant?.id
        return grid to d
    }
}
