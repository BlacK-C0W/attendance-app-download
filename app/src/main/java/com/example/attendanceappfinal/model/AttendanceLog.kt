package com.example.attendanceappfinal.model


data class AttendanceLog(

    val studentUid:String="",

    val studentName:String="",

    val subject:String="",

    val beforeStatus:String="",

    val afterStatus:String="",

    val reason:String="",

    val time:String="",

    val grade:String="",

    val className:String="",

    val automatic:Boolean=false

)
