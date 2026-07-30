package com.example.attendanceappfinal.ui

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.attendanceappfinal.model.User

/** A single visual vocabulary for the three student data sources. */
@Composable
fun StudentTypeBadge(student: User) {
    val (label, color) = when {
        student.isPreStudent -> "앱 가입 예정" to MaterialTheme.colorScheme.tertiaryContainer
        student.isUnregisteredStudent -> "앱 미사용 학생" to MaterialTheme.colorScheme.secondaryContainer
        else -> "가입 완료" to MaterialTheme.colorScheme.primaryContainer
    }

    AssistChip(
        onClick = {},
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(containerColor = color)
    )
}
