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
import com.google.firebase.database.FirebaseDatabase

@Composable
fun AdminAcademySettingsPage(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val database = FirebaseDatabase.getInstance()
    var classesText by remember { mutableStateOf("A반, B반, C반, D반") }
    var subjectsText by remember { mutableStateOf("수학, 영어, 과학, 자습") }
    var message by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    fun parse(value: String) = value.split(',', '\n').map { it.trim() }.filter { it.isNotBlank() }.distinct()
    LaunchedEffect(Unit) {
        database.getReference("academySettings").get().addOnSuccessListener { snapshot ->
            snapshot.child("classes").children.mapNotNull { it.getValue(String::class.java) }
                .takeIf { it.isNotEmpty() }?.let { classesText = it.joinToString(", ") }
            snapshot.child("subjects").children.mapNotNull { it.getValue(String::class.java) }
                .takeIf { it.isNotEmpty() }?.let { subjectsText = it.joinToString(", ") }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
        top = UiConfig.topPadding, start = UiConfig.sidePadding,
        end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
    )) {
        Text("반 · 과목 설정", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("쉼표 또는 줄바꿈으로 여러 항목을 입력하세요. 저장 후 선생님 시간표 선택 항목에 반영됩니다.")
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = classesText, onValueChange = { classesText = it }, modifier = Modifier.fillMaxWidth(),
            minLines = 3, label = { Text("반 목록") }, placeholder = { Text("예: A반, B반, 중등 1반") })
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(value = subjectsText, onValueChange = { subjectsText = it }, modifier = Modifier.fillMaxWidth(),
            minLines = 3, label = { Text("과목 목록") }, placeholder = { Text("예: 수학, 영어, 과학") })
        Spacer(Modifier.height(20.dp))
        Button(modifier = Modifier.fillMaxWidth(), enabled = !saving, onClick = {
            val classes = parse(classesText); val subjects = parse(subjectsText)
            if (classes.isEmpty() || subjects.isEmpty()) { message = "반과 과목을 각각 한 개 이상 입력해 주세요."; return@Button }
            saving = true
            database.getReference("academySettings").updateChildren(mapOf("classes" to classes, "subjects" to subjects))
                .addOnSuccessListener { saving = false; message = "시간표 설정을 저장했습니다." }
                .addOnFailureListener { saving = false; message = "저장하지 못했습니다." }
        }) { Text(if (saving) "저장 중..." else "저장") }
        if (message.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(message) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onBack) { Text("뒤로가기") }
    }
}
