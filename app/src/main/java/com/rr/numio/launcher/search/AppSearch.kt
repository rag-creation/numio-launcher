package com.rr.numio.launcher.search

import java.text.Normalizer
import java.util.Locale

/**
 * Ranks apps for a typed query. Pure Kotlin (no Android), so it is unit tested.
 *
 * Best first: exact name, name starts with the query, a word starts with it,
 * initials ("gm" → Google Maps), anywhere in the name, then letters in order ("yt" → YouTube).
 */
object AppSearch {

    private val marks = Regex("\\p{Mn}+")
    private val wordSplit = Regex("[^\\p{L}\\p{N}]+")
    // "WhatsApp" → "Whats App" so a word can start in the middle
    private val camel = Regex("(?<=\\p{Ll})(?=\\p{Lu})")

    fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(marks, "").lowercase(Locale.ROOT).trim()

    /** 0 = no match; higher = better. */
    fun score(label: String, query: String): Int {
        val q = normalize(query)
        if (q.isEmpty()) return 1
        val l = normalize(label)
        if (l == q) return 1000
        if (l.startsWith(q)) return 900 - (l.length - q.length).coerceAtMost(100)

        val words = label.replace(camel, " ").let(::normalize).split(wordSplit).filter { it.isNotEmpty() }
        val wordIndex = words.indexOfFirst { it.startsWith(q) }
        if (wordIndex >= 0) return 700 - (wordIndex * 10).coerceAtMost(90)

        val compactQ = q.filter { it.isLetterOrDigit() }
        if (compactQ.isEmpty()) return 0
        if (compactQ.length >= 2 && words.joinToString("") { it.take(1) }.startsWith(compactQ)) return 600

        val compactL = l.filter { it.isLetterOrDigit() }
        if (compactL.startsWith(compactQ)) return 550

        val at = compactL.indexOf(compactQ)
        if (at >= 0) return 400 - at.coerceAtMost(100)

        if (compactQ.length >= 2 && isSubsequence(compactQ, compactL)) return 100
        return 0
    }

    private fun isSubsequence(q: String, s: String): Boolean {
        var i = 0
        for (c in s) if (i < q.length && c == q[i]) i++
        return i == q.length
    }

    /** Matching items, best first. Equal scores keep the incoming (A–Z) order. */
    fun <T> rank(query: String, items: List<T>, label: (T) -> String): List<T> {
        if (normalize(query).isEmpty()) return items
        return items
            .map { it to score(label(it), query) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
