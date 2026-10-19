package com.rr.numio.launcher.data

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.core.content.ContextCompat
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.widget.Toast
import com.rr.numio.launcher.icons.ThemedIcons
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

    /** The work profile as Android reports it (null when there is none). */
    data class WorkProfile(
        val user: UserHandle,
        /** False when Android wouldn't give its app list. */
        val listed: Boolean,
        /** "Pause work apps" is on. */
        val paused: Boolean,
        /** Unlocked (a work profile with its own lock can be locked while the phone isn't). */
        val unlocked: Boolean,
    )

    private val _work = MutableStateFlow<WorkProfile?>(null)
    val work: StateFlow<WorkProfile?> = _work

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

    /** Android tells launchers when a work profile is added, removed, paused, resumed or unlocked. */
    private val profileReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = reload()
    }

    fun start() {
        if (started) return
        started = true
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
        }
        ContextCompat.registerReceiver(appContext, profileReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        reload()
    }

    fun stop() {
        if (!started) return
        started = false
        launcherApps.unregisterCallback(callback)
        try {
            appContext.unregisterReceiver(profileReceiver)
        } catch (e: IllegalArgumentException) {
            // wasn't registered
        }
    }

    /**
     * Asks Android to turn work apps back on. Android may show its own unlock screen; if this
     * launcher isn't allowed to ask (not the default home app), it says so and nothing changes.
     */
    fun requestWorkOn(): Boolean {
        val user = _work.value?.user ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return try {
            userManager.requestQuietModeEnabled(false, user)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    private fun changed(packageName: String) {
        SketchIcons.evictPackage(packageName)
        ThemedIcons.evictPackage(packageName)
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = scope.launch(Dispatchers.IO) {
            val (apps, work) = load()
            _work.value = work
            _apps.value = apps
        }
    }

    private fun load(): Pair<List<AppEntry>, WorkProfile?> {
        val me = Process.myUserHandle()
        val out = ArrayList<AppEntry>()
        val usedProfileLabels = HashSet<String>()
        var work: WorkProfile? = null
        for (user in launcherApps.profiles) {
            val isMain = user == me
            val kind = Profiles.kind(isMain, userType(user))
            // Two profiles of the same kind get "Work", "Work 2"
            val profileName = kind.label?.let { base ->
                var label = base
                var n = 2
                while (!usedProfileLabels.add(label)) label = "$base ${n++}"
                label
            }
            // A locked or paused profile can throw; then there's nothing to list
            val list = try {
                launcherApps.getActivityList(null, user)
            } catch (e: Exception) {
                null
            }
            if (kind == ProfileKind.Work && work == null) {
                work = WorkProfile(
                    user = user,
                    listed = list != null,
                    paused = try { userManager.isQuietModeEnabled(user) } catch (e: Exception) { false },
                    unlocked = try { userManager.isUserUnlocked(user) } catch (e: Exception) { true },
                )
            }
            if (list == null) continue
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
                    isMainUser = isMain,
                    info = info,
                    profileLabel = profileName,
                    profileKind = kind,
                )
            }
        }
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        // The same activity in the same profile is listed once, however it was reported
        val unique = out.distinctBy { it.key }.sortedWith { a, b -> collator.compare(a.label, b.label) }

        // Same name inside one profile → give each a maker line
        val withLabels = arrayOfNulls<String>(unique.size)
        unique.indices.groupBy { unique[it].user }.values.forEach { idx ->
            val labels = AppNaming.clashLabels(
                idx.map { Triple(unique[it].label, unique[it].packageName, unique[it].component.className) }
            )
            idx.forEachIndexed { k, i -> withLabels[i] = labels[k] }
        }
        val apps = unique.mapIndexed { i, e ->
            if (withLabels[i] == null) e
            else AppEntry(e.key, e.label, e.component, e.user, e.isMainUser, e.info, e.profileLabel, e.profileKind, withLabels[i])
        }
        return apps to work
    }

    /** Android's user type for a profile (Android 15+), else null. */
    private fun userType(user: UserHandle): String? {
        if (Build.VERSION.SDK_INT < 35) return null
        return try {
            launcherApps.getLauncherUserInfo(user)?.userType
        } catch (e: Exception) {
            null
        }
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
