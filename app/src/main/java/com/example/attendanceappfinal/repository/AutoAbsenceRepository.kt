package com.example.attendanceappfinal.repository

import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.AttendanceLog
import com.example.attendanceappfinal.model.Holiday
import com.example.attendanceappfinal.model.Notification
import com.example.attendanceappfinal.model.PreStudent
import com.example.attendanceappfinal.model.Timetable
import com.example.attendanceappfinal.model.UnregisteredStudent
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Checks expired lessons while a teacher is using the app. All writes are idempotent. */
object AutoAbsenceRepository {
    private val database = FirebaseDatabase.getInstance()
    private val koreaZone = ZoneId.of("Asia/Seoul")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private var monitoredTeacherUid = ""
    private var timetableListener: ValueEventListener? = null
    private var holidayListener: ValueEventListener? = null
    private var cachedTimetable = emptyList<Timetable>()
    private var cachedHolidays = emptyList<Holiday>()
    private var hasTimetableCache = false
    private var hasHolidayCache = false
    private var processedDate = ""
    private val processedLessonKeys = mutableSetOf<String>()
    private var cachedStudents = emptyList<User>()
    private var studentCacheUpdatedAt = 0L
    private const val STUDENT_CACHE_TTL_MS = 5 * 60 * 1000L

    /**
     * Keeps exactly two lightweight realtime listeners active. After their first
     * response, minute ticks use only memory and make no timetable/holiday reads.
     */
    fun startMonitoring(teacherUid: String) {
        if (teacherUid.isBlank() || monitoredTeacherUid == teacherUid) return
        stopMonitoring()
        monitoredTeacherUid = teacherUid
        timetableListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cachedTimetable = snapshot.children.mapNotNull { child ->
                    child.getValue(Timetable::class.java)?.copy(id = child.key ?: "")
                }
                hasTimetableCache = true
            }
            override fun onCancelled(error: DatabaseError) { hasTimetableCache = false }
        }
        holidayListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cachedHolidays = snapshot.children.mapNotNull { it.getValue(Holiday::class.java) }
                hasHolidayCache = true
            }
            override fun onCancelled(error: DatabaseError) { hasHolidayCache = false }
        }
        database.getReference("teacherTimetable").child(teacherUid).addValueEventListener(timetableListener!!)
        database.getReference("holidays").addValueEventListener(holidayListener!!)
    }

    fun stopMonitoring(teacherUid: String = "") {
        if (teacherUid.isNotBlank() && teacherUid != monitoredTeacherUid) return
        timetableListener?.let { database.getReference("teacherTimetable").child(monitoredTeacherUid).removeEventListener(it) }
        holidayListener?.let { database.getReference("holidays").removeEventListener(it) }
        timetableListener = null
        holidayListener = null
        monitoredTeacherUid = ""
        cachedTimetable = emptyList()
        cachedHolidays = emptyList()
        hasTimetableCache = false
        hasHolidayCache = false
        processedLessonKeys.clear()
        processedDate = ""
        cachedStudents = emptyList()
        studentCacheUpdatedAt = 0L
    }

    fun checkExpiredClasses(teacherUid: String, onCompleted: (Int) -> Unit = {}) {
        if (teacherUid.isBlank() || teacherUid != monitoredTeacherUid || !hasTimetableCache || !hasHolidayCache) return onCompleted(0)
        val now = LocalDateTime.now(koreaZone)
        val date = now.toLocalDate().toString()
        if (processedDate != date) {
            processedDate = date
            processedLessonKeys.clear()
        }
        if (isAcademyClosed(now.toLocalDate())) return onCompleted(0)
        val expired = cachedTimetable.filter { isToday(it, now.toLocalDate()) && hasEnded(it, now.toLocalTime()) }
            .filter { lessonKey(it, date) !in processedLessonKeys }
        if (expired.isEmpty()) return onCompleted(0)
        loadAllStudents { students ->
            var remaining = expired.size
            var created = 0
            expired.forEach { timetable ->
                createAbsencesForClass(timetable, students, now) { count ->
                    created += count
                    processedLessonKeys.add(lessonKey(timetable, date))
                    if (--remaining == 0) onCompleted(created)
                }
            }
        }
    }

    private fun isAcademyClosed(date: LocalDate): Boolean {
        val dateText = date.toString()
        val customClosed = cachedHolidays.any { it.type == "academyVacation" && dateText >= it.date && dateText <= it.endDate }
        val classDays = cachedHolidays.filter { it.type == "publicHolidayClassDay" }.map { it.date }.toSet()
        val publicClosed = KoreanHolidayRepository.publicHolidays(date.year).any {
            dateText >= it.date && dateText <= it.endDate && it.date !in classDays
        }
        return customClosed || publicClosed
    }

    private fun lessonKey(timetable: Timetable, date: String): String =
        "$date|${timetable.id}|${timetable.grade}|${timetable.className}|${timetable.subject}|${timetable.startTime}|${timetable.endTime}"

    private fun loadAllStudents(onLoaded: (List<User>) -> Unit) {
        if (cachedStudents.isNotEmpty() && System.currentTimeMillis() - studentCacheUpdatedAt < STUDENT_CACHE_TTL_MS) {
            return onLoaded(cachedStudents)
        }
        database.getReference("users").get().addOnSuccessListener { userSnapshot ->
            val registered = userSnapshot.children.mapNotNull { child ->
                child.getValue(User::class.java)?.copy(uid = child.key ?: "")
            }.filter { it.role == "student" }
            database.getReference("preStudents").get().addOnSuccessListener { preSnapshot ->
                val preStudents = preSnapshot.children.mapNotNull { child ->
                    child.getValue(PreStudent::class.java)?.copy(id = child.key ?: "")
                }.map {
                    User(uid = "pre_${it.id}", name = it.name, phone = it.phone, grade = it.grade,
                        className = it.className, role = "student", isPreStudent = true, preStudentId = it.id)
                }
                database.getReference("unregisteredStudents").get().addOnSuccessListener { unSnapshot ->
                    val unregistered = unSnapshot.children.mapNotNull { child ->
                        child.getValue(UnregisteredStudent::class.java)?.copy(id = child.key ?: "")
                    }.map {
                        User(uid = "un_${it.id}", name = it.name, phone = it.phone, grade = it.grade,
                            className = it.className, role = "student", isUnregisteredStudent = true,
                            unregisteredStudentId = it.id)
                    }
                    completeStudentLoad(registered + preStudents + unregistered, onLoaded)
                }.addOnFailureListener { completeStudentLoad(registered + preStudents, onLoaded) }
            }.addOnFailureListener { completeStudentLoad(registered, onLoaded) }
        }.addOnFailureListener { completeStudentLoad(emptyList(), onLoaded) }
    }

    private fun completeStudentLoad(students: List<User>, onLoaded: (List<User>) -> Unit) {
        cachedStudents = students
        studentCacheUpdatedAt = System.currentTimeMillis()
        onLoaded(students)
    }

    private fun createAbsencesForClass(timetable: Timetable, allStudents: List<User>, now: LocalDateTime, onCompleted: (Int) -> Unit) {
        val students = allStudents.filter {
            it.className.trim() == timetable.className.trim() &&
                (timetable.grade.isBlank() || normalizeGrade(it.grade) == normalizeGrade(timetable.grade))
        }
        if (students.isEmpty()) return onCompleted(0)
        var remaining = students.size
        var created = 0
        students.forEach { student ->
            createAbsenceIfMissing(student, timetable, now) { wasCreated ->
                if (wasCreated) created++
                if (--remaining == 0) onCompleted(created)
            }
        }
    }

    private fun createAbsenceIfMissing(student: User, timetable: Timetable, now: LocalDateTime, onCompleted: (Boolean) -> Unit) {
        val date = now.toLocalDate().toString()
        val path = attendanceStoragePath(student.uid)
        val attendanceRef = database.getReference(path.root).child(path.studentId)
        val recordId = "${date}_${student.uid}_${timetable.subject}"

        // Registered students, NFC, and manual attendance all use this deterministic
        // key. Read that single record instead of downloading the student's history.
        attendanceRef.child(recordId).get().addOnSuccessListener { classRecord ->
            if (classRecord.exists()) return@addOnSuccessListener onCompleted(false)

            if (student.isPreStudent || student.isUnregisteredStudent) {
                // NFC for non-app students uses a date-and-id key without a subject.
                // Check that one small record as well, so a valid NFC scan is never
                // turned into an absence later in the day.
                val nfcRecordId = "${date}_${path.studentId}"
                attendanceRef.child(nfcRecordId).get().addOnSuccessListener { nfcRecord ->
                    if (nfcRecord.exists()) onCompleted(false)
                    else writeAbsence(attendanceRef, recordId, student, timetable, now, date, onCompleted)
                }.addOnFailureListener { onCompleted(false) }
            } else {
                writeAbsence(attendanceRef, recordId, student, timetable, now, date, onCompleted)
            }
        }.addOnFailureListener { onCompleted(false) }
    }

    private fun writeAbsence(
        attendanceRef: com.google.firebase.database.DatabaseReference,
        recordId: String,
        student: User,
        timetable: Timetable,
        now: LocalDateTime,
        date: String,
        onCompleted: (Boolean) -> Unit
    ) {
        val absence = Attendance(
            id = recordId, studentUid = student.uid, studentName = student.name.ifBlank { "이름 없음" },
            subject = timetable.subject, teacher = timetable.teacher, teacherUid = timetable.teacherUid,
            date = date, time = now.toLocalTime().format(timeFormatter), status = "결석",
            reason = "수업 종료 후 자동 결석 처리",
            notificationSent = !student.isPreStudent && !student.isUnregisteredStudent,
            timestamp = System.currentTimeMillis()
        )
        attendanceRef.child(recordId).runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    return if (currentData.value == null) {
                        currentData.value = absence
                        Transaction.success(currentData)
                    } else Transaction.abort()
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (committed) {
                        saveAutoAbsenceLog(student, timetable, now)
                        if (!student.isPreStudent && !student.isUnregisteredStudent) {
                            saveAbsenceNotifications(student, timetable, date)
                        }
                    }
                    onCompleted(committed)
                }
        })
    }

    private fun saveAutoAbsenceLog(student: User, timetable: Timetable, now: LocalDateTime) {
        database.getReference("attendance_logs").push().setValue(
            AttendanceLog(
                studentUid = student.uid,
                studentName = student.name.ifBlank { "이름 없음" },
                subject = timetable.subject,
                afterStatus = "결석",
                reason = "수업 종료 후 자동 결석 처리",
                time = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                grade = student.grade,
                className = student.className,
                automatic = true
            )
        )
    }

    private fun saveAbsenceNotifications(student: User, timetable: Timetable, date: String) {
        val title = "자동 결석 처리"
        val body = "$date ${timetable.subject} 수업에 출석 기록이 없어 결석 처리되었습니다."
        NotificationRepository.saveNotification(
            Notification(studentUid = student.uid, title = title, message = body, timestamp = System.currentTimeMillis())
        )
        val parentRef = database.getReference("parentNotifications").child(student.uid).push()
        parentRef.setValue(mapOf(
            "id" to (parentRef.key ?: ""), "studentUid" to student.uid, "title" to title,
            "message" to "${student.name} 학생이 $body", "timestamp" to System.currentTimeMillis(), "read" to false
        ))
    }

    private fun isToday(timetable: Timetable, today: LocalDate): Boolean {
        val expected = when (today.dayOfWeek) {
            DayOfWeek.MONDAY -> setOf("월", "월요일")
            DayOfWeek.TUESDAY -> setOf("화", "화요일")
            DayOfWeek.WEDNESDAY -> setOf("수", "수요일")
            DayOfWeek.THURSDAY -> setOf("목", "목요일")
            DayOfWeek.FRIDAY -> setOf("금", "금요일")
            DayOfWeek.SATURDAY -> setOf("토", "토요일")
            DayOfWeek.SUNDAY -> setOf("일", "일요일")
        }
        return timetable.day.trim() in expected
    }

    private fun hasEnded(timetable: Timetable, now: LocalTime): Boolean = try {
        !now.isBefore(LocalTime.parse(timetable.endTime, timeFormatter))
    } catch (_: Exception) { false }
}

private fun normalizeGrade(value: String): String =
    value.replace(" ", "").replace("학년", "").trim()
