package com.example.attendanceappfinal.admin

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
import com.google.firebase.database.FirebaseDatabase

@Composable
fun AdminDataIntegrityPage(onBack: () -> Unit) {
    BackHandler { onBack() }
    val database = FirebaseDatabase.getInstance()
    var summary by remember { mutableStateOf<DataAuditSummary?>(null) }
    var message by remember { mutableStateOf("") }
    var migrating by remember { mutableStateOf(false) }

    fun loadSummary() {
        database.getReference("users").get().addOnSuccessListener { users ->
            database.getReference("preStudents").get().addOnSuccessListener { pre ->
                database.getReference("unregisteredStudents").get().addOnSuccessListener { unregistered ->
                    database.getReference("nfc_tags").get().addOnSuccessListener { tags ->
                        summary = DataAuditSummary(
                            users.childrenCount.toInt(), pre.childrenCount.toInt(),
                            unregistered.childrenCount.toInt(), tags.childrenCount.toInt()
                        )
                    }
                }
            }
        }
    }

    fun migratePreStudents() {
        migrating = true
        database.getReference("preStudents").get().addOnSuccessListener { preStudents ->
            database.getReference("preAttendance").get().addOnSuccessListener { preAttendance ->
                database.getReference("nfc_tags").get().addOnSuccessListener { tags ->
                    val updates = mutableMapOf<String, Any?>()
                    preStudents.children.forEach { child ->
                        val id = child.key ?: return@forEach
                        updates["unregisteredStudents/$id"] = child.value
                        updates["preStudents/$id"] = null
                        if (preAttendance.hasChild(id)) {
                            updates["unregisteredAttendance/$id"] = preAttendance.child(id).value
                            updates["preAttendance/$id"] = null
                        }
                    }
                    tags.children.forEach { tag ->
                        val preId = tag.child("preStudentId").getValue(String::class.java).orEmpty()
                        if (preId.isNotBlank() && preStudents.hasChild(preId)) {
                            val tagId = tag.key ?: return@forEach
                            updates["nfc_tags/$tagId/preStudentId"] = ""
                            updates["nfc_tags/$tagId/unregisteredStudentId"] = preId
                            updates["nfc_tags/$tagId/type"] = "unregistered"
                        }
                    }
                    if (updates.isEmpty()) {
                        migrating = false
                        message = "통합할 사전등록 학생이 없습니다."
                    } else {
                        database.reference.updateChildren(updates).addOnSuccessListener {
                            migrating = false
                            message = "사전등록 학생 ${preStudents.childrenCount}명을 미등록 학생으로 통합했습니다."
                            loadSummary()
                        }.addOnFailureListener {
                            migrating = false
                            message = "통합 실패: ${it.message ?: "알 수 없는 오류"}"
                        }
                    }
                }.addOnFailureListener { migrating = false; message = "NFC 정보를 불러오지 못했습니다: ${it.message}" }
            }.addOnFailureListener { migrating = false; message = "사전 출결 정보를 불러오지 못했습니다: ${it.message}" }
        }.addOnFailureListener { migrating = false; message = "사전등록 학생 정보를 불러오지 못했습니다: ${it.message}" }
    }

    LaunchedEffect(Unit) { loadSummary() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = UiConfig.topPadding, start = UiConfig.sidePadding,
            end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
        ), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("데이터 점검", style = MaterialTheme.typography.headlineMedium)
        Text("사전등록 구조를 미등록 학생 구조로 안전하게 통합합니다.")
        summary?.let {
            AuditCard("학생 데이터", listOf(
                "가입 완료 학생  ${it.users}명",
                "이전 사전등록 학생  ${it.preStudents}명",
                "미등록 학생  ${it.unregisteredStudents}명",
                "등록된 NFC 태그  ${it.nfcTags}개"
            ))
            if (it.preStudents > 0) {
                Button(onClick = { migratePreStudents() }, enabled = !migrating, modifier = Modifier.fillMaxWidth()) {
                    Text(if (migrating) "통합 중..." else "사전등록 학생을 미등록 학생으로 통합")
                }
            }
        } ?: Text("점검 정보를 불러오는 중입니다.")
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
        TextButton(onClick = onBack, enabled = !migrating) { Text("뒤로가기") }
    }
}

@Composable
private fun AuditCard(title: String, lines: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            lines.forEach { Text(it) }
        }
    }
}

private data class DataAuditSummary(
    val users: Int, val preStudents: Int, val unregisteredStudents: Int, val nfcTags: Int
)
