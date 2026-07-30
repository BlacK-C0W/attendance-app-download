package com.example.attendanceappfinal.repository


import android.util.Log
import com.example.attendanceappfinal.model.Notification
import com.google.firebase.database.FirebaseDatabase



object NotificationRepository {


    private val database =

        FirebaseDatabase

            .getInstance()

            .reference





    fun saveNotification(

        notification: Notification,

        onSuccess: () -> Unit = {},

        onFail: (String) -> Unit = {}

    ){



        val ref =

            database

                .child("notifications")

                .child(notification.studentUid)

                .push()





        val id = ref.key



        if(id == null){


            Log.e(

                "NOTIFICATION_TEST",

                "알림 ID 생성 실패"

            )


            onFail("알림 ID 생성 실패")

            return


        }






        val data = notification.copy(

            id = id

        )






        Log.d(

            "NOTIFICATION_TEST",

            "저장 시도 : $data"

        )







        ref.setValue(data)

            .addOnSuccessListener {



                Log.d(

                    "NOTIFICATION_TEST",

                    "알림 저장 성공"

                )



                onSuccess()



            }



            .addOnFailureListener {



                Log.e(

                    "NOTIFICATION_TEST",

                    "알림 저장 실패 : ${it.message}"

                )



                onFail(

                    it.message ?: "알림 저장 실패"

                )



            }



    }





}