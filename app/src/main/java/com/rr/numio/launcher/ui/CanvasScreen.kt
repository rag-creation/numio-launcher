package com.rr.numio.launcher.ui

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rr.numio.launcher.LauncherViewModel
import com.rr.numio.launcher.ShelfMode
import com.rr.numio.launcher.data.AppEntry
import com.rr.numio.launcher.data.Background
import com.rr.numio.launcher.data.TodaySource
import com.rr.numio.launcher.sketch.SketchInk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * The home screen, "Quiet margin": the sketch clock sits a little way down, with the date,
 * a gold stroke that grows through the day and one quiet Today line. A calm empty middle,
 * then the dock and the search pill together at the bottom.
 */
@Composable
fun CanvasScreen(
    vm: LauncherViewModel,
    onRequestHome: () -> Unit,
    onOpenClock: () -> Unit,
    onOpenLauncherInfo: () -> Unit,
    onToggleCalendar: () -> Unit,
    onOpenEvent: (id: Long, begin: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHome.current
    val dock by vm.dock.collectAsStateWithLifecycle()
    val isDefault by vm.isDefaultHome.collectAsStateWithLifecycle()
    val bg by vm.background.collectAsStateWithLifecycle()
    val showCalendar by vm.showCalendar.collectAsStateWithLifecycle()
    val swipe = with(LocalDensity.current) { 56.dp.toPx() }
    val now = rememberNow()

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
            // The clock sits about a third of the way into the free space, not under the status bar
            Spacer(Modifier.weight(0.34f))

            ClockBlock(colors, now, showCalendar, onOpenClock, onOpenEvent)

            if (!isDefault) {
                Spacer(Modifier.height(24.dp))
                HomeBanner(colors, onRequestHome)
            }

            // The empty middle: long-press here for home options
            CanvasMenuArea(
                colors = colors,
                background = bg,
                isDefault = isDefault,
                showCalendar = showCalendar,
                onBackground = vm::setBackground,
                onToggleCalendar = onToggleCalendar,
                onRequestHome = onRequestHome,
                onLauncherInfo = onOpenLauncherInfo,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            Dock(vm, colors, dock)
            Spacer(Modifier.height(20.dp))
            SearchPill(colors) { vm.openShelf(ShelfMode.Search) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── time ────────────────────────────────────────────────────────────────────

/**
 * The current time, updated every minute and when the clock, time zone or next alarm
 * changes. Only while the home screen is visible, so it costs nothing in the background.
 */
@Composable
private fun rememberNow(): Long {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LifecycleStartEffect(context) {
        now = System.currentTimeMillis()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                now = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onStopOrDispose { context.unregisterReceiver(receiver) }
    }
    return now
}

private fun clockTime(context: Context, t: Long, now: Long): String {
    val is24 = DateFormat.is24HourFormat(context)
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.ENGLISH).format(Date(t)).lowercase(Locale.ENGLISH)
    return if (sameDay(t, now)) time else "$time tomorrow"
}

private fun sameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

/** 0 at midnight, 1 at the next midnight. */
private fun dayFraction(now: Long): Float {
    val c = Calendar.getInstance().apply { timeInMillis = now }
    val minutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    return minutes / 1440f
}

// ── clock, date, gold stroke, Today line ─────────────────────────────────────

@Composable
private fun ClockBlock(
    colors: NumioColors,
    now: Long,
    showCalendar: Boolean,
    onOpenClock: () -> Unit,
    onOpenEvent: (Long, Long) -> Unit,
) {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    // English on purpose: the hand-drawn sheet has A–Z only
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.ENGLISH).format(Date(now))
    val date = SimpleDateFormat("EEE d MMM", Locale.ENGLISH).format(Date(now)).uppercase(Locale.ENGLISH)
    val spoken = SimpleDateFormat(if (is24) "HH:mm, EEEE d MMMM" else "h:mm a, EEEE d MMMM", Locale.getDefault())
        .format(Date(now))

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Shrink the time if it would not fit the width (landscape, big font settings)
        val density = LocalDensity.current
        val widthAt100 = with(density) { SketchInk.measure(context, "88:88", 100.dp.toPx()).toDp() }
        val timeH = minOf(100.dp, 100.dp * (maxWidth / (widthAt100 * 1.12f)))
        val dateH = timeH * 0.26f

        Column {
            Column(
                Modifier
                    .semantics(mergeDescendants = true) { contentDescription = spoken }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClickLabel = "Open clock",
                        onClick = onOpenClock,
                    ),
            ) {
                SketchText(time, timeH, colors.ink, colonColor = colors.gold, shadow = colors.onWallpaper)
                Spacer(Modifier.height(6.dp))
                SketchText(
                    date, dateH, colors.inkSoft,
                    shadow = colors.onWallpaper,
                    modifier = Modifier.offset(x = 4.dp),
                )
                DayStroke(date, dateH, now, colors.gold)
            }
            TodayLine(colors, now, showCalendar, onOpenClock, onOpenEvent)
        }
    }
}

/**
 * The Numio signature: one imperfect gold stroke under the date. It grows through the
 * day (short in the morning, nearly the full date by night). The wobble is fixed for the
 * day, so as it grows the line already drawn never changes; tomorrow's is a new line.
 */
