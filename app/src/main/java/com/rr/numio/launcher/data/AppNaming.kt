package com.rr.numio.launcher.data

import java.util.Locale

/**
 * Small second lines for apps that share a name ("Calendar" from Samsung, Google and
 * Fossify). Pure Kotlin, so it is unit tested. Only clashing names get a label.
 */
object AppNaming {

    /** Package parts that say nothing about who made the app. */
    private val generic = setOf(
        "com", "org", "net", "io", "app", "apps", "android", "mobile", "github", "gitlab",
        "dev", "me", "de", "in", "co", "uk", "ae", "jp", "eu", "us", "ws", "xyz",
    )

    /** Endings that mark a second build of the same app. */
    private val buildTags = setOf("test", "debug", "beta", "dev", "nightly", "alpha", "staging")

    /** Who made it, guessed from the package: org.fossify.calendar → "Fossify". */
    fun maker(packageName: String): String {
        val core = packageName.split('.').let { p -> if (p.size > 1 && p.last() in buildTags) p.dropLast(1) else p }
        val owners = if (core.size > 1) core.dropLast(1) else core
        val pick = owners.firstOrNull { it.length >= 3 && it !in generic }
            ?: owners.firstOrNull { it !in generic }
            ?: core.last()
        return pick.replaceFirstChar { it.titlecase(Locale.ROOT) }
    }

    /** "test" for com.rr.numio.clock.test, otherwise null. */
    fun buildTag(packageName: String): String? =
        packageName.substringAfterLast('.').takeIf { it in buildTags && packageName.contains('.') }

    /**
     * A label for every item whose name clashes with another item's, null for the rest.
     * [items] are (label, packageName, activityClass) within one profile.
     */
    fun clashLabels(items: List<Triple<String, String, String>>): List<String?> {
        val out = arrayOfNulls<String>(items.size)
        items.indices
            .groupBy { items[it].first.trim().lowercase(Locale.ROOT) }
            .values
            .filter { it.size > 1 }
            .forEach { group ->
                // 1) maker, plus "test" etc. for second builds
                val first = group.associateWith { i ->
                    val pkg = items[i].second
                    listOfNotNull(maker(pkg), buildTag(pkg)).joinToString(" · ")
                }
                group.forEach { i ->
                    val mine = first.getValue(i)
                    out[i] = if (first.values.count { it == mine } == 1) {
                        mine
                    } else {
                        // 2) same maker too: two entries of one package, or two apps by one maker
                        val pkg = items[i].second
                        val samePackage = group.count { items[it].second == pkg } > 1
                        if (samePackage) {
                            items[i].third.substringAfterLast('.')
                                .removeSuffix("Activity").ifEmpty { mine }
                        } else {
                            pkg.substringAfterLast('.').replaceFirstChar { it.titlecase(Locale.ROOT) }
                        }
                    }
                }
            }
        return out.toList()
    }
}
