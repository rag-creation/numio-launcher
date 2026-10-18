package com.rr.numio.launcher.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.widget.Toast
import com.rr.numio.launcher.sketch.SketchIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.Collator

/**
 * The list of launchable apps, from LauncherApps.
 *
 * Loaded once in the background and reloaded only when an app is installed, removed
 * or updated, so opening the Shelf never queries the package manager again.
 */
class AppRepository(context: Context, private val scope: CoroutineScope) {

    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val ownPackage = appContext.packageName

    private val _apps = MutableStateFlow<List<AppEntry>?>(null)
    /** null until the first load finishes. Sorted A–Z by label. */
    val apps: StateFlow<List<AppEntry>?> = _apps

    private var loadJob: Job? = null
    private var started = false

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            packageNames.forEach { changed(it) }
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            packageNames.forEach { changed(it) }
    }

    fun start() {
        if (started) return
        started = true
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        reload()
    }

    fun stop() {
        if (!started) return
        started = false
        launcherApps.unregisterCallback(callback)
    }

    private fun changed(packageName: String) {
        SketchIcons.evictPackage(packageName)
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = scope.launch(Dispatchers.IO) { _apps.value = load() }
    }

    private fun load(): List<AppEntry> {
        val me = Process.myUserHandle()
        val out = ArrayList<AppEntry>()
        for (user in launcherApps.profiles) {
            // A locked work profile or private space can throw; just skip it
            val list = try {
                launcherApps.getActivityList(null, user)
            } catch (e: Exception) {
                continue
            }
            val serial = userManager.getSerialNumberForUser(user)
            for (info in list) {
                val cn = info.componentName
                if (cn.packageName == ownPackage) continue
                val label = info.label?.toString()?.trim().orEmpty().ifEmpty { cn.packageName }
                out += AppEntry(
                    key = "${cn.flattenToShortString()}#$serial",
                    label = label,
                    component = cn,
                    user = user,
                    isMainUser = user == me,
                    info = info,
                )
            }
        }
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return out.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun launch(entry: AppEntry, bounds: Rect?, options: Bundle?) {
        try {
            launcherApps.startMainActivity(entry.component, entry.user, bounds, options)
        } catch (e: Exception) {
            // Uninstalled a moment ago, disabled, or a locked profile
            Toast.makeText(appContext, "Couldn't open ${entry.label}", Toast.LENGTH_SHORT).show()
            reload()
        }
    }

    fun openAppInfo(entry: AppEntry) {
        try {
            launcherApps.startAppDetailsActivity(entry.component, entry.user, null, null)
        } catch (e: Exception) {
            Toast.makeText(appContext, "Couldn't open app info", Toast.LENGTH_SHORT).show()
        }
    }

    fun uninstall(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", entry.packageName, null))
            .putExtra(Intent.EXTRA_USER, entry.user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            appContext.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            openAppInfo(entry)
        }
    }

    fun findByPackage(packageName: String): AppEntry? =
        _apps.value?.firstOrNull { it.isMainUser && it.packageName == packageName }
}
