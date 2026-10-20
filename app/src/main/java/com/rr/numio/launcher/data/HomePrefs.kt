package com.rr.numio.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rr.numio.launcher.sketch.SketchChoices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.homeStore by preferencesDataStore(name = "home")

enum class Background { Paper, Wallpaper }

/** The two home screen designs. Both share the same apps, dock, Shelf and settings. */
enum class HomeTheme { Ink, Sol }

/** Sol's two wallpapers: Garden (painted, the default) and Petal (cut paper). */
enum class SolWallpaper { Garden, Petal }

/** How Ink looks: follow Android's dark theme (default), or always light or dark. */
enum class InkAppearance(val id: String, val label: String) {
    System("system", "Follow system"),
    Light("light", "Light"),
    Dark("dark", "Dark");

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: System
    }
}

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

    /** The Numio logo: each setting saved on its own key, so none resets another. */
    val portal: Flow<PortalSettings> = store.data.map { p ->
        PortalSettings(
            visible = p[PORTAL_VISIBLE] ?: true,
            action = PortalAction.of(p[PORTAL_ACTION]),
            color = PortalColor.of(p[PORTAL_COLOR]),
            style = PortalStyle.of(p[PORTAL_STYLE]),
            apps = p[PORTAL_APPS]?.split('\n')?.filter { it.isNotEmpty() }.orEmpty(),
        )
    }

    suspend fun setPortal(s: PortalSettings) {
        store.edit {
            it[PORTAL_VISIBLE] = s.visible
            if (s.action == null) it.remove(PORTAL_ACTION) else it[PORTAL_ACTION] = s.action.id
            it[PORTAL_COLOR] = s.color.id
            it[PORTAL_STYLE] = s.style.id
            it[PORTAL_APPS] = s.apps.joinToString("\n")
        }
    }

    /** Ink: sketch icons picked by hand, app key to choice (see [SketchChoices]). */
    val sketchChoices: Flow<Map<String, String>> = store.data.map { p -> SketchChoices.parse(p[SKETCH_ICONS]) }

    /** Sets one app's pick; null goes back to automatic. */
    suspend fun setSketchChoice(appKey: String, choice: String?) {
        store.edit {
            val all = SketchChoices.parse(it[SKETCH_ICONS]).toMutableMap()
            if (choice == null) all.remove(appKey) else all[appKey] = choice
            if (all.isEmpty()) it.remove(SKETCH_ICONS) else it[SKETCH_ICONS] = SketchChoices.serialize(all)
        }
    }

    /** Ink: Light, Dark or Follow system (default). */
    val inkAppearance: Flow<InkAppearance> = store.data.map { p -> InkAppearance.of(p[INK_APPEARANCE]) }

    suspend fun setInkAppearance(a: InkAppearance) {
        store.edit { it[INK_APPEARANCE] = a.id }
    }

    /** Light Ink's paper: the warm sketchbook paper (default) or Numio's website paper. */
    val inkWarmPaper: Flow<Boolean> = store.data.map { p -> p[INK_WARM_PAPER] ?: true }

    suspend fun setInkWarmPaper(warm: Boolean) {
        store.edit { it[INK_WARM_PAPER] = warm }
    }

    /** Ink's quote under the date: shown by default, with the default words. */
    val inkQuote: Flow<InkQuote> = store.data.map { p ->
        InkQuote(text = p[INK_QUOTE] ?: InkQuote.DEFAULT, visible = p[INK_QUOTE_ON] ?: true)
    }

    suspend fun setInkQuote(q: InkQuote) {
        store.edit {
            it[INK_QUOTE] = InkQuote.clean(q.text)
            it[INK_QUOTE_ON] = q.visible
        }
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
        private val PORTAL_VISIBLE = booleanPreferencesKey("portal_visible")
        private val PORTAL_ACTION = stringPreferencesKey("portal_action")
        private val PORTAL_COLOR = stringPreferencesKey("portal_color")
        private val PORTAL_APPS = stringPreferencesKey("portal_apps")
        private val PORTAL_STYLE = stringPreferencesKey("portal_style")
        private val SKETCH_ICONS = stringPreferencesKey("sketch_icons")
        private val INK_QUOTE = stringPreferencesKey("ink_quote")
        private val INK_QUOTE_ON = booleanPreferencesKey("ink_quote_on")
        private val INK_APPEARANCE = stringPreferencesKey("ink_appearance")
        private val INK_WARM_PAPER = booleanPreferencesKey("ink_warm_paper")
    }
}
