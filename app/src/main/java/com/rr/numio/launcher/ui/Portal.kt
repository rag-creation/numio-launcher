package com.rr.numio.launcher.ui

import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rr.numio.launcher.LauncherViewModel
import com.rr.numio.launcher.data.AppEntry
import com.rr.numio.launcher.data.HomeTheme
import com.rr.numio.launcher.data.PortalAction
import com.rr.numio.launcher.data.PortalColor
import com.rr.numio.launcher.data.PortalRules
import com.rr.numio.launcher.data.PortalSettings
import com.rr.numio.launcher.data.PortalStyle
import com.rr.numio.launcher.search.AppSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Customization menu ──────────────────────────────────────────────────────

/** Long-press the logo (or "Numio logo…" in the home menu): colour, what a tap opens, show/hide. */
@Composable
fun PortalSettingsDialog(vm: LauncherViewModel, theme: HomeTheme) {
    val settings by vm.portal.collectAsStateWithLifecycle()
    val hasWork by vm.hasWorkProfile.collectAsStateWithLifecycle()
    val chosen by vm.chosenApps.collectAsStateWithLifecycle()
    Dialog(onDismissRequest = vm::closePortalSettings) {
        PortalSettingsContent(
            initial = settings,
            hasWork = hasWork,
            theme = theme,
            chosenCount = chosen.first.size,
            onChooseApps = vm::openPicker,
            onCancel = vm::closePortalSettings,
            onApply = { new ->
                // the chosen apps are edited live in the picker; keep the latest list
                vm.setPortal(new.copy(apps = vm.portal.value.apps))
                vm.closePortalSettings()
            },
        )
    }
}

/**
 * The menu itself. Choices are a draft shown in the preview; nothing is saved until Apply.
 * Separate from the dialog so it can be tested.
 */
@Composable
fun PortalSettingsContent(
    initial: PortalSettings,
    hasWork: Boolean,
    theme: HomeTheme,
    chosenCount: Int,
    onChooseApps: () -> Unit,
    onCancel: () -> Unit,
    onApply: (PortalSettings) -> Unit,
) {
    val colors = LocalPaper.current
    var draft by remember { mutableStateOf(initial) }
    val shownAction = PortalRules.action(draft, hasWork)
    val previewBg = colors.background
    val title = TextStyle(color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(24.dp))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            "Numio logo",
            style = TextStyle(color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(14.dp))

        // Preview, on the theme's own background
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(previewBg, RoundedCornerShape(18.dp))
                .semantics { contentDescription = "Preview, ${draft.style.label}, ${draft.color.label}" },
        ) {
            PortalVisual(draft.style, PortalRules.color(draft.color, theme), Modifier.alpha(if (draft.visible) 1f else 0.35f), size = 60.dp)
        }
        if (PortalRules.lowContrast(draft.color, draft.style)) {
            Text(
                "Hard to see on this button",
                style = TextStyle(color = colors.inkSoft, fontSize = 12.sp),
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Button", style = title)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            PortalStyle.entries.forEach { st ->
                val selected = draft.style == st
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) colors.ink.copy(alpha = 0.08f) else Color.Transparent, RoundedCornerShape(14.dp))
                        .border(if (selected) 2.dp else 0.dp, if (selected) colors.ink else Color.Transparent, RoundedCornerShape(14.dp))
                        .semantics {
                            contentDescription = "${st.label} button"
                            stateDescription = if (selected) "Selected" else "Not selected"
                        }
                        .selectable(selected = selected, role = Role.RadioButton) { draft = draft.copy(style = st) }
                        .padding(vertical = 8.dp),
                ) {
                    PortalVisual(st, PortalRules.color(draft.color, theme), size = 40.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(st.label, style = TextStyle(color = colors.ink, fontSize = 11.sp), maxLines = 1)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Colour", style = title)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PortalColor.entries.forEach { c ->
                val swatch = Color(PortalRules.color(c, theme) ?: PortalRules.GOLD)
                val selected = draft.color == c
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = c.label
                            stateDescription = if (selected) "Selected" else "Not selected"
                        }
                        .selectable(selected = selected, role = Role.RadioButton) { draft = draft.copy(color = c) },
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .background(swatch, CircleShape)
                            .border(if (selected) 3.dp else 1.dp, if (selected) colors.ink else colors.inkSoft.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (c == PortalColor.Auto) {
                            Text("A", style = TextStyle(color = Color(0xFF2B2722), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
        Text(draft.color.label, style = TextStyle(color = colors.inkSoft, fontSize = 12.sp), modifier = Modifier.padding(start = 8.dp))

        Spacer(Modifier.height(16.dp))
        Text("Tap opens", style = title)
        ActionRow(
            label = "Workspace",
            detail = if (hasWork) "Your work-profile apps" else "Not available: no work profile on this phone",
            selected = shownAction == PortalAction.Workspace,
            enabled = hasWork,
            colors = colors,
        ) { draft = draft.copy(action = PortalAction.Workspace) }
        ActionRow(
            label = "Selected apps",
            detail = if (chosenCount == 1) "1 app chosen" else "$chosenCount apps chosen",
            selected = shownAction == PortalAction.Selected,
            enabled = true,
            colors = colors,
        ) { draft = draft.copy(action = PortalAction.Selected) }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .padding(start = 48.dp)
                .heightIn(min = 44.dp)
                .clickable(role = Role.Button, onClick = onChooseApps)
                .padding(horizontal = 8.dp),
        ) {
            Text("Choose apps…", style = TextStyle(color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium))
        }
        ActionRow(
            label = "Nothing",
            detail = "The logo stays; tapping it does nothing",
            selected = shownAction == PortalAction.Off,
            enabled = true,
            colors = colors,
        ) { draft = draft.copy(action = PortalAction.Off) }

        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .toggleable(value = draft.visible, role = Role.Switch) { draft = draft.copy(visible = it) },
        ) {
            Column(Modifier.weight(1f)) {
                Text("Show on home screen", style = TextStyle(color = colors.ink, fontSize = 15.sp))
                Text(
                    "Hiding keeps its place, colour and apps. Turn it back on from the home menu",
                    style = TextStyle(color = colors.inkSoft, fontSize = 12.sp),
                )
            }
            Switch(
                checked = draft.visible,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.gold, checkedThumbColor = Color.White),
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            DialogButton("Cancel", colors, filled = false, onClick = onCancel)
            Spacer(Modifier.width(8.dp))
            DialogButton("Apply", colors, filled = true) { onApply(draft) }
        }
    }
}

@Composable
private fun ActionRow(
    label: String,
    detail: String,
    selected: Boolean,
    enabled: Boolean,
    colors: NumioColors,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .alpha(if (enabled) 1f else 0.45f),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(selectedColor = colors.ink),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = TextStyle(color = colors.ink, fontSize = 15.sp))
            Text(detail, style = TextStyle(color = colors.inkSoft, fontSize = 12.sp))
        }
    }
}

