package com.example.attendanceappfinal.admin

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase

/** Parent contact and in-app notice staging, ready for a future FCM sender. */
@Composable
fun ParentCommunicationPage(student: User, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var title by remember { mutableStateOf("학원 안내") }
    var message by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    val phone = student.parentPhone
    Column(Modifier.fillMaxSize().padding(
        top = UiConfig.topPadding, start = UiConfig.sidePadding,
        end = UiConfig.sidePadding, bottom = UiConfig.bottomPadding
    ), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("학부모 연락 · 안내", style = MaterialTheme.typography.headlineMedium)
        Text("${student.name} 학생 · ${phone.ifBlank { "부모님 연락처 미등록" }}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = phone.isNotBlank(), onClick = {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            }) { Text("전화") }
            OutlinedButton(enabled = phone.isNotBlank(), onClick = {
                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")))
            }) { Text("문자") }
        }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
            val code = (100000..999999).random().toString()
            FirebaseDatabase.getInstance().getReference("parentInvites").child(code).setValue(
                mapOf("studentUid" to student.uid, "used" to false,
                    "createdAt" to System.currentTimeMillis())
            ).addOnSuccessListener { inviteCode = code }
        }) { Text("학부모 초대 코드 발급") }
        if (inviteCode.isNotBlank()) Text("초대 코드: $inviteCode", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("제목") }, singleLine = true)
        OutlinedTextField(message, { message = it }, Modifier.fillMaxWidth().weight(1f), label = { Text("안내 내용") })
        Button(modifier = Modifier.fillMaxWidth(), enabled = title.isNotBlank() && message.isNotBlank(), onClick = {
            val ref = FirebaseDatabase.getInstance().getReference("parentNotifications").child(student.uid).push()
            ref.setValue(mapOf("id" to (ref.key ?: ""), "title" to title, "message" to message,
                "studentUid" to student.uid, "parentPhone" to phone, "timestamp" to System.currentTimeMillis()))
                .addOnSuccessListener { result = "학부모 앱 알림용 안내를 저장했습니다."; message = "" }
                .addOnFailureListener { result = "저장하지 못했습니다." }
        }) { Text("안내 저장") }
        Text("현재는 앱 알림용 기록만 저장합니다. 추후 FCM 연결 시 같은 기록으로 푸시를 발송할 수 있습니다.", style = MaterialTheme.typography.bodySmall)
        if (result.isNotBlank()) Text(result, color = MaterialTheme.colorScheme.primary)
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onBack) { Text("뒤로가기") }
    }
}
