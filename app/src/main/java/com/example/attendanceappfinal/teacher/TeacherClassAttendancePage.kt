package com.example.attendanceappfinal.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.PreStudent
import com.example.attendanceappfinal.model.Timetable
import com.example.attendanceappfinal.model.UnregisteredStudent
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.repository.AttendanceRepository
import com.example.attendanceappfinal.ui.StudentTypeBadge
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Attendance management for one class opened from a teacher timetable entry. */
@Composable
fun TeacherClassAttendancePage(
    timetable: Timetable,
    teacherName: String,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val database = FirebaseDatabase.getInstance()
    var students by remember { mutableStateOf(emptyList<User>()) }
    val statusMap = remember { mutableStateMapOf<String, String>() }
    val reasonMap = remember { mutableStateMapOf<String, String>() }
    var message by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var sendParentNotifications by remember { mutableStateOf(false) }

    fun isInTimetableClass(grade: String, className: String): Boolean =
        className.trim() == timetable.className.trim() &&
            (timetable.grade.isBlank() || normalizeGrade(grade) == normalizeGrade(timetable.grade))

    fun loadStudents() {
        database.getReference("users").get().addOnSuccessListener { userSnapshot ->
            val registered = userSnapshot.children.mapNotNull { child ->
                child.getValue(User::class.java)?.copy(uid = child.key ?: "")
            }.filter { it.role == "student" && isInTimetableClass(it.grade, it.className) }

            database.getReference("preStudents").get().addOnSuccessListener { preSnapshot ->
                val preStudents = preSnapshot.children.mapNotNull { child ->
                    child.getValue(PreStudent::class.java)?.copy(id = child.key ?: "")
                }.filter { isInTimetableClass(it.grade, it.className) }
                    .map {
                        User(
                            uid = "pre_${it.id}", name = it.name, phone = it.phone,
                            grade = it.grade, className = it.className, role = "student",
                            isPreStudent = true, preStudentId = it.id
                        )
                    }

            database.getReference("unregisteredStudents").get().addOnSuccessListener { unregisteredSnapshot ->
                val unregistered = unregisteredSnapshot.children.mapNotNull { child ->
                    child.getValue(UnregisteredStudent::class.java)?.copy(id = child.key ?: "")
                }.filter { isInTimetableClass(it.grade, it.className) }
                    .map {
                        User(
                            uid = "un_${it.id}",
                            name = it.name,
                            phone = it.phone,
                            grade = it.grade,
                            className = it.className,
                            role = "student",
                            isUnregisteredStudent = true,
                            unregisteredStudentId = it.id
                        )
                    }

                students = (registered + preStudents + unregistered)
                    .filter { isInTimetableClass(it.grade, it.className) }
                    .sortedBy { it.name }
                students.forEach { student ->
                    if (statusMap[student.uid] == null) statusMap[student.uid] = "출석"
                }
            }.addOnFailureListener {
                message = "미가입 학생 목록을 불러오지 못했습니다."
                students = registered + preStudents
            }
            }.addOnFailureListener {
                message = "가입 예정 학생 목록을 불러오지 못했습니다."
                database.getReference("unregisteredStudents").get().addOnSuccessListener { unregisteredSnapshot ->
                    val unregistered = unregisteredSnapshot.children.mapNotNull { child ->
                        child.getValue(UnregisteredStudent::class.java)?.copy(id = child.key ?: "")
                    }.filter { isInTimetableClass(it.grade, it.className) }.map {
                        User(uid = "un_${it.id}", name = it.name, phone = it.phone, grade = it.grade,
                            className = it.className, role = "student", isUnregisteredStudent = true,
                            unregisteredStudentId = it.id)
                    }
                    students = (registered + unregistered).sortedBy { it.name }
                }
            }
        }.addOnFailureListener {
            message = "학생 목록을 불러오지 못했습니다."
        }
    }

    fun saveAttendance() {
        if (students.isEmpty()) {
            message = "이 수업에 등록된 학생이 없습니다."
            return
        }
        saving = true
        val now = Date()
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        var remaining = students.size
        var failed = 0
        students.forEach { student ->
            val attendance = Attendance(
                id = "${date}_${student.uid}_${timetable.subject}",
                studentUid = student.uid,
                studentName = student.name.ifBlank { "이름 없음" },
                subject = timetable.subject,
                teacher = teacherName,
                teacherUid = timetable.teacherUid,
                date = date,
                time = time,
                status = statusMap[student.uid] ?: "출석",
                reason = reasonMap[student.uid].orEmpty(),
                timestamp = System.currentTimeMillis()
            )
            AttendanceRepository.saveAttendance(
                attendance = attendance,
                onSuccess = {
                    if (sendParentNotifications && !student.isPreStudent && !student.isUnregisteredStudent) {
                        saveParentAttendanceNotification(database, attendance)
                    }
                    remaining--
                    if (remaining == 0) {
                        saving = false
                        message = if (failed == 0) "${students.size}명의 출결을 저장했습니다."
                        else "${students.size - failed}명 저장, ${failed}명 저장 실패"
                    }
                },
                onFail = {
                    failed++
                    remaining--
                    if (remaining == 0) {
                        saving = false
                        message = "${students.size - failed}명 저장, ${failed}명 저장 실패"
                    }
                }
            )
        }
    }

    LaunchedEffect(timetable.id, timetable.grade, timetable.className) {
        // A single composable instance is reused when the teacher opens another
        // lesson. Clear the previous class immediately so stale students can
        // never be submitted or shown for the newly selected timetable.
        students = emptyList()
        statusMap.clear()
        reasonMap.clear()
        message = ""
        loadStudents()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = UiConfig.topPadding, start = UiConfig.sidePadding,
            end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
        )
    ) {
        Text("수업 출결 관리", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("${timetable.grade.ifBlank { "학년 미지정" }} ${timetable.className} · ${timetable.subject}", style = MaterialTheme.typography.titleLarge)
                Text("${timetable.day} ${timetable.startTime} ~ ${timetable.endTime}")
                Text("가입 완료 학생과 앱 미사용 학생을 함께 표시합니다.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth()) {
            Checkbox(
                checked = sendParentNotifications,
                onCheckedChange = { sendParentNotifications = it }
            )
            Column(Modifier.padding(top = 10.dp)) {
                Text("학부모에게 출결 알림 보내기")
                Text(
                    "체크한 경우에만 가입 완료 학생의 학부모 알림함에 저장됩니다.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("수강 학생 ${students.size}명", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        students.forEach { student ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(student.name.ifBlank { "이름 없음" }, style = MaterialTheme.typography.titleMedium)
                            Text("${student.grade} · ${student.className}", style = MaterialTheme.typography.bodySmall)
                        }
                        StudentTypeBadge(student)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("출석", "지각", "결석").forEach { status ->
                            Button(onClick = { statusMap[student.uid] = status }) {
                                Text(if (statusMap[student.uid] == status) "✓ $status" else status)
                            }
                        }
                    }
                    if ((statusMap[student.uid] ?: "출석") != "출석") {
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = reasonMap[student.uid].orEmpty(),
                            onValueChange = { reasonMap[student.uid] = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("지각·결석 사유 (선택)") },
                            placeholder = { Text("예: 병원 진료, 교통 지연") }
                        )
                    }
                }
            }
        }

        if (students.isEmpty()) Text("이 반에 배정된 학생이 없습니다.")
        Spacer(Modifier.height(16.dp))
        Button(modifier = Modifier.fillMaxWidth(), enabled = !saving, onClick = { saveAttendance() }) {
            Text(if (saving) "저장 중..." else "출결 저장")
        }
        if (message.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(message) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onBack) { Text("시간표로 돌아가기") }
    }
}

private fun normalizeGrade(value: String): String =
    value.replace(" ", "").replace("학년", "").trim()

private fun saveParentAttendanceNotification(database: FirebaseDatabase, attendance: Attendance) {
    val ref = database.getReference("parentNotifications").child(attendance.studentUid).push()
    val status = attendance.status.ifBlank { "출석" }
    val title = "출결 안내"
    val message = "${attendance.studentName} 학생이 ${attendance.date} ${attendance.time} ${attendance.subject} 수업에 $status 처리되었습니다."
    ref.setValue(
        mapOf(
            "id" to (ref.key ?: ""),
            "studentUid" to attendance.studentUid,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "read" to false
        )
    )
}
