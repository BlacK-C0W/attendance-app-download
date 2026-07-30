package com.example.attendanceappfinal.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.Holiday
import com.example.attendanceappfinal.repository.KoreanHolidayRepository
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HolidayManagePage(onBack: () -> Unit) {
    BackHandler { onBack() }
    val database = FirebaseDatabase.getInstance()
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var holidays by remember { mutableStateOf(emptyList<Holiday>()) }
    var message by remember { mutableStateOf("") }
    var publicHolidaysExpanded by remember { mutableStateOf(false) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }

    fun selectRangeDate(value: String) {
        when {
            startDate.isBlank() || endDate.isNotBlank() -> {
                startDate = value
                endDate = ""
            }
            value >= startDate -> endDate = value
            else -> {
                endDate = startDate
                startDate = value
            }
        }
    }

    fun load() {
        database.getReference("holidays").get().addOnSuccessListener { snapshot ->
            holidays = snapshot.children.mapNotNull { child ->
                child.getValue(Holiday::class.java)?.let { holiday ->
                    if (holiday.date.isBlank()) holiday.copy(date = child.key ?: "") else holiday
                }
            }.sortedBy { it.date }
        }.addOnFailureListener { message = "휴일 목록을 불러오지 못했습니다." }
    }
    fun save() {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(startDate) ||
            !Regex("\\d{4}-\\d{2}-\\d{2}").matches(endDate) || name.isBlank() || endDate < startDate) {
            message = "시작일·종료일(YYYY-MM-DD)과 휴가명을 확인하세요."
            return
        }
        database.getReference("holidays").child(startDate).setValue(
            Holiday(startDate, endDate, name.trim(), "academyVacation")
        ).addOnSuccessListener { message = "학원 휴가를 저장했습니다."; name = ""; load() }
            .addOnFailureListener { message = "저장 실패: ${it.message}" }
    }

    LaunchedEffect(Unit) { load() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = UiConfig.topPadding, start = UiConfig.sidePadding,
            end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
        ), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("휴일 · 학원 휴가 관리", style = MaterialTheme.typography.headlineMedium)
        Text("대한민국 공휴일은 자동 적용됩니다. 아래에는 학원 휴가 기간만 등록하세요.")
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("휴가 기간 선택", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (startDate.isBlank()) "시작일을 선택하세요."
                    else if (endDate.isBlank()) "종료일을 선택하세요: $startDate"
                    else "$startDate ~ $endDate"
                )
                VacationRangeCalendar(
                    month = calendarMonth,
                    startDate = startDate,
                    endDate = endDate,
                    academyVacations = holidays,
                    onPreviousMonth = { calendarMonth = calendarMonth.minusMonths(1) },
                    onNextMonth = { calendarMonth = calendarMonth.plusMonths(1) },
                    onDateClick = { selectRangeDate(it) }
                )
                Text(
                    "연한 빨간색은 공휴일, 보라색은 등록된 학원 휴가입니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("휴가명 (예: 여름방학)") })
                Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text("학원 휴가 저장") }
            }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
        val publicHolidays = KoreanHolidayRepository.publicHolidays(LocalDate.now().year)
            .filter { !LocalDate.parse(it.endDate.ifBlank { it.date }).isBefore(LocalDate.now()) }
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("대한민국 공휴일", style = MaterialTheme.typography.titleLarge)
                    Text("${LocalDate.now().year}년 ${publicHolidays.size}건")
                }
                TextButton(onClick = { publicHolidaysExpanded = !publicHolidaysExpanded }) {
                    Text(if (publicHolidaysExpanded) "접기" else "펼치기")
                }
            }
        }
        if (publicHolidaysExpanded) {
            publicHolidays.forEach { holiday ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(holiday.name, style = MaterialTheme.typography.titleMedium)
                        Text(if (holiday.date == holiday.endDate) holiday.date else "${holiday.date} ~ ${holiday.endDate}")
                        val isClassDayException = holidays.any {
                            it.type == "publicHolidayClassDay" && it.date == holiday.date
                        }
                        TextButton(onClick = {
                            val ref = database.getReference("holidays").child("exception_${holiday.date}")
                            if (isClassDayException) {
                                ref.removeValue().addOnSuccessListener { load() }
                            } else {
                                ref.setValue(Holiday(holiday.date, holiday.date, holiday.name, "publicHolidayClassDay"))
                                    .addOnSuccessListener { message = "공휴일 수업일 예외를 저장했습니다."; load() }
                            }
                        }) {
                            Text(if (isClassDayException) "수업일 예외 해제" else "이 날 수업 진행")
                        }
                    }
                }
            }
        }
        Text("학원 휴가", style = MaterialTheme.typography.titleLarge)
        holidays.filter { it.type == "academyVacation" }.forEach { holiday ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(holiday.name, style = MaterialTheme.typography.titleMedium); Text(if (holiday.endDate.isBlank() || holiday.date == holiday.endDate) holiday.date else "${holiday.date} ~ ${holiday.endDate}") }
                    TextButton(onClick = {
                        database.getReference("holidays").child(holiday.date).removeValue()
                            .addOnSuccessListener { message = "휴일을 삭제했습니다."; load() }
                    }) { Text("삭제") }
                }
            }
        }
    }
}

@Composable
private fun VacationRangeCalendar(
    month: YearMonth,
    startDate: String,
    endDate: String,
    academyVacations: List<Holiday>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onPreviousMonth) { Text("‹") }
            Text("${month.year}년 ${month.monthValue}월", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onNextMonth) { Text("›") }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(day) }
            }
        }
        val firstOffset = month.atDay(1).dayOfWeek.value % 7
        val days = month.lengthOfMonth()
        val publicHolidayDates = KoreanHolidayRepository.publicHolidays(month.year)
            .flatMap { holiday ->
                val end = LocalDate.parse(holiday.endDate.ifBlank { holiday.date })
                generateSequence(LocalDate.parse(holiday.date)) { date ->
                    date.plusDays(1).takeIf { !it.isAfter(end) }
                }.map { it.toString() }.toList()
            }.toSet() - academyVacations.filter { it.type == "publicHolidayClassDay" }.map { it.date }.toSet()
        val academyVacationDates = academyVacations.filter { it.type == "academyVacation" }.flatMap { holiday ->
            val end = LocalDate.parse(holiday.endDate.ifBlank { holiday.date })
            generateSequence(LocalDate.parse(holiday.date)) { date ->
                date.plusDays(1).takeIf { !it.isAfter(end) }
            }.map { it.toString() }.toList()
        }.toSet()
        repeat(6) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { weekday ->
                    val number = week * 7 + weekday - firstOffset + 1
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (number in 1..days) {
                            val date = "%04d-%02d-%02d".format(month.year, month.monthValue, number)
                            val selected = date == startDate || date == endDate
                            val inRange = startDate.isNotBlank() && endDate.isNotBlank() && date > startDate && date < endDate
                            val isPublicHoliday = date in publicHolidayDates
                            val isAcademyVacation = date in academyVacationDates
                            TextButton(
                                onClick = { onDateClick(date) },
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = when {
                                        selected -> MaterialTheme.colorScheme.primary
                                        inRange -> MaterialTheme.colorScheme.primaryContainer
                                        isPublicHoliday -> Color(0xFFFFDAD6)
                                        isAcademyVacation -> Color(0xFFE8DEF8)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    contentColor = when {
                                        selected -> MaterialTheme.colorScheme.onPrimary
                                        isPublicHoliday -> Color(0xFF410002)
                                        isAcademyVacation -> Color(0xFF322F3A)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text(number.toString()) }
                        }
                    }
                }
            }
        }
    }
}

