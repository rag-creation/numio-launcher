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
        private val DOCK = stringPreferencesKey("dock")
        private val BACKGROUND = stringPreferencesKey("background")
        private val SHOW_CALENDAR = booleanPreferencesKey("show_calendar")
    }
}
