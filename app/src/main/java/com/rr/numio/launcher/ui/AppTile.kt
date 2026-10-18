package com.rr.numio.launcher.ui

import android.app.ActivityOptions
import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.launcher.LauncherViewModel
import com.rr.numio.launcher.data.AppEntry
import com.rr.numio.launcher.icons.ThemedIcons
import com.rr.numio.launcher.sketch.SketchIcons

/** Ink draws its own sketch icon; Sol draws the app's themed icon on a sage or cream circle. */
@Composable
fun AppIcon(entry: AppEntry, size: Dp, modifier: Modifier = Modifier, sketch: Boolean = true) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val start = if (sketch) SketchIcons.cached(entry, px) else ThemedIcons.cached(entry, px)
    val bitmap: ImageBitmap? by produceState(start, entry.key, px, sketch) {
        if (value == null) {
            value = if (sketch) SketchIcons.load(context, entry, px) else ThemedIcons.load(entry, px)
        }
    }
    Box(modifier.size(size)) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

/**
 * An app: tap opens it, long-press shows Dock / App info / Uninstall.
 * The icon grows into the app as it opens.
 */
@Composable
fun AppTile(
    entry: AppEntry,
    vm: LauncherViewModel,
    colors: NumioColors,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    highlighted: Boolean = false,
    /** Small grey line under the name: the maker for same-name apps, or the profile. */
    subtitle: String? = null,
    /** Set for icons that can be rearranged (Sol's home rows and dock): hold and move to drag. */
    reorder: Reorder? = null,
    index: Int = -1,
) {
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    var bounds by remember { mutableStateOf(ComposeRect.Zero) }
    var menu by remember { mutableStateOf(false) }

    val open = {
        val r = Rect(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())
        val options = ActivityOptions.makeScaleUpAnimation(view, r.left, r.top, r.width(), r.height()).toBundle()
        vm.launch(entry, r, options)
    }
    val hold = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        menu = true
    }
    val description = listOfNotNull(entry.label, subtitle, entry.profileLabel?.let { "$it profile" })
        .distinct().joinToString(", ")

    val isTarget = reorder != null && reorder.dragging >= 0 && reorder.target == index && reorder.dragging != index

    // Plain tiles (Shelf, Ink) keep the tested click handling; home rows and dock get
    // tap / hold / drag on a box that stays put while the icon is lifted.
    val outer = if (reorder == null) {
        modifier
    } else {
        modifier
            .semantics {
                contentDescription = description
                onClick(label = "Open") { open(); true }
                onLongClick(label = "App options") { menu = true; true }
            }
            .reorderSlot(
                reorder, index, entry.key,
                onTap = open,
                onHoldStart = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                onMenu = { menu = true },
            )
    }
    val plainClick = if (reorder == null) {
        Modifier
            .semantics { contentDescription = description }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = open,
                onLongClick = hold,
                onLongClickLabel = "App options",
            )
    } else Modifier

    Box(outer) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .then(plainClick)
                .padding(vertical = 6.dp),
        ) {
            val ring = when {
                !(highlighted || isTarget) -> Modifier
                colors.sketch -> Modifier.sketchBorder(colors.gold, iconSize, seed = 5, width = 2.dp)
                else -> Modifier.border(2.dp, colors.gold, CircleShape)
            }
            Box(ring.padding(if (highlighted || isTarget) 3.dp else 0.dp)) {
                AppIcon(
                    entry, iconSize,
                    Modifier.onGloballyPositioned { bounds = it.boundsInWindow() },
                    sketch = colors.sketch,
                )
            }
            if (showLabel) {
                Spacer(Modifier.height(6.dp))
                Text(
                    entry.label,
                    style = TextStyle(color = colors.ink, fontSize = 12.sp, textAlign = TextAlign.Center),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = TextStyle(color = colors.inkSoft, fontSize = 10.5.sp, textAlign = TextAlign.Center),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    )
                }
            }
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (!colors.sketch) {
                // Sol also has home rows
                val onHome = vm.isOnHome(entry)
                DropdownMenuItem(
                    text = { Text(if (onHome) "Remove from home" else "Add to home") },
                    onClick = {
                        menu = false
                        if (onHome) vm.removeFromHome(entry) else vm.addToHome(entry)
                    },
                )
            }
            val inDock = vm.isInDock(entry)
            DropdownMenuItem(
                text = { Text(if (inDock) "Remove from dock" else "Add to dock") },
                onClick = {
                    menu = false
                    if (inDock) vm.removeFromDock(entry) else vm.addToDock(entry)
                },
            )
            DropdownMenuItem(text = { Text("App info") }, onClick = { menu = false; vm.openAppInfo(entry) })
            DropdownMenuItem(text = { Text("Uninstall") }, onClick = { menu = false; vm.uninstall(entry) })
        }
    }
}
