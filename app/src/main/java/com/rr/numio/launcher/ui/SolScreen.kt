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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
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
    val homeCells by vm.homeCells.collectAsStateWithLifecycle()
    val dockCells by vm.dockCells.collectAsStateWithLifecycle()
    // One drag-and-drop for home and dock, so apps can move between them
    val placement = rememberPlacement(vm::drop)
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
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 24.dp),
        ) {
            // On short screens the month card steps aside so the apps never get squeezed
            val roomForCard = maxHeight >= 700.dp
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(28.dp))
                SolClockView(colors, now, clockStyle, onOpenClock, vm::setSolClock)
                SolToday(colors, now, showCalendar, onOpenClock, onOpenEvent)

                if (!isDefault) {
                    Spacer(Modifier.height(16.dp))
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
                } else if (roomForCard) {
                    Spacer(Modifier.height(18.dp))
                    MonthCard(now, showCalendar, onOpenCalendar, Modifier.fillMaxWidth(0.6f))
                }

                SolMenuArea(
                    colors = colors,
                    isDefault = isDefault,
                    showCalendar = showCalendar,
                    wallpaper = wallpaper,
                    onWallpaper = vm::setSolWallpaper,
                    onToggleCalendar = onToggleCalendar,
                    onSwitchTheme = onSwitchTheme,
                    onRequestHome = onRequestHome,
                    onLauncherInfo = onOpenLauncherInfo,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )

                // whichever group holds the lifted app is drawn on top, so it passes over the other
                HomeGrid(
                    vm, colors, homeCells, placement,
                    Modifier.zIndex(if (placement.dragging?.area == Slot.HOME) 1f else 0f),
                )
                Spacer(Modifier.height(14.dp))
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
}

@Composable
private fun SolClockView(
    colors: NumioColors,
    now: Long,
    style: SolClock,
    onOpenClock: () -> Unit,
    onPickStyle: (SolClock) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var picker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.ENGLISH).format(Date(now))
    val date = SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(Date(now))
    val spoken = SimpleDateFormat(if (is24) "HH:mm, EEEE d MMMM" else "h:mm a, EEEE d MMMM", Locale.getDefault())
        .format(Date(now))

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = "Open clock",
                onLongClickLabel = "Change clock style",
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    picker = true
                },
                onClick = onOpenClock,
            ),
    ) {
        // Long-press the clock: pick a style, each one shown in its own numerals
        DropdownMenu(expanded = picker, onDismissRequest = { picker = false }, offset = DpOffset(0.dp, 0.dp)) {
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
                    onClick = { picker = false; onPickStyle(option) },
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
            .padding(top = 14.dp)
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

@Composable
private fun SolMenuArea(
    colors: NumioColors,
    isDefault: Boolean,
    showCalendar: Boolean,
    wallpaper: SolWallpaper,
    onWallpaper: (SolWallpaper) -> Unit,
    onToggleCalendar: () -> Unit,
    onSwitchTheme: () -> Unit,
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
        Box(Modifier.offset(x = with(density) { (at?.x ?: 0f).toDp() }, y = with(density) { (at?.y ?: 0f).toDp() })) {
            DropdownMenu(expanded = at != null, onDismissRequest = { menuAt = null }, offset = DpOffset.Zero) {
                DropdownMenuItem(
                    text = { Text(if (wallpaper == SolWallpaper.Garden) "✓  Garden wallpaper" else "Garden wallpaper") },
                    onClick = { menuAt = null; onWallpaper(SolWallpaper.Garden) },
                )
                DropdownMenuItem(
                    text = { Text(if (wallpaper == SolWallpaper.Petal) "✓  Petal wallpaper" else "Petal wallpaper") },
                    onClick = { menuAt = null; onWallpaper(SolWallpaper.Petal) },
                )
                DropdownMenuItem(
                    text = { Text(if (showCalendar) "✓  Show next calendar event" else "Show next calendar event") },
                    onClick = { menuAt = null; onToggleCalendar() },
                )
                DropdownMenuItem(text = { Text("Switch to Ink theme") }, onClick = { menuAt = null; onSwitchTheme() })
                if (!isDefault) {
                    DropdownMenuItem(text = { Text("Set as home screen") }, onClick = { menuAt = null; onRequestHome() })
                }
                DropdownMenuItem(text = { Text("Launcher app info") }, onClick = { menuAt = null; onLauncherInfo() })
            }
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
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
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

/** Sol's home: two rows of five cells. Add apps with "Add to home", then drag them anywhere. */
@Composable
private fun HomeGrid(
    vm: LauncherViewModel,
    colors: NumioColors,
    cells: List<AppEntry?>,
    placement: Placement,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cells.chunked(5).forEachIndexed { r, row ->
                // the row holding the lifted app is drawn on top, so it passes over the other row
                val liftedHere = placement.dragging?.let { it.area == Slot.HOME && it.index / 5 == r } == true
                CellRow(vm, colors, row, Slot.HOME, r * 5, placement, Modifier.zIndex(if (liftedHere) 1f else 0f))
            }
        }
        if (cells.all { it == null } && placement.dragging == null) {
            Text(
                "Long-press any app, then Add to home",
                style = TextStyle(color = colors.inkSoft, fontSize = 13.sp),
                modifier = Modifier
                    .background(colors.card.copy(alpha = 0.78f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
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
