package com.example.attendanceappfinal.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.FirebaseDatabase

@Composable
fun TeacherFirstLoginPage(
    user: User,
    onComplete: (User) -> Unit
) {
    BackHandler { /* 첫 설정 화면에서는 뒤로 가기를 막습니다. */ }

    val database = FirebaseDatabase.getInstance()
    var temporaryPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    fun errorMessage(prefix: String, error: Exception): String {
        val code = (error as? FirebaseAuthException)?.errorCode ?: "UNKNOWN"
        return "$prefix [$code]: ${error.message ?: "알 수 없는 오류"}"
    }

    fun updatePassword() {
        if (temporaryPassword.isBlank() || newPassword.isBlank()) {
            message = "임시 비밀번호와 새 비밀번호를 입력해주세요."
            return
        }
        if (newPassword != confirmPassword) {
            message = "새 비밀번호가 일치하지 않습니다."
            return
        }

        val authUser = FirebaseAuth.getInstance().currentUser
        val email = authUser?.email
        if (authUser == null || authUser.uid != user.uid || email.isNullOrBlank()) {
            message = "로그인 정보를 확인하지 못했습니다. 다시 로그인해 주세요."
            return
        }

        // Firebase는 비밀번호 변경을 위해 최근 로그인한 인증 정보를 요구합니다.
        val credential = EmailAuthProvider.getCredential(email, temporaryPassword)
        authUser.reauthenticate(credential)
            .addOnSuccessListener {
                authUser.updatePassword(newPassword)
                    .addOnSuccessListener {
                        val updateUser = user.copy(tempPassword = "", firstLogin = false)
                        database.getReference("users").child(user.uid).setValue(updateUser)
                            .addOnSuccessListener {
                                message = "비밀번호 변경 완료"
                                onComplete(updateUser)
                            }
                            .addOnFailureListener { error ->
                                message = errorMessage("프로필 저장 실패", error)
                            }
                    }
                    .addOnFailureListener { error ->
                        message = errorMessage("비밀번호 변경 실패", error)
                    }
            }
            .addOnFailureListener { error ->
                message = errorMessage("임시 비밀번호 확인 실패", error)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = UiConfig.topPadding,
                start = UiConfig.sidePadding,
                end = UiConfig.sidePadding,
                bottom = UiConfig.bottomPadding
            )
    ) {
        Text("선생님 계정 설정", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))
        Text("아이디")
        Text(user.loginId, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))
        Text("임시 비밀번호를 확인한 뒤 새 비밀번호를 설정해주세요.")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = temporaryPassword,
            onValueChange = { temporaryPassword = it },
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("임시 비밀번호") }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = newPassword,
            onValueChange = { newPassword = it },
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("새 비밀번호") }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("새 비밀번호 확인") }
        )
        Spacer(Modifier.height(20.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = ::updatePassword) {
            Text("비밀번호 변경 완료")
        }
        Spacer(Modifier.height(15.dp))
        if (message.isNotBlank()) Text(message)
    }
}
