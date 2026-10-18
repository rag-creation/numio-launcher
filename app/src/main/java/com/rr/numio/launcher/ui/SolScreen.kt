package com.rr.numio.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rr.numio.launcher.LauncherViewModel
import com.rr.numio.launcher.R
import com.rr.numio.launcher.ShelfMode
import com.rr.numio.launcher.data.AppEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Light display numerals for Sol's clock (Inter Display Light, digits and colon only). */
private val SolClockFont = FontFamily(Font(R.font.inter_display_light, FontWeight.Light))

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
    modifier: Modifier = Modifier,
) {
    val colors = SolColors
    val dock by vm.dock.collectAsStateWithLifecycle()
    val isDefault by vm.isDefaultHome.collectAsStateWithLifecycle()
    val showCalendar by vm.showCalendar.collectAsStateWithLifecycle()
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
            bitmap = ImageBitmap.imageResource(R.drawable.sol_wallpaper),
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
                .padding(horizontal = 26.dp),
        ) {
            Spacer(Modifier.height(36.dp))
            SolClock(colors, now, onOpenClock)
            SolToday(colors, now, showCalendar, onOpenClock, onOpenEvent)

            if (!isDefault) {
                Spacer(Modifier.height(18.dp))
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
            }

            SolMenuArea(
                colors = colors,
                isDefault = isDefault,
                showCalendar = showCalendar,
                onToggleCalendar = onToggleCalendar,
                onSwitchTheme = onSwitchTheme,
                onRequestHome = onRequestHome,
                onLauncherInfo = onOpenLauncherInfo,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            SolDock(vm, colors, dock)
            Spacer(Modifier.height(18.dp))
            SolSearchPill(colors) { vm.openShelf(ShelfMode.Search) }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SolClock(colors: NumioColors, now: Long, onOpenClock: () -> Unit) {
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = "Open clock",
                onClick = onOpenClock,
            ),
    ) {
        // Sized in dp, not sp: the clock is already large, so it shouldn't grow with font scale
        val density = LocalDensity.current
        val clockSize = minOf(118.dp, maxWidth * 0.52f)
        val clockSp = with(density) { clockSize.toSp() }
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
                    fontFamily = SolClockFont,
                    fontWeight = FontWeight.Light,
                    fontSize = clockSp,
                    lineHeight = clockSp,
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

/** The same dock as Ink, shown as the apps' real icons on soft cream tiles. */
@Composable
private fun SolDock(vm: LauncherViewModel, colors: NumioColors, dock: List<AppEntry>) {
    if (dock.isEmpty()) {
        Text(
            "Long-press an app in the Shelf to add it here",
            style = TextStyle(color = colors.inkSoft, fontSize = 13.sp),
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                .padding(14.dp),
        )
        return
    }
    val tile = RoundedCornerShape(20.dp)
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        dock.forEach { entry ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .shadow(3.dp, tile, clip = false)
                    .background(colors.card, tile),
            ) {
                AppTile(entry, vm, colors, iconSize = 44.dp, showLabel = false, modifier = Modifier.width(56.dp))
            }
        }
    }
}

@Composable
private fun SolSearchPill(colors: NumioColors, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(colors.card.copy(alpha = 0.86f), CircleShape)
            .border(1.dp, colors.ink.copy(alpha = 0.08f), CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Search apps", onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        Box(Modifier.size(8.dp).background(colors.gold, CircleShape))
        Spacer(Modifier.width(12.dp))
        Text("Search", style = TextStyle(color = colors.inkSoft, fontSize = 15.sp))
    }
}
