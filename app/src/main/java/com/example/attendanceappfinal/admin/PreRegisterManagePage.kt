package com.example.attendanceappfinal.admin


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase



@Composable
fun PreRegisterManagePage(){


    val database =
        FirebaseDatabase.getInstance()



    data class PreStudent(

        val key:String="",

        val name:String="",

        val phone:String=""

    )



    var students by remember {

        mutableStateOf(
            emptyList<PreStudent>()
        )

    }



    var message by remember {

        mutableStateOf("")

    }





    fun load(){


        database

            .getReference("preStudents")

            .get()

            .addOnSuccessListener { snapshot ->



                val list =
                    mutableListOf<PreStudent>()



                snapshot.children.forEach { child ->



                    val name =

                        child.child("name")

                            .getValue(
                                String::class.java
                            )
                            ?: ""



                    val phone =

                        child.child("phone")

                            .getValue(
                                String::class.java
                            )
                            ?: ""




                    list.add(

                        PreStudent(

                            key =
                                child.key ?: "",

                            name =
                                name,

                            phone =
                                phone

                        )

                    )



                }



                students =
                    list



            }



    }







    LaunchedEffect(Unit){

        load()

    }







    Column(

        modifier =

            Modifier

                .fillMaxSize()

                .verticalScroll(

                    rememberScrollState()

                )

                .padding(20.dp)

    ){



        Text(

            "사전등록 학생 관리",

            style =
                MaterialTheme.typography.headlineMedium

        )





        Spacer(

            Modifier.height(20.dp)

        )







        if(students.isEmpty()){



            Text(
                "등록된 사전 학생 없음"
            )



        }








        students.forEach { student ->




            Card(

                modifier =

                    Modifier

                        .fillMaxWidth()

                        .padding(

                            vertical = 5.dp

                        )

            ){



                Column(

                    modifier =
                        Modifier.padding(15.dp)

                ){



                    Text(

                        "이름 : ${student.name}",

                        style =
                            MaterialTheme.typography.titleMedium

                    )



                    Text(

                        "전화번호 : ${student.phone}"

                    )






                    Spacer(

                        Modifier.height(10.dp)

                    )







                    Button(

                        modifier =
                            Modifier.fillMaxWidth(),


                        onClick = {




                            database

                                .getReference("preStudents")

                                .child(student.key)

                                .removeValue()

                                .addOnSuccessListener {


                                    message =
                                        "${student.name} 삭제 완료"


                                    load()


                                }



                        }


                    ){



                        Text("사전등록 삭제")



                    }





                }



            }





        }






        Spacer(

            Modifier.height(20.dp)

        )





        if(message.isNotEmpty()){


            Text(message)


        }





    }



}
