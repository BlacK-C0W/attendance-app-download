package com.example.attendanceappfinal.model


data class Notification(

    val id:String = "",

    val studentUid:String = "",

    val title:String = "",

    val message:String = "",

    val timestamp:Long = 0L,

    val read:Boolean = false

)