package com.rr.numio.launcher.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Drag-to-rearrange for one row set (Sol's home rows, or the dock).
 * Tiles report where they sit; while one is dragged, the nearest slot is the drop target.
 */
class Reorder(private val onMove: (from: Int, to: Int) -> Unit) {
    /** Where each slot sits on screen, by index. Plain map: it's only read during a drag. */
    val slots = HashMap<Int, Rect>()

    var dragging by mutableIntStateOf(-1)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var target by mutableIntStateOf(-1)
        private set

    fun start(index: Int) {
        dragging = index
        offset = Offset.Zero
        target = index
    }

    /** [total] is how far the finger has moved since the hold, in screen pixels. */
    fun drag(total: Offset) {
        if (dragging < 0) return
        offset = total
        val centre = (slots[dragging] ?: return).center + offset
        target = slots.minByOrNull { (it.value.center - centre).getDistance() }?.key ?: dragging
    }

    fun end() {
        if (dragging >= 0 && target >= 0 && target != dragging) onMove(dragging, target)
        dragging = -1
        offset = Offset.Zero
        target = -1
    }
}

@Composable
fun rememberReorder(onMove: (from: Int, to: Int) -> Unit): Reorder {
    val latest by rememberUpdatedState(onMove)
    return remember { Reorder { a, b -> latest(a, b) } }
}

/**
 * One gesture for a home icon: tap opens, hold shows the menu, hold-and-move drags it.
 * Moving before the hold isn't consumed, so a swipe that starts on an icon still opens the Shelf.
 * Put it on a part of the tile that does NOT move while dragging, so [onDrag]'s total is the
 * real finger travel (measured inside the moving icon it would only follow at half speed).
 */
fun Modifier.tapHoldDrag(
    key: Any,
    onTap: () -> Unit,
    onHold: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (total: Offset) -> Unit,
    onDragEnd: () -> Unit,
): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val slop = viewConfiguration.touchSlop
        // 0 = tap, 1 = moved away or taken by someone else, null = held long enough
        val early: Int? = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var result = -1
            while (result < 0) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                result = when {
                    change == null || change.isConsumed -> 1
                    change.changedToUp() -> { change.consume(); 0 }
                    (change.position - down.position).getDistance() > slop -> 1
                    else -> -1
                }
            }
            result
        }
        when (early) {
            0 -> onTap()
            1 -> Unit
            else -> {
                onHold()
                var dragging = false
                var done = false
                while (!done) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    if (change == null || change.changedToUp()) {
                        change?.consume()
                        done = true
                    } else {
                        val total = change.position - down.position
                        if (!dragging && total.getDistance() > slop) {
                            dragging = true
                            onDragStart()
                        }
                        if (dragging) onDrag(total)
                        change.consume()
                    }
                }
                if (dragging) onDragEnd()
            }
        }
    }
}
