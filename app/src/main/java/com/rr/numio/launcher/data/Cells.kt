package com.rr.numio.launcher.data

/** A place an app can sit: one of the home cells or one of the dock cells. */
data class Slot(val area: Int, val index: Int) {
    companion object {
        const val HOME = 0
        const val DOCK = 1
    }
}

/**
 * Home and dock as fixed rows of cells, any of which may be empty (null).
 * Pure Kotlin, so the moves are unit tested.
 */
object Cells {

    /** Saved keys → exactly [size] cells; "" or missing means empty. Older saves were packed lists. */
    fun pad(keys: List<String?>, size: Int): List<String?> =
        (keys.map { it?.ifEmpty { null } } + List(size) { null }).take(size)

    /**
     * Drag [from] onto [to]: into an empty cell it moves; onto another app the two swap.
     * Works within and between home and dock. Dragging an empty cell does nothing.
     */
    fun move(home: List<String?>, dock: List<String?>, from: Slot, to: Slot): Pair<List<String?>, List<String?>> {
        if (from == to) return home to dock
        val h = home.toMutableList()
        val d = dock.toMutableList()
        fun list(s: Slot) = if (s.area == Slot.HOME) h else d
        val a = list(from)
        val b = list(to)
        if (from.index !in a.indices || to.index !in b.indices) return home to dock
        val moving = a[from.index] ?: return home to dock
        a[from.index] = b[to.index]
        b[to.index] = moving
        return h to d
    }

    /** The first empty cell, or -1 when full. */
    fun firstEmpty(cells: List<String?>): Int = cells.indexOfFirst { it == null }
}
