package com.rr.numio.launcher.ui

import android.app.ActivityOptions
import android.graphics.Rect
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.launcher.LauncherViewModel
import com.rr.numio.launcher.data.AppEntry
import com.rr.numio.launcher.sketch.SketchIcons

@Composable
fun AppIcon(entry: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bitmap: ImageBitmap? by produceState(SketchIcons.cached(entry, px), entry.key, px) {
        if (value == null) value = SketchIcons.load(context, entry, px)
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
) {
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    var bounds by remember { mutableStateOf(ComposeRect.Zero) }
    var menu by remember { mutableStateOf(false) }

    Box(modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = entry.label }
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        val r = Rect(
                            bounds.left.toInt(), bounds.top.toInt(),
                            bounds.right.toInt(), bounds.bottom.toInt()
                        )
                        val options = ActivityOptions.makeScaleUpAnimation(
                            view, r.left, r.top, r.width(), r.height()
                        ).toBundle()
                        vm.launch(entry, r, options)
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu = true
                    },
                    onLongClickLabel = "App options",
                )
                .padding(vertical = 6.dp),
        ) {
            val ring = if (highlighted) {
                Modifier.sketchBorder(colors.gold, iconSize, seed = 5, width = 2.dp)
            } else Modifier
            Box(ring.padding(if (highlighted) 3.dp else 0.dp)) {
                AppIcon(
                    entry, iconSize,
                    Modifier.onGloballyPositioned { bounds = it.boundsInWindow() },
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
            }
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
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
