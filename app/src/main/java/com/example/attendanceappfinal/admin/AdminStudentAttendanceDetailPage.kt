package com.example.attendanceappfinal.admin


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.repository.attendanceStoragePath
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar



@Composable
fun AdminStudentAttendanceDetailPage(

    student: User,

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



    var currentMonth by remember {

        mutableStateOf(
            Calendar.getInstance()
        )

    }



    var selectedDate by remember {

        mutableStateOf<String?>(null)

    }






    fun loadAttendance(){

        val storagePath = attendanceStoragePath(student.uid)

        database
            .getReference(storagePath.root)
            .child(storagePath.studentId)
            .get()
            .addOnSuccessListener { snapshot ->


                val result =
                    mutableListOf<Attendance>()


                snapshot.children.forEach { record ->


                    val attendance =

                        record.getValue(
                            Attendance::class.java
                        )


                    if(attendance != null){


                        result.add(

                            attendance.copy(

                                id = record.key ?: "",

                                studentUid = attendance.studentUid.ifBlank { student.uid }

                            )

                        )


                    }


                }


                println(
                    "관리자 학생 출결 개수 : ${result.size}"
                )

                println("===== 캘린더 전달 데이터 =====")

                result.forEach {

                    println(
                        "날짜=${it.date}, 과목=${it.subject}, 상태=${it.status}"
                    )

                }
                println("선택 학생 UID : ${student.uid}")

                snapshot.children.forEach { record ->

                    val attendance =
                        record.getValue(Attendance::class.java)

                    println(
                        "DB출결 UID=${attendance?.studentUid}"
                    )

                }
                attendanceList = result


            }

            .addOnFailureListener {

                attendanceList = emptyList()

            }


    }







    LaunchedEffect(student.uid){

        loadAttendance()

    }






    val year =

        currentMonth.get(
            Calendar.YEAR
        )



    val month =

        currentMonth.get(
            Calendar.MONTH
        ) + 1





    val monthlyList =

        attendanceList.filter {


            it.date.startsWith(

                "%04d-%02d".format(
                    year,
                    month
                )

            )


        }







    val total =
        monthlyList.size



    val present =

        monthlyList.count {

            it.status == "출석"

        }



    val late =

        monthlyList.count {

            it.status == "지각"

        }



    val absent =

        monthlyList.count {

            it.status == "결석"

        }



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

            "${student.name} 출결 관리",

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
                    Modifier.padding(16.dp)

            ){


                Text(

                    "${year}년 ${month}월 통계",

                    style =
                        MaterialTheme.typography.titleLarge

                )


                Spacer(
                    Modifier.height(10.dp)
                )


                Text(
                    "출결 횟수 : ${total}회"
                )


                Text(
                    "출석 : ${present}회"
                )


                Text(
                    "지각 : ${late}회"
                )


                Text(
                    "결석 : ${absent}회"
                )


                Text(
                    "출석률 : ${rate}%"
                )


            }


        }




        Spacer(
            Modifier.height(20.dp)
        )




        AttendanceCalendar(

            currentMonth = currentMonth,

            attendanceList = attendanceList,

            onPrev = {


                currentMonth =

                    (currentMonth.clone() as Calendar)
                        .apply {

                            add(
                                Calendar.MONTH,
                                -1
                            )

                        }


            },


            onNext = {


                currentMonth =

                    (currentMonth.clone() as Calendar)
                        .apply {

                            add(
                                Calendar.MONTH,
                                1
                            )

                        }


            },


            onSelect = {

                selectedDate = it

            }


        )
        selectedDate?.let { date ->


            Spacer(
                Modifier.height(20.dp)
            )



            Text(

                "${date} 출결",

                style =
                    MaterialTheme.typography.titleLarge

            )





            attendanceList

                .filter {

                    it.date == date

                }

                .forEach { item ->




                    Card(

                        modifier = Modifier

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
                                "학생 : ${student.name}"
                            )


                            Text(
                                "과목 : ${item.subject}"
                            )


                            Text(
                                "상태 : ${item.status}"
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









@Composable
fun AttendanceCalendar(

    currentMonth: Calendar,

    attendanceList: List<Attendance>,

    onPrev: () -> Unit,

    onNext: () -> Unit,

    onSelect: (String) -> Unit

){



    val year =

        currentMonth.get(
            Calendar.YEAR
        )



    val month =

        currentMonth.get(
            Calendar.MONTH
        ) + 1





    Column{


        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween

        ){



            Button(
                onClick = onPrev
            ){

                Text("이전")

            }




            Text(

                "${year}년 ${month}월",

                style =
                    MaterialTheme.typography.titleLarge

            )




            Button(
                onClick = onNext
            ){

                Text("다음")

            }



        }






        Spacer(
            Modifier.height(10.dp)
        )






        val days =
            mutableListOf<String>()



        val cal =

            currentMonth.clone() as Calendar



        cal.set(

            Calendar.DAY_OF_MONTH,

            1

        )





        val startDay =

            cal.get(
                Calendar.DAY_OF_WEEK
            ) - 1





        repeat(startDay){

            days.add("")

        }





        val maxDay =

            cal.getActualMaximum(

                Calendar.DAY_OF_MONTH

            )





        for(day in 1..maxDay){


            days.add(

                "%04d-%02d-%02d".format(

                    year,

                    month,

                    day

                )

            )


        }







        LazyVerticalGrid(

            columns =
                GridCells.Fixed(7),

            modifier =
                Modifier.height(320.dp)

        ){



            items(days){ date ->



                if(date.isEmpty()){



                    Box(

                        modifier =
                            Modifier.height(45.dp)

                    )



                }else{





                    val dayStatus =

                        attendanceList.filter {

                            it.date.trim() == date.trim()

                        }






                    val color =

                        when{


                            dayStatus.any {

                                it.status == "결석"

                            } ->

                                Color.Red



                            dayStatus.any {

                                it.status == "지각"

                            } ->

                                Color.Yellow



                            dayStatus.any {

                                it.status == "출석"

                            } ->

                                Color.Green



                            else ->

                                Color.LightGray



                        }







                    Box(

                        modifier = Modifier

                            .padding(3.dp)

                            .height(45.dp)

                            .fillMaxWidth()

                            .background(color)

                            .clickable {


                                onSelect(date)


                            }


                    ){



                        Text(

                            date.substring(8),

                            modifier =
                                Modifier.padding(8.dp)

                        )



                    }



                }



            }



        }



    }



}
