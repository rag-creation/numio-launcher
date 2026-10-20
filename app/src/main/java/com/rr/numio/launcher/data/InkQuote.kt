package com.rr.numio.launcher.data

/** The short line under Ink's date ("Good things take time."): optional and editable. */
data class InkQuote(val text: String = DEFAULT, val visible: Boolean = true) {

    companion object {
        const val DEFAULT = "Good things take time."
        const val MAX_LENGTH = 60

        /** What's saved: trimmed, single spaces, at most [MAX_LENGTH] characters. */
        fun clean(text: String): String = text.replace(Regex("\\s+"), " ").trim().take(MAX_LENGTH)

        /** True when RR's hand-drawn letters can write it (A–Z, 0–9, space . , :); else the system font. */
        fun handwritable(text: String): Boolean =
            text.isNotBlank() && text.uppercase().all { it in 'A'..'Z' || it in '0'..'9' || it in " .,:" }

        /**
         * Splits [text] into lines no wider than [maxWidth], whole words only (a word longer
         * than the line gets a line of its own). [measure] gives a string's width.
         */
        fun wrap(text: String, maxWidth: Float, measure: (String) -> Float): List<String> {
            val lines = ArrayList<String>()
            var line = ""
            for (word in text.split(' ').filter { it.isNotEmpty() }) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (line.isNotEmpty() && measure(candidate) > maxWidth) {
                    lines += line
                    line = word
                } else {
                    line = candidate
                }
            }
            if (line.isNotEmpty()) lines += line
            return lines
        }
    }
}
