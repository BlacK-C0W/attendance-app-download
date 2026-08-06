package com.example.attendanceappfinal.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.Timetable
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private data class ClassOption(val grade: String, val className: String) {
    val label get() = "${grade.ifBlank { "학년 미지정" }} $className"
}

private data class DailyAttendanceSummary(val present: Int = 0, val late: Int = 0, val absent: Int = 0)

@Composable
fun TeacherClassAttendanceCalendarPage(teacherUid: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val database = FirebaseDatabase.getInstance()
    var classes by remember { mutableStateOf(emptyList<ClassOption>()) }
    var selectedClass by remember { mutableStateOf<ClassOption?>(null) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var summaries by remember { mutableStateOf(emptyMap<String, DailyAttendanceSummary>()) }
    var message by remember { mutableStateOf("") }

    fun loadAttendance() {
        val target = selectedClass ?: return
        database.getReference("users").get().addOnSuccessListener { usersSnapshot ->
            val students = usersSnapshot.children.mapNotNull { child ->
                child.getValue(User::class.java)?.copy(uid = child.key ?: "")
            }.filter { it.role == "student" && it.grade == target.grade && it.className == target.className }
                .associateBy { it.uid }
            database.getReference("attendance").get().addOnSuccessListener { attendanceSnapshot ->
                val counts = mutableMapOf<String, DailyAttendanceSummary>()
                attendanceSnapshot.children.forEach { studentNode ->
                    studentNode.children.forEach { recordNode ->
                        val record = recordNode.getValue(Attendance::class.java) ?: return@forEach
                        if (record.teacherUid != teacherUid || !students.containsKey(record.studentUid) || !record.date.startsWith(month.toString())) return@forEach
                        val old = counts[record.date] ?: DailyAttendanceSummary()
                        counts[record.date] = when (record.status) {
                            "지각" -> old.copy(late = old.late + 1)
                            "결석" -> old.copy(absent = old.absent + 1)
                            else -> old.copy(present = old.present + 1)
                        }
                    }
                }
                summaries = counts
                message = if (students.isEmpty()) "이 반에 가입 완료 학생이 없습니다." else ""
            }.addOnFailureListener { message = "출결 기록을 불러오지 못했습니다." }
        }.addOnFailureListener { message = "학생 정보를 불러오지 못했습니다." }
    }

    LaunchedEffect(teacherUid) {
        database.getReference("teacherTimetable").child(teacherUid).get().addOnSuccessListener { snapshot ->
            classes = snapshot.children.mapNotNull { it.getValue(Timetable::class.java) }
                .map { ClassOption(it.grade, it.className) }
                .filter { it.className.isNotBlank() }
                .distinct()
            selectedClass = classes.firstOrNull()
        }.addOnFailureListener { message = "수업 반 정보를 불러오지 못했습니다." }
    }

    LaunchedEffect(selectedClass, month) { if (selectedClass != null) loadAttendance() }

    Column(Modifier.fillMaxSize().padding(top = UiConfig.topPadding, start = UiConfig.sidePadding, end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding)) {
        Text("반별 출석 현황", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        if (classes.isEmpty()) Text("등록된 수업 반이 없습니다.")
        classes.forEach { option ->
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { selectedClass = option },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selectedClass == option) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) { Text(option.label) }
        }
        if (selectedClass != null) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { month = month.minusMonths(1) }) { Text("‹ 이전") }
                Text(month.format(DateTimeFormatter.ofPattern("yyyy년 M월")), style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { month = month.plusMonths(1) }) { Text("다음 ›") }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) { listOf("일", "월", "화", "수", "목", "금", "토").forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium) } }
            val leading = month.atDay(1).dayOfWeek.value % 7
            val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { it }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f).padding(2.dp).height(74.dp)) {
                            if (day != null) {
                                val summary = summaries[month.atDay(day).toString()]
                                Column {
                                    Text(day.toString(), style = MaterialTheme.typography.labelLarge)
                                    summary?.let {
                                        Text("출 ${it.present}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                        Text("지 ${it.late} · 결 ${it.absent}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("출: 출석 · 지: 지각 · 결: 결석", style = MaterialTheme.typography.bodySmall)
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.weight(1f))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onBack) { Text("뒤로가기") }
    }
}
