package com.example.attendanceappfinal.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.attendanceappfinal.MainActivity


class MyFirebaseMessagingService :
    FirebaseMessagingService(){

    override fun onNewToken(token: String) {

        super.onNewToken(token)

        println("FCM TOKEN : $token")

    }

    override fun onMessageReceived(
        message: RemoteMessage
    ){

        super.onMessageReceived(message)

        val title =
            message.notification?.title
                ?: "출결 알림"


        val body =
            message.notification?.body
                ?: ""



        showNotification(
            title,
            body
        )


    }





    private fun showNotification(

        title:String,

        body:String

    ){


        val manager =
            getSystemService(
                NotificationManager::class.java
            )



        val channelId =
            "attendance_channel"



        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){


            val channel =
                NotificationChannel(

                    channelId,

                    "출결 알림",

                    NotificationManager.IMPORTANCE_HIGH

                )


            channel.enableVibration(true)

            channel.vibrationPattern =
                longArrayOf(
                    0,
                    300,
                    200,
                    300
                )


            manager.createNotificationChannel(
                channel
            )

        }





        val intent =
            Intent(
                this,
                MainActivity::class.java
            )



        val pendingIntent =
            PendingIntent.getActivity(

                this,

                0,

                intent,

                PendingIntent.FLAG_IMMUTABLE

            )





        val notification =
            NotificationCompat.Builder(

                this,

                channelId

            )

                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )

                .setContentTitle(title)

                .setContentText(body)

                .setContentIntent(
                    pendingIntent
                )

                .setAutoCancel(true)

                .build()



        manager.notify(

            100,

            notification

        )


    }




}