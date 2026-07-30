package com.example.attendanceappfinal.util

import android.content.Context
import android.content.ClipData
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.User
import java.io.File
import java.nio.charset.StandardCharsets

fun shareAttendanceCsv(
    context: Context,
    grade: String,
    students: List<User>,
    attendance: List<Attendance>
) {
    val studentNames = students.associate { it.uid to it.name }
    val directory = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(directory, "${grade}_출결_${System.currentTimeMillis()}.csv")
    val rows = buildString {
        append('\uFEFF') // Excel opens UTF-8 Korean text correctly.
        appendLine("이름,학년,반,날짜,시간,과목,상태,담당 선생님")
        attendance.sortedWith(compareByDescending<Attendance> { it.date }.thenBy { it.studentName })
            .forEach { record ->
                val name = record.studentName.ifBlank { studentNames[record.studentUid].orEmpty() }
                val student = students.firstOrNull { it.uid == record.studentUid }
                appendLine(listOf(name, grade, student?.className.orEmpty(), csvDate(record.date), csvTime(record.time),
                    record.subject, record.status, record.teacher).joinToString(",") { csvValue(it) })
            }
    }
    file.writeText(rows, StandardCharsets.UTF_8)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/comma-separated-values"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("attendance_csv", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "출결 CSV 공유"))
}

private fun csvValue(value: String): String = "\"${value.replace("\"", "\"\"")}\""

/** Keep dates as text so Excel does not render a narrow date column as ####. */
private fun csvDate(value: String): String {
    val parts = value.split("-")
    return if (parts.size == 3) "${parts[0]}년 ${parts[1]}월 ${parts[2]}일" else value
}

/** Export only hour and minute, keeping a stable 24-hour HH:mm display in Excel. */
private fun csvTime(value: String): String {
    val parts = value.split(":")
    return if (parts.size == 2 || parts.size == 3) {
        "\t${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
    } else {
        value
    }
}
