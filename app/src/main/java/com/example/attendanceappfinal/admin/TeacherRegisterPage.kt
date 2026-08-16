package com.example.attendanceappfinal.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.Timetable
import com.example.attendanceappfinal.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

@Composable
fun TeacherRegisterPage(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val database = FirebaseDatabase.getInstance()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var loginId by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var legacyTeachers by remember { mutableStateOf(emptyList<User>()) }
    var selectedLegacyTeacher by remember { mutableStateOf<User?>(null) }
    var showLegacyTeacherPicker by remember { mutableStateOf(false) }

    fun loadTeachers() {
        database.getReference("users").get().addOnSuccessListener { snapshot ->
            legacyTeachers = snapshot.children.mapNotNull { child ->
                child.getValue(User::class.java)?.copy(uid = child.key ?: "")
            }.filter { it.role == "teacher" }
        }
    }

    LaunchedEffect(Unit) { loadTeachers() }

    fun createTeacherAccount() {
        val normalizedId = loginId.trim()
        if (name.isBlank() || phone.isBlank() || normalizedId.isBlank()) {
            message = "이름, 전화번호, 사용할 아이디를 입력해주세요."
            return
        }
        if (!normalizedId.matches(Regex("[A-Za-z0-9._-]{3,30}"))) {
            message = "아이디는 영문, 숫자, 마침표(.), 밑줄(_), 하이픈(-) 3~30자만 사용할 수 있습니다."
            return
        }

        val password = Random.nextInt(100000, 999999).toString()
        val secondaryApp = FirebaseApp.getApps(context)
            .firstOrNull { it.name == "teacher-account-creator" }
            ?: FirebaseApp.initializeApp(
                context,
                FirebaseApp.getInstance().options,
                "teacher-account-creator"
            ) ?: run {
                message = "선생님 계정 인증을 초기화하지 못했습니다."
                return
            }
        val teacherAuth = FirebaseAuth.getInstance(secondaryApp)
        val email = "$normalizedId@attendance.com"

        teacherAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: run {
                    teacherAuth.signOut()
                    message = "생성된 계정 정보를 확인하지 못했습니다."
                    return@addOnSuccessListener
                }
                val teacher = User(
                    uid = uid,
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email,
                    role = "teacher",
                    loginId = normalizedId,
                    firstLogin = true,
                    createdAt = System.currentTimeMillis()
                )
                database.getReference("users").child(uid).setValue(teacher)
                    .addOnSuccessListener {
                        teacherAuth.signOut()
                        val previousTeacher = selectedLegacyTeacher
                        val complete = {
                            message = buildString {
                                append("선생님 계정 발급 완료\n\n")
                                append("이름: ${teacher.name}\n")
                                append("아이디: $normalizedId\n")
                                append("임시 비밀번호: $password\n\n")
                                append("첫 로그인 후 비밀번호를 변경해야 합니다.")
                            }
                            name = ""
                            phone = ""
                            loginId = ""
                            selectedLegacyTeacher = null
                            loadTeachers()
                        }
                        if (previousTeacher == null) {
                            complete()
                        } else {
                            migrateTeacherTimetables(
                                database = database,
                                oldTeacherUid = previousTeacher.uid,
                                newTeacherUid = uid,
                                newTeacherName = teacher.name,
                                onSuccess = complete,
                                onFailure = { error ->
                                    message = "계정은 생성됐지만 시간표 이전에 실패했습니다: $error"
                                }
                            )
                        }
                    }
                    .addOnFailureListener {
                        result.user?.delete()
                        teacherAuth.signOut()
                        message = "선생님 정보를 저장하지 못했습니다: ${it.message}"
                    }
            }
            .addOnFailureListener {
                message = "계정 발급 실패: ${it.message}"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = UiConfig.topPadding,
                start = UiConfig.sidePadding,
                end = UiConfig.sidePadding,
                bottom = UiConfig.bottomPadding
            )
    ) {
        Text("선생님 추가", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = name,
            onValueChange = { name = it },
            label = { Text("선생님 이름") }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = phone,
            onValueChange = { phone = it },
            label = { Text("전화번호") }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = loginId,
            onValueChange = { loginId = it },
            label = { Text("사용할 아이디") },
            supportingText = { Text("발급 후 아이디는 바꿀 수 없습니다.") }
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showLegacyTeacherPicker = true }
        ) {
            Text(selectedLegacyTeacher?.let { "계정 이전·재발급: ${it.name}" } ?: "기존 선생님 계정 이전·재발급 없음")
        }
        Text(
            "기존 계정을 선택하면 새 아이디로 계정을 재발급하고 담당 시간표를 함께 이전합니다.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(10.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = ::createTeacherAccount) {
            Text("선생님 계정 발급")
        }
        Spacer(Modifier.height(20.dp))
        if (message.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(message, modifier = Modifier.padding(15.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onBack) {
            Text("돌아가기")
        }
    }

    if (showLegacyTeacherPicker) {
        AlertDialog(
            onDismissRequest = { showLegacyTeacherPicker = false },
            title = { Text("선생님 계정 이전·재발급") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (legacyTeachers.isEmpty()) {
                        Text("이전할 선생님 계정이 없습니다.")
                    } else {
                        legacyTeachers.forEach { teacher ->
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    selectedLegacyTeacher = teacher
                                    name = teacher.name
                                    phone = teacher.phone
                                    showLegacyTeacherPicker = false
                                }
                            ) { Text("${teacher.name} (${teacher.loginId})") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedLegacyTeacher = null
                    showLegacyTeacherPicker = false
                }) { Text("선택 안 함") }
            }
        )
    }
}

