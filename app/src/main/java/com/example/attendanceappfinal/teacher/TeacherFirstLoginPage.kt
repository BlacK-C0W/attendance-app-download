package com.example.attendanceappfinal.teacher


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase




@Composable
fun TeacherFirstLoginPage(

    user: User,

    onComplete:(User)->Unit

){



    BackHandler {

        // 최초 설정 중 뒤로가기 막음

    }





    val database =

        FirebaseDatabase.getInstance()





    var newId by remember {

        mutableStateOf("")

    }





    var newPassword by remember {

        mutableStateOf("")

    }





    var confirmPassword by remember {

        mutableStateOf("")

    }





    var message by remember {

        mutableStateOf("")

    }







    fun updateAccount(){



        if(newId.isBlank() || newPassword.isBlank()){


            message =
                "새 아이디와 비밀번호를 입력하세요"


            return


        }







        if(newPassword != confirmPassword){


            message =
                "비밀번호가 일치하지 않습니다"


            return


        }







        database

            .getReference("users")

            .get()

            .addOnSuccessListener { snapshot ->




                var duplicate = false





                snapshot.children.forEach { child ->



                    val other =

                        child.getValue(
                            User::class.java
                        )



                    if(

                        other != null &&

                        other.uid != user.uid &&

                        other.loginId == newId

                    ){

                        duplicate = true

                    }



                }







                if(duplicate){


                    message =
                        "이미 사용중인 아이디입니다"


                    return@addOnSuccessListener


                }







                val updateUser =

                    user.copy(


                        loginId = newId,


                        tempPassword = newPassword,


                        firstLogin = false


                    )







                database

                    .getReference("users")

                    .child(user.uid)

                    .setValue(updateUser)

                    .addOnSuccessListener {


                        message =
                            "계정 변경 완료"


                        onComplete(updateUser)


                    }


                    .addOnFailureListener {


                        message =
                            "저장 실패 : ${it.message}"


                    }




            }




    }









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


            )


    ){





        Text(


            "선생님 계정 설정",


            style =
                MaterialTheme.typography.headlineMedium


        )






        Spacer(

            Modifier.height(20.dp)

        )







        Text(

            "임시 아이디",

            )



        Text(

            user.loginId,

            style =
                MaterialTheme.typography.titleLarge


        )







        Spacer(

            Modifier.height(20.dp)

        )







        Text(

            "새 아이디"

        )







        OutlinedTextField(


            modifier =
                Modifier.fillMaxWidth(),


            value =
                newId,


            onValueChange = {

                newId = it

            },


            label = {

                Text("사용할 아이디")

            }


        )







        Spacer(

            Modifier.height(10.dp)

        )







        OutlinedTextField(


            modifier =
                Modifier.fillMaxWidth(),


            value =
                newPassword,


            onValueChange = {

                newPassword = it

            },


            label = {

                Text("새 비밀번호")

            }


        )







        Spacer(

            Modifier.height(10.dp)

        )







        OutlinedTextField(


            modifier =
                Modifier.fillMaxWidth(),


            value =
                confirmPassword,


            onValueChange = {

                confirmPassword = it

            },


            label = {

                Text("비밀번호 확인")

            }


        )







        Spacer(

            Modifier.height(20.dp)

        )







        Button(


            modifier =
                Modifier.fillMaxWidth(),


            onClick = {

                updateAccount()

            }


        ){


            Text(

                "계정 변경 완료"

            )


        }








        Spacer(

            Modifier.height(15.dp)

        )






        if(message.isNotBlank()){


            Text(message)


        }




    }



}