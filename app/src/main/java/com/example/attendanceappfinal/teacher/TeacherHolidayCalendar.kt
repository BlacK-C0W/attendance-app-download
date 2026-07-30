package com.example.attendanceappfinal.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.model.Holiday
import com.example.attendanceappfinal.repository.KoreanHolidayRepository
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun TeacherHolidayCalendar() {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var customHolidays by remember { mutableStateOf(emptyList<Holiday>()) }
    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().getReference("holidays").get().addOnSuccessListener { snapshot ->
            customHolidays = snapshot.children.mapNotNull { child ->
                child.getValue(Holiday::class.java)?.let { holiday ->
                    if (holiday.date.isBlank()) holiday.copy(date = child.key ?: "") else holiday
                }
            }
        }
    }
    val publicDates = KoreanHolidayRepository.publicHolidays(month.year)
        .flatMap { teacherHolidayDates(it.date, it.endDate.ifBlank { it.date }) }.toSet() -
        customHolidays.filter { it.type == "publicHolidayClassDay" }.map { it.date }.toSet()
    val vacationDates = customHolidays.filter { it.type == "academyVacation" }
        .flatMap { teacherHolidayDates(it.date, it.endDate.ifBlank { it.date }) }.toSet()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("휴일 달력", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹") }
                Text("${month.year}년 ${month.monthValue}월", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { month = month.plusMonths(1) }) { Text("›") }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(day, style = MaterialTheme.typography.bodySmall) }
                }
            }
            val firstOffset = month.atDay(1).dayOfWeek.value % 7
            repeat(6) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { weekday ->
                        val number = week * 7 + weekday - firstOffset + 1
                        Box(Modifier.weight(1f).height(34.dp), contentAlignment = Alignment.Center) {
                            if (number in 1..month.lengthOfMonth()) {
                                val date = "%04d-%02d-%02d".format(month.year, month.monthValue, number)
                                val color = when {
                                    date in vacationDates -> Color(0xFFE8DEF8)
                                    date in publicDates -> Color(0xFFFFDAD6)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                Surface(color = color, shape = MaterialTheme.shapes.small) {
                                    Box(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), contentAlignment = Alignment.Center) { Text(number.toString()) }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("연한 빨간색: 공휴일 · 보라색: 학원 휴가", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun teacherHolidayDates(start: String, end: String): List<String> = try {
    val last = LocalDate.parse(end)
    generateSequence(LocalDate.parse(start)) { date -> date.plusDays(1).takeIf { !it.isAfter(last) } }
        .map { it.toString() }.toList()
} catch (_: Exception) { emptyList() }