private fun migrateTeacherTimetables(
    database: FirebaseDatabase,
    oldTeacherUid: String,
    newTeacherUid: String,
    newTeacherName: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    database.getReference("teacherTimetable").child(oldTeacherUid).get()
        .addOnSuccessListener { teacherTimetable ->
            database.getReference("timetable").get()
                .addOnSuccessListener { studentTimetables ->
                    val updates = mutableMapOf<String, Any?>()
                    teacherTimetable.children.forEach { child ->
                        val schedule = child.getValue(Timetable::class.java) ?: return@forEach
                        val scheduleId = child.key ?: return@forEach
                        updates["teacherTimetable/$newTeacherUid/$scheduleId"] = schedule.copy(
                            id = scheduleId,
                            teacherUid = newTeacherUid,
                            teacher = newTeacherName
                        )
                    }
                    updates["teacherTimetable/$oldTeacherUid"] = null
                    studentTimetables.children.forEach { student ->
                        val studentUid = student.key ?: return@forEach
                        student.children.forEach { child ->
                            val schedule = child.getValue(Timetable::class.java) ?: return@forEach
                            if (schedule.teacherUid == oldTeacherUid) {
                                val scheduleId = child.key ?: return@forEach
                                updates["timetable/$studentUid/$scheduleId/teacherUid"] = newTeacherUid
                                updates["timetable/$studentUid/$scheduleId/teacher"] = newTeacherName
                            }
                        }
                    }
                    migrateAttendanceTeacherReferences(
                        database = database,
                        oldTeacherUid = oldTeacherUid,
                        newTeacherUid = newTeacherUid,
                        onSuccess = { attendanceUpdates ->
                            updates.putAll(attendanceUpdates)
                            updates["users/$oldTeacherUid"] = null
                            database.reference.updateChildren(updates)
                                .addOnSuccessListener { onSuccess() }
                                .addOnFailureListener { onFailure(it.message ?: "데이터베이스 오류") }
                        },
                        onFailure = onFailure
                    )
                    return@addOnSuccessListener

                    updates["users/$oldTeacherUid"] = null
                    database.reference.updateChildren(updates)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it.message ?: "데이터베이스 오류") }
                }
                .addOnFailureListener { onFailure(it.message ?: "학생 시간표를 읽을 수 없습니다.") }
        }
        .addOnFailureListener { onFailure(it.message ?: "기존 시간표를 읽을 수 없습니다.") }
}
