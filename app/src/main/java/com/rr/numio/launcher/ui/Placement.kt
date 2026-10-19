package com.rr.numio.launcher.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import com.rr.numio.launcher.data.Slot

/**
 * Drag-and-drop over Sol's home cells and dock cells (filled or empty).
 * Every cell reports where it sits; while an app is dragged, the cell nearest the finger is
 * the drop target, unless the finger is far from every cell (then nothing moves).
 */
class Placement(private val onDrop: (from: Slot, to: Slot) -> Unit) {
    /** Where each cell sits on screen. Plain map: only read during a drag. */
    val slots = HashMap<Slot, Rect>()

    var dragging by mutableStateOf<Slot?>(null)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var target by mutableStateOf<Slot?>(null)
        private set

    fun start(slot: Slot) {
        dragging = slot
        offset = Offset.Zero
        target = slot
    }

    /** [total] is how far the finger has moved since the hold, in screen pixels. */
    fun drag(total: Offset) {
        val from = dragging ?: return
        offset = total
        val home = slots[from] ?: return
        val finger = home.center + total
        val nearest = slots.minByOrNull { (it.value.center - finger).getDistance() } ?: return
        val reach = maxOf(nearest.value.width, nearest.value.height) * 1.2f
        target = if ((nearest.value.center - finger).getDistance() <= reach) nearest.key else from
    }

    /** Held but not moved: put it back without changing anything. */
    fun cancel() {
        dragging = null
        offset = Offset.Zero
        target = null
    }

    fun end() {
        val from = dragging
        val to = target
        cancel()
        if (from != null && to != null && to != from) onDrop(from, to)
    }

    /** This cell is where the dragged app would land. */
    fun isTarget(slot: Slot) = dragging != null && dragging != slot && target == slot
}

@Composable
fun rememberPlacement(onDrop: (from: Slot, to: Slot) -> Unit): Placement {
    val latest by rememberUpdatedState(onDrop)
    return remember { Placement { a, b -> latest(a, b) } }
}

/** An empty cell: nothing to drag, but apps can be dropped on it. */
fun Modifier.dropCell(placement: Placement, slot: Slot): Modifier =
    onGloballyPositioned { placement.slots[slot] = it.boundsInWindow() }

/**
 * A cell with an app (used by the home cells, the dock and the unit test): reports where the
 * cell sits, handles tap / hold / drag, and lifts the icon while it's held. The gesture sits
 * before the lift, so it measures real finger travel.
 *
 * Hold lifts the icon at once. Moving drags it; letting go without moving calls [onMenu].
 * The menu only opens after the finger is up, so no popup appears in the middle of a drag.
 */
fun Modifier.appCell(
    placement: Placement,
    slot: Slot,
    key: Any,
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onMenu: () -> Unit,
): Modifier = this
    .onGloballyPositioned { if (placement.dragging != slot) placement.slots[slot] = it.boundsInWindow() }
    .tapHoldDrag(
        key = key to slot,
        onTap = onTap,
        onHoldStart = { placement.start(slot); onHoldStart() },
        onHoldRelease = { placement.cancel(); onMenu() },
        onDrag = placement::drag,
        onDragEnd = placement::end,
    )
    .zIndex(if (placement.dragging == slot) 1f else 0f)
    .graphicsLayer {
        if (placement.dragging == slot) {
            translationX = placement.offset.x
            translationY = placement.offset.y
            scaleX = 1.12f
            scaleY = 1.12f
        }
    }

/**
 * The raw gesture: tap, hold (then release without moving), or hold-and-drag.
 * Moving before the hold isn't consumed, so a swipe that starts on an icon still opens the Shelf.
 * [onDrag] gets the total finger travel since the hold.
 */
fun Modifier.tapHoldDrag(
    key: Any,
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldRelease: () -> Unit,
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
                onHoldStart()
                var dragging = false
                var done = false
                while (!done) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    if (change == null || change.changedToUp()) {
                        change?.consume()
                        done = true
                    } else {
                        val total = change.position - down.position
                        if (!dragging && total.getDistance() > slop) dragging = true
                        if (dragging) onDrag(total)
                        change.consume()
                    }
                }
                if (dragging) onDragEnd() else onHoldRelease()
            }
        }
    }
}
