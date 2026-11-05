package com.deepintv.launcher.data

import java.text.Normalizer

/**
 * Filters and ranks [items] against [query] by their [label]. Best matches first:
 * 1. the label starts with the query ("gma" -> Gmail),
 * 2. a word of the label starts with it ("maps" -> Google Maps),
 * 3. the query is a prefix of the word initials ("gm" -> Google Maps),
 * 4. the query appears anywhere in the label.
 * Items that match none are dropped; ties keep their input order. A blank query returns [items].
 * Case and accents are ignored. Pure, so it can be unit-tested without Android.
 */
fun <T> rankByQuery(items: List<T>, query: String, label: (T) -> String): List<T> {
    val q = normalizeForSearch(query).filter { !it.isWhitespace() }
    if (q.isEmpty()) return items
    return items
        .mapNotNull { item ->
            val rank = matchRank(label(item), q)
            if (rank == NO_MATCH) null else rank to item
        }
        .sortedBy { it.first } // stable: equal ranks keep the input order
        .map { it.second }
}

private const val NO_MATCH = Int.MAX_VALUE

/** Lower-cased, accent-free form of [text]. */
internal fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
        .lowercase()

/** Words of [label]: split on anything that is not a letter or digit, and on camelCase humps. */
internal fun searchWords(label: String): List<String> {
    val words = ArrayList<String>()
    val current = StringBuilder()
    fun flush() {
        if (current.isNotEmpty()) {
            words += current.toString()
            current.setLength(0)
        }
    }
    var prev = ' '
    for (c in label) {
        when {
            !c.isLetterOrDigit() -> flush()
            c.isUpperCase() && prev.isLowerCase() -> {
                flush()
                current.append(c)
            }
            else -> current.append(c)
        }
        prev = c
    }
    flush()
    return words
}

/** [q] must already be normalized and free of whitespace. */
private fun matchRank(label: String, q: String): Int {
    val words = searchWords(label).map { normalizeForSearch(it) }
    val flat = normalizeForSearch(label)
    return when {
        flat.startsWith(q) || words.joinToString("").startsWith(q) -> 0
        words.any { it.startsWith(q) } -> 1
        words.map { it.first() }.joinToString("").startsWith(q) -> 2
        flat.contains(q) -> 3
        else -> NO_MATCH
    }
}
