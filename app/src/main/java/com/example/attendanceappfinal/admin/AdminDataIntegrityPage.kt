package com.example.attendanceappfinal.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.google.firebase.database.FirebaseDatabase

@Composable
fun AdminDataIntegrityPage(onBack: () -> Unit) {
    BackHandler { onBack() }

    val database = FirebaseDatabase.getInstance()
    var summary by androidx.compose.runtime.remember { mutableStateOf<DataAuditSummary?>(null) }
    var migrationMessage by androidx.compose.runtime.remember { mutableStateOf("") }

    fun migrateLegacyPreStudents() {
        database.getReference("preregister").get().addOnSuccessListener { oldPre ->
            database.getReference("students_register").get().addOnSuccessListener { oldRegister ->
                val updates = mutableMapOf<String, Any?>()
                oldPre.children.forEach { child ->
                    child.key?.let { key ->
                        updates["preStudents/$key"] = child.value
                        updates["preregister/$key"] = null
                    }
                }
                oldRegister.children.forEach { child ->
                    child.key?.let { key ->
                        updates["preStudents/$key"] = child.value
                        updates["students_register/$key"] = null
                    }
                }
                if (updates.isEmpty()) {
                    migrationMessage = "이전할 구형 사전등록 데이터가 없습니다."
                } else {
                    database.reference.updateChildren(updates)
                        .addOnSuccessListener {
                            migrationMessage = "${updates.size / 2}건을 preStudents로 이전했습니다."
                        }
                        .addOnFailureListener {
                            migrationMessage = "이전 실패: ${it.message ?: "알 수 없는 오류"}"
                        }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        database.getReference("users").get().addOnSuccessListener { users ->
            database.getReference("preStudents").get().addOnSuccessListener { preStudents ->
                database.getReference("unregisteredStudents").get().addOnSuccessListener { unregistered ->
                    database.getReference("nfc_tags").get().addOnSuccessListener { tags ->
                        database.getReference("preregister").get().addOnSuccessListener { legacyPre ->
                            database.getReference("students_register").get().addOnSuccessListener { legacyRegister ->
                                val completedIds = users.children.mapNotNull { it.key }.toSet()
                                val preIds = preStudents.children.mapNotNull { it.key }.toSet()
                                val unregisteredIds = unregistered.children.mapNotNull { it.key }.toSet()
                                val brokenTags = tags.children.count { tag ->
                                    val uid = tag.child("studentUid").getValue(String::class.java).orEmpty()
                                    val preId = tag.child("preStudentId").getValue(String::class.java).orEmpty()
                                    val unId = tag.child("unregisteredStudentId").getValue(String::class.java).orEmpty()
                                    (uid.isNotBlank() && uid !in completedIds) ||
                                        (preId.isNotBlank() && preId !in preIds) ||
                                        (unId.isNotBlank() && unId !in unregisteredIds) ||
                                        (uid.isBlank() && preId.isBlank() && unId.isBlank())
                                }
                                summary = DataAuditSummary(
                                    users.childrenCount.toInt(), preStudents.childrenCount.toInt(),
                                    unregistered.childrenCount.toInt(), tags.childrenCount.toInt(),
                                    brokenTags, legacyPre.childrenCount.toInt(), legacyRegister.childrenCount.toInt()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            top = UiConfig.topPadding, start = UiConfig.sidePadding,
            end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
        ), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("데이터 점검", style = MaterialTheme.typography.headlineMedium)
        Text("구조를 점검하고, 백업된 구형 사전등록 데이터만 통합할 수 있습니다.")

        if (summary == null) {
            Text("점검 정보를 불러오는 중입니다.")
        } else {
            AuditCard("학생 데이터", listOf(
                "가입 완료 학생  ${summary!!.users}명",
                "앱 가입 예정 학생  ${summary!!.preStudents}명",
                "앱 미사용 학생  ${summary!!.unregisteredStudents}명"
            ))
            AuditCard("NFC 연결", listOf(
                "등록된 NFC 태그  ${summary!!.nfcTags}개",
                "확인이 필요한 NFC 연결  ${summary!!.brokenNfcTags}개"
            ))
            AuditCard("이전 데이터", listOf(
                "preregister  ${summary!!.legacyPreRegister}개",
                "students_register  ${summary!!.legacyStudentRegister}개",
                "이전 실행 시 원본 경로의 값은 제거됩니다."
            ))
            if (summary!!.legacyPreRegister + summary!!.legacyStudentRegister > 0) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { migrateLegacyPreStudents() }
                ) {
                    Text("구형 사전등록 데이터를 preStudents로 이전")
                }
            }
            if (migrationMessage.isNotBlank()) {
                Text(migrationMessage, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun AuditCard(title: String, lines: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            lines.forEach { Text(it) }
        }
    }
}

private data class DataAuditSummary(
    val users: Int,
    val preStudents: Int,
    val unregisteredStudents: Int,
    val nfcTags: Int,
    val brokenNfcTags: Int,
    val legacyPreRegister: Int,
    val legacyStudentRegister: Int
)
