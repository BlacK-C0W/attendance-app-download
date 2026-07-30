package com.example.attendanceappfinal.repository

import com.example.attendanceappfinal.model.Timetable
import com.google.firebase.database.DataSnapshot


fun DataSnapshot.toTimetableSafe(): Timetable? {


    return try {


        // 문자열 데이터 방어
        if(value is String){

            return null

        }


        getValue(
            Timetable::class.java
        )


    }catch(e:Exception){


        null


    }


}