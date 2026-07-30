package com.example.attendanceappfinal.admin


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random



@Composable
fun TeacherRegisterPage(

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



    var message by remember {

        mutableStateOf("")

    }



    var tempId by remember {

        mutableStateOf("")

    }



    var tempPassword by remember {

        mutableStateOf("")

    }





    fun createTeacherAccount(){



        if(
            name.isBlank() ||
            phone.isBlank()
        ){

            message =
                "이름과 전화번호를 입력하세요"

            return

        }





        val uid =

            database

                .getReference("users")

                .push()

                .key ?: return





        // 임시 아이디 생성

        val loginId =

            "T" +
                    Random.nextInt(
                        1000,
                        9999
                    )





        // 임시 비밀번호 생성

        val password =

            Random.nextInt(
                100000,
                999999
            )
                .toString()







        val teacher = User(


            uid = uid,


            name = name,


            phone = phone,


            email = "",


            role = "teacher",


            grade = "",


            className = "",


            nfcId = "",


            loginId = loginId,


            tempPassword = password,


            firstLogin = true,


            createdAt =
                System.currentTimeMillis()


        )







        database

            .getReference("users")

            .child(uid)

            .setValue(teacher)

            .addOnSuccessListener {



                tempId = loginId

                tempPassword = password



                message =

                    """
                    선생님 등록 완료
                    
                    이름 : $name
                    
                    임시 아이디 : $loginId
                    
                    임시 비밀번호 : $password
                    
                    최초 로그인 후 계정을 변경하세요.
                    """.trimIndent()



                name = ""

                phone = ""



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

            "선생님 추가",

            style =
                MaterialTheme.typography.headlineMedium

        )



        Spacer(

            Modifier.height(20.dp)

        )







        OutlinedTextField(

            modifier =
                Modifier.fillMaxWidth(),


            value =
                name,


            onValueChange = {

                name = it

            },


            label = {

                Text("선생님 이름")

            }


        )







        Spacer(

            Modifier.height(10.dp)

        )







        OutlinedTextField(

            modifier =
                Modifier.fillMaxWidth(),


            value =
                phone,


            onValueChange = {

                phone = it

            },


            label = {

                Text("전화번호")

            }


        )







        Spacer(

            Modifier.height(20.dp)

        )








        Button(

            modifier =
                Modifier.fillMaxWidth(),


            onClick = {

                createTeacherAccount()

            }


        ){

            Text(

                "선생님 등록"

            )

        }







        Spacer(

            Modifier.height(20.dp)

        )







        if(message.isNotBlank()){


            Card(

                modifier =
                    Modifier.fillMaxWidth()

            ){


                Text(

                    message,

                    modifier =
                        Modifier.padding(15.dp)

                )


            }



        }







        Spacer(

            Modifier.height(20.dp)

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