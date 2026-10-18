package com.rr.numio.launcher

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rr.numio.launcher.data.AppEntry
import com.rr.numio.launcher.data.AppRepository
import com.rr.numio.launcher.data.Background
import com.rr.numio.launcher.data.DefaultDock
import com.rr.numio.launcher.data.HomePrefs
import com.rr.numio.launcher.data.HomeTheme
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

    val background: StateFlow<Background> =
        prefs.background.stateIn(viewModelScope, SharingStarted.Eagerly, Background.Paper)

    val dock: StateFlow<List<AppEntry>> =
        combine(repo.apps.filterNotNull(), prefs.dock) { apps, keys ->
            val byKey = apps.associateBy { it.key }
            keys.orEmpty().mapNotNull { byKey[it] }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
        val current = dock.value.map { it.key }
        if (entry.key in current) return
        if (current.size >= HomePrefs.DOCK_MAX) {
            Toast.makeText(getApplication(), "The dock holds ${HomePrefs.DOCK_MAX} apps", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch { prefs.setDock(current + entry.key) }
    }

    fun removeFromDock(entry: AppEntry) {
        viewModelScope.launch { prefs.setDock(dock.value.map { it.key } - entry.key) }
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
