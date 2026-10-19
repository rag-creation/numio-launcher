package com.rr.numio.launcher.sketch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings

/**
 * The apps that have their own Numio Sketch symbol (the first, prototype set).
 * Everything else keeps the fallback: its monochrome layer, or a hand-drawn letter.
 */
enum class SketchKind(
    /** Words in an app's name that point to this kind, used only to split one package that
     *  offers two of them (e.g. a "Phone" and a "Contacts" icon from the same app). */
    val words: List<String>,
) {
    Phone(listOf("phone", "dial", "call")),
    Messages(listOf("message", "sms", "chat")),
    Camera(listOf("camera")),
    Settings(listOf("setting")),
    Calendar(listOf("calendar")),
    Browser(listOf("browser", "internet", "web")),
    Contacts(listOf("contact", "people")),
    Clock(listOf("clock", "alarm")),
}

/** Colours a glyph may use. Charcoal ink, warm cream and a little gold, nothing else. */
enum class Tone(val argb: Int) {
    INK(0xFF1C1A17.toInt()),
    CREAM(0xFFFFF9EE.toInt()),
    GOLD(0xFFC9962E.toInt()),
}

/** One drawing step of a glyph, in the 100-unit grid. Paths use M, L, C and Z. */
sealed interface Layer {
    /** Colour laid under the lines, slightly off them. */
    data class Fill(val tone: Tone, val path: String) : Layer
    /** The main pen line. */
    data class Ink(val path: String) : Layer
    /** A finer line (details, accents). */
    data class Thin(val tone: Tone, val path: String) : Layer
    /** The one small group of shading strokes an icon may have. */
    data class Hatch(val path: String) : Layer
    /** A solid dot. */
    data class Dot(val tone: Tone, val x: Float, val y: Float, val r: Float) : Layer
}

/**
 * Which app gets which symbol. [roles] says which packages handle each job on this phone
 * (the default dialer, the default SMS app, the camera…), found by asking Android, so no
 * app or package names are assumed.
 *
 * Only apps that came with the phone get these shared symbols. An app you installed
 * yourself (another browser, a different SMS app) keeps its own look until it gets its
 * own drawing, so it never loses its identity to a generic globe or bubble.
 */
object SketchRoles {

    @Volatile private var cached: Map<SketchKind, Set<String>>? = null

    fun roles(context: Context): Map<SketchKind, Set<String>> =
        cached ?: detect(context).also { cached = it }

    /** After apps change, look again. */
    fun reset() {
        cached = null
    }

    /** The intents that identify each job. */
    fun intents(): Map<SketchKind, List<Intent>> = mapOf(
        SketchKind.Phone to listOf(Intent(Intent.ACTION_DIAL)),
        SketchKind.Messages to listOf(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))),
        SketchKind.Camera to listOf(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
        SketchKind.Settings to listOf(Intent(Settings.ACTION_SETTINGS)),
        SketchKind.Calendar to listOf(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)),
        SketchKind.Browser to listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org")).addCategory(Intent.CATEGORY_BROWSABLE),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
        ),
        SketchKind.Contacts to listOf(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS)),
        SketchKind.Clock to listOf(Intent(AlarmClock.ACTION_SHOW_ALARMS)),
    )

    /** For each job, the phone's own (preinstalled) apps that do it. */
    fun detect(context: Context): Map<SketchKind, Set<String>> {
        val pm = context.packageManager
        return intents().mapValues { (_, list) ->
            list.flatMap { intent ->
                try {
                    pm.queryIntentActivities(intent, PackageManager.MATCH_SYSTEM_ONLY).map { it.activityInfo.packageName }
                } catch (e: Exception) {
                    emptyList()
                }
            }.filter { it != "android" }.toSet()
        }
    }

    fun kindFor(packageName: String, label: String, roles: Map<SketchKind, Set<String>>): SketchKind? {
        val matches = SketchKind.entries.filter { packageName in roles[it].orEmpty() }
        if (matches.size <= 1) return matches.firstOrNull()
        // One package, several jobs: let the app's own name decide, else the first job.
        val name = label.lowercase()
        return matches.firstOrNull { k -> k.words.any { it in name } } ?: matches.first()
    }
}
