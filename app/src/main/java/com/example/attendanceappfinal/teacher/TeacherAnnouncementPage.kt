package com.example.attendanceappfinal.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.Notification
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.repository.NotificationRepository
import com.google.firebase.database.FirebaseDatabase

@Composable
fun TeacherAnnouncementPage(onBack: () -> Unit) {
    BackHandler { onBack() }
    var students by remember { mutableStateOf(emptyList<User>()) }
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().getReference("users").get().addOnSuccessListener { snapshot ->
            students = snapshot.children.mapNotNull { child ->
                child.getValue(User::class.java)?.copy(uid = child.key ?: "")
            }.filter { it.role == "student" }
            students.forEach { selected[it.uid] = true }
        }.addOnFailureListener { message = "학생 목록을 불러오지 못했습니다." }
    }
    fun send() {
        val recipients = students.filter { selected[it.uid] == true }
        if (title.isBlank() || body.isBlank() || recipients.isEmpty()) {
            message = "제목, 내용, 수신 학생을 확인하세요."
            return
        }
        var completed = 0
        recipients.forEach { student ->
            NotificationRepository.saveNotification(
                Notification(studentUid = student.uid, title = title.trim(), message = body.trim(), timestamp = System.currentTimeMillis()),
                onSuccess = { if (++completed == recipients.size) { message = "${recipients.size}명에게 공지를 보냈습니다."; title = ""; body = "" } },
                onFail = { message = "공지 전송 실패: $it" }
            )
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = UiConfig.topPadding, start = UiConfig.sidePadding,
            end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
        ), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("학생 공지", style = MaterialTheme.typography.headlineMedium)
        Text("앱 가입이 완료된 학생의 알림함으로 공지를 보냅니다.")
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("공지 제목") })
                OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth().height(130.dp), label = { Text("공지 내용") })
                Button(onClick = { send() }, modifier = Modifier.fillMaxWidth()) { Text("선택 학생에게 공지 보내기") }
            }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
        Text("수신 대상 ${students.count { selected[it.uid] == true }}명", style = MaterialTheme.typography.titleMedium)
        students.forEach { student ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text(student.name, style = MaterialTheme.typography.titleMedium); Text("${student.grade} ${student.className}") }
                    Checkbox(checked = selected[student.uid] == true, onCheckedChange = { selected[student.uid] = it })
                }
            }
        }
    }
}
