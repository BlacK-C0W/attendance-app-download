package com.example.attendanceappfinal.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.UnregisteredStudent
import com.example.attendanceappfinal.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase

private data class RegistrationStudent(
    val key: String,
    val attendanceRoot: String,
    val name: String,
    val phone: String,
    val grade: String,
    val className: String,
    val nfcTag: String
)

private fun findRegistrationStudent(
    snapshot: DataSnapshot,
    attendanceRoot: String,
    name: String,
    phone: String,
    grade: String
): RegistrationStudent? = snapshot.children.firstNotNullOfOrNull { child ->
    val values = child.getValue(UnregisteredStudent::class.java)?.let {
        listOf(it.name, it.phone, it.grade, it.className, it.nfcTag)
    } ?: return@firstNotNullOfOrNull null

    if (values[0].trim() == name.trim() &&
        values[1].filter(Char::isDigit) == phone.filter(Char::isDigit) &&
        values[2].trim() == grade.trim()
    ) {
        RegistrationStudent(
            key = child.key ?: return@firstNotNullOfOrNull null,
            attendanceRoot = attendanceRoot,
            name = values[0], phone = values[1], grade = values[2],
            className = values[3], nfcTag = values[4]
        )
    } else null
}

@Composable
fun RegisterPage(onBack: () -> Unit) {
    BackHandler { onBack() }

    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance()
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var gradeOpen by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val grades = listOf("초1", "초2", "중1", "중2", "중3", "고1", "고2", "고3")

    fun fail(text: String, removeAuthUser: Boolean = false) {
        if (removeAuthUser) auth.currentUser?.delete()
        submitting = false
        message = text
    }

    fun completeRegistration(student: RegistrationStudent) {
        val authUid = auth.currentUser?.uid ?: return fail("인증 정보를 확인할 수 없습니다.")
        val fakeEmail = "${id.trim()}@attendance.com"
        database.getReference(student.attendanceRoot).child(student.key).get()
            .addOnSuccessListener { attendanceSnapshot ->
                val user = User(
                    uid = authUid, name = student.name, phone = student.phone,
                    email = fakeEmail, role = "student", grade = student.grade,
                    className = student.className, nfcId = student.nfcTag,
                    unregisteredStudentId = student.key,
                    createdAt = System.currentTimeMillis()
                )
                val updates = mutableMapOf<String, Any?>(
                    "unregisteredStudents/${student.key}" to null,
                    "${student.attendanceRoot}/${student.key}" to null
                )
                attendanceSnapshot.children.forEach { child ->
                    child.getValue(Attendance::class.java)?.let { attendance ->
                        updates["attendance/$authUid/${child.key}"] = attendance.copy(
                            studentUid = authUid, studentName = student.name
                        )
                    }
                }
                if (student.nfcTag.isNotBlank()) {
                    updates["nfc_tags/${student.nfcTag}"] = mapOf(
                        "studentUid" to authUid, "name" to student.name
                    )
                }
                database.getReference("users").child(authUid).setValue(user)
                    .addOnSuccessListener {
                        database.reference.updateChildren(updates)
                            .addOnSuccessListener {
                                submitting = false
                                message = "회원가입이 완료되었습니다."
                                onBack()
                            }
                            .addOnFailureListener { fail("학생 데이터 전환 실패: ${it.message}") }
                    }
                    .addOnFailureListener { fail("회원 정보를 저장하지 못했습니다: ${it.message}", true) }
            }
            .addOnFailureListener { fail("기존 출결 정보를 불러오지 못했습니다: ${it.message}") }
    }

    fun findStudentAfterAuthentication() {
        database.getReference("unregisteredStudents").get()
            .addOnSuccessListener { snapshot ->
                val student = findRegistrationStudent(snapshot, "unregisteredAttendance", name, phone, grade)
                if (student == null) {
                    fail("등록된 미등록 학생 정보와 일치하지 않습니다.", removeAuthUser = true)
                } else completeRegistration(student)
            }
            .addOnFailureListener { fail("미등록 학생 정보를 확인하지 못했습니다: ${it.message}", true) }
    }

    fun register() {
        if (listOf(id, password, name, phone, grade).any { it.isBlank() }) {
            message = "모든 정보를 입력하세요."
            return
        }
        submitting = true
        message = "학생 정보를 확인하는 중입니다."
        auth.createUserWithEmailAndPassword("${id.trim()}@attendance.com", password)
            .addOnSuccessListener { findStudentAfterAuthentication() }
            .addOnFailureListener { fail(it.message ?: "회원가입에 실패했습니다.") }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(
            top = UiConfig.topPadding, start = UiConfig.sidePadding, end = UiConfig.sidePadding
        )
    ) {
        Text("학생 회원가입", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(id, { id = it }, label = { Text("아이디") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            password, { password = it }, label = { Text("비밀번호") },
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(name, { name = it }, label = { Text("이름") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("전화번호") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Box {
            Button(modifier = Modifier.fillMaxWidth(), onClick = { gradeOpen = true }) {
                Text(grade.ifBlank { "학년 선택" })
            }
            DropdownMenu(expanded = gradeOpen, onDismissRequest = { gradeOpen = false }) {
                grades.forEach { item ->
                    DropdownMenuItem(text = { Text(item) }, onClick = { grade = item; gradeOpen = false })
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(modifier = Modifier.fillMaxWidth(), enabled = !submitting, onClick = { register() }) {
            Text(if (submitting) "처리 중..." else "회원가입")
        }
        Spacer(Modifier.height(10.dp))
        Text(message)
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onBack, enabled = !submitting) { Text("뒤로가기") }
    }
}
