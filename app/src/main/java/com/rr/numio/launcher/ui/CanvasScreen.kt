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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import com.rr.numio.launcher.data.GridItem
import com.rr.numio.launcher.data.InkDrawing
import com.rr.numio.launcher.data.InkPage
import com.rr.numio.launcher.data.Slot
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
import com.rr.numio.launcher.data.HomeTheme
import com.rr.numio.launcher.data.PortalRules
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
    onSwitchTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHome.current
    val dockCells by vm.dockCells.collectAsStateWithLifecycle()
    val pageApps by vm.inkPage.collectAsStateWithLifecycle()
    val apps by vm.appsByKey.collectAsStateWithLifecycle()
    // Hold an app on the page or in the dock, then move it: drops on any free cell or swaps
    val placement = rememberPlacement(vm::inkDrop, vm::inkCanDrop)
    val isDefault by vm.isDefaultHome.collectAsStateWithLifecycle()
    val bg by vm.background.collectAsStateWithLifecycle()
    val showCalendar by vm.showCalendar.collectAsStateWithLifecycle()
    val hasWork by vm.hasWorkProfile.collectAsStateWithLifecycle()
    val portal by vm.portal.collectAsStateWithLifecycle()
    val quote by vm.inkQuote.collectAsStateWithLifecycle()
    var editQuote by remember { mutableStateOf(false) }
    val appearance by vm.inkAppearance.collectAsStateWithLifecycle()
    val warmPaper by vm.inkWarmPaper.collectAsStateWithLifecycle()
    val drawing by vm.inkDrawing.collectAsStateWithLifecycle()
    var editAppearance by remember { mutableStateOf(false) }
    val showStatusBar by vm.inkStatusBar.collectAsStateWithLifecycle()
    val swipe = with(LocalDensity.current) { 56.dp.toPx() }
    val now = rememberNow()
    // Ink on paper is a sketchbook page; on the wallpaper it keeps its clean look
    val page = colors.sketchbook

    if (editQuote) {
        QuoteDialog(quote, onSave = { vm.setInkQuote(it); editQuote = false }, onDismiss = { editQuote = false })
    }

    if (editAppearance) {
        AppearanceDialog(
            appearance, warmPaper, vm::setInkAppearance, vm::setInkWarmPaper,
            drawing = drawing, onDrawing = vm::setInkDrawing,
        ) { editAppearance = false }
    }

    Box(
        modifier
            .fillMaxSize()
            .sketchbookPaper(colors)
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
                .windowInsetsPadding(inkTopInsets())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            // The clock sits about a third of the way into the free space, not under the status bar.
            // On the sketchbook page it starts near the top, below the logo, to leave room for the drawing.
            if (page) Spacer(Modifier.height(72.dp)) else Spacer(Modifier.weight(0.34f))

            ClockBlock(colors, now, showCalendar, onOpenClock, onOpenEvent)

            if (page && quote.visible) {
                Spacer(Modifier.height(14.dp))
                InkQuoteText(quote, colors, Modifier.padding(start = 6.dp))
            }

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
                onSwitchTheme = onSwitchTheme,
                onPortalSettings = vm::openPortalSettings,
                portalVisible = portal.visible,
                onRequestHome = onRequestHome,
                onLauncherInfo = onOpenLauncherInfo,
                onEditQuote = if (page) ({ editQuote = true }) else null,
                onAppearance = { editAppearance = true },
                showStatusBar = showStatusBar,
                onToggleStatusBar = { vm.setInkStatusBar(!showStatusBar) },
                landscape = page,
                drawing = drawing,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { openMenu ->
                InkPageGrid(vm, colors, pageApps, apps, placement, openMenu)
            }

            Dock(vm, colors, dockCells, placement)
            Spacer(Modifier.height(20.dp))
            SearchPill(colors) { vm.openShelf(ShelfMode.Search) }
            Spacer(Modifier.height(16.dp))
        }

        // Ink: the Numio logo sits at a fixed spot, top right, in the open space above the
        // clock (Ink has no grid to place it on). Hidden entirely when turned off.
        if (portal.visible) {
            PortalButton(
                tint = PortalRules.color(portal.color, HomeTheme.Ink),
                description = portalDescription(PortalRules.tap(portal, hasWork)),
                onTap = vm::tapPortal,
                onLongPress = vm::openPortalSettings,
                style = portal.style,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(inkTopInsets())
                    .padding(top = 16.dp, end = 16.dp),
            )
        }
    }
}

