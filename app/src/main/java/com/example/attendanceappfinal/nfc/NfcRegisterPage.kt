package com.example.attendanceappfinal.nfc


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



@Composable
fun NfcRegisterPage(

    nfcTag:String?,

    onBack:()->Unit

){


    BackHandler {

        onBack()

    }



    val database =
        FirebaseDatabase.getInstance()



    var students by remember {

        mutableStateOf(
            emptyList<User>()
        )

    }



    var selectedStudent by remember {

        mutableStateOf<User?>(null)

    }



    var selectedGrade by remember {

        mutableStateOf("")

    }



    var selectedClass by remember {

        mutableStateOf("")

    }



    var gradeOpen by remember {

        mutableStateOf(false)

    }



    var classOpen by remember {

        mutableStateOf(false)

    }



    var message by remember {

        mutableStateOf(
            "학년 선택 후 학생을 선택하세요"
        )

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



    val classes = listOf(

        "A반",
        "B반",
        "C반",
        "D반"

    )






    fun loadStudents(){


        database

            .getReference("users")

            .get()

            .addOnSuccessListener { snapshot ->


                val list =
                    mutableListOf<User>()



                snapshot.children.forEach { child ->


                    val user =

                        child.getValue(User::class.java)
                            ?.copy(

                                uid =
                                    child.key ?: ""

                            )



                    if(

                        user != null &&

                        user.role == "student"

                    ){

                        list.add(user)

                    }


                }



                students = list


            }



    }






    LaunchedEffect(Unit){

        loadStudents()

    }






    // ★ 기존 반 무시하고 학년만 검색
    val filteredStudents =

        students.filter {


            selectedGrade.isNotBlank()

                    &&

                    it.grade == selectedGrade


        }







    fun saveNfc(){



        val student =
            selectedStudent



        if(student == null){


            message =
                "학생을 선택하세요"


            return

        }



        if(selectedClass.isBlank()){


            message =
                "변경할 반을 선택하세요"


            return


        }



        if(nfcTag == null){


            message =
                "NFC 태그를 찍어주세요"


            return


        }






        database

            .getReference("nfc_tags")

            .child(nfcTag)

            .get()

            .addOnSuccessListener { snapshot ->





                // NFC가 이미 존재하는 경우
                if(snapshot.exists()){


                    val oldUid =

                        snapshot.child("studentUid")

                            .getValue(String::class.java)



                    // 다른 학생 NFC면 막기
                    if(oldUid != null && oldUid != student.uid){


                        val oldName =

                            snapshot.child("name")

                                .getValue(String::class.java)
                                ?: ""


                        message =

                            "이미 다른 학생에게 등록된 NFC입니다 : $oldName"


                        return@addOnSuccessListener


                    }



                }







                // 학생 정보 변경
                database

                    .getReference("users")

                    .child(student.uid)

                    .updateChildren(

                        mapOf(

                            "grade" to selectedGrade,

                            "className" to selectedClass,

                            "nfcId" to nfcTag

                        )

                    )

                    .addOnSuccessListener {



                        // NFC 테이블 업데이트
                        database

                            .getReference("nfc_tags")

                            .child(nfcTag)

                            .setValue(

                                mapOf(

                                    "studentUid" to student.uid,

                                    "name" to student.name,

                                    "grade" to selectedGrade,

                                    "className" to selectedClass

                                )

                            )

                            .addOnSuccessListener {



                                message =

                                    "${student.name} ${selectedClass} 변경 완료"



                                selectedStudent = null


                                loadStudents()



                            }



                    }



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

            "NFC 학생 등록 / 반 변경",

            style =
                MaterialTheme.typography.headlineMedium

        )





        Spacer(

            Modifier.height(20.dp)

        )







        Card(

            modifier =
                Modifier.fillMaxWidth()

        ){



            Column(

                modifier =
                    Modifier.padding(15.dp)

            ){



                Text(

                    if(selectedStudent == null)

                        "선택 학생 없음"

                    else

                        "선택 학생 : ${selectedStudent!!.name}"

                )





                Spacer(

                    Modifier.height(5.dp)

                )





                Text(message)





                Spacer(

                    Modifier.height(5.dp)

                )





                Text(

                    if(nfcTag == null)

                        "NFC : 미감지"

                    else

                        "NFC : $nfcTag"

                )



            }



        }







        Spacer(

            Modifier.height(20.dp)

        )







        // 학년 선택

        Box{



            Button(

                modifier =
                    Modifier.fillMaxWidth(),


                onClick = {

                    gradeOpen = true

                }



            ){



                Text(

                    if(selectedGrade.isBlank())

                        "학년 선택"

                    else

                        selectedGrade

                )



            }








            DropdownMenu(

                expanded = gradeOpen,


                onDismissRequest = {

                    gradeOpen = false

                }



            ){



                grades.forEach { item ->



                    DropdownMenuItem(

                        text = {

                            Text(item)

                        },


                        onClick = {


                            selectedGrade = item


                            selectedStudent = null


                            gradeOpen = false



                        }



                    )



                }



            }



        }









        Spacer(

            Modifier.height(10.dp)

        )








        // 변경할 반 선택

        Box{



            Button(

                modifier =
                    Modifier.fillMaxWidth(),


                onClick = {

                    classOpen = true

                }



            ){



                Text(

                    if(selectedClass.isBlank())

                        "변경할 반 선택"

                    else

                        selectedClass

                )



            }








            DropdownMenu(

                expanded = classOpen,


                onDismissRequest = {

                    classOpen = false

                }



            ){



                classes.forEach { item ->



                    DropdownMenuItem(

                        text = {

                            Text(item)

                        },


                        onClick = {


                            selectedClass = item


                            classOpen = false



                        }



                    )



                }



            }



        }









        Spacer(

            Modifier.height(20.dp)

        )








        Text(

            "학생 목록",

            style =
                MaterialTheme.typography.titleLarge

        )






        Spacer(

            Modifier.height(10.dp)

        )








        if(filteredStudents.isEmpty()){



            Text(

                "해당 학년 학생 없음"

            )



        }








        filteredStudents.forEach { student ->





            Button(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(

                        vertical = 4.dp

                    ),



                onClick = {



                    selectedStudent = student



                    message =

                        "${student.name} 선택됨 (현재 ${student.className})"



                }



            ){



                Text(

                    "${student.name} (${student.className})"

                )



            }



        }








        Spacer(

            Modifier.height(20.dp)

        )









        Button(

            modifier =
                Modifier.fillMaxWidth(),



            onClick = {


                saveNfc()


            }



        ){



            Text(

                "NFC 저장 / 반 변경"

            )



        }








        Spacer(

            Modifier.height(10.dp)

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