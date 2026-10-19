package com.rr.numio.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rr.numio.launcher.LauncherViewModel
import com.rr.numio.launcher.R
import com.rr.numio.launcher.ShelfMode
import com.rr.numio.launcher.data.AppEntry
import com.rr.numio.launcher.data.Slot
import com.rr.numio.launcher.data.HomeTheme
import com.rr.numio.launcher.data.PortalRules
import com.rr.numio.launcher.data.HomeGrid
import com.rr.numio.launcher.data.GridItem
import com.rr.numio.launcher.data.SolClock
import com.rr.numio.launcher.data.SolWallpaper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Sol's clock fonts, each bundled as digits and colon only. */
private val ClockFonts = mapOf(
    SolClock.Fraunces to FontFamily(Font(R.font.fraunces_bold, FontWeight.Bold)),
    SolClock.DmSerif to FontFamily(Font(R.font.dmserif_display, FontWeight.Normal)),
    SolClock.Baloo to FontFamily(Font(R.font.baloo2_extrabold, FontWeight.ExtraBold)),
    SolClock.Moirai to FontFamily(Font(R.font.moirai_one, FontWeight.Normal)),
)

/**
 * Numio Sol home: the Petal wallpaper does the work. Date and a large light clock in the
 * open cream sky, a Today chip only when there's a real alarm or event, the art in the
 * middle, then real app icons and the search pill. Same apps, dock and Shelf as Ink.
 */
@Composable
fun SolScreen(
    vm: LauncherViewModel,
    onRequestHome: () -> Unit,
    onOpenClock: () -> Unit,
    onOpenLauncherInfo: () -> Unit,
    onToggleCalendar: () -> Unit,
    onOpenEvent: (id: Long, begin: Long) -> Unit,
    onSwitchTheme: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SolColors
    val gridItems by vm.gridItems.collectAsStateWithLifecycle()
    val appsByKey by vm.appsByKey.collectAsStateWithLifecycle()
    val dockCells by vm.dockCells.collectAsStateWithLifecycle()
    // One drag-and-drop for the home grid and the dock, so apps can move between them
    val placement = rememberPlacement(vm::drop, vm::canDrop)
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(placement.target) {
        if (placement.dragging != null && placement.target != placement.dragging) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val isDefault by vm.isDefaultHome.collectAsStateWithLifecycle()
    val showCalendar by vm.showCalendar.collectAsStateWithLifecycle()
    val wallpaper by vm.solWallpaper.collectAsStateWithLifecycle()
    val clockStyle by vm.solClock.collectAsStateWithLifecycle()
    val portal by vm.portal.collectAsStateWithLifecycle()
    val hasWork by vm.hasWorkProfile.collectAsStateWithLifecycle()
    val swipe = with(LocalDensity.current) { 56.dp.toPx() }
    val now = rememberNow()

    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged < -swipe) vm.openShelf(ShelfMode.Browse) },
                    onVerticalDrag = { _, dy -> dragged += dy },
                )
            }
    ) {
        // The wallpaper fills the screen; on shorter or wider screens it trims mostly from the sky
        Image(
            bitmap = ImageBitmap.imageResource(
                if (wallpaper == SolWallpaper.Petal) R.drawable.sol_petal else R.drawable.sol_garden
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0f, 0.4f),
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            if (!isDefault) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.card.copy(alpha = 0.92f), RoundedCornerShape(18.dp))
                        .border(1.dp, colors.gold, RoundedCornerShape(18.dp))
                        .clickable(onClick = onRequestHome)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text("Make Numio your home screen", style = TextStyle(color = colors.ink, fontSize = 15.sp), modifier = Modifier.weight(1f))
                    Text("Set", style = TextStyle(color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                }
                Spacer(Modifier.height(12.dp))
            }

            // The home grid: clock, alarm chip, calendar and apps, all movable.
            // Whichever group holds the lifted item is drawn on top, so it passes over the other.
            SolHomeGrid(
                vm = vm,
                colors = colors,
                items = gridItems,
                apps = appsByKey,
                placement = placement,
                now = now,
                clockStyle = clockStyle,
                showCalendar = showCalendar,
                onOpenClock = onOpenClock,
                onOpenEvent = onOpenEvent,
                onOpenCalendar = onOpenCalendar,
                portalTint = PortalRules.color(portal.color, HomeTheme.Sol),
                portalDescription = portalDescription(PortalRules.tap(portal, hasWork)),
                onTapPortal = vm::tapPortal,
                onPortalMenu = vm::openPortalSettings,
                menuItems = { close ->
                    fun tick(on: Boolean, label: String) = if (on) "✓  $label" else label
                    DropdownMenuItem(
                        text = { Text(tick(wallpaper == SolWallpaper.Garden, "Garden wallpaper")) },
                        onClick = { close(); vm.setSolWallpaper(SolWallpaper.Garden) },
                    )
                    DropdownMenuItem(
                        text = { Text(tick(wallpaper == SolWallpaper.Petal, "Petal wallpaper")) },
                        onClick = { close(); vm.setSolWallpaper(SolWallpaper.Petal) },
                    )
                    DropdownMenuItem(
                        text = { Text(tick(showCalendar, "Show next calendar event")) },
                        onClick = { close(); onToggleCalendar() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (portal.visible) "Numio logo…" else "Numio logo… (hidden)") },
                        onClick = { close(); vm.openPortalSettings() },
                    )
                    DropdownMenuItem(text = { Text("Reset home layout") }, onClick = { close(); vm.resetHomeLayout() })
                    DropdownMenuItem(text = { Text("Switch to Ink theme") }, onClick = { close(); onSwitchTheme() })
                    if (!isDefault) {
                        DropdownMenuItem(text = { Text("Set as home screen") }, onClick = { close(); onRequestHome() })
                    }
                    DropdownMenuItem(text = { Text("Launcher app info") }, onClick = { close(); onOpenLauncherInfo() })
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .zIndex(if (placement.dragging?.area == Slot.HOME) 1f else 0f),
            )
            Spacer(Modifier.height(10.dp))
            SolDock(
                vm, colors, dockCells, placement,
                Modifier.zIndex(if (placement.dragging?.area == Slot.DOCK) 1f else 0f),
            )
            Spacer(Modifier.height(16.dp))
            SolSearchPill(colors) { vm.openShelf(ShelfMode.Search) }
            Spacer(Modifier.height(14.dp))
        }
    }
}

