package com.example.attendanceappfinal.teacher

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
import com.example.attendanceappfinal.model.AttendanceLog
import com.google.firebase.database.FirebaseDatabase

@Composable
fun TeacherAutoAbsencePage(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var logs by remember { mutableStateOf(emptyList<AttendanceLog>()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }

    fun load() {
        loading = true
        FirebaseDatabase.getInstance().getReference("attendance_logs").get()
            .addOnSuccessListener { snapshot ->
                logs = snapshot.children.mapNotNull { it.getValue(AttendanceLog::class.java) }
                    .filter { it.automatic }.sortedByDescending { it.time }
                loading = false
            }
            .addOnFailureListener {
                message = "자동 결석 처리 내역을 불러오지 못했습니다."
                loading = false
            }
    }
    LaunchedEffect(Unit) { load() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = UiConfig.topPadding, start = UiConfig.sidePadding,
            end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
        ), verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("자동 결석 처리 내역", style = MaterialTheme.typography.headlineMedium)
        Text("수업 종료 후 출석 기록이 없어 자동으로 결석 처리된 학생입니다.")
        if (loading) Text("불러오는 중...")
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
        if (!loading && logs.isEmpty()) Text("자동 결석 처리 내역이 없습니다.")
        logs.groupBy { it.time.take(10) }.forEach { (date, dayLogs) ->
            Text(date, style = MaterialTheme.typography.titleMedium)
            dayLogs.forEach { log ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${log.grade} ${log.className} · ${log.studentName}", style = MaterialTheme.typography.titleMedium)
                        Text("${log.subject} · ${log.time.takeLast(5)} · 자동 결석")
                        Text(log.reason, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = ::load) { Text("새로고침") }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onBack) { Text("뒤로가기") }
    }
}
