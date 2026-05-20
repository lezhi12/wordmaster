package com.example.wordmaster.data

import kotlin.math.pow

object ForgettingCurve {
    private val REVIEW_INTERVALS = listOf(
        5L * 60L * 1000L,
        30L * 60L * 1000L,
        12L * 60L * 60L * 1000L,
        1L * 24L * 60L * 60L * 1000L,
        2L * 24L * 60L * 60L * 1000L,
        4L * 24L * 60L * 60L * 1000L,
        7L * 24L * 60L * 60L * 1000L,
        15L * 24L * 60L * 60L * 1000L,
        30L * 24L * 60L * 60L * 1000L
    )

    fun calculateNextReviewTime(currentLevel: Int, remembered: Boolean): Pair<Int, Long> {
        return if (remembered) {
            val nextLevel = (currentLevel + 1).coerceAtMost(REVIEW_INTERVALS.size)
            val interval = REVIEW_INTERVALS.getOrElse(nextLevel - 1) { REVIEW_INTERVALS.last() }
            nextLevel to System.currentTimeMillis() + interval
        } else {
            val nextLevel = (currentLevel - 1).coerceAtLeast(0)
            val interval = REVIEW_INTERVALS.getOrElse(nextLevel) { REVIEW_INTERVALS.first() }
            nextLevel to System.currentTimeMillis() + interval
        }
    }

    fun getIntervalForLevel(level: Int): Long {
        return REVIEW_INTERVALS.getOrElse(level) { REVIEW_INTERVALS.last() }
    }
}