/**
 * Sol's home grid: [HomeGrid.COLS] columns, as many ~80dp rows as fit. Items sit at their
 * cells; empty cells take drops and open the home menu on long-press. While something is
 * dragged, its landing spot is outlined in gold (only where it's allowed to go).
 */
@Composable
private fun SolHomeGrid(
    vm: LauncherViewModel,
    colors: NumioColors,
    items: List<GridItem>,
    apps: Map<String, AppEntry>,
    placement: Placement,
    now: Long,
    clockStyle: SolClock,
    showCalendar: Boolean,
    onOpenClock: () -> Unit,
    onOpenEvent: (Long, Long) -> Unit,
    onOpenCalendar: () -> Unit,
    portalTint: Long?,
    portalDescription: String,
    onTapPortal: () -> Unit,
    onPortalMenu: () -> Unit,
    menuItems: @Composable (close: () -> Unit) -> Unit,
    modifier: Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val cols = HomeGrid.COLS
    BoxWithConstraints(modifier) {
        val rows = (maxHeight / 80.dp).toInt().coerceIn(6, 10)
        LaunchedEffect(rows) { vm.setGridRows(rows) }
        val cellW = maxWidth / cols
        val cellH = maxHeight / rows
        var menuAt by remember { mutableStateOf<DpOffset?>(null) }

        Box(
            Modifier
                .fillMaxSize()
                // the grid reports every cell's place on screen, filled or not
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
            // Empty cells: long-press for the home menu
            for (r in 0 until rows) for (c in 0 until cols) {
                if (items.none { it.covers(c, r) }) {
                    Box(
                        Modifier
                            .offset(cellW * c, cellH * r)
                            .size(cellW, cellH)
                            .pointerInput(c, r, rows) {
                                detectTapGestures(onLongPress = { p ->
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuAt = DpOffset(cellW * c + p.x.toDp(), cellH * r + p.y.toDp())
                                })
                            }
                    )
                }
            }

            // Where the dragged item would land (apps under an app show their own ring)
            val from = placement.dragging
            val to = placement.target
            if (from != null && to != null && to != from && to.area == Slot.HOME) {
                val size = if (from.area == Slot.HOME) {
                    items.firstOrNull { it.col == from.index % cols && it.row == from.index / cols }?.let { it.w to it.h }
                } else null
                val (w, h) = size ?: (1 to 1)
                val tc = to.index % cols
                val tr = to.index / cols
                val onAnApp = items.any { !it.isWidget && it.covers(tc, tr) }
                if (!(w == 1 && h == 1 && onAnApp)) {
                    Box(
                        Modifier.offset(cellW * tc, cellH * tr).size(cellW * w, cellH * h),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (w == 1 && h == 1) {
                            Box(Modifier.size(60.dp).border(2.dp, colors.gold, CircleShape))
                        } else {
                            Box(Modifier.fillMaxSize().padding(3.dp).border(2.dp, colors.gold, RoundedCornerShape(20.dp)))
                        }
                    }
                }
            }

            items.forEach { item ->
                key(item.id) {
                    val slot = Slot(Slot.HOME, item.row * cols + item.col)
                    Box(
                        Modifier
                            .offset(cellW * item.col, cellH * item.row)
                            .size(cellW * item.w, cellH * item.h)
                            .zIndex(if (placement.dragging == slot) 1f else 0f),
                        contentAlignment = if (item.isWidget) Alignment.TopStart else Alignment.Center,
                    ) {
                        if (item.id == HomeGrid.PORTAL) {
                            PortalWidget(slot, placement, portalTint, portalDescription, onTapPortal, onPortalMenu)
                        } else if (!item.isWidget) {
                            apps[item.id]?.let { entry ->
                                AppTile(
                                    entry, vm, colors, iconSize = 56.dp, showLabel = false,
                                    modifier = Modifier.width(64.dp),
                                    placement = placement, slot = slot, registerSlot = false,
                                )
                            }
                        } else {
                            GridWidget(
                                id = item.id,
                                slot = slot,
                                placement = placement,
                                colors = colors,
                                now = now,
                                clockStyle = clockStyle,
                                showCalendar = showCalendar,
                                onOpenClock = onOpenClock,
                                onOpenEvent = onOpenEvent,
                                onOpenCalendar = onOpenCalendar,
                                onPickStyle = vm::setSolClock,
                            )
                        }
                    }
                }
            }

            if (items.none { !it.isWidget } && placement.dragging == null) {
                Text(
                    "Long-press any app, then Add to home",
                    style = TextStyle(color = colors.inkSoft, fontSize = 13.sp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(colors.card.copy(alpha = 0.78f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }

            menuAt?.let { at ->
                Box(Modifier.offset(at.x, at.y)) {
                    DropdownMenu(expanded = true, onDismissRequest = { menuAt = null }) {
                        menuItems { menuAt = null }
                    }
                }
            }
        }
    }
}

/**
 * The Numio logo on Sol's grid: the bare mark, with only a 56dp area around it touchable (the
 * rest of its cell lets gestures through). Tap does what it's set to; hold and release opens
 * the customization menu (never also a tap); hold and move drags it like any widget.
 */
@Composable
private fun PortalWidget(
    slot: Slot,
    placement: Placement,
    tint: Long?,
    description: String,
    onTap: () -> Unit,
    onMenu: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .semantics {
                    contentDescription = description
                    onClick(label = description) { onTap(); true }
                    onLongClick(label = "Customize the Numio logo") { onMenu(); true }
                }
                .appCell(
                    placement, slot, HomeGrid.PORTAL,
                    onTap = onTap,
                    onHoldStart = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onMenu = onMenu,
                    register = false,
                ),
        ) {
            NumioLogo(tint, 44.dp)
        }
    }
}

/**
 * A widget on the grid. Hold and move it to drag it into free space; a tap goes to the
 * widget itself (open the clock or calendar). Holding the clock without moving opens its
 * style picker. While anything is dragged, widgets show a faint outline of the space they take.
 */
@Composable
private fun GridWidget(
    id: String,
    slot: Slot,
    placement: Placement,
    colors: NumioColors,
    now: Long,
    clockStyle: SolClock,
    showCalendar: Boolean,
    onOpenClock: () -> Unit,
    onOpenEvent: (Long, Long) -> Unit,
    onOpenCalendar: () -> Unit,
    onPickStyle: (SolClock) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var picker by remember { mutableStateOf(false) }
    val onTap: () -> Unit = when (id) {
        HomeGrid.CLOCK -> onOpenClock
        HomeGrid.MONTH -> onOpenCalendar
        else -> ({})
    }
    val label = when (id) {
        HomeGrid.CLOCK -> "Clock"
        HomeGrid.MONTH -> "Calendar"
        else -> "Next alarm"
    }
    Box(
        Modifier
            .fillMaxSize()
            .semantics {
                onLongClick(label = if (id == HomeGrid.CLOCK) "Change clock style" else "Move $label") {
                    if (id == HomeGrid.CLOCK) picker = true
                    true
                }
            }
            .appCell(
                placement, slot, id,
                onTap = onTap,
                onHoldStart = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                onMenu = { if (id == HomeGrid.CLOCK) picker = true },
                register = false,
            )
            .then(
                if (placement.dragging != null) {
                    Modifier.border(1.dp, colors.inkSoft.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                } else Modifier
            ),
        contentAlignment = when (id) {
            HomeGrid.TODAY -> Alignment.CenterStart
            else -> Alignment.TopStart
        },
    ) {
        when (id) {
            HomeGrid.CLOCK -> SolClockView(colors, now, clockStyle, picker, { picker = false }, onPickStyle)
            HomeGrid.TODAY -> SolToday(colors, now, showCalendar, onOpenClock, onOpenEvent)
            HomeGrid.MONTH -> MonthCard(now, showCalendar, onOpenCalendar, Modifier.fillMaxWidth().padding(end = 8.dp))
        }
    }
}

@Composable
private fun SolClockView(
    colors: NumioColors,
    now: Long,
    style: SolClock,
    picker: Boolean,
    onDismissPicker: () -> Unit,
    onPickStyle: (SolClock) -> Unit,
) {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.ENGLISH).format(Date(now))
    val date = SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(Date(now))
    val spoken = SimpleDateFormat(if (is24) "HH:mm, EEEE d MMMM" else "h:mm a, EEEE d MMMM", Locale.getDefault())
        .format(Date(now))

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = spoken },
    ) {
        // Hold the clock (without moving): pick a style, each one shown in its own numerals
        DropdownMenu(expanded = picker, onDismissRequest = onDismissPicker, offset = DpOffset(0.dp, 0.dp)) {
            SolClock.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (if (option == style) "✓  " else "     ") + option.label,
                                modifier = Modifier.width(130.dp),
                            )
                            Text(
                                "10:41",
                                style = TextStyle(fontFamily = ClockFonts.getValue(option), fontSize = 24.sp, color = colors.ink),
                            )
                        }
                    },
                    onClick = { onDismissPicker(); onPickStyle(option) },
                )
            }
        }
        // Sized in dp, not sp: the clock is already large, so it shouldn't grow with font scale.
        // Each font is a different width, so measure "00:00" and shrink until it fits.
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val family = ClockFonts.getValue(style)
        val clockSp = remember(style, maxWidth, density) {
            var size = 112.dp
            while (size > 48.dp) {
                val w = measurer.measure(
                    "00:00",
                    TextStyle(fontFamily = family, fontSize = with(density) { size.toSp() }, fontFeatureSettings = "tnum"),
                ).size.width
                if (w <= with(density) { (maxWidth - 8.dp).toPx() }) break
                size -= 4.dp
            }
            with(density) { size.toSp() }
        }
        Column {
            Text(
                date,
                style = TextStyle(color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 3.dp),
            )
            Text(
                time,
                style = TextStyle(
                    color = colors.ink,
                    fontFamily = family,
                    fontSize = clockSp,
                    lineHeight = clockSp * 1.08f,
                    letterSpacing = (-0.02).em,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
                modifier = Modifier.offset(x = (-4).dp),
            )
        }
    }
}

