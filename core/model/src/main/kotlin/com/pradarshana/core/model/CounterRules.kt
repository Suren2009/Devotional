package com.pradarshana.core.model

import kotlin.math.roundToInt

object CounterRules {

    const val MAX_TARGET = 100_000

    sealed interface TargetValidation {
        data class Valid(val target: Int) : TargetValidation
        data object Empty : TargetValidation
        data object NotPositive : TargetValidation
        data object TooLarge : TargetValidation
        data object NotANumber : TargetValidation
    }

    fun validateTarget(input: String): TargetValidation {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return TargetValidation.Empty
        val value = trimmed.toLongOrNull() ?: return TargetValidation.NotANumber
        return validateTarget(value)
    }

    fun validateTarget(value: Long): TargetValidation = when {
        value <= 0 -> TargetValidation.NotPositive
        value > MAX_TARGET -> TargetValidation.TooLarge
        else -> TargetValidation.Valid(value.toInt())
    }

    fun remaining(target: Int, completed: Int): Int = (target - completed).coerceAtLeast(0)

    fun progressFraction(target: Int, completed: Int): Float =
        if (target <= 0) 0f else (completed.toFloat() / target).coerceIn(0f, 1f)

    /** Rounded down so that 100% is only shown once the target is actually reached. */
    fun progressPercent(target: Int, completed: Int): Int {
        if (target <= 0) return 0
        val exact = completed.toDouble() * 100.0 / target
        val rounded = exact.roundToInt()
        return if (rounded >= 100 && completed < target) 99 else rounded.coerceIn(0, 100)
    }

    fun canIncrement(status: SessionStatus, target: Int, completed: Int): Boolean =
        status == SessionStatus.ACTIVE && completed < target

    /** Total of per-device contributions, never negative and never above the target. */
    fun mergedCount(target: Int, deviceCounts: Map<String, Int>): Int =
        deviceCounts.values.sumOf { it.coerceAtLeast(0).toLong() }.coerceAtMost(target.toLong()).toInt()
}
