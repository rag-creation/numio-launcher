package com.rr.numio.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rr.numio.launcher.LauncherViewModel
import com.rr.numio.launcher.ShelfMode
import com.rr.numio.launcher.data.Background
import com.rr.numio.launcher.sketch.SketchInk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The home screen: sketch clock, an empty calm middle, the dock and the search pill. */
@Composable
fun CanvasScreen(
    vm: LauncherViewModel,
    onRequestHome: () -> Unit,
    onOpenClock: () -> Unit,
    onOpenLauncherInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHome.current
    val dock by vm.dock.collectAsStateWithLifecycle()
    val isDefault by vm.isDefaultHome.collectAsStateWithLifecycle()
    val bg by vm.background.collectAsStateWithLifecycle()
    val swipe = with(LocalDensity.current) { 56.dp.toPx() }

    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            // Swipe up anywhere opens the Shelf
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged < -swipe) vm.openShelf(ShelfMode.Browse) },
                    onVerticalDrag = { _, dy -> dragged += dy },
                )
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(40.dp))
            SketchClock(colors, onClick = onOpenClock)

            if (!isDefault) {
                Spacer(Modifier.height(20.dp))
                HomeBanner(colors, onRequestHome)
            }

            // The empty middle: long-press here for home options
            CanvasMenuArea(
                colors = colors,
                background = bg,
                isDefault = isDefault,
                onBackground = vm::setBackground,
                onRequestHome = onRequestHome,
                onLauncherInfo = onOpenLauncherInfo,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            Dock(vm, colors, dock)
            Spacer(Modifier.height(14.dp))
            SearchPill(colors) { vm.openShelf(ShelfMode.Search) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ── clock ───────────────────────────────────────────────────────────────────

/** The current time, updated every minute and whenever the clock or time zone changes. */
@Composable
private fun rememberNow(): Long {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                now = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    // Ticks don't arrive while the screen is off
    LifecycleResumeEffect(Unit) {
        now = System.currentTimeMillis()
        onPauseOrDispose { }
    }
    return now
}

@Composable
private fun SketchClock(colors: NumioColors, onClick: () -> Unit) {
    val context = LocalContext.current
    val now = rememberNow()
    val is24 = DateFormat.is24HourFormat(context)
    // English on purpose: the hand-drawn sheet has A–Z only
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.ENGLISH).format(Date(now))
    val date = SimpleDateFormat("EEE d MMM", Locale.ENGLISH).format(Date(now)).uppercase(Locale.ENGLISH)
    val spoken = SimpleDateFormat(if (is24) "HH:mm, EEEE d MMMM" else "h:mm a, EEEE d MMMM", Locale.getDefault())
        .format(Date(now))

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = "Open clock",
                onClick = onClick,
            ),
    ) {
        // Shrink the time if it would not fit the width (landscape, big font settings)
        val density = LocalDensity.current
        val widthAt100 = with(density) { SketchInk.measure(context, "88:88", 100.dp.toPx()).toDp() }
        val timeH = minOf(100.dp, 100.dp * (maxWidth / (widthAt100 * 1.12f)))
        Column {
            SketchText(
                time, timeH, colors.ink,
                colonColor = colors.gold, shadow = colors.onWallpaper,
            )
            Spacer(Modifier.height(8.dp))
            SketchText(
                date, timeH * 0.26f, colors.inkSoft,
                shadow = colors.onWallpaper,
                modifier = Modifier.offset(x = 4.dp),
            )
        }
    }
}

// ── banner, menu, dock, search ──────────────────────────────────────────────

@Composable
private fun HomeBanner(colors: NumioColors, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .sketchBorder(colors.gold, 14.dp, seed = 11, fill = colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            "Make Numio your home screen",
            style = TextStyle(color = colors.ink, fontSize = 15.sp),
            modifier = Modifier.weight(1f),
        )
        Text("Set", style = TextStyle(color = colors.gold, fontSize = 15.sp))
    }
}

@Composable
private fun CanvasMenuArea(
    colors: NumioColors,
    background: Background,
    isDefault: Boolean,
    onBackground: (Background) -> Unit,
    onRequestHome: () -> Unit,
    onLauncherInfo: () -> Unit,
    modifier: Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    var menuAt by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                menuAt = it
            })
        }
    ) {
        val at = menuAt
        Box(
            Modifier.offset(
                x = with(density) { (at?.x ?: 0f).toDp() },
                y = with(density) { (at?.y ?: 0f).toDp() },
            )
        ) {
            DropdownMenu(
                expanded = at != null,
                onDismissRequest = { menuAt = null },
                offset = DpOffset.Zero,
            ) {
                DropdownMenuItem(
                    text = { Text(if (background == Background.Paper) "✓  Paper background" else "Paper background") },
                    onClick = { menuAt = null; onBackground(Background.Paper) },
                )
                DropdownMenuItem(
                    text = { Text(if (background == Background.Wallpaper) "✓  Show my wallpaper" else "Show my wallpaper") },
                    onClick = { menuAt = null; onBackground(Background.Wallpaper) },
                )
                if (!isDefault) {
                    DropdownMenuItem(text = { Text("Set as home screen") }, onClick = { menuAt = null; onRequestHome() })
                }
                DropdownMenuItem(text = { Text("Launcher app info") }, onClick = { menuAt = null; onLauncherInfo() })
            }
        }
    }
}

@Composable
private fun Dock(vm: LauncherViewModel, colors: NumioColors, dock: List<com.rr.numio.launcher.data.AppEntry>) {
    if (dock.isEmpty()) {
        Text(
            "Long-press an app in the Shelf to add it here",
            style = TextStyle(color = colors.inkSoft, fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        )
        return
    }
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        dock.forEach { entry ->
            AppTile(
                entry, vm, colors,
                iconSize = 54.dp,
                showLabel = false,
                modifier = Modifier.width(64.dp),
            )
        }
    }
}

@Composable
private fun SearchPill(colors: NumioColors, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .sketchBorder(colors.inkSoft, 25.dp, seed = 3, fill = colors.card)
            .clickable(role = Role.Button, onClickLabel = "Search apps", onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        Box(Modifier.size(9.dp).background(colors.gold, CircleShape))
        Spacer(Modifier.width(12.dp))
        Text("Search apps", style = TextStyle(color = colors.inkSoft, fontSize = 15.sp))
    }
}