/** A small cream chip with the next alarm and/or event. Not shown at all when there's neither. */
@Composable
private fun SolToday(
    colors: NumioColors,
    now: Long,
    showCalendar: Boolean,
    onOpenClock: () -> Unit,
    onOpenEvent: (Long, Long) -> Unit,
) {
    val context = LocalContext.current
    val (alarm, event) = rememberToday(now, showCalendar)
    if (alarm == null && event == null) return

    val style = TextStyle(color = colors.inkSoft, fontSize = 14.sp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(colors.card.copy(alpha = 0.8f), CircleShape)
            .border(1.dp, colors.ink.copy(alpha = 0.08f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Box(Modifier.size(7.dp).background(colors.gold, CircleShape))
        Spacer(Modifier.width(10.dp))
        if (alarm != null) {
            val text = "alarm ${clockTime(context, alarm, now)}"
            Text(
                text, style = style, maxLines = 1,
                modifier = Modifier
                    .clickable(onClickLabel = "Open clock", role = Role.Button, onClick = onOpenClock)
                    .semantics { contentDescription = "Next $text" },
            )
        }
        if (event != null) {
            if (alarm != null) Text("  ·  ", style = style)
            Text(
                "${event.title} ${clockTime(context, event.begin, now)}",
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable(onClickLabel = "Open event", role = Role.Button) { onOpenEvent(event.id, event.begin) },
            )
        }
    }
}


/** One row of cells (home or dock). Empty cells take drops; the target gets a gold ring. */
@Composable
private fun CellRow(
    vm: LauncherViewModel,
    colors: NumioColors,
    cells: List<AppEntry?>,
    area: Int,
    first: Int,
    placement: Placement,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        cells.forEachIndexed { c, entry ->
            val slot = Slot(area, first + c)
            // the cell holding the lifted app is drawn above its neighbours, so it floats over them
            Box(
                Modifier.weight(1f).zIndex(if (placement.dragging == slot) 1f else 0f),
                contentAlignment = Alignment.Center,
            ) {
                if (entry != null) {
                    AppTile(
                        entry, vm, colors, iconSize = 56.dp, showLabel = false,
                        modifier = Modifier.width(64.dp),
                        placement = placement, slot = slot,
                    )
                } else {
                    Box(
                        Modifier.width(64.dp).height(68.dp).dropCell(placement, slot),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (placement.isTarget(slot)) {
                            Box(Modifier.size(56.dp).border(2.dp, colors.gold, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

/** Sol's dock: five cells in line with the home columns; apps can be dragged in and out. */
@Composable
private fun SolDock(
    vm: LauncherViewModel,
    colors: NumioColors,
    cells: List<AppEntry?>,
    placement: Placement,
    modifier: Modifier = Modifier,
) {
    CellRow(vm, colors, cells, Slot.DOCK, 0, placement, modifier)
}

/** A dark pill, the one strong contrast on the light screen. */
@Composable
private fun SolSearchPill(colors: NumioColors, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xF2282520), CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Search apps", onClick = onClick)
            .padding(horizontal = 22.dp),
    ) {
        Box(Modifier.size(9.dp).background(colors.gold, CircleShape))
        Spacer(Modifier.width(14.dp))
        Text("Search", style = TextStyle(color = Color(0xFFE1DACC), fontSize = 16.sp))
    }
}
