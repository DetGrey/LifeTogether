package com.example.lifetogether.ui.feature.tipTracker

import com.example.lifetogether.domain.model.TipItem
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object TipStatisticsCalculator {
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val dayFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.ENGLISH)

    fun calculateTipStats(
        tips: List<TipItem>,
        today: LocalDate = LocalDate.now(),
    ): TipTrackerStats {
        val currentTips = tips.filter { it.localDate() <= today }

        return TipTrackerStats(
            weeklyTotal = calculateTotal(currentTips, TipStatisticsPeriod.WEEK, today),
            monthlyTotal = calculateTotal(currentTips, TipStatisticsPeriod.MONTH, today),
            yearlyTotal = calculateTotal(currentTips, TipStatisticsPeriod.YEAR, today),
            total = calculateTotal(currentTips, TipStatisticsPeriod.ALL, today),
            weeklyAverage = calculateAverage(currentTips, TipStatisticsPeriod.WEEK, today),
            monthlyAverage = calculateAverage(currentTips, TipStatisticsPeriod.MONTH, today),
            yearlyAverage = calculateAverage(currentTips, TipStatisticsPeriod.YEAR, today),
            totalAverage = calculateAverage(currentTips, TipStatisticsPeriod.ALL, today),
            topMonths = calculateTopMonths(currentTips),
            topDays = calculateTopDays(currentTips),
            bestWeekdays = calculateBestWeekdays(currentTips),
            monthlyTrend = calculateMonthlyTrend(currentTips, today),
            periodComparisons = calculatePeriodComparisons(currentTips, today),
        )
    }

    fun filterTipsByPeriod(
        tips: List<TipItem>,
        period: TipStatisticsPeriod,
        today: LocalDate = LocalDate.now(),
    ): List<TipItem> {
        val currentTips = tips.filter { it.localDate() <= today }
        if (period == TipStatisticsPeriod.ALL) return currentTips

        val range = currentPeriodRange(period, today)
        return currentTips.filter { it.localDate() in range }
    }

    private fun calculateTotal(
        tips: List<TipItem>,
        period: TipStatisticsPeriod,
        today: LocalDate,
    ): Float = filterTipsByPeriod(tips, period, today)
        .sumOf { it.amount.toDouble() }
        .toFloat()

    private fun calculateAverage(
        tips: List<TipItem>,
        period: TipStatisticsPeriod,
        today: LocalDate,
    ): Float {
        val filteredTips = filterTipsByPeriod(tips, period, today)
        return if (filteredTips.isNotEmpty()) {
            (filteredTips.sumOf { it.amount.toDouble() } / filteredTips.size).toFloat().roundedTipValue()
        } else {
            0f
        }
    }

    private fun calculateTopMonths(
        tips: List<TipItem>,
        limit: Int = 3,
    ): List<TipTotalSummary> {
        return tips.groupBy { YearMonth.from(it.localDate()) }
            .map { (month, monthTips) ->
                TipTotalSummary(
                    label = month.format(monthFormatter),
                    total = monthTips.sumTipAmount(),
                )
            }
            .sortedByDescending { it.total }
            .take(limit)
    }

    private fun calculateTopDays(tips: List<TipItem>): List<TipTotalSummary> {
        return tips.groupBy { it.localDate() }
            .map { (date, dayTips) ->
                TipTotalSummary(
                    label = date.format(dayFormatter),
                    total = dayTips.sumTipAmount(),
                )
            }
            .sortedByDescending { it.total }
            .take(3)
    }

    private fun calculateBestWeekdays(tips: List<TipItem>): List<TipTotalSummary> {
        return tips.groupBy { it.localDate().dayOfWeek }
            .map { (weekday, weekdayTips) ->
                TipTotalSummary(
                    label = weekday.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    total = weekdayTips
                        .groupBy { it.localDate() }
                        .values
                        .map { it.sumTipAmount() }
                        .average()
                        .toFloat()
                        .roundedTipValue(),
                )
            }
            .sortedByDescending { it.total }
            .take(3)
    }

    private fun calculateMonthlyTrend(
        tips: List<TipItem>,
        today: LocalDate,
    ): List<TipTotalSummary> {
        val currentMonth = YearMonth.from(today)
        val totalsByMonth = tips.groupBy { YearMonth.from(it.localDate()) }
            .mapValues { (_, monthTips) -> monthTips.sumTipAmount() }

        return (5 downTo 0).map { offset ->
            val month = currentMonth.minusMonths(offset.toLong())
            TipTotalSummary(
                label = month.format(monthFormatter),
                total = totalsByMonth[month] ?: 0f,
            )
        }
    }

    private fun calculatePeriodComparisons(
        tips: List<TipItem>,
        today: LocalDate,
    ): Map<TipStatisticsPeriod, TipPeriodComparison> {
        return listOf(
            TipStatisticsPeriod.WEEK,
            TipStatisticsPeriod.MONTH,
            TipStatisticsPeriod.YEAR,
        ).associateWith { period ->
            val currentRange = currentPeriodRange(period, today)
            val previousRange = previousPeriodRange(period, currentRange)
            val currentTotal = tips.filter { it.localDate() in currentRange }.sumTipAmount()
            val previousTotal = tips.filter { it.localDate() in previousRange }.sumTipAmount()
            val difference = currentTotal - previousTotal
            val differencePercent = if (previousTotal > 0f) {
                ((difference / previousTotal) * 100f).roundedTipValue()
            } else {
                null
            }

            TipPeriodComparison(
                currentTotal = currentTotal,
                previousTotal = previousTotal,
                difference = difference,
                differencePercent = differencePercent,
            )
        }
    }

    private fun currentPeriodRange(
        period: TipStatisticsPeriod,
        today: LocalDate,
    ): ClosedRange<LocalDate> {
        val startDate = when (period) {
            TipStatisticsPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            TipStatisticsPeriod.MONTH -> today.with(TemporalAdjusters.firstDayOfMonth())
            TipStatisticsPeriod.YEAR -> today.with(TemporalAdjusters.firstDayOfYear())
            TipStatisticsPeriod.ALL -> LocalDate.MIN
        }

        return startDate..today
    }

    private fun previousPeriodRange(
        period: TipStatisticsPeriod,
        currentRange: ClosedRange<LocalDate>,
    ): ClosedRange<LocalDate> {
        val previousStart = when (period) {
            TipStatisticsPeriod.WEEK -> currentRange.start.minusWeeks(1)
            TipStatisticsPeriod.MONTH -> currentRange.start.minusMonths(1)
            TipStatisticsPeriod.YEAR -> currentRange.start.minusYears(1)
            TipStatisticsPeriod.ALL -> LocalDate.MIN
        }
        val previousEnd = when (period) {
            TipStatisticsPeriod.WEEK -> previousStart.plusDays(6)
            TipStatisticsPeriod.MONTH -> previousStart.with(TemporalAdjusters.lastDayOfMonth())
            TipStatisticsPeriod.YEAR -> previousStart.with(TemporalAdjusters.lastDayOfYear())
            TipStatisticsPeriod.ALL -> LocalDate.MIN
        }

        return previousStart..previousEnd
    }

    private fun TipItem.localDate(): LocalDate =
        date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    private fun List<TipItem>.sumTipAmount(): Float =
        sumOf { it.amount.toDouble() }.toFloat()
}

fun formatTipTotal(total: Float): String {
    return total
        .toBigDecimal()
        .setScale(2, RoundingMode.HALF_DOWN)
        .stripTrailingZeros()
        .toPlainString()
}

private fun Float.roundedTipValue(): Float {
    return toBigDecimal()
        .setScale(2, RoundingMode.HALF_DOWN)
        .toFloat()
}
