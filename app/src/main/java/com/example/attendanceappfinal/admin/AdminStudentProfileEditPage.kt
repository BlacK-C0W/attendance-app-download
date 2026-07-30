package com.example.attendanceappfinal.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase

/** Admin-only profile fields that define a student's class placement. */
@Composable
fun AdminStudentProfileEditPage(
    student: User,
    onBack: () -> Unit,
    onScoreHistoryClick: () -> Unit = {},
    onParentCommunicationClick: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    var className by remember(student.uid) { mutableStateOf(student.className) }
    var studentPhone by remember(student.uid) { mutableStateOf(student.phone) }
    var parentPhone by remember(student.uid) { mutableStateOf(student.parentPhone) }
    var schoolName by remember(student.uid) { mutableStateOf(student.schoolName) }
    var mathScore by remember(student.uid) { mutableStateOf(student.mathScore) }
    var scienceScore by remember(student.uid) { mutableStateOf(student.scienceScore) }
    var englishScore by remember(student.uid) { mutableStateOf(student.englishScore) }
    var message by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    fun save() {
        if (className.isBlank()) {
            message = "반을 입력해 주세요."
            return
        }
        saving = true
        val now = System.currentTimeMillis()
        val updates = mapOf<String, Any>(
            "users/${student.uid}/className" to className.trim(),
            "users/${student.uid}/phone" to studentPhone.trim(),
            "users/${student.uid}/parentPhone" to parentPhone.trim(),
            "users/${student.uid}/schoolName" to schoolName.trim(),
            "users/${student.uid}/mathScore" to mathScore.trim(),
            "users/${student.uid}/scienceScore" to scienceScore.trim(),
            "users/${student.uid}/englishScore" to englishScore.trim(),
            "studentScoreHistory/${student.uid}/$now" to mapOf(
                "mathScore" to mathScore.trim(),
                "scienceScore" to scienceScore.trim(),
                "englishScore" to englishScore.trim(),
                "updatedAt" to now
            )
        )
        FirebaseDatabase.getInstance().reference
            .updateChildren(updates)
            .addOnSuccessListener {
                saving = false
                message = "학생 정보를 저장했습니다."
            }
            .addOnFailureListener {
                saving = false
                message = "저장하지 못했습니다: ${it.message ?: "오류"}"
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = UiConfig.topPadding, start = UiConfig.sidePadding,
            end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
        )
    ) {
        Text("학생 관리", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(student.name.ifBlank { "이름 없음" }, style = MaterialTheme.typography.titleLarge)
                Text("학생 연락처: ${student.phone.ifBlank { "미등록" }}")
                Text("학년: ${student.grade.ifBlank { "미등록" }}")
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = className,
            onValueChange = { className = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("반") },
            placeholder = { Text("예: A반") }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = studentPhone,
            onValueChange = { studentPhone = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("학생 연락처") },
            placeholder = { Text("예: 010-1234-5678") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = parentPhone,
            onValueChange = { parentPhone = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("부모님 연락처") },
            placeholder = { Text("예: 010-1234-5678") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = schoolName,
            onValueChange = { schoolName = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("학교 정보") },
            placeholder = { Text("예: OO중학교") }
        )
        Spacer(Modifier.height(20.dp))
        Text("성적 정보", style = MaterialTheme.typography.titleMedium)
        Text("점수 또는 등급을 입력할 수 있습니다.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = mathScore,
            onValueChange = { mathScore = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("수학") },
            placeholder = { Text("예: 92점 또는 A") }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = scienceScore,
            onValueChange = { scienceScore = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("과학") },
            placeholder = { Text("예: 88점 또는 B") }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = englishScore,
            onValueChange = { englishScore = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("영어") },
            placeholder = { Text("예: 95점 또는 A") }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onScoreHistoryClick) {
            Text("성적 이력 보기")
        }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onParentCommunicationClick) {
            Text("학부모 연락 · 안내")
        }
        Spacer(Modifier.height(20.dp))
        Button(modifier = Modifier.fillMaxWidth(), enabled = !saving, onClick = { save() }) {
            Text(if (saving) "저장 중..." else "저장")
        }
        if (message.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(message) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onBack) { Text("뒤로가기") }
    }
}
