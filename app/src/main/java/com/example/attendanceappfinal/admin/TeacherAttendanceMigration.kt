package com.example.attendanceappfinal.admin

import com.google.firebase.database.FirebaseDatabase

/**
 * Keeps historical attendance linked to a teacher when that teacher account is
 * reissued. Attendance is stored beneath three roots, so all of them must move
 * together with the timetable.
 */
fun migrateAttendanceTeacherReferences(
    database: FirebaseDatabase,
    oldTeacherUid: String,
    newTeacherUid: String,
    onSuccess: (Map<String, Any?>) -> Unit,
    onFailure: (String) -> Unit
) {
    val roots = listOf("attendance", "preAttendance", "unregisteredAttendance")
    val updates = mutableMapOf<String, Any?>()
    var completed = 0
    var error: String? = null

    roots.forEach { root ->
        database.getReference(root).get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach { studentNode ->
                    val studentKey = studentNode.key ?: return@forEach
                    studentNode.children.forEach { recordNode ->
                        val recordKey = recordNode.key ?: return@forEach
                        if (recordNode.child("teacherUid").getValue(String::class.java) == oldTeacherUid) {
                            updates["$root/$studentKey/$recordKey/teacherUid"] = newTeacherUid
                        }
                    }
                }
            }
            .addOnFailureListener { error = it.message ?: "출석 기록을 읽을 수 없습니다." }
            .addOnCompleteListener {
                completed++
                if (completed == roots.size) {
                    error?.let(onFailure) ?: onSuccess(updates)
                }
            }
    }
}
