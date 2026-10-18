package com.rr.numio.launcher

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/** Checking and asking to be the default home app. */
object HomeRole {

    fun isDefault(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) return rm.isRoleHeld(RoleManager.ROLE_HOME)
        }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val pkg = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        return pkg == context.packageName
    }

    /** The system "be my home app?" dialog, or null where there isn't one (Android 9 and older). */
    fun requestIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val rm = context.getSystemService(RoleManager::class.java) ?: return null
        if (!rm.isRoleAvailable(RoleManager.ROLE_HOME)) return null
        return rm.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }

    fun openHomeSettings(context: Context) {
        for (action in listOf(Settings.ACTION_HOME_SETTINGS, Settings.ACTION_SETTINGS)) {
            try {
                context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: ActivityNotFoundException) {
                // try the next one
            }
        }
    }
}
