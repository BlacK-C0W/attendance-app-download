package com.example.attendanceappfinal.admin


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.google.firebase.database.FirebaseDatabase



@Composable
fun StudentPreRegisterPage(

    onBack: () -> Unit

){


    BackHandler {

        onBack()

    }



    val database =
        FirebaseDatabase.getInstance()



    var name by remember {

        mutableStateOf("")

    }



    var phone by remember {

        mutableStateOf("")

    }

    var className by remember {

        mutableStateOf("")

    }



    var message by remember {

        mutableStateOf("")

    }






    Column(

        modifier = Modifier

            .fillMaxSize()

            .verticalScroll(
                rememberScrollState()
            )

            .padding(

                top = UiConfig.topPadding,

                start = UiConfig.sidePadding,

                end = UiConfig.sidePadding,

                bottom = UiConfig.bottomPadding

            )

    ){



        Text(

            "📝 학생 사전등록",

            style =
                MaterialTheme.typography.headlineMedium

        )



        Spacer(
            Modifier.height(5.dp)
        )



        Text(

            "가입 가능한 학생을 미리 등록합니다",

            style =
                MaterialTheme.typography.bodyMedium

        )



        Spacer(
            Modifier.height(25.dp)
        )





        Card(

            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(20.dp),

            elevation =
                CardDefaults.cardElevation(

                    defaultElevation = 5.dp

                )

        ){



            Column(

                modifier =
                    Modifier.padding(20.dp)

            ){



                Text(

                    "학생 정보 입력",

                    style =
                        MaterialTheme.typography.titleLarge

                )



                Spacer(
                    Modifier.height(15.dp)
                )




                OutlinedTextField(

                    value = name,

                    onValueChange = {

                        name = it

                    },

                    label = {

                        Text("학생 이름")

                    },

                    modifier =
                        Modifier.fillMaxWidth()

                )

                Spacer(
                    Modifier.height(12.dp)
                )

                OutlinedTextField(

                    value = className,

                    onValueChange = {

                        className = it

                    },

                    label = {

                        Text("반")

                    },

                    modifier =
                        Modifier.fillMaxWidth()

                )



                Spacer(
                    Modifier.height(12.dp)
                )




                OutlinedTextField(

                    value = phone,

                    onValueChange = {

                        phone = it

                    },

                    label = {

                        Text("전화번호")

                    },

                    modifier =
                        Modifier.fillMaxWidth()

                )





                Spacer(
                    Modifier.height(20.dp)
                )





                Button(

                    modifier =
                        Modifier

                            .fillMaxWidth()

                            .height(55.dp),


                    shape =
                        RoundedCornerShape(16.dp),


                    onClick = {



                        if(

                            name.isBlank() ||

                            phone.isBlank()

                        ){

                            message =
                                "이름과 전화번호를 입력하세요"


                            return@Button

                        }






                        database

                            .getReference("preStudents")

                            .push()

                            .setValue(

                                mapOf(

                                    "name" to name,

                                    "phone" to phone,

                                    "className" to className

                                )

                            )

                            .addOnSuccessListener {



                                message =
                                    "학생 사전등록 완료"



                                name = ""

                                phone = ""

                                className = ""



                            }

                            .addOnFailureListener {



                                message =
                                    "등록 실패 : ${it.message}"



                            }



                    }

                ){

                    Text(
                        "학생 등록"
                    )

                }





            }



        }






        Spacer(
            Modifier.height(20.dp)
        )






        if(message.isNotBlank()){



            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp)

            ){



                Text(

                    message,

                    modifier =
                        Modifier.padding(18.dp)

                )



            }



        }






        Spacer(
            Modifier.height(30.dp)
        )






        OutlinedButton(

            modifier =
                Modifier

                    .fillMaxWidth()

                    .height(55.dp),


            shape =
                RoundedCornerShape(16.dp),


            onClick = {

                onBack()

            }

        ){


            Text(
                "뒤로가기"
            )


        }





    }



}
