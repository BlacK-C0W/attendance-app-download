package com.example.attendanceappfinal.admin


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.AttendanceLog
import com.example.attendanceappfinal.repository.AttendanceRepository



@Composable
fun AdminAttendanceLogPage(

    onBack: () -> Unit

){


    BackHandler {

        onBack()

    }



    val logs by AttendanceRepository.getAttendanceLogFlow()

        .collectAsState(

            initial = emptyList()

        )



    var searchText by remember {

        mutableStateOf("")

    }



    var selectedFilter by remember {

        mutableStateOf("전체")

    }



    val filters = listOf(

        "전체",
        "출석",
        "결석",
        "지각"

    )




    val filteredLogs =

        logs

            .filter { log ->



                val searchMatch =


                    searchText.isBlank()

                            ||

                            log.studentName.contains(

                                searchText,

                                ignoreCase = true

                            )

                            ||

                            log.subject.contains(

                                searchText,

                                ignoreCase = true

                            )

                            ||

                            log.reason.contains(

                                searchText,

                                ignoreCase = true

                            )




                val statusMatch =

                    when(selectedFilter){


                        "전체" -> true


                        else ->

                            log.afterStatus == selectedFilter


                    }



                searchMatch && statusMatch



            }

            .reversed()





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





        Card(

            modifier = Modifier

                .fillMaxWidth()

        ){


            Column(

                modifier =
                    Modifier.padding(18.dp)

            ){



                Text(

                    "출결 수정 기록",

                    style =
                        MaterialTheme.typography.titleLarge

                )



                Spacer(

                    Modifier.height(5.dp)

                )



                Text(

                    "관리자가 수정한 출결 내역 확인",

                    style =
                        MaterialTheme.typography.bodyMedium

                )



            }


        }




        Spacer(

            Modifier.height(20.dp)

        )






        OutlinedTextField(

            modifier =
                Modifier.fillMaxWidth(),


            value = searchText,


            onValueChange = {

                searchText = it

            },


            label = {

                Text(

                    "학생명 / 과목 / 사유 검색"

                )

            }


        )





        Spacer(

            Modifier.height(15.dp)

        )






        Row(

            modifier =
                Modifier.fillMaxWidth(),


            horizontalArrangement =
                Arrangement.spacedBy(6.dp)

        ){



            filters.forEach { filter ->



                FilterChip(

                    selected =
                        selectedFilter == filter,


                    onClick = {

                        selectedFilter = filter

                    },


                    label = {

                        Text(filter)

                    }


                )



            }



        }






        Spacer(

            Modifier.height(15.dp)

        )






        Text(

            "검색 결과 ${filteredLogs.size}건",

            style =
                MaterialTheme.typography.titleMedium

        )



        Spacer(

            Modifier.height(10.dp)

        )
        if(filteredLogs.isEmpty()){



            Card(

                modifier =
                    Modifier.fillMaxWidth()

            ){



                Text(

                    "출결 수정 기록이 없습니다.",

                    modifier =
                        Modifier.padding(20.dp)

                )



            }



        }else{



            filteredLogs.forEach { log ->



                AttendanceLogCard(

                    log = log

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









@Composable
fun AttendanceLogCard(

    log: AttendanceLog

){



    Card(

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 6.dp),


        elevation =

            CardDefaults.cardElevation(

                defaultElevation = 4.dp

            )

    ){



        Column(

            modifier =
                Modifier.padding(16.dp)

        ){



            Text(

                log.studentName,

                style =
                    MaterialTheme.typography.titleLarge

            )





            Spacer(

                Modifier.height(8.dp)

            )





            Text(

                "📚 과목 : ${log.subject}"

            )





            Spacer(

                Modifier.height(8.dp)

            )





            Row{



                Text(

                    "변경 : "

                )



                Text(

                    log.beforeStatus,

                    color = Color.Gray

                )



                Text(

                    "  →  "

                )



                Text(

                    log.afterStatus,

                    color = MaterialTheme.colorScheme.primary

                )



            }







            if(log.reason.isNotBlank()){



                Spacer(

                    Modifier.height(8.dp)

                )



                Text(

                    "📝 사유 : ${log.reason}"

                )



            }







            Spacer(

                Modifier.height(8.dp)

            )






            Text(

                "🕒 수정 시간 : ${log.time}"

            )





        }



    }



}