@Composable
private fun DayStroke(date: String, dateH: Dp, now: Long, gold: Color) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val hPx = with(density) { dateH.toPx() }
    val pad = hPx * 0.08f // SketchText's own padding, so the stroke lines up with the letters
    val widthDp = with(density) { SketchInk.measure(context, date, hPx).toDp() }
    val padDp = with(density) { pad.toDp() }
    val fraction = dayFraction(now)
    val day = Calendar.getInstance().apply { timeInMillis = now }.let { it.get(Calendar.YEAR) * 400 + it.get(Calendar.DAY_OF_YEAR) }

    Canvas(
        Modifier
            .offset(x = 4.dp)
            .padding(start = padDp, top = 4.dp)
            .width(widthDp)
            .height(10.dp)
    ) {
        val sw = 2.2.dp.toPx()
        val len = size.width * fraction.coerceAtLeast(0.05f)
        val step = 3.dp.toPx()
        val rnd = Random(day)
        val path = Path()
        var x = sw / 2
        var drift = 0f
        val mid = size.height * 0.5f
        path.moveTo(x, mid)
        while (x < len) {
            x = (x + step).coerceAtMost(len)
            drift = (drift + (rnd.nextFloat() - 0.5f) * sw * 0.35f).coerceIn(-sw * 0.8f, sw * 0.8f)
            path.lineTo(x, mid + drift)
        }
        drawPath(path, gold, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** "alarm 6:30 am · Dentist 4:30 pm". Each part only when real; the line hides when both are empty. */
@Composable
private fun TodayLine(
    colors: NumioColors,
    now: Long,
    showCalendar: Boolean,
    onOpenClock: () -> Unit,
    onOpenEvent: (Long, Long) -> Unit,
) {
    val context = LocalContext.current
    val alarm = remember(now) { TodaySource.nextAlarm(context, now) }
    val event by produceState<TodaySource.NextEvent?>(null, now, showCalendar) {
        value = if (showCalendar) withContext(Dispatchers.IO) { TodaySource.nextEvent(context, now) } else null
    }
    if (alarm == null && event == null) return

    val style = TextStyle(
        color = colors.inkSoft,
        fontSize = 14.sp,
        shadow = if (colors.onWallpaper) Shadow(Color(0x8C000000), Offset(0f, 2f), 6f) else null,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 6.dp, top = 12.dp),
    ) {
        if (alarm != null) {
            val text = "alarm ${clockTime(context, alarm, now)}"
            Text(
                text, style = style, maxLines = 1,
                modifier = Modifier
                    .clickable(onClickLabel = "Open clock", role = Role.Button, onClick = onOpenClock)
                    .semantics { contentDescription = "Next $text" },
            )
        }
        val e = event
        if (e != null) {
            if (alarm != null) Text("  ·  ", style = style)
            Text(
                "${e.title} ${clockTime(context, e.begin, now)}",
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable(onClickLabel = "Open event", role = Role.Button) { onOpenEvent(e.id, e.begin) },
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
            .sketchBorder(colors.gold, 14.dp, seed = 11, fill = colors.card, width = 1.3.dp, wobble = 0.6f)
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
    showCalendar: Boolean,
    onBackground: (Background) -> Unit,
    onToggleCalendar: () -> Unit,
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
                fun tick(on: Boolean, label: String) = if (on) "✓  $label" else label
                DropdownMenuItem(
                    text = { Text(tick(background == Background.Paper, "Paper background")) },
                    onClick = { menuAt = null; onBackground(Background.Paper) },
                )
                DropdownMenuItem(
                    text = { Text(tick(background == Background.Wallpaper, "Show my wallpaper")) },
                    onClick = { menuAt = null; onBackground(Background.Wallpaper) },
                )
                DropdownMenuItem(
                    text = { Text(tick(showCalendar, "Show next calendar event")) },
                    onClick = { menuAt = null; onToggleCalendar() },
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
private fun Dock(vm: LauncherViewModel, colors: NumioColors, dock: List<AppEntry>) {
    if (dock.isEmpty()) {
        Text(
            "Long-press an app in the Shelf to add it here",
            style = TextStyle(color = colors.inkSoft, fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        )
        return
    }
    // Icons sit a little in from the edges, lined up with the pill's rounded ends
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        dock.forEach { entry ->
            AppTile(
                entry, vm, colors,
                iconSize = 50.dp,
                showLabel = false,
                modifier = Modifier.width(62.dp),
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
            .height(44.dp)
            .sketchBorder(colors.inkSoft.copy(alpha = 0.7f), 22.dp, seed = 3, fill = colors.card, width = 1.2.dp, wobble = 0.5f)
            .clickable(role = Role.Button, onClickLabel = "Search apps", onClick = onClick)
            .padding(horizontal = 18.dp),
    ) {
        Box(Modifier.size(8.dp).background(colors.gold, CircleShape))
        Spacer(Modifier.width(12.dp))
        Text("Search apps", style = TextStyle(color = colors.inkSoft, fontSize = 14.sp))
    }
}