// ── time ────────────────────────────────────────────────────────────────────

/**
 * The current time, updated every minute and when the clock, time zone or next alarm
 * changes. Only while the home screen is visible, so it costs nothing in the background.
 */
@Composable
internal fun rememberNow(): Long {
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

internal fun clockTime(context: Context, t: Long, now: Long): String {
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

/** The real next alarm (within a day) and, if turned on and allowed, the next event. Shared by Ink and Sol. */
@Composable
internal fun rememberToday(now: Long, showCalendar: Boolean): Pair<Long?, TodaySource.NextEvent?> {
    val context = LocalContext.current
    val alarm = remember(now) { TodaySource.nextAlarm(context, now) }
    val event by produceState<TodaySource.NextEvent?>(null, now, showCalendar) {
        value = if (showCalendar) withContext(Dispatchers.IO) { TodaySource.nextEvent(context, now) } else null
    }
    return alarm to event
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
    val (alarm, event) = rememberToday(now, showCalendar)
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
        Text("Set", style = TextStyle(color = colors.goldText, fontSize = 15.sp))
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
    onSwitchTheme: () -> Unit,
    onPortalSettings: () -> Unit,
    portalVisible: Boolean,
    onRequestHome: () -> Unit,
    onLauncherInfo: () -> Unit,
    onEditQuote: (() -> Unit)?,
    onAppearance: () -> Unit,
    showStatusBar: Boolean,
    onToggleStatusBar: () -> Unit,
    landscape: Boolean,
    drawing: InkDrawing,
    modifier: Modifier,
    /** The page's apps, drawn over the drawing. Its empty cells call openMenu on long-press. */
    grid: @Composable (openMenu: (Offset) -> Unit) -> Unit,
) {
    val density = LocalDensity.current
    var menuAt by remember { mutableStateOf<Offset?>(null) }

    Box(modifier) {
        // The page's drawing fills the open space, behind the apps (decoration only, never touched)
        if (landscape) InkLandscape(colors.icons, Modifier.fillMaxSize().padding(top = 8.dp, bottom = 14.dp), drawing)
        // The grid covers the whole area: long-press on an empty spot opens the home menu,
        // long-press on an app is the app's own (hold and move to drag it)
        grid { menuAt = it }
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
                DropdownMenuItem(text = { Text("Ink appearance…") }, onClick = { menuAt = null; onAppearance() })
                DropdownMenuItem(
                    text = { Text(tick(showStatusBar, "Show status bar")) },
                    onClick = { menuAt = null; onToggleStatusBar() },
                )
                if (onEditQuote != null) {
                    DropdownMenuItem(text = { Text("Quote…") }, onClick = { menuAt = null; onEditQuote() })
                }
                DropdownMenuItem(
                    text = { Text(if (portalVisible) "Numio logo…" else "Numio logo… (hidden)") },
                    onClick = { menuAt = null; onPortalSettings() },
                )
                DropdownMenuItem(text = { Text("Switch to Sol theme") }, onClick = { menuAt = null; onSwitchTheme() })
                if (!isDefault) {
                    DropdownMenuItem(text = { Text("Set as home screen") }, onClick = { menuAt = null; onRequestHome() })
                }
                DropdownMenuItem(text = { Text("Launcher app info") }, onClick = { menuAt = null; onLauncherInfo() })
            }
        }
    }
}

