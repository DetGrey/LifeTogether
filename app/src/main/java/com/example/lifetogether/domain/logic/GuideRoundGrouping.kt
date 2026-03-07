package com.example.lifetogether.domain.logic

import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType

data class GuideRoundGroupContext(
    val startIndex: Int,
    val endIndex: Int,
    val range: IntRange,
    val completedCount: Int,
) {
    val size: Int
        get() = endIndex - startIndex + 1
}

object GuideRoundGrouping {
    private val exactRoundSpanRegex = Regex("^\\s*[Rr]\\s*(\\d+)(?:\\s*[-–]\\s*(\\d+))?\\s*$")
    private val exactRoundNumberRegex = Regex("^\\s*[Rr]\\s*(\\d+)\\s*$")
    private val roundPrefixRegex =
        Regex("^\\s*[Rr]\\s*(\\d+)(?:\\s*[-–]\\s*(\\d+))?(?:\\s*[:.)-])?\\s*(.*)$")

    fun parseRoundSpan(value: String?): IntRange? {
        if (value.isNullOrBlank()) return null
        val match = exactRoundSpanRegex.matchEntire(value.trim()) ?: return null
        val start = match.groupValues[1].toIntOrNull() ?: return null
        val end = match.groupValues[2].toIntOrNull() ?: start
        if (end < start) return null
        return start..end
    }

    fun parseRoundNumber(value: String?): Int? {
        if (value.isNullOrBlank()) return null
        val match = exactRoundNumberRegex.matchEntire(value.trim()) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    fun parseRoundPrefix(value: String): Pair<IntRange, String>? {
        val match = roundPrefixRegex.matchEntire(value.trim()) ?: return null
        val start = match.groupValues[1].toIntOrNull() ?: return null
        val end = match.groupValues[2].toIntOrNull() ?: start
        if (end < start) return null
        return (start..end) to match.groupValues[3].trim()
    }

    fun formatRoundLabel(range: IntRange): String {
        return if (range.first == range.last) {
            "R${range.first}"
        } else {
            "R${range.first}-${range.last}"
        }
    }

    fun findRoundGroupContext(
        steps: List<GuideStep>,
        index: Int,
    ): GuideRoundGroupContext? {
        if (index !in steps.indices) return null
        val current = steps[index]
        if (current.type != GuideStepType.ROUND) return null

        val currentNumber = parseRoundNumber(roundLabelSource(current)) ?: return null
        val contentKey = current.content.trim()

        var startIndex = index
        var startRound = currentNumber
        var expectedPrevious = currentNumber - 1

        while (startIndex > 0) {
            val previous = steps[startIndex - 1]
            if (previous.type != GuideStepType.ROUND) break

            val previousNumber = parseRoundNumber(roundLabelSource(previous)) ?: break
            if (previousNumber != expectedPrevious) break
            if (previous.content.trim() != contentKey) break

            startIndex -= 1
            startRound = previousNumber
            expectedPrevious -= 1
        }

        var endIndex = index
        var endRound = currentNumber
        var expectedNext = currentNumber + 1

        while (endIndex < steps.lastIndex) {
            val next = steps[endIndex + 1]
            if (next.type != GuideStepType.ROUND) break

            val nextNumber = parseRoundNumber(roundLabelSource(next)) ?: break
            if (nextNumber != expectedNext) break
            if (next.content.trim() != contentKey) break

            endIndex += 1
            endRound = nextNumber
            expectedNext += 1
        }

        val groupedSteps = steps.subList(startIndex, endIndex + 1)
        return GuideRoundGroupContext(
            startIndex = startIndex,
            endIndex = endIndex,
            range = startRound..endRound,
            completedCount = groupedSteps.count { it.completed },
        )
    }

    private fun roundLabelSource(step: GuideStep): String {
        return step.name.ifBlank { step.title }
    }
}
