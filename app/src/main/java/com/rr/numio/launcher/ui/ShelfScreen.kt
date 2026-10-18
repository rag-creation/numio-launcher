package com.rr.numio.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rr.numio.launcher.LauncherViewModel
import com.rr.numio.launcher.ShelfMode
import com.rr.numio.launcher.data.AppEntry
import com.rr.numio.launcher.search.AppSearch
import com.rr.numio.launcher.sketch.SketchInk

/** One row of the Shelf grid: a heading (a letter or a profile name) or an app. */
private sealed interface ShelfItem {
    data class Header(val title: String, val isProfile: Boolean = false) : ShelfItem
    data class App(val entry: AppEntry, val subtitle: String?) : ShelfItem
}

/** First letter for the A–Z headings; everything else (digits, other scripts) goes under "#". */
private fun sectionOf(label: String): String {
    val c = AppSearch.normalize(label).firstOrNull()?.uppercaseChar() ?: return "#"
    return if (c in 'A'..'Z') c.toString() else "#"
}

/**
 * The Shelf: every app A–Z with hand-drawn letter headings, and the search bar at the
 * bottom near your thumb. Opened from the search pill it starts in typing mode (this is
 * the start of Pulse): Enter opens the top result.
 */
@Composable
fun ShelfScreen(vm: LauncherViewModel, mode: ShelfMode, modifier: Modifier = Modifier) {
    val colors = LocalPaper.current
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val searching = query.isNotBlank()

    BackHandler { vm.closeShelf() }

    LaunchedEffect(mode) {
        if (mode == ShelfMode.Search) {
            focus.requestFocus()
            keyboard?.show()
        }
    }
    // New search → back to the top so the best match is visible
    LaunchedEffect(query) { gridState.scrollToItem(0) }

    val shelfItems: List<ShelfItem> = remember(results, searching) {
        if (searching) {
            // One ranked list; second-profile apps say which profile they're from
            results.map { ShelfItem.App(it, listOfNotNull(it.clashLabel, it.profileLabel).joinToString(" · ").ifEmpty { null }) }
        } else {
            buildList {
                // Main profile A–Z with letter headings…
                results.filter { it.isMainUser }.groupBy { sectionOf(it.label) }.forEach { (letter, list) ->
                    add(ShelfItem.Header(letter))
                    list.forEach { add(ShelfItem.App(it, it.clashLabel)) }
                }
                // …then each second profile (Work, Secure Folder…) as its own section at the end
                results.filter { !it.isMainUser }.groupBy { it.profileLabel ?: "Second profile" }.forEach { (name, list) ->
                    add(ShelfItem.Header(name, isProfile = true))
                    list.forEach { add(ShelfItem.App(it, it.clashLabel)) }
                }
            }
        }
    }

    // Pull down at the top of the list to close; scrolling hides the keyboard
    val closeDistance = with(LocalDensity.current) { 96.dp.toPx() }
    val close by rememberUpdatedState { vm.closeShelf() }
    val pullToClose = remember(keyboard) {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) keyboard?.hide()
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > closeDistance) {
                        pulled = 0f
                        close()
                    }
                } else if (consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    ) {
        // Grab handle: drag down here to close
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(Unit) {
                    var dragged = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = { if (dragged > closeDistance / 2) close() },
                        onVerticalDrag = { _, dy -> dragged += dy },
                    )
                },
        ) {
            if (colors.sketch) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(6.dp)
                        .sketchBorder(colors.inkSoft, 3.dp, seed = 21, width = 1.4.dp)
                )
            } else {
                Box(Modifier.width(36.dp).height(4.dp).background(colors.inkSoft.copy(alpha = 0.45f), CircleShape))
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                apps == null -> Unit // first load takes a moment; nothing to show yet
                searching && results.isEmpty() -> Text(
                    "No app called “${query.trim()}”",
                    style = TextStyle(color = colors.inkSoft, fontSize = 15.sp),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 78.dp),
                    state = gridState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize().nestedScroll(pullToClose),
                ) {
                    items(
                        shelfItems,
                        key = { if (it is ShelfItem.App) it.entry.key else "h:" + (it as ShelfItem.Header).title },
                        span = { if (it is ShelfItem.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
                        contentType = { if (it is ShelfItem.Header) 0 else 1 },
                    ) { item ->
                        when (item) {
                            is ShelfItem.Header -> SectionHeader(item, colors)
                            is ShelfItem.App -> AppTile(
                                item.entry, vm, colors,
                                iconSize = 52.dp,
                                highlighted = searching && item.entry.key == results.firstOrNull()?.key,
                                subtitle = item.subtitle,
                            )
                        }
                    }
                }
            }
        }

        SearchBar(
            query = query,
            colors = colors,
            focus = focus,
            onQuery = vm::setQuery,
            onGo = { results.firstOrNull()?.let { vm.launch(it, null, null) } },
        )
    }
}

@Composable
private fun SectionHeader(header: ShelfItem.Header, colors: NumioColors) {
    val title = header.title
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = if (header.isProfile) 28.dp else 14.dp, bottom = 2.dp)
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = when {
                    header.isProfile -> "$title apps"
                    title == "#" -> "Other"
                    else -> title
                }
            },
    ) {
        if (header.isProfile) {
            // A short ink rule, then the profile name with a gold badge like the icons'
            Box(
                Modifier
                    .padding(end = 10.dp, bottom = 14.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(colors.inkSoft.copy(alpha = 0.35f))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(colors.gold, CircleShape))
                Spacer(Modifier.width(10.dp))
                val upper = title.uppercase()
                if (colors.sketch && upper.all { it == ' ' || SketchInk.hasGlyph(it) }) {
                    SketchText(upper, 18.dp, colors.gold)
                } else {
                    Text(title, style = TextStyle(color = colors.gold, fontSize = 18.sp))
                }
            }
        } else if (title == "#" || !colors.sketch) {
            // Sol: clean type in ink, the letter a touch bolder
            Text(
                title,
                style = TextStyle(
                    color = if (colors.sketch) colors.gold else colors.inkSoft,
                    fontSize = if (colors.sketch) 20.sp else 15.sp,
                    fontWeight = if (colors.sketch) null else FontWeight.SemiBold,
                ),
            )
        } else {
            SketchText(title, 20.dp, colors.gold)
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    colors: NumioColors,
    focus: FocusRequester,
    onQuery: (String) -> Unit,
    onGo: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(46.dp)
            .then(
                if (colors.sketch) {
                    Modifier.sketchBorder(colors.inkSoft.copy(alpha = 0.7f), 23.dp, seed = 3, fill = colors.card, width = 1.2.dp, wobble = 0.5f)
                } else {
                    Modifier
                        .background(colors.card, CircleShape)
                        .border(1.dp, colors.ink.copy(alpha = 0.10f), CircleShape)
                }
            )
            .padding(start = 18.dp, end = 8.dp),
    ) {
        Box(Modifier.size(8.dp).background(colors.gold, CircleShape))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("Search apps", style = TextStyle(color = colors.inkSoft, fontSize = 15.sp))
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = colors.ink, fontSize = 15.sp),
                cursorBrush = SolidColor(colors.gold),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .semantics { contentDescription = "Search apps" },
            )
        }
        if (query.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(role = Role.Button, onClickLabel = "Clear search") { onQuery("") },
            ) {
                Text("×", style = TextStyle(color = colors.inkSoft, fontSize = 22.sp))
            }
        }
    }
}
