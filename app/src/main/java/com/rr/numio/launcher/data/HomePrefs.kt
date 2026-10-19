package com.rr.numio.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.homeStore by preferencesDataStore(name = "home")

enum class Background { Paper, Wallpaper }

/** The two home screen designs. Both share the same apps, dock, Shelf and settings. */
enum class HomeTheme { Ink, Sol }

/** Sol's two wallpapers: Garden (painted, the default) and Petal (cut paper). */
enum class SolWallpaper { Garden, Petal }

/** Sol's clock styles. [id] is what's saved, so renaming a label never loses the choice. */
enum class SolClock(val id: String, val label: String) {
    Fraunces("fraunces", "Fraunces"),
    DmSerif("dmserif", "DM Serif"),
    Baloo("baloo", "Baloo"),
    Moirai("moirai", "Moirai"),
}

/** Home screen settings that must survive restarts: the dock and the background. */
class HomePrefs(context: Context) {

    private val store = context.applicationContext.homeStore

    /** Dock cells (null = empty cell). null overall = never set, so the default dock gets picked. */
    val dock: Flow<List<String?>?> = store.data.map { p ->
        p[DOCK]?.split('\n')?.map { it.ifEmpty { null } }
    }

    val background: Flow<Background> = store.data.map { p ->
        if (p[BACKGROUND] == "wallpaper") Background.Wallpaper else Background.Paper
    }

    /** Sol's home cells, row by row ([HOME_MAX] of them; null = empty cell). */
    val homeApps: Flow<List<String?>> = store.data.map { p ->
        p[HOME_APPS]?.split('\n')?.map { it.ifEmpty { null } }.orEmpty()
    }

    suspend fun setHomeApps(cells: List<String?>) {
        store.edit { it[HOME_APPS] = cells.joinToString("\n") { k -> k.orEmpty() } }
    }

    /** Sol's home grid (see [HomeGrid.serialize]); null until it's first changed. */
    val solGrid: Flow<String?> = store.data.map { p -> p[SOL_GRID] }

    suspend fun setSolGrid(text: String?) {
        store.edit { if (text == null) it.remove(SOL_GRID) else it[SOL_GRID] = text }
    }

    /** Grid and dock in one save, so a move between them is never half done. */
    suspend fun setGridAndDock(grid: String, dock: List<String?>) {
        store.edit {
            it[SOL_GRID] = grid
            it[DOCK] = dock.joinToString("\n") { k -> k.orEmpty() }
        }
    }

    /** Home and dock in one save, so a move between them is never half done. */
    suspend fun setCells(home: List<String?>, dock: List<String?>) {
        store.edit {
            it[HOME_APPS] = home.joinToString("\n") { k -> k.orEmpty() }
            it[DOCK] = dock.joinToString("\n") { k -> k.orEmpty() }
        }
    }

    val theme: Flow<HomeTheme> = store.data.map { p ->
        if (p[THEME] == "sol") HomeTheme.Sol else HomeTheme.Ink
    }

    suspend fun setTheme(theme: HomeTheme) {
        store.edit { it[THEME] = if (theme == HomeTheme.Sol) "sol" else "ink" }
    }

    val solWallpaper: Flow<SolWallpaper> = store.data.map { p ->
        if (p[SOL_WALLPAPER] == "petal") SolWallpaper.Petal else SolWallpaper.Garden
    }

    suspend fun setSolWallpaper(w: SolWallpaper) {
        store.edit { it[SOL_WALLPAPER] = if (w == SolWallpaper.Petal) "petal" else "garden" }
    }

    val solClock: Flow<SolClock> = store.data.map { p ->
        SolClock.entries.firstOrNull { it.id == p[SOL_CLOCK] } ?: SolClock.Fraunces
    }

    suspend fun setSolClock(c: SolClock) {
        store.edit { it[SOL_CLOCK] = c.id }
    }

    /** Show the next calendar event on the Today line. Off until the user turns it on. */
    val showCalendar: Flow<Boolean> = store.data.map { p -> p[SHOW_CALENDAR] ?: false }

    suspend fun setShowCalendar(on: Boolean) {
        store.edit { it[SHOW_CALENDAR] = on }
    }

    suspend fun setDock(cells: List<String?>) {
        store.edit { it[DOCK] = cells.joinToString("\n") { k -> k.orEmpty() } }
    }

    suspend fun setBackground(bg: Background) {
        store.edit { it[BACKGROUND] = if (bg == Background.Wallpaper) "wallpaper" else "paper" }
    }

    companion object {
        const val DOCK_MAX = 5
        const val HOME_MAX = 10
        private val DOCK = stringPreferencesKey("dock")
        private val BACKGROUND = stringPreferencesKey("background")
        private val SHOW_CALENDAR = booleanPreferencesKey("show_calendar")
        private val THEME = stringPreferencesKey("theme")
        private val SOL_WALLPAPER = stringPreferencesKey("sol_wallpaper")
        private val HOME_APPS = stringPreferencesKey("home_apps")
        private val SOL_CLOCK = stringPreferencesKey("sol_clock")
        private val SOL_GRID = stringPreferencesKey("sol_grid")
    }
}
