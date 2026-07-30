package com.example.attendanceappfinal.notification


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.attendanceappfinal.R


object AttendanceNotificationManager {


    private const val CHANNEL_ID =
        "attendance_change"



    fun show(
        context: Context,
        title: String,
        message: String
    ){


        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager



        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){


            val channel =
                NotificationChannel(

                    CHANNEL_ID,

                    "출석 변경 알림",

                    NotificationManager.IMPORTANCE_HIGH

                )


            manager.createNotificationChannel(
                channel
            )

        }



        val notification =
            NotificationCompat.Builder(

                context,

                CHANNEL_ID

            )

                .setSmallIcon(
                    R.mipmap.ic_launcher
                )

                .setContentTitle(title)

                .setContentText(message)

                .setAutoCancel(true)

                .build()



        manager.notify(
            200,
            notification
        )

    }


}