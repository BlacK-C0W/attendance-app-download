package com.example.attendanceappfinal.util

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.*

@Composable
fun rememberCurrentTime(): String {

    val timeFlow = remember {

        flow {

            while (true) {

                emit(getCurrentTime())

                delay(1000)

            }

        }

    }

    return timeFlow.collectAsState(
        initial = getCurrentTime()
    ).value
}

fun getCurrentTime(): String {

    return SimpleDateFormat(
        "yyyy년 MM월 dd일\nHH시 mm분 ss초",
        Locale.KOREA
    ).format(Date())

}

fun getToday(): String {

    val cal = Calendar.getInstance(
        TimeZone.getTimeZone("Asia/Seoul")
    )

    return when(
        cal.get(Calendar.DAY_OF_WEEK)
    ){

        Calendar.SUNDAY -> "일"
        Calendar.MONDAY -> "월"
        Calendar.TUESDAY -> "화"
        Calendar.WEDNESDAY -> "수"
        Calendar.THURSDAY -> "목"
        Calendar.FRIDAY -> "금"
        Calendar.SATURDAY -> "토"

        else -> ""

    }

}

fun getClassStatus(start: String): String {

    return try {

        val now = Calendar.getInstance()

        val parts = start.split(":")

        val startMinute =
            parts[0].toInt() * 60 + parts[1].toInt()

        val nowMinute =
            now.get(Calendar.HOUR_OF_DAY) * 60 +
                    now.get(Calendar.MINUTE)

        val diff = nowMinute - startMinute

        when {

            diff < -30 -> "수업 전"

            diff <= 10 -> "출석 가능"

            diff < 90 -> "지각 가능"

            else -> "결석 처리 예정"

        }

    } catch (e: Exception) {

        "시간 오류"

    }

}