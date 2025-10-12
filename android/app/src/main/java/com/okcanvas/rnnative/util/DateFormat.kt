package com.okcanvas.rnnative.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val korea = Locale.KOREAN
private val iso = DateTimeFormatter.ISO_LOCAL_DATE
private val pretty = DateTimeFormatter.ofPattern("M월 d일 E", korea)

/** "2025-10-04" -> "10월 4일 토" */
fun formatKoreanDate(isoDate: String): String =
    LocalDate.parse(isoDate, iso).format(pretty)
