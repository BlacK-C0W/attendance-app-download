package com.example.attendanceappfinal.auth


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


@Composable
fun RegisterPage(

    onBack:()->Unit

){


    BackHandler {

        onBack()

    }



    val auth =
        FirebaseAuth.getInstance()


    val database =
        FirebaseDatabase.getInstance()



    var id by remember {
        mutableStateOf("")
    }


    var password by remember {
        mutableStateOf("")
    }


    var name by remember {
        mutableStateOf("")
    }


    var phone by remember {
        mutableStateOf("")
    }


    var grade by remember {
        mutableStateOf("")
    }


    var gradeOpen by remember {
        mutableStateOf(false)
    }


    var message by remember {
        mutableStateOf("")
    }

    fun deletePreData(
        studentKey:String,
        onComplete:()->Unit
    ){

        database
            .getReference("preAttendance")
            .child(studentKey)
            .removeValue()
            .addOnSuccessListener {


                database
                    .getReference("preStudents")
                    .child(studentKey)
                    .removeValue()
                    .addOnSuccessListener {


                        onComplete()

                    }


            }

    }



    val grades = listOf(

        "초5",
        "초6",

        "중1",
        "중2",
        "중3",

        "고1",
        "고2",
        "고3"

    )





    fun register(){


        if(
            id.isBlank() ||
            password.isBlank() ||
            name.isBlank() ||
            phone.isBlank() ||
            grade.isBlank()
        ){

            message="모든 정보를 입력하세요"
            return

        }



        val fakeEmail =
            "${id}@attendance.com"




        database

            .getReference("preStudents")

            .get()

            .addOnSuccessListener { snapshot ->



                var matchedStudent = false


                var studentKey = ""


                var studentNfc = ""





                snapshot.children.forEach { child ->



                    val pre =
                        child.getValue(
                            com.example.attendanceappfinal.model.PreStudent::class.java
                        )



                    if(
                        pre != null &&
                        pre.name == name &&
                        pre.grade == grade &&
                        pre.phone == phone
                    ){

                        matchedStudent = true


                        studentKey =
                            child.key ?: ""


                        studentNfc =
                            pre.nfcTag

                    }


                }





                if(!matchedStudent){


                    message =
                        "등록된 학생 정보가 없습니다"


                    return@addOnSuccessListener

                }





                auth

                    .createUserWithEmailAndPassword(

                        fakeEmail,

                        password

                    )

                    .addOnSuccessListener {


                        val authUid =
                            auth.currentUser!!.uid





                        val user = User(


                            uid = authUid,


                            name = name,


                            phone = phone,


                            email = fakeEmail,


                            role = "student",


                            grade = grade,


                            className = "",


                            nfcId = studentNfc,

                            preStudentId = studentKey,


                            createdAt =
                                System.currentTimeMillis()


                        )






                        database

                            .getReference("users")

                            .child(authUid)

                            .setValue(user)

                            .addOnSuccessListener {



                                // NFC 연결
                                if(studentNfc.isNotBlank()){


                                    database

                                        .getReference("nfc_tags")

                                        .child(studentNfc)

                                        .setValue(

                                            mapOf(

                                                "studentUid" to authUid,

                                                "name" to name,

                                                "preStudentId" to studentKey

                                            )

                                        )


                                }






                                // 미가입 출석 이동
                                database

                                    .getReference("preAttendance")

                                    .child(studentKey)

                                    .get()

                                    .addOnSuccessListener { attendanceSnapshot ->



                                        val total =
                                            attendanceSnapshot.childrenCount.toInt()


                                        if(total == 0){


                                            deletePreData(
                                                studentKey
                                            ){
                                                message = "회원가입 완료"
                                                onBack()
                                            }


                                        }else{


                                            var count = 0



                                            attendanceSnapshot.children.forEach { child ->



                                                val attendance =
                                                    child.getValue(
                                                        com.example.attendanceappfinal.model.Attendance::class.java
                                                    )



                                                if(attendance != null){


                                                    val newAttendance =
                                                        attendance.copy(

                                                            studentUid = authUid,

                                                            studentName = name

                                                        )



                                                    database

                                                        .getReference("attendance")

                                                        .child(authUid)

                                                        .child(child.key ?: "")

                                                        .setValue(newAttendance)

                                                        .addOnSuccessListener {


                                                            count++



                                                            if(count == total){


                                                                deletePreData(
                                                                    studentKey
                                                                ){
                                                                    message = "회원가입 완료"
                                                                    onBack()
                                                                }


                                                            }


                                                        }


                                                }else{


                                                    count++


                                                }



                                            }



                                        }



                                    }


                            }



                    }

                    .addOnFailureListener {


                        message =
                            it.message
                                ?: "회원가입 실패"


                    }





            }



    }




    Column(

        modifier = Modifier

            .fillMaxSize()

            .padding(

                top = UiConfig.topPadding,

                start = UiConfig.sidePadding,

                end = UiConfig.sidePadding

            )

    ){



        Text(

            "학생 회원가입",

            style =
                MaterialTheme.typography.headlineMedium

        )



        Spacer(
            Modifier.height(20.dp)
        )




        OutlinedTextField(

            value=id,

            onValueChange={
                id=it
            },

            label={
                Text("아이디")
            },

            modifier =
                Modifier.fillMaxWidth()

        )





        OutlinedTextField(

            value=password,

            onValueChange={
                password=it
            },

            label={
                Text("비밀번호")
            },

            modifier =
                Modifier.fillMaxWidth()

        )





        OutlinedTextField(

            value=name,

            onValueChange={
                name=it
            },

            label={
                Text("이름")
            },

            modifier =
                Modifier.fillMaxWidth()

        )





        OutlinedTextField(

            value=phone,

            onValueChange={
                phone=it
            },

            label={
                Text("전화번호")
            },

            modifier =
                Modifier.fillMaxWidth()

        )





        Spacer(
            Modifier.height(10.dp)
        )





        Box{


            Button(

                modifier =
                    Modifier.fillMaxWidth(),


                onClick = {

                    gradeOpen=true

                }

            ){

                Text(

                    if(grade.isBlank())

                        "학년 선택"

                    else

                        grade

                )

            }





            DropdownMenu(

                expanded = gradeOpen,

                onDismissRequest = {

                    gradeOpen=false

                }

            ){



                grades.forEach { item ->



                    DropdownMenuItem(

                        text={

                            Text(item)

                        },


                        onClick={


                            grade=item

                            gradeOpen=false


                        }


                    )

                }


            }


        }







        Spacer(
            Modifier.height(20.dp)
        )







        Button(

            modifier =
                Modifier.fillMaxWidth(),


            onClick={

                register()

            }

        ){

            Text("회원가입")

        }







        Spacer(
            Modifier.height(10.dp)

        )



        Text(message)







        Spacer(
            Modifier.height(20.dp)

        )







        TextButton(

            onClick={

                onBack()

            }

        ){

            Text("뒤로가기")

        }





    }



}
