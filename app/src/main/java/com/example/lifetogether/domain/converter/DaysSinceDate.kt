package com.example.lifetogether.domain.converter

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

fun daysSinceDate(date: Date): Long {
    val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val currentDate = LocalDate.now()
    return ChronoUnit.DAYS.between(localDate, currentDate)
}
