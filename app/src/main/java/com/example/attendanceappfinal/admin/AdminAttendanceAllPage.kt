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
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate



@Composable
fun AdminAttendanceAllPage(

    onBack: () -> Unit

){


    BackHandler {

        onBack()

    }



    val database =
        FirebaseDatabase.getInstance()



    var attendanceList by remember {

        mutableStateOf(
            emptyList<Attendance>()
        )

    }



    var userMap by remember {

        mutableStateOf(
            emptyMap<String, User>()
        )

    }



    var selectedDate by remember {

        mutableStateOf<String?>(null)

    }



    val now =
        LocalDate.now()



    var currentYear by remember {

        mutableStateOf(now.year)

    }



    var currentMonth by remember {

        mutableStateOf(now.monthValue)

    }





    fun loadUsers(){


        database

            .getReference("users")

            .get()

            .addOnSuccessListener { snapshot ->



                val result =
                    mutableMapOf<String, User>()



                snapshot.children.forEach { child ->



                    val user =

                        child.getValue(
                            User::class.java
                        )



                    if(user != null){


                        result[child.key ?: ""] =
                            user.copy(
                                uid = child.key ?: ""
                            )


                    }


                }



                userMap = result



            }


    }






    fun loadAttendance(){

        database
            .getReference("attendance")
            .get()
            .addOnSuccessListener { snapshot ->


                val result =
                    mutableListOf<Attendance>()


                println("===== Firebase attendance 확인 =====")


                snapshot.children.forEach { studentNode ->


                    println("학생 UID : ${studentNode.key}")


                    studentNode.children.forEach { record ->


                        println("출결 키 : ${record.key}")
                        println("출결 데이터 : ${record.value}")


                        val attendance =

                            record.getValue(
                                Attendance::class.java
                            )


                        if(attendance != null){


                            println(
                                "변환 성공 : ${attendance.studentName} / ${attendance.subject} / ${attendance.status}"
                            )


                            result.add(

                                attendance.copy(

                                    id = record.key ?: ""

                                )

                            )


                        }else{


                            println("변환 실패")

                        }


                    }


                }


                attendanceList = result


            }


    }





    LaunchedEffect(Unit){


        loadUsers()

        loadAttendance()


    }






    LaunchedEffect(attendanceList){

        println("===== 전체 출결 확인 =====")

        attendanceList.forEach {

            println(
                "날짜=${it.date}, 학생=${it.studentName}, 과목=${it.subject}, 상태=${it.status}"
            )

        }

    }

    val monthData =

        attendanceList.filter {


            val split =
                it.date.split("-")



            split.size == 3 &&

                    split[0].toIntOrNull() == currentYear &&

                    split[1].toIntOrNull() == currentMonth



        }







    val present =

        monthData.count {

            it.status == "출석"

        }




    val late =

        monthData.count {

            it.status == "지각"

        }




    val absent =

        monthData.count {

            it.status == "결석"

        }




    val total =
        monthData.size




    val rate =

        if(total == 0)

            0

        else

            ((present + late) * 100) / total








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

            "전체 출결 조회",

            style =
                MaterialTheme.typography.headlineMedium

        )




        Spacer(
            Modifier.height(20.dp)
        )







        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween

        ){



            Button(

                onClick = {


                    currentMonth--


                    if(currentMonth == 0){

                        currentMonth = 12

                        currentYear--

                    }


                }

            ){

                Text("<")

            }






            Text(

                "${currentYear}년 ${currentMonth}월",

                style =
                    MaterialTheme.typography.titleLarge

            )






            Button(

                onClick = {


                    currentMonth++


                    if(currentMonth == 13){

                        currentMonth = 1

                        currentYear++

                    }


                }

            ){

                Text(">")

            }



        }







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


                Text("월별 출결 통계")

                Spacer(
                    Modifier.height(10.dp)
                )


                Text("전체 : ${total}회")

                Text("출석 : ${present}회")

                Text("지각 : ${late}회")

                Text("결석 : ${absent}회")

                Text("출석률 : ${rate}%")



            }



        }







        Spacer(
            Modifier.height(20.dp)
        )








        AttendanceMonthCalendar(

            year = currentYear,

            month = currentMonth,

            attendanceList = monthData,

            onSelect = {

                selectedDate = it

            }

        )








        selectedDate?.let { date ->



            Spacer(
                Modifier.height(20.dp)
            )



            Text(

                "${date} 출결 상세",

                style =
                    MaterialTheme.typography.titleLarge

            )






            monthData

                .filter {

                    it.date == date

                }

                .forEach { item ->



                    val student =

                        userMap[item.studentUid]





                    Card(

                        modifier =
                            Modifier

                                .fillMaxWidth()

                                .padding(
                                    vertical = 5.dp
                                ),

                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ){



                        Column(

                            modifier =
                                Modifier.padding(15.dp)

                        ){



                            Text(

                                "학생 : ${student?.name ?: item.studentName.ifBlank { "이름 없음" }}",

                                style = MaterialTheme.typography.titleMedium

                            )



                            Text(

                                "학년 : ${student?.grade ?: "-"}"

                            )



                            Text(

                                "과목 : ${item.subject}"

                            )



                            AssistChip(
                                onClick = {},
                                label = { Text("상태 : ${item.status}") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = when(item.status) {
                                        "출석" -> MaterialTheme.colorScheme.primaryContainer
                                        "지각" -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.errorContainer
                                    }
                                )
                            )



                            Text(

                                "시간 : ${item.time}"

                            )



                        }



                    }



                }





        }







        Spacer(
            Modifier.height(20.dp)
        )







        OutlinedButton(

            modifier =
                Modifier.fillMaxWidth(),

            onClick =
                onBack

        ){

            Text("뒤로가기")

        }





    }


}
