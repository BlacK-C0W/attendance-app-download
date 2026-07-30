package com.example.attendanceappfinal.notification


import android.content.Context
import com.google.firebase.database.*


class AttendanceListener(

    private val context: Context,

    private val studentUid: String

) {


    private val database =
        FirebaseDatabase.getInstance()


    fun start(){


        database
            .getReference("attendance")
            .addChildEventListener(

                object : ChildEventListener {


                    override fun onChildAdded(
                        snapshot: DataSnapshot,
                        previousChildName: String?
                    ) {


                        checkAttendance(snapshot)


                    }



                    override fun onChildChanged(
                        snapshot: DataSnapshot,
                        previousChildName: String?
                    ) {


                        checkAttendance(snapshot)


                    }



                    override fun onChildRemoved(
                        snapshot: DataSnapshot
                    ) {}



                    override fun onChildMoved(
                        snapshot: DataSnapshot,
                        previousChildName: String?
                    ) {}



                    override fun onCancelled(
                        error: DatabaseError
                    ) {}

                }

            )

    }



    private fun checkAttendance(
        snapshot: DataSnapshot
    ){


        val uid =
            snapshot.child("studentUid")
                .getValue(String::class.java)



        if(uid != studentUid){

            return

        }



        val status =
            snapshot.child("status")
                .getValue(String::class.java)
                ?: return



        val name =
            snapshot.child("studentName")
                .getValue(String::class.java)
                ?: ""



        AttendanceNotificationManager.show(

            context,

            "출석 상태 변경",

            "$name 학생이 $status 처리되었습니다."

        )


    }


}