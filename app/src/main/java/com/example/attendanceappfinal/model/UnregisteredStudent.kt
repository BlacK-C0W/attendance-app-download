package com.example.attendanceappfinal.model


data class UnregisteredStudent(

    val id:String = "",

    val name:String = "",

    val phone:String = "",

    val grade:String = "",

    val className:String = "",

    val nfcTag:String = "",

    val hasNfc:Boolean = false,

    val createdAt:Long = 0L

)