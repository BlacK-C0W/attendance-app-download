package com.example.attendanceappfinal.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ScoreSnapshot(val updatedAt: Long, val math: String, val science: String, val english: String)

@Composable
fun AdminStudentScoreHistoryPage(student: User, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var history by remember { mutableStateOf(emptyList<ScoreSnapshot>()) }
    LaunchedEffect(student.uid) {
        FirebaseDatabase.getInstance().getReference("studentScoreHistory").child(student.uid).get()
            .addOnSuccessListener { snapshot ->
                history = snapshot.children.map { child ->
                    ScoreSnapshot(
                        child.child("updatedAt").getValue(Long::class.java) ?: child.key?.toLongOrNull() ?: 0L,
                        child.child("mathScore").getValue(String::class.java).orEmpty(),
                        child.child("scienceScore").getValue(String::class.java).orEmpty(),
                        child.child("englishScore").getValue(String::class.java).orEmpty()
                    )
                }.sortedByDescending { it.updatedAt }
            }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
        top = UiConfig.topPadding, start = UiConfig.sidePadding,
        end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
    )) {
        Text("성적 이력", style = MaterialTheme.typography.headlineMedium)
        Text(student.name)
        Spacer(Modifier.height(16.dp))
        if (history.isEmpty()) Text("저장된 성적 이력이 없습니다.")
        history.forEach { item ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date(item.updatedAt)))
                    Spacer(Modifier.height(6.dp))
                    Text("수학 ${item.math.ifBlank { "-" }} · 과학 ${item.science.ifBlank { "-" }} · 영어 ${item.english.ifBlank { "-" }}")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onBack) { Text("학생 관리로 돌아가기") }
    }
}
