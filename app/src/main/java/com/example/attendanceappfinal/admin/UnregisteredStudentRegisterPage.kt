package com.example.attendanceappfinal.admin

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.UnregisteredStudent
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

@Composable
fun UnregisteredStudentRegisterPage(onBack: () -> Unit) {
    BackHandler { onBack() }
    val database = FirebaseDatabase.getInstance()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var gradeOpen by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var issuedRegistrationCode by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val grades = listOf("초1", "초2", "중1", "중2", "중3", "고1", "고2", "고3")

    fun registerStudent() {
        if (name.isBlank() || phone.isBlank() || grade.isBlank() || className.isBlank()) {
            message = "이름, 학년, 반, 전화번호를 모두 입력하세요."
            return
        }
        submitting = true
        val registrationCode = UUID.randomUUID().toString().replace("-", "").take(12)
        val ref = database.getReference("unregisteredStudents").child(registrationCode)
        val student = UnregisteredStudent(
            id = ref.key.orEmpty(), name = name.trim(), phone = phone.trim(),
            grade = grade, className = className.trim(), createdAt = System.currentTimeMillis()
        )
        ref.setValue(student).addOnSuccessListener {
            name = ""; phone = ""; grade = ""; className = ""
            submitting = false
            issuedRegistrationCode = registrationCode
            message = "미등록 학생을 등록했습니다."
        }.addOnFailureListener {
            submitting = false
            message = "등록에 실패했습니다: ${it.message ?: "알 수 없는 오류"}"
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
        top = UiConfig.topPadding, start = UiConfig.sidePadding,
        end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
    )) {
        Text("미등록 학생 등록", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text("앱 계정이 없는 학생을 등록합니다. 가입 전 출결과 NFC도 이 학생 정보로 관리됩니다.")
        Spacer(Modifier.height(24.dp))
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("학생 이름") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(12.dp))
                Box {
                    Button(onClick = { gradeOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(grade.ifBlank { "학년 선택" }) }
                    DropdownMenu(expanded = gradeOpen, onDismissRequest = { gradeOpen = false }) {
                        grades.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { grade = value; gradeOpen = false }) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(className, { className = it }, label = { Text("반") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(phone, { phone = it }, label = { Text("전화번호") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { registerStudent() }, enabled = !submitting, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (submitting) "등록 중..." else "미등록 학생 등록") }
            }
        }
        if (issuedRegistrationCode.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("학생 가입 코드", style = MaterialTheme.typography.titleMedium)
                    Text(issuedRegistrationCode, style = MaterialTheme.typography.headlineSmall)
                    OutlinedButton(onClick = {
                        context.getSystemService(ClipboardManager::class.java)
                            .setPrimaryClip(ClipData.newPlainText("학생 가입 코드", issuedRegistrationCode))
                        message = "가입 코드를 복사했습니다."
                    }) { Text("코드 복사") }
                }
            }
        }
        if (message.isNotBlank()) { Spacer(Modifier.height(16.dp)); Text(message, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack, enabled = !submitting) { Text("뒤로가기") }
    }
}
