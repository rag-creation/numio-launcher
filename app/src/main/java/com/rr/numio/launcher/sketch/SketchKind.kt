package com.rr.numio.launcher.sketch

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telecom.TelecomManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
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
 * Which app counts, for each job:
 *  1. the app you chose as the default (your Phone, SMS or browser app), even one you
 *     installed yourself, such as Fossify Phone or Fennec;
 *  2. with no default chosen, the phone's own preinstalled app for that job;
 *  3. with neither, the only app that does the job, if there is just one.
 * Other apps that can do the same job (a second calendar, say) keep their own look, so
 * you can still tell them apart.
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
        SketchKind.Contacts to listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS),
            // Many contacts apps (Fossify Contacts among them) never declare APP_CONTACTS,
            // but every one of them offers "create a new contact"; other apps don't.
            Intent(Intent.ACTION_INSERT).setType(ContactsContract.Contacts.CONTENT_TYPE),
        ),
        SketchKind.Clock to listOf(Intent(AlarmClock.ACTION_SHOW_ALARMS)),
    )

    /** One app that can do a job: its package and whether it came with the phone. */
    data class Handler(val packageName: String, val preinstalled: Boolean)

    /**
     * The apps that get a job's symbol: your chosen default, else the preinstalled ones,
     * else the only one there is. "android" (the app chooser) is never a choice.
     */
    fun choose(defaults: List<String?>, handlers: List<Handler>): Set<String> {
        val chosen = defaults.filterNotNull().filter { it.isNotEmpty() && it != "android" }.toSet()
        if (chosen.isNotEmpty()) return chosen
        val real = handlers.filter { it.packageName != "android" }
        val preinstalled = real.filter { it.preinstalled }.map { it.packageName }.toSet()
        if (preinstalled.isNotEmpty()) return preinstalled
        val all = real.map { it.packageName }.toSet()
        return if (all.size == 1) all else emptySet()
    }

    fun detect(context: Context): Map<SketchKind, Set<String>> {
        val pm = context.packageManager
        fun safe(block: () -> String?): String? = try { block() } catch (e: Exception) { null }
        return intents().mapValues { (kind, list) ->
            val defaults = list.map { intent ->
                safe { pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName }
            } + when (kind) {
                SketchKind.Phone -> listOf(safe { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage })
                SketchKind.Messages -> listOf(safe { Telephony.Sms.getDefaultSmsPackage(context) })
                else -> emptyList()
            }
            val handlers = list.flatMap { intent ->
                try {
                    pm.queryIntentActivities(intent, 0).map {
                        val flags = it.activityInfo.applicationInfo.flags
                        Handler(it.activityInfo.packageName, flags and ApplicationInfo.FLAG_SYSTEM != 0)
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            choose(defaults, handlers)
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
