package com.rr.numio.launcher.data

import com.rr.numio.launcher.search.AppSearch
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfilesTest {

    private data class App(val key: String, val label: String, val work: Boolean)

    private val apps = listOf(
        App("com.goodwy.calendar/.Main#0", "Calendar", work = false),
        App("com.goodwy.calendar/.Main#10", "Calendar", work = true),
        App("com.whatsapp/.Main#0", "WhatsApp", work = false),
        App("dev.imranr.obtainium/.Main#10", "Obtainium", work = true),
        App("com.aurora.store/.Main#10", "Aurora Store", work = true),
    )
    private val isWork: (App) -> Boolean = { it.work }

    @Test fun drawerLeavesOutEveryWorkApp() {
        assertEquals(
            listOf("com.goodwy.calendar/.Main#0", "com.whatsapp/.Main#0"),
            Profiles.drawer(apps, isWork).map { it.key },
        )
    }

    @Test fun drawerSearchNeverFindsWorkApps_evenWithTheSameName() {
        val found = AppSearch.rank("cal", Profiles.drawer(apps, isWork)) { it.label }
        assertEquals(listOf("com.goodwy.calendar/.Main#0"), found.map { it.key })
        assertEquals(emptyList<App>(), AppSearch.rank("obtain", Profiles.drawer(apps, isWork)) { it.label })
    }

    @Test fun workspaceListsOnlyWorkApps_keepingTheirOwnIdentity() {
        val work = Profiles.workspace(apps, isWork)
        assertEquals(
            listOf("com.goodwy.calendar/.Main#10", "dev.imranr.obtainium/.Main#10", "com.aurora.store/.Main#10"),
            work.map { it.key },
        )
        // the work Calendar is not the personal Calendar, though the names match
        assertEquals(1, work.count { it.label == "Calendar" })
    }

    @Test fun profileKindFromAndroidsUserType() {
        assertEquals(ProfileKind.Personal, Profiles.kind(isMain = true, userType = "android.os.usertype.full.SYSTEM"))
        assertEquals(ProfileKind.Work, Profiles.kind(false, "android.os.usertype.profile.MANAGED"))
        assertEquals(ProfileKind.Private, Profiles.kind(false, "android.os.usertype.profile.PRIVATE"))
        assertEquals(ProfileKind.Clone, Profiles.kind(false, "android.os.usertype.profile.CLONE"))
        assertEquals(ProfileKind.SecureFolder, Profiles.kind(false, "com.samsung.knox.securefolder"))
        // before Android 15 the type isn't available: a second profile is treated as work
        assertEquals(ProfileKind.Work, Profiles.kind(false, null))
    }

    @Test fun workspaceStates() {
        assertEquals(WorkState.NoProfile, Profiles.workState(false, false, false, true, 0))
        assertEquals(WorkState.Ready, Profiles.workState(true, true, false, true, 3))
        assertEquals(WorkState.Empty, Profiles.workState(true, true, false, true, 0))
        assertEquals(WorkState.Paused, Profiles.workState(true, true, paused = true, unlocked = true, apps = 3))
        // paused wins even if Android won't list the apps
        assertEquals(WorkState.Paused, Profiles.workState(true, listed = false, paused = true, unlocked = false, apps = 0))
        assertEquals(WorkState.Locked, Profiles.workState(true, true, false, unlocked = false, apps = 3))
        assertEquals(WorkState.Unavailable, Profiles.workState(true, listed = false, paused = false, unlocked = true, apps = 0))
    }
}
