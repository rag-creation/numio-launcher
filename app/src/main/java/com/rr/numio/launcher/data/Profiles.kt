package com.rr.numio.launcher.data

/** Which profile an app lives in. Only [Work] apps move behind the Workspace portal. */
enum class ProfileKind(val label: String?) {
    Personal(null),
    Work("Work"),
    Private("Private"),
    Clone("Dual apps"),
    SecureFolder("Secure Folder"),
}

/** What the Workspace screen can show, from Android's own state for the work profile. */
enum class WorkState { NoProfile, Ready, Empty, Paused, Locked, Unavailable }

/**
 * Splitting apps between the normal drawer and the Workspace. Pure Kotlin (generic over the
 * app type), so it is unit tested. This only changes what Numio shows: Android's work
 * profile itself is never changed, hidden or bypassed.
 */
object Profiles {

    /**
     * Android's user type → kind. Android 15+ reports the type; on older versions a launcher
     * can't ask, and a second profile there is almost always a work profile, so it's treated
     * as one.
     */
    fun kind(isMain: Boolean, userType: String?): ProfileKind {
        if (isMain) return ProfileKind.Personal
        val t = userType?.lowercase() ?: return ProfileKind.Work
        return when {
            t.endsWith(".managed") -> ProfileKind.Work
            t.endsWith(".private") -> ProfileKind.Private
            t.endsWith(".clone") || "dual" in t -> ProfileKind.Clone
            "knox" in t || "secure" in t -> ProfileKind.SecureFolder
            else -> ProfileKind.Work
        }
    }

    /** Everything the normal drawer (and its search) may show: all but work-profile apps. */
    fun <T> drawer(apps: List<T>, isWork: (T) -> Boolean): List<T> = apps.filterNot(isWork)

    /** Only the work-profile apps, for the Workspace screen. */
    fun <T> workspace(apps: List<T>, isWork: (T) -> Boolean): List<T> = apps.filter(isWork)

    /**
     * The Workspace screen's state. Android decides all of it; Numio only reports it.
     * [listed] is false when Android wouldn't give the app list at all.
     */
    fun workState(hasProfile: Boolean, listed: Boolean, paused: Boolean, unlocked: Boolean, apps: Int): WorkState = when {
        !hasProfile -> WorkState.NoProfile
        paused -> WorkState.Paused
        !listed -> WorkState.Unavailable
        !unlocked -> WorkState.Locked
        apps == 0 -> WorkState.Empty
        else -> WorkState.Ready
    }
}
