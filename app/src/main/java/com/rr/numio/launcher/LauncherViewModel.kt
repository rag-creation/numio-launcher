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

    /** Sol's home: two rows of five fixed cells. */
    val homeCells: StateFlow<List<AppEntry?>> = cellsOf(prefs.homeApps, HomePrefs.HOME_MAX)

    private fun keys(cells: List<AppEntry?>) = cells.map { it?.key }

    fun isOnHome(entry: AppEntry) = homeCells.value.any { it?.key == entry.key }

    fun addToHome(entry: AppEntry) {
        val cells = keys(homeCells.value)
        if (entry.key in cells) return
        val i = Cells.firstEmpty(cells)
        if (i < 0) {
            Toast.makeText(getApplication(), "Home is full. Remove an app first", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch { prefs.setHomeApps(cells.toMutableList().also { it[i] = entry.key }) }
    }

    fun removeFromHome(entry: AppEntry) {
        viewModelScope.launch { prefs.setHomeApps(keys(homeCells.value).map { if (it == entry.key) null else it }) }
    }

    /** Drag on Sol's home or dock: into an empty cell it moves, onto an app they swap. */
    fun drop(from: Slot, to: Slot) {
        val (home, dock) = Cells.move(keys(homeCells.value), keys(dockCells.value), from, to)
        viewModelScope.launch { prefs.setCells(home, dock) }
    }

    /** Search results; with no query, every app A–Z. */
    val results: StateFlow<List<AppEntry>> =
        combine(repo.apps.filterNotNull(), _query) { apps, q -> AppSearch.rank(q, apps) { it.label } }
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
    fun goHome() = closeShelf()

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
