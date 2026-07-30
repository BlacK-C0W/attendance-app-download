package com.example.attendanceappfinal.model


data class Attendance(

    val id:String = "",

    val studentUid:String = "",

    val studentName:String = "",

    val subject:String = "",

    val teacher:String = "",

    val teacherUid:String = "",

    val date:String = "",

    val time:String = "",

    val status:String = "",

    val reason:String = "",

    val nfcTag:String = "",

    val modified:Boolean = false,

    val modifiedBy:String = "",

    val modifiedTime:String = "",

    val notificationSent:Boolean = false,

    val timestamp:Long = 0L

)