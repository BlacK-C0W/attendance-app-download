package com.example.attendanceappfinal.admin


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase



@Composable
fun AdminTeacherRegisterPage(

    onBack: () -> Unit

){


    BackHandler {

        onBack()

    }



    val auth =
        FirebaseAuth.getInstance()


    val database =
        FirebaseDatabase.getInstance()



    var name by remember {

        mutableStateOf("")

    }



    var phone by remember {

        mutableStateOf("")

    }



    var password by remember {

        mutableStateOf("")

    }



    var message by remember {

        mutableStateOf("")

    }



    var createdId by remember {

        mutableStateOf("")

    }



    var createdPassword by remember {

        mutableStateOf("")

    }






    fun registerTeacher(){



        if(

            name.isBlank() ||

            phone.isBlank() ||

            password.isBlank()

        ){

            message =
                "모든 항목을 입력하세요"

            return

        }






        val loginId =

            phone + "@teacher.com"







        auth

            .createUserWithEmailAndPassword(

                loginId,

                password

            )

            .addOnSuccessListener { result ->



                val uid =

                    result.user!!.uid







                val teacherData = mapOf(


                    "uid" to uid,


                    "name" to name,


                    "phone" to phone,


                    "email" to loginId,


                    "role" to "teacher",


                    "loginId" to loginId,


                    "tempPassword" to password,


                    "firstLogin" to true,


                    "createdAt" to System.currentTimeMillis()


                )







                database

                    .getReference("users")

                    .child(uid)

                    .setValue(teacherData)

                    .addOnSuccessListener {



                        createdId = loginId


                        createdPassword = password



                        message =

                            "${name} 선생님 등록 완료"




                        name = ""


                        phone = ""


                        password = ""



                    }



            }


            .addOnFailureListener {


                message =

                    "등록 실패 : ${it.message}"


            }



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

            "선생님 추가",

            style =
                MaterialTheme.typography.headlineMedium

        )



        Spacer(

            Modifier.height(8.dp)

        )



        Text(

            "선생님 계정을 생성합니다",

            style =
                MaterialTheme.typography.bodyMedium

        )





        Spacer(

            Modifier.height(25.dp)

        )







        OutlinedTextField(

            modifier =
                Modifier.fillMaxWidth(),

            value = name,

            onValueChange = {

                name = it

            },

            label = {

                Text("선생님 이름")

            }

        )





        Spacer(

            Modifier.height(12.dp)

        )







        OutlinedTextField(

            modifier =
                Modifier.fillMaxWidth(),

            value = phone,

            onValueChange = {

                phone = it

            },

            label = {

                Text("전화번호")

            }

        )





        Spacer(

            Modifier.height(12.dp)

        )







        OutlinedTextField(

            modifier =
                Modifier.fillMaxWidth(),

            value = password,

            onValueChange = {

                password = it

            },

            label = {

                Text("초기 비밀번호")

            },

            visualTransformation =

                PasswordVisualTransformation()

        )







        Spacer(

            Modifier.height(25.dp)

        )








        Button(

            modifier =
                Modifier

                    .fillMaxWidth()

                    .height(55.dp),


            onClick = {

                registerTeacher()

            }

        ){

            Text(

                "선생님 계정 생성"

            )

        }








        if(message.isNotBlank()){


            Spacer(

                Modifier.height(20.dp)

            )



            Card(

                modifier =
                    Modifier.fillMaxWidth()

            ){



                Column(

                    modifier =
                        Modifier.padding(16.dp)

                ){



                    Text(

                        message,

                        style =
                            MaterialTheme.typography.titleMedium

                    )



                    if(createdId.isNotBlank()){


                        Spacer(

                            Modifier.height(10.dp)

                        )



                        Text(

                            "로그인 아이디"

                        )


                        Text(

                            createdId,

                            style =
                                MaterialTheme.typography.bodyLarge

                        )



                        Spacer(

                            Modifier.height(8.dp)

                        )



                        Text(

                            "초기 비밀번호"

                        )


                        Text(

                            createdPassword,

                            style =
                                MaterialTheme.typography.bodyLarge

                        )



                        Spacer(

                            Modifier.height(8.dp)

                        )



                        Text(

                            "※ 첫 로그인 후 비밀번호 변경 필요"

                        )



                    }


                }


            }


        }







        Spacer(

            Modifier.height(25.dp)

        )







        OutlinedButton(

            modifier =
                Modifier.fillMaxWidth(),


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