@Composable
private fun Dock(vm: LauncherViewModel, colors: NumioColors, cells: List<AppEntry?>, placement: Placement) {
    // A free dock cell, offered only while a page app is being dragged (the dock packs its apps)
    val free = cells.indexOfFirst { it == null }
    val offerFree = free >= 0 && placement.dragging?.area == Slot.HOME
    if (cells.all { it == null } && !offerFree) {
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
        // labelled: share the width evenly, so five fit on any phone
        val cell = if (colors.sketchbook) Modifier.weight(1f) else Modifier.width(62.dp)
        cells.forEachIndexed { i, entry ->
            if (entry != null) {
                key(entry.key) {
                    // On the sketchbook page the dock is labelled, like a page of notes
                    AppTile(
                        entry, vm, colors,
                        iconSize = 50.dp,
                        showLabel = colors.sketchbook,
                        modifier = cell,
                        placement = placement,
                        slot = Slot(Slot.DOCK, i),
                    )
                }
            }
        }
        if (offerFree) {
            val slot = Slot(Slot.DOCK, free)
            DisposableEffect(slot) { onDispose { placement.slots.remove(slot) } }
            Box(cell.height(if (colors.sketchbook) 80.dp else 62.dp).dropCell(placement, slot), contentAlignment = Alignment.Center) {
                DropRing(colors, placement.isTarget(slot))
            }
        }
    }
}

/** A free spot an app can be dropped on: a faint pencil circle, amber when the app is over it. */
@Composable
private fun DropRing(colors: NumioColors, active: Boolean) {
    Box(
        Modifier
            .size(56.dp)
            .sketchBorder(
                if (active) colors.gold else colors.inkSoft.copy(alpha = 0.35f),
                28.dp, seed = 7, width = if (active) 2.dp else 1.dp,
            )
    )
}

/**
 * Ink's page: [InkPage.COLS] columns and as many rows as fit the open space under the clock,
 * drawn over the sketchbook drawing. Apps sit in their cells; hold one and move it to place
 * it anywhere (or into the dock). Empty cells take drops and open the home menu on long-press.
 */
@Composable
private fun InkPageGrid(
    vm: LauncherViewModel,
    colors: NumioColors,
    items: List<GridItem>,
    apps: Map<String, AppEntry>,
    placement: Placement,
    openMenu: (Offset) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val cols = InkPage.COLS
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rows = (maxHeight / 92.dp).toInt().coerceIn(1, 8)
        LaunchedEffect(rows) { vm.setInkRows(rows) }
        val cellW = maxWidth / cols
        val cellH = maxHeight / rows
        val dragging = placement.dragging
        Box(
            Modifier
                .fillMaxSize()
                // the page reports every cell's place on screen, filled or not
                .onGloballyPositioned { coords ->
                    val o = coords.boundsInWindow().topLeft
                    val cw = coords.size.width / cols.toFloat()
                    val ch = coords.size.height / rows.toFloat()
                    placement.slots.keys.removeAll { it.area == Slot.HOME }
                    for (r in 0 until rows) for (c in 0 until cols) {
                        placement.slots[Slot(Slot.HOME, r * cols + c)] =
                            Rect(o.x + c * cw, o.y + r * ch, o.x + (c + 1) * cw, o.y + (r + 1) * ch)
                    }
                }
        ) {
            // Empty cells: long-press for the home menu; while dragging, a faint ring shows the spot
            for (r in 0 until rows) for (c in 0 until cols) {
                if (items.none { it.covers(c, r) }) {
                    val slot = Slot(Slot.HOME, r * cols + c)
                    Box(
                        Modifier
                            .offset(cellW * c, cellH * r)
                            .size(cellW, cellH)
                            .pointerInput(c, r, rows) {
                                detectTapGestures(onLongPress = { p ->
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    openMenu(Offset(cellW.toPx() * c + p.x, cellH.toPx() * r + p.y))
                                })
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (dragging != null && placement.isTarget(slot)) DropRing(colors, true)
                    }
                }
            }

            items.forEach { item ->
                val entry = apps[item.id] ?: return@forEach
                key(item.id) {
                    val slot = Slot(Slot.HOME, item.row * cols + item.col)
                    Box(
                        Modifier
                            .offset(cellW * item.col, cellH * item.row)
                            .size(cellW, cellH)
                            .zIndex(if (placement.dragging == slot) 1f else 0f),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppTile(
                            entry, vm, colors, iconSize = 50.dp,
                            showLabel = colors.sketchbook,
                            modifier = Modifier.fillMaxWidth(),
                            placement = placement, slot = slot, registerSlot = false,
                        )
                    }
                }
            }
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
