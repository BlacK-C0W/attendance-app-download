package com.example.attendanceappfinal.teacher

import com.google.firebase.database.FirebaseDatabase


object TimetableRepository {


    private val database =
        FirebaseDatabase.getInstance()
            .reference



    fun getTodayClass(
        onResult:(String,String)->Unit
    ){


        val calendar =
            java.util.Calendar.getInstance()



        val day =

            when(
                calendar.get(
                    java.util.Calendar.DAY_OF_WEEK
                )
            ){

                java.util.Calendar.MONDAY ->
                    "monday"

                java.util.Calendar.TUESDAY ->
                    "tuesday"

                java.util.Calendar.WEDNESDAY ->
                    "wednesday"

                java.util.Calendar.THURSDAY ->
                    "thursday"

                java.util.Calendar.FRIDAY ->
                    "friday"

                else ->
                    "monday"

            }





        database

            .child("timetable")

            .child(day)

            .get()

            .addOnSuccessListener { snapshot ->



                if(!snapshot.exists()){


                    onResult(
                        "수업",
                        "선생님"
                    )


                    return@addOnSuccessListener

                }




                val first =

                    snapshot.children.firstOrNull()



                val subject =

                    first?.child("subject")
                        ?.value
                        ?.toString()
                        ?: "수업"



                val teacher =

                    first?.child("teacher")
                        ?.value
                        ?.toString()
                        ?: "선생님"




                onResult(

                    subject,

                    teacher

                )



            }





    }


}