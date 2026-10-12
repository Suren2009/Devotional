package com.pradarshana.core.model

/**
 * Suggested round counts for the start screen.
 * The most recent target comes first, then targets used more than once, then [defaults].
 * Each number appears only once.
 */
object CommonCounts {
    val defaults = listOf(3, 9, 18, 54, 108)

    fun suggest(targetsNewestFirst: List<Int>): List<Int> {
        if (targetsNewestFirst.isEmpty()) return defaults
        val latest = targetsNewestFirst.first()
        val frequent = targetsNewestFirst.groupingBy { it }.eachCount()
            .filter { it.value > 1 }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<Int, Int>> { it.value }
                    .thenBy { targetsNewestFirst.indexOf(it.key) },
            )
            .map { it.key }
        return (listOf(latest) + frequent + defaults).distinct()
    }
}
