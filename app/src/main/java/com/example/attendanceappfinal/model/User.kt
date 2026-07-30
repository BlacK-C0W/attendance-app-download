package com.example.attendanceappfinal.model


data class User(

    val uid:String = "",

    val name:String = "",

    val phone:String = "",

    val email:String = "",

    val role:String = "student",

    val grade:String = "",

    val className:String = "",

    val parentPhone:String = "",

    val schoolName:String = "",

    val mathScore:String = "",

    val scienceScore:String = "",

    val englishScore:String = "",

    val linkedStudentId:String = "",

    val parentInviteCode:String = "",

    val nfcId:String = "",

    val createdAt:Long = 0L,


    val loginId:String = "",


    val tempPassword:String = "",

    val firstLogin:Boolean = false,


    // 앱 가입 예정 학생
    val isPreStudent:Boolean = false,

    val preStudentId:String = "",


    // 앱 미가입 학생 (NFC/수동 출결 대상)
    val isUnregisteredStudent:Boolean = false,

    val unregisteredStudentId:String = ""

)
