package com.example.attendanceappfinal.repository

/** Pure mapping shared by attendance writers and unit tests. */
data class AttendanceStoragePath(val root: String, val studentId: String)

fun attendanceStoragePath(studentUid: String): AttendanceStoragePath = when {
    studentUid.startsWith("pre_") -> AttendanceStoragePath("preAttendance", studentUid.removePrefix("pre_"))
    studentUid.startsWith("un_") -> AttendanceStoragePath("unregisteredAttendance", studentUid.removePrefix("un_"))
    else -> AttendanceStoragePath("attendance", studentUid)
}
