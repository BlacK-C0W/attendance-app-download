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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.attendanceappfinal.student.StudentAttendanceCalendar
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
    var showNotifications by remember { mutableStateOf(false) }
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
                .sortedByDescending { it.timestamp }
        }
        loadNotifications()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = UiConfig.topPadding, start = UiConfig.sidePadding,
            end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
        ), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val unread = notifications.count { !it.read }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("학부모 페이지", style = MaterialTheme.typography.headlineMedium)
            BadgedBox(
                badge = {
                    if (unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) }
                }
            ) {
                IconButton(onClick = { showNotifications = true }) {
                    Icon(Icons.Default.Notifications, contentDescription = "학부모 알림")
                }
            }
        }
        Text("${child?.name ?: "연결된 학생"} 학생의 최근 출결")
        records.take(10).forEach { Text("${it.date} ${it.time} · ${it.subject} · ${it.status}") }
        if (records.isEmpty()) Text("출결 기록이 없습니다.")

        Text("학생 출결 달력", style = MaterialTheme.typography.titleMedium)
        StudentAttendanceCalendar(attendance = records)

        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.padding(vertical = 4.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = onLogout) { Text("로그아웃") }
    }

    if (showNotifications) {
        ParentNotificationDialog(
            notifications = notifications,
            onDismiss = { showNotifications = false },
            onMarkRead = { notification ->
                database.getReference("parentNotifications").child(studentId).child(notification.id)
                    .child("read").setValue(true)
                    .addOnSuccessListener { loadNotifications() }
                    .addOnFailureListener { message = "읽음 처리에 실패했습니다." }
            },
            onDelete = { notification ->
                database.getReference("parentNotifications").child(studentId).child(notification.id)
                    .removeValue()
                    .addOnSuccessListener { loadNotifications() }
                    .addOnFailureListener { message = "삭제에 실패했습니다." }
            }
        )
    }
}

@Composable
private fun ParentNotificationDialog(
    notifications: List<Notification>,
    onDismiss: () -> Unit,
    onMarkRead: (Notification) -> Unit,
    onDelete: (Notification) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("학부모 알림") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (notifications.isEmpty()) Text("알림이 없습니다.")
                notifications.groupBy { notificationDate(it.timestamp) }.forEach { (date, dayNotifications) ->
                    Text(date, style = MaterialTheme.typography.titleSmall)
                    dayNotifications.forEach { notification ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(notification.title, style = MaterialTheme.typography.titleMedium)
                                Text(notification.message)
                                Text(
                                    "${notificationTime(notification.timestamp)} · ${if (notification.read) "읽음" else "새 알림"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!notification.read) {
                                        OutlinedButton(onClick = { onMarkRead(notification) }) { Text("읽음") }
                                    }
                                    OutlinedButton(onClick = { onDelete(notification) }) { Text("삭제") }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } }
    )
}

private fun notificationDate(timestamp: Long): String =
    SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date(timestamp))

private fun notificationTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(timestamp))
