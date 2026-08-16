package com.example.attendanceappfinal.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun ParentRegisterPage(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var id by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }; var message by remember { mutableStateOf("") }
    val database = FirebaseDatabase.getInstance()
    Column(Modifier.fillMaxSize().padding(top = UiConfig.topPadding, start = UiConfig.sidePadding, end = UiConfig.sidePadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("학부모 회원가입", style = MaterialTheme.typography.headlineMedium)
        Text("관리자에게 받은 6자리 초대 코드를 입력하세요.")
        OutlinedTextField(id, { id = it }, Modifier.fillMaxWidth(), label = { Text("아이디") }, singleLine = true)
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("비밀번호") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("이름") }, singleLine = true)
        OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("초대 코드") }, singleLine = true)
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            if (id.isBlank() || password.isBlank() || name.isBlank() || code.length != 6) { message = "모든 정보를 입력해 주세요."; return@Button }
            FirebaseAuth.getInstance().createUserWithEmailAndPassword("$id@attendance.com", password).addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                database.getReference("parentInvites").child(code).get().addOnSuccessListener { invite ->
                    if (!invite.exists() || invite.child("used").getValue(Boolean::class.java) == true) {
                        result.user?.delete()
                        message = "유효하지 않은 초대 코드입니다."
                        return@addOnSuccessListener
                    }
                    val studentUid = invite.child("studentUid").getValue(String::class.java).orEmpty()
                    val user = User(uid = uid, name = name, loginId = id, role = "parent", linkedStudentId = studentUid, parentInviteCode = code)
                    database.reference.updateChildren(mapOf(
                        "users/$uid" to user,
                        "parentInvites/$code/used" to true
                    )).addOnSuccessListener { message = "가입 완료. 로그인해 주세요." }
                }.addOnFailureListener {
                    result.user?.delete()
                    message = "초대 코드를 확인하지 못했습니다."
                }
            }.addOnFailureListener { message = it.message ?: "가입하지 못했습니다." }
        }) { Text("가입") }
        if (message.isNotBlank()) Text(message)
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onBack) { Text("로그인으로") }
    }
}
