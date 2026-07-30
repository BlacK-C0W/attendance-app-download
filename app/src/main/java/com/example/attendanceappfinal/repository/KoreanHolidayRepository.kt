package com.example.attendanceappfinal.repository

import com.example.attendanceappfinal.model.Holiday

/** Built-in Korean public holidays. Custom academy closures stay in Firebase. */
object KoreanHolidayRepository {
    fun publicHolidays(year: Int): List<Holiday> {
        val fixed = listOf(
            "$year-01-01" to "신정", "$year-03-01" to "삼일절",
            "$year-05-05" to "어린이날", "$year-06-06" to "현충일",
            "$year-08-15" to "광복절", "$year-10-03" to "개천절",
            "$year-10-09" to "한글날", "$year-12-25" to "성탄절"
        ).map { (date, name) -> Holiday(date = date, endDate = date, name = name, type = "public") }

        val movable = when (year) {
            2026 -> listOf(
                Holiday("2026-02-16", "2026-02-18", "설날 연휴", "public"),
                Holiday("2026-03-02", "2026-03-02", "삼일절 대체공휴일", "public"),
                Holiday("2026-05-24", "2026-05-24", "부처님오신날", "public"),
                Holiday("2026-05-25", "2026-05-25", "부처님오신날 대체공휴일", "public"),
                Holiday("2026-06-03", "2026-06-03", "전국동시지방선거", "public"),
                Holiday("2026-08-17", "2026-08-17", "광복절 대체공휴일", "public"),
                Holiday("2026-09-24", "2026-09-26", "추석 연휴", "public"),
                Holiday("2026-10-05", "2026-10-05", "개천절 대체공휴일", "public")
            )
            else -> emptyList()
        }
        return (fixed + movable).distinctBy { it.date to it.name }.sortedBy { it.date }
    }
}
