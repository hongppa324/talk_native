package com.okcanvas.rnnative.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object KDate {
    private val parser = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.KOREA)
    private val days = arrayOf("월","화","수","목","금","토","일") // dayOfWeek 1=월 ... 7=일 (ISO)

    fun formatPretty(yyyyMMdd: String): String {
        return try {
            val d = LocalDate.parse(yyyyMMdd, parser)
            val dow = days[d.dayOfWeek.value - 1]
            "${d.monthValue}월 ${d.dayOfMonth}일 $dow"
        } catch (e: Exception) {
            yyyyMMdd
        }
    }
}
