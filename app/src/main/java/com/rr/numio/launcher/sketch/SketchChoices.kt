package com.rr.numio.launcher.sketch

import androidx.compose.runtime.mutableIntStateOf

/**
 * The sketch icon you picked for an app by hand (long-press, "Sketch icon…"), which beats
 * the automatic rules. Per app entry key; "own" keeps the app's own look; a missing entry
 * means automatic. Only the drawing changes, never the app's name or how it opens.
 */
object SketchChoices {

    const val OWN = "own"

    /** Saved as "appKey<TAB>choice" lines. Unknown choices are dropped (treated as automatic). */
    fun parse(text: String?): Map<String, String> =
        text.orEmpty().lineSequence().mapNotNull { line ->
            val tab = line.indexOf('\t')
            if (tab <= 0) return@mapNotNull null
            val key = line.substring(0, tab)
            val value = line.substring(tab + 1)
            if (value == OWN || SketchKind.entries.any { it.name == value }) key to value else null
        }.toMap()

    fun serialize(choices: Map<String, String>): String =
        choices.entries.sortedBy { it.key }.joinToString("\n") { "${it.key}\t${it.value}" }

    /** The drawing to use: the hand-picked one, none for "own", else what [auto] finds. */
    fun decide(choice: String?, auto: () -> SketchKind?): SketchKind? = when (choice) {
        null -> auto()
        OWN -> null
        else -> SketchKind.entries.firstOrNull { it.name == choice } ?: auto()
    }

    @Volatile var current: Map<String, String> = emptyMap()
        private set

    /** Bumped on every change, so icons on screen draw again. */
    val version = mutableIntStateOf(0)

    fun update(choices: Map<String, String>) {
        if (choices != current) {
            current = choices
            version.intValue++
        }
    }
}
