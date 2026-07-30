package com.example.attendanceappfinal

import org.junit.Test

import org.junit.Assert.*
import com.example.attendanceappfinal.repository.attendanceStoragePath

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun attendance_storage_path_separates_three_student_types() {
        assertEquals("attendance", attendanceStoragePath("uid-1").root)
        assertEquals("uid-1", attendanceStoragePath("uid-1").studentId)
        assertEquals("preAttendance", attendanceStoragePath("pre_waiting-1").root)
        assertEquals("waiting-1", attendanceStoragePath("pre_waiting-1").studentId)
        assertEquals("unregisteredAttendance", attendanceStoragePath("un_card-1").root)
        assertEquals("card-1", attendanceStoragePath("un_card-1").studentId)
    }
}
