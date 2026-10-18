package com.rr.numio.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppNamingTest {

    private fun t(label: String, pkg: String, cls: String = "$pkg.MainActivity") = Triple(label, pkg, cls)

    @Test fun makerFromPackage() {
        assertEquals("Samsung", AppNaming.maker("com.samsung.android.calendar"))
        assertEquals("Google", AppNaming.maker("com.google.android.calendar"))
        assertEquals("Fossify", AppNaming.maker("org.fossify.calendar"))
        // short initials like "rr" are skipped for the brand
        assertEquals("Numio", AppNaming.maker("com.rr.numio.clock"))
        assertEquals("Numio", AppNaming.maker("com.rr.numio.clock.test"))
    }

    @Test fun onlyClashingNamesGetALabel() {
        val labels = AppNaming.clashLabels(
            listOf(
                t("Calendar", "com.samsung.android.calendar"),
                t("Camera", "com.sec.android.app.camera"),
                t("Calendar", "org.fossify.calendar"),
                t("calendar", "com.google.android.calendar"),
            )
        )
        assertEquals(listOf("Samsung", null, "Fossify", "Google"), labels)
    }

    @Test fun releaseAndTestBuild() {
        val labels = AppNaming.clashLabels(
            listOf(
                t("Clock by Numio", "com.rr.numio.clock"),
                t("Clock by Numio", "com.rr.numio.clock.test"),
            )
        )
        assertEquals(listOf("Numio", "Numio · test"), labels)
    }

    @Test fun twoEntriesFromOnePackage() {
        val labels = AppNaming.clashLabels(
            listOf(
                t("Photos", "org.example.gallery", "org.example.gallery.PhotosActivity"),
                t("Photos", "org.example.gallery", "org.example.gallery.Videos"),
            )
        )
        assertEquals(listOf("Photos", "Videos"), labels)
    }

    @Test fun twoAppsBySameMaker() {
        val labels = AppNaming.clashLabels(
            listOf(
                t("Photos", "com.google.android.apps.photos"),
                t("Photos", "com.google.android.gallery"),
            )
        )
        assertEquals(listOf("Photos", "Gallery"), labels)
    }
}
