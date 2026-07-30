package com.example.attendanceappfinal.repository


import android.util.Log
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.AttendanceLog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


object AttendanceRepository {


    fun saveAttendance(

        attendance: Attendance,

        onSuccess: () -> Unit = {},

        onFail: (String) -> Unit = {}

    ){

        // Manual attendance used to pass an empty id, overwriting
        // attendance/{uid}. Always create a record key before writing.
        val savedAttendance = if(attendance.id.isBlank()) {
            attendance.copy(
                id = "${attendance.date}_${attendance.studentUid}_${attendance.subject}_${attendance.timestamp}"
            )
        } else {
            attendance
        }

        val storagePath = attendanceStoragePath(savedAttendance.studentUid)

        FirebaseDatabase

            .getInstance()

            .getReference(storagePath.root)

            .child(storagePath.studentId)

            .child(savedAttendance.id)

            .setValue(savedAttendance)

            .addOnSuccessListener {

                onSuccess()

            }

            .addOnFailureListener {

                onFail(
                    it.message ?: "저장 실패"
                )

            }

    }

    fun getAttendanceFlow(

        uid: String

    ): Flow<List<Attendance>> = callbackFlow {



        val reference =

            FirebaseDatabase

                .getInstance()

                .getReference("attendance")

                .child(uid)





        val listener = object : ValueEventListener {



            override fun onDataChange(

                snapshot: DataSnapshot

            ) {



                val list =

                    mutableListOf<Attendance>()





                snapshot.children.forEach { child ->



                    try {



                        val attendance =

                            child.getValue(

                                Attendance::class.java

                            )





                        if(attendance != null){



                            list.add(

                                attendance.copy(

                                    id =

                                        child.key ?: ""

                                )

                            )

                        }





                    }catch(e:Exception){



                        Log.e(

                            "ATTENDANCE_FLOW",

                            "출결 데이터 변환 실패 : ${child.key}"

                        )

                    }


                }







                trySend(

                    list.sortedByDescending {

                        it.timestamp

                    }

                )



            }








            override fun onCancelled(

                error: DatabaseError

            ){



                close(

                    error.toException()

                )

            }



        }







        reference.addValueEventListener(

            listener

        )







        awaitClose {



            reference.removeEventListener(

                listener

            )

        }



    }








    fun getAttendanceLogFlow():

            Flow<List<AttendanceLog>> = callbackFlow {





        val reference =

            FirebaseDatabase

                .getInstance()

                .getReference("attendance_logs")








        val listener = object : ValueEventListener {



            override fun onDataChange(

                snapshot: DataSnapshot

            ){



                val list =

                    mutableListOf<AttendanceLog>()






                snapshot.children.forEach { child ->



                    try {



                        child.getValue(

                            AttendanceLog::class.java

                        )

                            ?.let { data ->


                                list.add(data)


                            }




                    }catch(e:Exception){



                        Log.e(

                            "ATTENDANCE_LOG",

                            "출결 로그 변환 실패 : ${child.key}"

                        )


                    }


                }







                trySend(

                    list

                )



            }








            override fun onCancelled(

                error: DatabaseError

            ){



                close(

                    error.toException()

                )

            }




        }







        reference.addValueEventListener(

            listener

        )







        awaitClose {



            reference.removeEventListener(

                listener

            )

        }



    }



}
