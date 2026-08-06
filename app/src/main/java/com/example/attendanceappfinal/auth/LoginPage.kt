package com.example.attendanceappfinal.auth


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.BuildConfig
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.repository.FirebaseRepository
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging


@Composable
fun LoginPage(

    onStudentLogin: (User) -> Unit,

    onTeacherLogin: (User) -> Unit,

    onAdminLogin: (User) -> Unit,

    onParentLogin: (User) -> Unit,

    onRegisterClick: () -> Unit,

    onParentRegisterClick: () -> Unit

){


    var id by remember {

        mutableStateOf("")

    }


    var password by remember {

        mutableStateOf("")

    }


    var message by remember {

        mutableStateOf("")

    }



    val database =

        FirebaseDatabase.getInstance()






    fun teacherLogin(){


        database

            .getReference("users")

            .get()

            .addOnSuccessListener { snapshot ->



                var found:User? = null



                snapshot.children.forEach { child ->


                    val user =

                        child.getValue(
                            User::class.java
                        )



                    if(

                        user != null &&

                        user.role == "teacher" &&

                        user.loginId == id &&

                        user.tempPassword == password

                    ){

                        found =

                            user.copy(

                                uid =
                                    child.key ?: ""

                            )

                    }


                }




                if(found == null){


                    message =
                        "아이디 또는 비밀번호가 올바르지 않습니다."


                }else{

                    FirebaseMessaging.getInstance()
                        .token
                        .addOnSuccessListener { token ->

                            database
                                .getReference("deviceTokens")
                                .child(found!!.uid)
                                .child("fcmToken")
                                .setValue(token)

                        }

                    onTeacherLogin(found!!)


                }



            }



    }









    Surface(

        modifier =
            Modifier.fillMaxSize()

    ){



        Column(


            modifier = Modifier

                .fillMaxSize()

                .padding(

                    top =
                        UiConfig.topPadding,

                    start =
                        UiConfig.sidePadding,

                    end =
                        UiConfig.sidePadding,

                    bottom =
                        UiConfig.bottomPadding

                ),



            horizontalAlignment =
                Alignment.CenterHorizontally,


            verticalArrangement =
                Arrangement.Center


        ){



            Text(

                "출석관리 시스템",

                style =
                    MaterialTheme.typography.headlineMedium

            )



            Spacer(

                Modifier.height(30.dp)

            )





            OutlinedTextField(

                modifier =
                    Modifier.fillMaxWidth(),


                value =
                    id,


                onValueChange = {

                    id = it

                },


                label = {

                    Text("아이디")

                }


            )







            Spacer(

                Modifier.height(10.dp)

            )







            OutlinedTextField(

                modifier =
                    Modifier.fillMaxWidth(),


                value =
                    password,


                onValueChange = {

                    password = it

                },


                visualTransformation =
                    PasswordVisualTransformation(),


                label = {

                    Text("비밀번호")

                }


            )







            Spacer(

                Modifier.height(20.dp)

            )







            Button(

                modifier =
                    Modifier.fillMaxWidth(),


                onClick = {


                    message = ""



                    // 먼저 선생 임시 계정 확인

                    teacherLogin()



                    // 기존 학생/관리자 로그인도 같이 시도

                    FirebaseRepository.login(

                        id,

                        password

                    ){ success, uid ->



                        if(!success || uid == null){

                            return@login

                        }



                        database

                            .getReference("users")

                            .child(uid)

                            .get()

                            .addOnSuccessListener {


                                val user =

                                    it.getValue(
                                        User::class.java
                                    )


                                if(user == null){

                                    return@addOnSuccessListener

                                }


                                FirebaseMessaging.getInstance()
                                    .token
                                    .addOnSuccessListener { token ->

                                        database
                                        .getReference("deviceTokens")
                                        .child(uid)
                                            .child("fcmToken")
                                            .setValue(token)

                                    }

                                when(user.role){


                                    "student" ->

                                        onStudentLogin(user)



                                    "admin" ->

                                        onAdminLogin(user)

                                    "parent" ->

                                        onParentLogin(user)



                                }


                            }



                    }



                }

            ){

                Text("로그인")

            }







            Spacer(

                Modifier.height(10.dp)

            )







            TextButton(

                onClick =
                    onRegisterClick

            ){

                Text("학생 회원가입")

            }

            TextButton(onClick = onParentRegisterClick) {
                Text("학부모 회원가입")
            }







            Spacer(

                Modifier.height(15.dp)

            )







            if(message.isNotBlank()){


                Text(

                    message,

                    color =
                        MaterialTheme.colorScheme.error

                )


            }

            Spacer(Modifier.height(12.dp))
            Text(
                "앱 버전 v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )




        }


    }


}