@Composable
private fun DialogButton(text: String, colors: NumioColors, filled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .then(
                if (filled) Modifier.background(colors.ink, CircleShape)
                else Modifier.border(1.dp, colors.inkSoft.copy(alpha = 0.5f), CircleShape)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        Text(text, style = TextStyle(color = if (filled) colors.card else colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium))
    }
}

// ── Selected apps screen ────────────────────────────────────────────────────

/** The apps the logo opens when set to Selected apps (personal apps only). */
@Composable
fun SelectedAppsScreen(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val colors = LocalPaper.current
    val chosen by vm.chosenApps.collectAsStateWithLifecycle()
    val (apps, missing) = chosen
    BackHandler(onBack = vm::closeSelected)
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
        ) {
            NumioLogo(null, 30.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                "Your apps",
                style = TextStyle(color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = vm::openPicker).padding(horizontal = 12.dp),
            ) { Text("Edit", style = TextStyle(color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Back to home" }
                    .clickable(role = Role.Button, onClick = vm::closeSelected),
            ) { Text("✕", style = TextStyle(color = colors.inkSoft, fontSize = 20.sp)) }
        }
        if (missing > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .background(colors.card, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    if (missing == 1) "1 app you chose isn't installed any more" else "$missing apps you chose aren't installed any more",
                    style = TextStyle(color = colors.inkSoft, fontSize = 13.sp),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = vm::forgetMissingChosen).padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Remove", style = TextStyle(color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)) }
            }
        }
        if (apps.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No apps chosen yet", style = TextStyle(color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(6.dp))
                Text("Pick the apps this logo should open.", style = TextStyle(color = colors.inkSoft, fontSize = 14.sp))
                Spacer(Modifier.height(14.dp))
                DialogButton("Choose apps", colors, filled = true, onClick = vm::openPicker)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 78.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(apps, key = { it.key }) { entry -> AppTile(entry, vm, colors, iconSize = 52.dp, subtitle = entry.clashLabel) }
            }
        }
    }
}

// ── App picker ──────────────────────────────────────────────────────────────

private val realIconCache = object : LruCache<String, ImageBitmap>(16 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
}

/** The app's own launcher icon, as it really looks, so it's easy to recognise in the picker. */
@Composable
private fun RealIcon(entry: AppEntry, sizeDp: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { sizeDp.roundToPx() }
    val cacheKey = "${entry.key}@$px"
    val bitmap by produceState(realIconCache.get(cacheKey), cacheKey) {
        if (value == null) {
            value = withContext(Dispatchers.Default) {
                try {
                    entry.info.getIcon(context.resources.displayMetrics.densityDpi).toBitmap(px, px).asImageBitmap()
                        .also { realIconCache.put(cacheKey, it) }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    Box(Modifier.size(sizeDp)) { bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) } }
}

/** Choose which personal apps the logo opens. Saved as each app's launcher key, not its name. */
@Composable
fun AppPickerDialog(vm: LauncherViewModel) {
    val colors = LocalPaper.current
    val apps by vm.personalApps.collectAsStateWithLifecycle()
    val settings by vm.portal.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query) { AppSearch.rank(query, apps) { it.label } }
    Dialog(
        onDismissRequest = vm::closePicker,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Choose apps",
                        style = TextStyle(color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.semantics { heading() },
                    )
                    Text("${settings.apps.size} chosen · personal apps only", style = TextStyle(color = colors.inkSoft, fontSize = 13.sp))
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = vm::closePicker).padding(horizontal = 14.dp),
                ) { Text("Done", style = TextStyle(color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)) }
            }
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(colors.card, CircleShape)
                    .padding(horizontal = 16.dp),
            ) {
                if (query.isEmpty()) Text("Search", style = TextStyle(color = colors.inkSoft, fontSize = 15.sp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.ink, fontSize = 15.sp),
                    cursorBrush = SolidColor(colors.gold),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search apps" },
                )
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(shown, key = { it.key }) { entry ->
                    val picked = entry.key in settings.apps
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .toggleable(value = picked, role = Role.Checkbox) { vm.toggleChosen(entry) }
                            .padding(horizontal = 20.dp),
                    ) {
                        RealIcon(entry, 40.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.label, style = TextStyle(color = colors.ink, fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            entry.clashLabel?.let {
                                Text(it, style = TextStyle(color = colors.inkSoft, fontSize = 12.sp), maxLines = 1)
                            }
                        }
                        Checkbox(
                            checked = picked,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = colors.ink, checkmarkColor = colors.card),
                        )
                    }
                }
            }
        }
    }
}
