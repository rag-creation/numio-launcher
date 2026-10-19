package com.rr.numio.launcher

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rr.numio.launcher.data.AppEntry
import com.rr.numio.launcher.data.AppRepository
import com.rr.numio.launcher.data.Background
import com.rr.numio.launcher.data.Cells
import com.rr.numio.launcher.data.DefaultDock
import com.rr.numio.launcher.data.GridItem
import com.rr.numio.launcher.data.HomeGrid
import com.rr.numio.launcher.data.Profiles
import com.rr.numio.launcher.data.WorkState
import com.rr.numio.launcher.data.Slot
import com.rr.numio.launcher.data.HomePrefs
import com.rr.numio.launcher.data.HomeTheme
import com.rr.numio.launcher.data.SolClock
import com.rr.numio.launcher.data.SolWallpaper
import com.rr.numio.launcher.data.TodaySource
import com.rr.numio.launcher.search.AppSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How the Shelf was opened: browsing (swipe up) or typing (search pill). */
enum class ShelfMode { Closed, Browse, Search }

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app, viewModelScope)
    private val prefs = HomePrefs(app)

    val apps: StateFlow<List<AppEntry>?> = repo.apps

    private val _shelf = MutableStateFlow(ShelfMode.Closed)
    val shelf: StateFlow<ShelfMode> = _shelf.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isDefaultHome = MutableStateFlow(true)
    val isDefaultHome: StateFlow<Boolean> = _isDefaultHome.asStateFlow()

    private val showCalendarPref: StateFlow<Boolean> =
        prefs.showCalendar.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _calendarAccess = MutableStateFlow(TodaySource.hasCalendarAccess(app))

    /** The next-event part of the Today line is on only when chosen AND allowed. */
    val showCalendar: StateFlow<Boolean> =
        combine(showCalendarPref, _calendarAccess) { on, access -> on && access }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** null for the first few milliseconds, until the saved choice is read (so Ink never flashes before Sol). */
    val theme: StateFlow<HomeTheme?> =
        prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Switching only changes the look; the dock, apps and other settings stay as they are. */
    fun setTheme(theme: HomeTheme) {
        viewModelScope.launch { prefs.setTheme(theme) }
    }

    val solWallpaper: StateFlow<SolWallpaper> =
        prefs.solWallpaper.stateIn(viewModelScope, SharingStarted.Eagerly, SolWallpaper.Garden)

    fun setSolWallpaper(w: SolWallpaper) {
        viewModelScope.launch { prefs.setSolWallpaper(w) }
    }

    val solClock: StateFlow<SolClock> =
        prefs.solClock.stateIn(viewModelScope, SharingStarted.Eagerly, SolClock.Fraunces)

    fun setSolClock(c: SolClock) {
        viewModelScope.launch { prefs.setSolClock(c) }
    }

    val background: StateFlow<Background> =
        prefs.background.stateIn(viewModelScope, SharingStarted.Eagerly, Background.Paper)

    private fun cellsOf(keys: kotlinx.coroutines.flow.Flow<List<String?>>, size: Int): StateFlow<List<AppEntry?>> =
        combine(repo.apps.filterNotNull(), keys) { apps, k ->
            val byKey = apps.associateBy { it.key }
            // an uninstalled app's cell simply shows as empty
            Cells.pad(k, size).map { key -> key?.let(byKey::get) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, List(size) { null })

    /** Sol's dock: [HomePrefs.DOCK_MAX] fixed cells, some may be empty. */
    val dockCells: StateFlow<List<AppEntry?>> = cellsOf(prefs.dock.map { it.orEmpty() }, HomePrefs.DOCK_MAX)

    /** Ink's dock: the same apps with the gaps closed up. */
    val dock: StateFlow<List<AppEntry>> =
        dockCells.map { it.filterNotNull() }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Rows the home grid has on this screen; set by the screen once it's measured. */
    private val gridRows = MutableStateFlow(8)

    fun setGridRows(rows: Int) {
        gridRows.value = rows
    }

    /** Every installed app by key, for drawing the grid. */
    val appsByKey: StateFlow<Map<String, AppEntry>> =
        repo.apps.filterNotNull().map { list -> list.associateBy { it.key } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // ── Workspace (work-profile apps, behind the Numio portal) ─────────────────

    /** The work profile as Android reports it; null when there's none. */
    val workProfile: StateFlow<AppRepository.WorkProfile?> = repo.work

    /** Show the portal only when the phone has a work profile. */
    val hasWorkProfile: StateFlow<Boolean> =
        repo.work.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Only the work-profile apps, A–Z. */
    val workApps: StateFlow<List<AppEntry>> =
        repo.apps.filterNotNull().map { Profiles.workspace(it) { a -> a.isWork } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val workState: StateFlow<WorkState> =
        combine(repo.work, workApps) { w, apps ->
            Profiles.workState(
                hasProfile = w != null,
                listed = w?.listed ?: false,
                paused = w?.paused ?: false,
                unlocked = w?.unlocked ?: true,
                apps = apps.size,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, WorkState.NoProfile)

    private val _workspaceOpen = MutableStateFlow(false)
    val workspaceOpen: StateFlow<Boolean> = _workspaceOpen.asStateFlow()

    fun openWorkspace() {
        closeShelf()
        _workspaceOpen.value = true
    }

    fun closeWorkspace() {
        _workspaceOpen.value = false
    }

    /** "Turn on work apps": a request to Android, which may ask to unlock. Never done silently. */
    fun turnOnWorkApps() {
        if (!repo.requestWorkOn()) {
            Toast.makeText(
                getApplication(),
                "Android didn't allow it from here. Use the work apps switch in Settings or quick settings",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    /** Sol's home grid, always valid for the current screen (see [HomeGrid.normalize]). */
    val gridItems: StateFlow<List<GridItem>> =
        combine(appsByKey, prefs.solGrid, prefs.homeApps, gridRows, hasWorkProfile) { apps, saved, legacy, rows, work ->
            val items = if (saved != null) HomeGrid.parse(saved)
            else HomeGrid.fromLegacy(Cells.pad(legacy, HomePrefs.HOME_MAX), rows)
            HomeGrid.normalize(items, rows, apps.keys, portal = work)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun keys(cells: List<AppEntry?>) = cells.map { it?.key }

    private fun saveGrid(items: List<GridItem>) {
        viewModelScope.launch { prefs.setSolGrid(HomeGrid.serialize(items)) }
    }

    fun isOnHome(entry: AppEntry) = gridItems.value.any { it.id == entry.key }

    fun addToHome(entry: AppEntry) {
        val items = gridItems.value
        if (items.any { it.id == entry.key }) return
        val spot = HomeGrid.firstFree(items, gridRows.value)
        if (spot == null) {
            Toast.makeText(getApplication(), "Home is full. Remove an app first", Toast.LENGTH_SHORT).show()
            return
        }
        saveGrid(items + GridItem(entry.key, spot.first, spot.second, 1, 1))
    }

    fun removeFromHome(entry: AppEntry) {
        saveGrid(gridItems.value.filter { it.id != entry.key })
    }

    /** Can [from] be dropped on [to]? Used while dragging, to show only real drop spots. */
    fun canDrop(from: Slot, to: Slot): Boolean =
        HomeGrid.move(gridItems.value, keys(dockCells.value), gridRows.value, from, to) != null

    /** Drag on Sol's home or dock. Widgets go into free space; apps move or swap, grid ↔ dock too. */
    fun drop(from: Slot, to: Slot) {
        val (grid, dock) = HomeGrid.move(gridItems.value, keys(dockCells.value), gridRows.value, from, to) ?: return
        viewModelScope.launch { prefs.setGridAndDock(HomeGrid.serialize(grid), dock) }
    }

    /** Widgets back to the top, apps onto the bottom rows in reading order. */
    fun resetHomeLayout() {
        val apps = gridItems.value.filter { !it.isWidget }.sortedWith(compareBy({ it.row }, { it.col })).map { it.id }
        val rows = gridRows.value
        val first = HomeGrid.fromLegacy(apps.take(HomePrefs.HOME_MAX), rows)
        val rest = apps.drop(HomePrefs.HOME_MAX).map { GridItem(it, -1, -1, 1, 1) }
        saveGrid(HomeGrid.normalize(first + rest, rows, appsByKey.value.keys, portal = hasWorkProfile.value))
    }

    /** Search results; with no query, every app A–Z. */
    val results: StateFlow<List<AppEntry>> =
        // The normal drawer and its search never include work-profile apps (they're in the Workspace)
        combine(repo.apps.filterNotNull(), _query) { apps, q ->
            AppSearch.rank(q, Profiles.drawer(apps) { it.isWork }) { it.label }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        repo.start()
        // First run: fill the dock with the phone's own dialer, messages, browser and camera
        viewModelScope.launch {
            val all = repo.apps.filterNotNull().first()
            if (prefs.dock.first() == null) {
                prefs.setDock(DefaultDock.pick(getApplication(), all).map { it.key })
            }
        }
    }

    override fun onCleared() {
        repo.stop()
    }

    fun openShelf(mode: ShelfMode) {
        _shelf.value = mode
    }

    fun closeShelf() {
        _shelf.value = ShelfMode.Closed
        _query.value = ""
    }

    /** Home button: always back to a clean Canvas. */
    fun goHome() {
        closeShelf()
        closeWorkspace()
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun refreshDefaultHome() {
        _isDefaultHome.value = HomeRole.isDefault(getApplication())
        // Calendar access can be taken away in Settings at any time
        _calendarAccess.value = TodaySource.hasCalendarAccess(getApplication())
    }

    fun hasCalendarAccess() = TodaySource.hasCalendarAccess(getApplication())

    fun setShowCalendar(on: Boolean) {
        _calendarAccess.value = TodaySource.hasCalendarAccess(getApplication())
        viewModelScope.launch { prefs.setShowCalendar(on) }
    }

    fun setBackground(bg: Background) {
        viewModelScope.launch { prefs.setBackground(bg) }
    }

    fun isInDock(entry: AppEntry) = dock.value.any { it.key == entry.key }

    fun addToDock(entry: AppEntry) {
        val current = keys(dockCells.value)
        if (entry.key in current) return
        val i = Cells.firstEmpty(current)
        if (i < 0) {
            Toast.makeText(getApplication(), "The dock holds ${HomePrefs.DOCK_MAX} apps", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch { prefs.setDock(current.toMutableList().also { it[i] = entry.key }) }
    }

    fun removeFromDock(entry: AppEntry) {
        viewModelScope.launch { prefs.setDock(keys(dockCells.value).map { if (it == entry.key) null else it }) }
    }

    fun launch(entry: AppEntry, bounds: android.graphics.Rect?, options: android.os.Bundle?) =
        repo.launch(entry, bounds, options)

    fun openAppInfo(entry: AppEntry) = repo.openAppInfo(entry)
    fun uninstall(entry: AppEntry) = repo.uninstall(entry)

    /** Clock by Numio if it's installed, otherwise the phone's alarm screen. */
    fun clockApp(): AppEntry? = repo.findByPackage(CLOCK_PACKAGE)

    companion object {
        const val CLOCK_PACKAGE = "com.rr.numio.clock"
    }
}
