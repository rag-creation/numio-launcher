package com.rr.numio.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore

/** First-run dock: the phone's own dialer, messages, browser and camera, when they exist. */
object DefaultDock {

    fun pick(context: Context, apps: List<AppEntry>): List<AppEntry> {
        val pm = context.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        )
        val picked = ArrayList<AppEntry>()
        for (intent in intents) {
            val pkg = try {
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            } catch (e: Exception) {
                null
            }
            // "android" means no default was chosen (the app chooser)
            if (pkg == null || pkg == "android") continue
            val entry = apps.firstOrNull { it.isMainUser && it.packageName == pkg } ?: continue
            if (picked.none { it.key == entry.key }) picked += entry
        }
        return picked
    }
}
