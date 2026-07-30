package com.example.attendanceappfinal.model


data class PreStudent(

    var id: String = "",

    var name: String = "",

    var grade: String = "",

    var className: String = "",

    var nfcTag: String = "",

    var phone:String = "",


    var hasNfc: Boolean = false,

    var registered: Boolean = false

)
