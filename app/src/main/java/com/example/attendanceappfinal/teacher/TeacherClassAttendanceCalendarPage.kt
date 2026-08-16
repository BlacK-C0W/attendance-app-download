package com.example.attendanceappfinal.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.PreStudent
import com.example.attendanceappfinal.model.Timetable
import com.example.attendanceappfinal.model.UnregisteredStudent
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.repository.attendanceStoragePath
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private data class ClassOption(val grade: String = "", val className: String = "") {
    val isAll get() = grade.isBlank() && className.isBlank()
    val label get() = if (isAll) "전체 학년 출석 현황" else "${grade.ifBlank { "학년 미지정" }} ${className}"
}
private data class DailyAttendanceSummary(val late: Int = 0, val absent: Int = 0)

private fun isLessonOnDate(day: String, date: String): Boolean {
    val weekday = when (java.time.LocalDate.parse(date).dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "월"
        java.time.DayOfWeek.TUESDAY -> "화"
        java.time.DayOfWeek.WEDNESDAY -> "수"
        java.time.DayOfWeek.THURSDAY -> "목"
        java.time.DayOfWeek.FRIDAY -> "금"
        java.time.DayOfWeek.SATURDAY -> "토"
        java.time.DayOfWeek.SUNDAY -> "일"
    }
    return day.trim() == weekday || day.trim() == "${weekday}요일"
}

