package com.example.attendanceappfinal.student


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*



data class StudentNotification(

    val id:String = "",

    val title:String = "",

    val message:String = "",

    val timestamp:Long = 0L

)





@Composable
fun StudentNotificationPage(

    user: User,

    onBack: () -> Unit = {}

){



    BackHandler {

        onBack()

    }





    val database =
        FirebaseDatabase.getInstance()





    var list by remember {

        mutableStateOf(
            emptyList<StudentNotification>()
        )

    }





    var message by remember {

        mutableStateOf("")

    }







    fun loadNotifications(){



        database

            .getReference("notifications")

            .child(user.uid)

            .get()

            .addOnSuccessListener { snapshot ->



                val temp =
                    mutableListOf<StudentNotification>()





                snapshot.children.forEach { child ->



                    child.getValue(
                        StudentNotification::class.java
                    )
                        ?.copy(

                            id =
                                child.key ?: ""

                        )
                        ?.let {

                            temp.add(it)

                        }



                }





                list =

                    temp.sortedByDescending {

                        it.timestamp

                    }



            }



    }







    LaunchedEffect(user.uid){


        loadNotifications()


    }









    Column(

        modifier = Modifier

            .fillMaxSize()

            .verticalScroll(
                rememberScrollState()
            )

            .padding(
                top = 40.dp,
                start = 20.dp,
                end = 20.dp,
                bottom = 20.dp
            )

    ){





        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween

        ){



            Text(

                "🔔 출결 알림",

                style =
                    MaterialTheme.typography.headlineMedium

            )





            Button(

                onClick = {



                    val deletions = list.associate { it.id to null }

                    database

                        .getReference("notifications")

                        .child(user.uid)

                        .updateChildren(deletions)

                        .addOnSuccessListener {



                            list =
                                emptyList()



                            message =
                                "전체 삭제 완료"



                        }



                },


                colors =
                    ButtonDefaults.buttonColors(

                        containerColor =
                            MaterialTheme.colorScheme.error

                    ),


                shape =
                    RoundedCornerShape(12.dp)

            ){


                Text(
                    "전체 삭제"
                )


            }





        }








        Spacer(
            Modifier.height(20.dp)
        )







        if(list.isEmpty()){



            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(18.dp)

            ){


                Text(

                    "새로운 알림이 없습니다.",

                    modifier =
                        Modifier.padding(20.dp)

                )


            }



        }







        list.forEach { item ->





            Card(

                modifier =
                    Modifier

                        .fillMaxWidth()

                        .padding(vertical = 6.dp),


                shape =
                    RoundedCornerShape(18.dp),


                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )

            ){





                Column(

                    modifier =
                        Modifier.padding(18.dp)

                ){





                    Text(

                        item.title,

                        style =
                            MaterialTheme.typography.titleLarge

                    )






                    Spacer(
                        Modifier.height(8.dp)
                    )







                    Text(

                        item.message

                    )







                    Spacer(
                        Modifier.height(8.dp)
                    )






                    Text(

                        formatDate(item.timestamp),

                        style =
                            MaterialTheme.typography.bodySmall

                    )








                    Spacer(
                        Modifier.height(12.dp)
                    )








                    Button(

                        onClick = {



                            database

                                .getReference("notifications")

                                .child(user.uid)

                                .child(item.id)

                                .removeValue()

                                .addOnSuccessListener {



                                    loadNotifications()



                                    message =
                                        "삭제 완료"



                                }



                        },


                        shape =
                            RoundedCornerShape(12.dp)

                    ){



                        Text(
                            "삭제"
                        )


                    }




                }





            }





        }








        Spacer(
            Modifier.height(20.dp)
        )





        Text(message)





    }



}







private fun formatDate(
    time:Long
):String{


    if(time == 0L)

        return ""



    return SimpleDateFormat(

        "yyyy-MM-dd HH:mm",

        Locale.getDefault()

    )
        .format(Date(time))



}
