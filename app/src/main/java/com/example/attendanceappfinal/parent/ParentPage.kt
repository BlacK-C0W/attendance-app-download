package com.example.attendanceappfinal.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.Notification
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ParentPage(user: User, onLogout: () -> Unit) {
    var child by remember { mutableStateOf<User?>(null) }
    var records by remember { mutableStateOf(emptyList<Attendance>()) }
    var notifications by remember { mutableStateOf(emptyList<Notification>()) }
    var message by remember { mutableStateOf("") }
    val studentId = user.linkedStudentId
    val database = FirebaseDatabase.getInstance()

    fun loadNotifications() {
        if (studentId.isBlank()) return
        database.getReference("parentNotifications").child(studentId).get().addOnSuccessListener { snapshot ->
            notifications = snapshot.children.mapNotNull { childSnapshot ->
                childSnapshot.getValue(Notification::class.java)?.copy(id = childSnapshot.key ?: "")
            }.sortedByDescending { it.timestamp }
        }.addOnFailureListener { message = "알림을 불러오지 못했습니다." }
    }

    LaunchedEffect(studentId) {
        if (studentId.isBlank()) return@LaunchedEffect
        database.getReference("users").child(studentId).get().addOnSuccessListener { snapshot ->
            child = snapshot.getValue(User::class.java)?.copy(uid = studentId)
        }
        database.getReference("attendance").child(studentId).get().addOnSuccessListener { snapshot ->
            records = snapshot.children.mapNotNull { it.getValue(Attendance::class.java) }
                .sortedByDescending { it.timestamp }.take(10)
        }
        loadNotifications()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = UiConfig.topPadding, start = UiConfig.sidePadding,
            end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
        ), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("학부모 페이지", style = MaterialTheme.typography.headlineMedium)
        Text("${child?.name ?: "연결된 학생"} 학생의 최근 출결")
        records.forEach { Text("${it.date} ${it.time} · ${it.subject} · ${it.status}") }
        if (records.isEmpty()) Text("출결 기록이 없습니다.")

        Text("학부모 알림", style = MaterialTheme.typography.titleMedium)
        val unread = notifications.count { !it.read }
        if (unread > 0) Text("읽지 않은 알림 ${unread}개", color = MaterialTheme.colorScheme.primary)
        if (notifications.isEmpty()) Text("알림이 없습니다.")
        notifications.groupBy { notificationDate(it.timestamp) }.forEach { (date, dayNotifications) ->
            Text(date, style = MaterialTheme.typography.titleSmall)
            dayNotifications.forEach { notification ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(notification.title, style = MaterialTheme.typography.titleMedium)
                        Text(notification.message)
                        Text(
                            "${notificationTime(notification.timestamp)} · ${if (notification.read) "읽음" else "새 알림"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!notification.read) {
                                OutlinedButton(onClick = {
                                    database.getReference("parentNotifications").child(studentId).child(notification.id)
                                        .child("read").setValue(true)
                                        .addOnSuccessListener { loadNotifications() }
                                        .addOnFailureListener { message = "읽음 처리에 실패했습니다." }
                                }) { Text("읽음") }
                            }
                            OutlinedButton(onClick = {
                                database.getReference("parentNotifications").child(studentId).child(notification.id)
                                    .removeValue()
                                    .addOnSuccessListener { loadNotifications() }
                                    .addOnFailureListener { message = "삭제에 실패했습니다." }
                            }) { Text("삭제") }
                        }
                    }
                }
            }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.padding(vertical = 4.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = onLogout) { Text("로그아웃") }
    }
}

private fun notificationDate(timestamp: Long): String =
    SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date(timestamp))

private fun notificationTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(timestamp))