@Composable
fun TeacherClassAttendanceCalendarPage(teacherUid: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val database = FirebaseDatabase.getInstance()
    val gradeOrder = listOf("초1", "초2", "초3", "초4", "초5", "초6", "중1", "중2", "중3", "고1", "고2", "고3")
    var classes by remember { mutableStateOf(listOf(ClassOption())) }
    var selectedClass by remember { mutableStateOf(ClassOption()) }
    var classExpanded by remember { mutableStateOf(false) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var summaries by remember { mutableStateOf(emptyMap<String, DailyAttendanceSummary>()) }
    var recordsByDate by remember { mutableStateOf(emptyMap<String, List<Attendance>>()) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var studentsById by remember { mutableStateOf(emptyMap<String, User>()) }
    var teacherName by remember { mutableStateOf("") }
    var teacherLessons by remember { mutableStateOf(emptyList<Timetable>()) }
    var showAbsenceDialog by remember { mutableStateOf(false) }
    var selectedAbsentStudentIds by remember { mutableStateOf(emptySet<String>()) }
    var savingAbsences by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun readRecords(snapshot: DataSnapshot, students: Map<String, User>, counts: MutableMap<String, DailyAttendanceSummary>, records: MutableMap<String, MutableList<Attendance>>) {
        snapshot.children.forEach { studentNode -> studentNode.children.forEach { recordNode ->
            val record = recordNode.getValue(Attendance::class.java) ?: return@forEach
            val student = students[record.studentUid] ?: return@forEach
            val matchesClass = selectedClass.isAll || (student.grade == selectedClass.grade && student.className == selectedClass.className)
            // Legacy attendance records keep the former teacher UID after an account
            // reissue. The teacher name is preserved, so accept it as a migration bridge.
            val belongsToTeacher = record.teacherUid == teacherUid ||
                (teacherName.isNotBlank() && record.teacher == teacherName)
            if (!belongsToTeacher || !matchesClass || !record.date.startsWith(month.toString())) return@forEach
            val old = counts[record.date] ?: DailyAttendanceSummary()
            when (record.status) {
                "지각" -> counts[record.date] = old.copy(late = old.late + 1)
                "결석" -> counts[record.date] = old.copy(absent = old.absent + 1)
            }
            records.getOrPut(record.date) { mutableListOf() }.add(record)
        } }
    }

    fun loadAttendance() {
        database.getReference("users").get().addOnSuccessListener { usersSnapshot ->
            teacherName = usersSnapshot.child(teacherUid)
                .getValue(User::class.java)?.name.orEmpty()
            val students = usersSnapshot.children.mapNotNull { it.getValue(User::class.java)?.copy(uid = it.key ?: "") }.filter { it.role == "student" }.associateBy { it.uid }.toMutableMap()
            database.getReference("unregisteredStudents").get().addOnSuccessListener { unSnapshot ->
                unSnapshot.children.mapNotNull { it.getValue(UnregisteredStudent::class.java)?.copy(id = it.key ?: "") }.forEach {
                    students["un_${it.id}"] = User(uid = "un_${it.id}", name = it.name, grade = it.grade, className = it.className, role = "student", isUnregisteredStudent = true, unregisteredStudentId = it.id)
                }
                database.getReference("preStudents").get().addOnSuccessListener { preSnapshot ->
                    preSnapshot.children.mapNotNull { it.getValue(PreStudent::class.java)?.copy(id = it.key ?: "") }.forEach {
                        students["pre_${it.id}"] = User(uid = "pre_${it.id}", name = it.name, grade = it.grade, className = it.className, role = "student", isPreStudent = true, preStudentId = it.id)
                    }
                    studentsById = students
                    val counts = mutableMapOf<String, DailyAttendanceSummary>(); val records = mutableMapOf<String, MutableList<Attendance>>()
                    database.getReference("attendance").get().addOnSuccessListener { normal ->
                        readRecords(normal, students, counts, records)
                        database.getReference("unregisteredAttendance").get().addOnSuccessListener { unregistered ->
                            readRecords(unregistered, students, counts, records)
                            database.getReference("preAttendance").get().addOnSuccessListener { pre ->
                                readRecords(pre, students, counts, records)
                                summaries = counts; recordsByDate = records
                            }.addOnFailureListener { summaries = counts; recordsByDate = records }
                        }.addOnFailureListener { summaries = counts; recordsByDate = records }
                    }.addOnFailureListener { message = "출결 기록을 불러오지 못했습니다." }
                }
            }
        }.addOnFailureListener { message = "학생 정보를 불러오지 못했습니다." }
    }

    fun saveAbsences(date: String) {
        if (selectedClass.isAll) {
            message = "결석 입력을 하려면 먼저 반을 선택하세요."
            return
        }
        val selectedStudents = studentsById.values.filter { it.uid in selectedAbsentStudentIds }
        if (selectedStudents.isEmpty()) {
            message = "결석 학생을 선택하세요."
            return
        }
        val subject = teacherLessons.firstOrNull {
            it.grade == selectedClass.grade && it.className == selectedClass.className && isLessonOnDate(it.day, date)
        }?.subject ?: "수동 결석 입력"
        val time = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        savingAbsences = true
        var remaining = selectedStudents.size
        selectedStudents.forEach { student ->
            val storagePath = attendanceStoragePath(student.uid)
            val recordId = "${date}_${student.uid}_${subject}"
            val attendance = Attendance(
                id = recordId,
                studentUid = student.uid,
                studentName = student.name,
                subject = subject,
                teacherUid = teacherUid,
                date = date,
                time = time,
                status = "결석",
                reason = "선생님 수동 입력",
                timestamp = System.currentTimeMillis()
            )
            database.getReference(storagePath.root).child(storagePath.studentId).child(recordId)
                .setValue(attendance)
                .addOnCompleteListener {
                    if (--remaining == 0) {
                        savingAbsences = false
                        showAbsenceDialog = false
                        selectedAbsentStudentIds = emptySet()
                        message = "결석 처리를 저장했습니다."
                        loadAttendance()
                    }
                }
        }
    }

    LaunchedEffect(teacherUid) {
        database.getReference("teacherTimetable").child(teacherUid).get().addOnSuccessListener { snapshot ->
            teacherLessons = snapshot.children.mapNotNull { child ->
                child.getValue(Timetable::class.java)?.copy(id = child.key ?: "")
            }
            val options = teacherLessons.map { ClassOption(it.grade, it.className) }.filter { it.className.isNotBlank() }.distinct()
                .sortedWith(compareBy<ClassOption> { gradeOrder.indexOf(it.grade).let { index -> if (index < 0) Int.MAX_VALUE else index } }.thenBy { it.className })
            classes = listOf(ClassOption()) + options
        }.addOnFailureListener { message = "수업 반 정보를 불러오지 못했습니다." }
    }
    LaunchedEffect(selectedClass, month) { selectedDate = null; loadAttendance() }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = UiConfig.topPadding, start = UiConfig.sidePadding, end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding)
    ) {
        Text("반별 출석 현황", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Box {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { classExpanded = true }) { Text(selectedClass.label) }
            DropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                classes.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { selectedClass = option; classExpanded = false }) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { month = month.minusMonths(1) }) { Text("‹ 이전") }
            Text(month.format(DateTimeFormatter.ofPattern("yyyy년 M월")), style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { month = month.plusMonths(1) }) { Text("다음 ›") }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) { listOf("일", "월", "화", "수", "목", "금", "토").forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium) } }
        val leading = month.atDay(1).dayOfWeek.value % 7
        (List(leading) { null } + (1..month.lengthOfMonth()).map { it }).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day -> Box(Modifier.weight(1f).padding(2.dp).height(74.dp).clickable(enabled = day != null) { selectedDate = day?.let { month.atDay(it).toString() } }) {
                    if (day != null) { val summary = summaries[month.atDay(day).toString()]; Column { Text(day.toString(), style = MaterialTheme.typography.labelLarge); summary?.let { if (it.late > 0) Text("지 ${it.late}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall); if (it.absent > 0) Text("결 ${it.absent}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) } } }
                } }; repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text("지: 지각 · 결: 결석 · 날짜를 누르면 상세 확인", style = MaterialTheme.typography.bodySmall)
        selectedDate?.let { date ->
            val exceptions = recordsByDate[date].orEmpty().filter { it.status == "지각" || it.status == "결석" }
            Spacer(Modifier.height(12.dp)); Text("$date 지각·결석 학생", style = MaterialTheme.typography.titleMedium)
            if (exceptions.isEmpty()) Text("지각·결석 학생이 없습니다.") else exceptions.forEach { record -> Text("${record.studentName} · ${record.status}${record.reason.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""}") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    if (selectedClass.isAll) message = "결석 입력을 하려면 먼저 반을 선택하세요."
                    else showAbsenceDialog = true
                }
            ) { Text("결석 입력") }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp)); OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onBack) { Text("뒤로가기") }
    }

    if (showAbsenceDialog) {
        val classStudents = studentsById.values
            .filter { it.grade == selectedClass.grade && it.className == selectedClass.className }
            .sortedBy { it.name }
        AlertDialog(
            onDismissRequest = { if (!savingAbsences) showAbsenceDialog = false },
            title = { Text("${selectedDate.orEmpty()} 결석 입력") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text("결석 학생을 선택하세요.")
                    classStudents.forEach { student ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = student.uid in selectedAbsentStudentIds,
                                onCheckedChange = { checked ->
                                    selectedAbsentStudentIds = if (checked) selectedAbsentStudentIds + student.uid
                                    else selectedAbsentStudentIds - student.uid
                                }
                            )
                            Text(student.name)
                        }
                    }
                    if (classStudents.isEmpty()) Text("이 반의 학생 정보를 불러오지 못했습니다.")
                }
            },
            confirmButton = {
                Button(
                    enabled = !savingAbsences && selectedAbsentStudentIds.isNotEmpty(),
                    onClick = { selectedDate?.let(::saveAbsences) }
                ) { Text(if (savingAbsences) "저장 중..." else "결석 저장") }
            },
            dismissButton = {
                TextButton(enabled = !savingAbsences, onClick = { showAbsenceDialog = false }) { Text("취소") }
            }
        )
    }
}
