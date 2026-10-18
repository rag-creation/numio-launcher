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

    /** Dock app keys in order. null = never set, so the default dock gets picked. */
    val dock: Flow<List<String>?> = store.data.map { p ->
        p[DOCK]?.split('\n')?.filter { it.isNotEmpty() }
    }

    val background: Flow<Background> = store.data.map { p ->
        if (p[BACKGROUND] == "wallpaper") Background.Wallpaper else Background.Paper
    }

    /** Apps placed on Sol's home rows, in order (up to [HOME_MAX]). */
    val homeApps: Flow<List<String>> = store.data.map { p ->
        p[HOME_APPS]?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()
    }

    suspend fun setHomeApps(keys: List<String>) {
        store.edit { it[HOME_APPS] = keys.joinToString("\n") }
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

    suspend fun setDock(keys: List<String>) {
        store.edit { it[DOCK] = keys.joinToString("\n") }
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
    }
}
