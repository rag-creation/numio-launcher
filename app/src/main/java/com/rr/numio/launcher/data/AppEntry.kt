package com.rr.numio.launcher.data

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle

/** One launchable app (an app can have more than one, and work-profile apps are separate). */
class AppEntry(
    /** Stable id, e.g. "com.whatsapp/.Main#0". Used for the dock and the icon cache. */
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    /** False for work-profile, Secure Folder, clone or private-space apps. */
    val isMainUser: Boolean,
    val info: LauncherActivityInfo,
    /** "Work", "Secure Folder"… for apps outside the main profile, else null. */
    val profileLabel: String? = null,
    /** Maker line ("Samsung", "Fossify") shown only when another app has the same name. */
    val clashLabel: String? = null,
) {
    val packageName: String get() = component.packageName
}
