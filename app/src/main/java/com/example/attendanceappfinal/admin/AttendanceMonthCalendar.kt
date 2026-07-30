package com.example.attendanceappfinal.admin


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.model.Attendance
import java.time.LocalDate
import java.time.YearMonth



@Composable
fun AttendanceMonthCalendar(

    year: Int,

    month: Int,

    attendanceList: List<Attendance>,

    onSelect: (String) -> Unit

){



    val yearMonth =

        YearMonth.of(
            year,
            month
        )



    val totalDays =

        yearMonth.lengthOfMonth()



    val startOffset =

        yearMonth
            .atDay(1)
            .dayOfWeek
            .value % 7





    Column(

        modifier =
            Modifier.fillMaxWidth()

    ){



        Text(

            "${year}년 ${month}월",

            style =
                MaterialTheme.typography.titleLarge

        )



        Spacer(
            Modifier.height(10.dp)
        )





        Row(

            modifier =
                Modifier.fillMaxWidth()

        ){


            listOf(
                "일",
                "월",
                "화",
                "수",
                "목",
                "금",
                "토"

            ).forEach { day ->


                Text(

                    day,

                    modifier =
                        Modifier.weight(1f)

                )


            }


        }





        Spacer(
            Modifier.height(5.dp)
        )





        var currentDay = 1





        repeat(6){ week ->



            Row(

                modifier =
                    Modifier.fillMaxWidth()

            ){



                repeat(7){ index ->



                    val position =
                        week * 7 + index




                    if(

                        position >= startOffset &&

                        currentDay <= totalDays

                    ){



                        val date =

                            LocalDate.of(

                                year,

                                month,

                                currentDay

                            )



                        val dateString =
                            date.toString()




                        val dayAttendance =

                            attendanceList.filter {


                                it.date.trim() == dateString


                            }






                        val status =

                            when {


                                dayAttendance.any {

                                    it.status == "결석"

                                } -> "결석"



                                dayAttendance.any {

                                    it.status == "지각"

                                } -> "지각"



                                dayAttendance.any {

                                    it.status == "출석"

                                } -> "출석"



                                else -> ""



                            }






                        Box(

                            modifier = Modifier

                                .weight(1f)

                                .padding(3.dp)

                                .height(55.dp)

                                .background(


                                    when(status){


                                        "출석" ->

                                            Color.Green.copy(
                                                alpha = 0.35f
                                            )


                                        "지각" ->

                                            Color.Yellow.copy(
                                                alpha = 0.45f
                                            )


                                        "결석" ->

                                            Color.Red.copy(
                                                alpha = 0.35f
                                            )


                                        else ->

                                            Color.Transparent


                                    }

                                )

                                .clickable {


                                    if(dayAttendance.isNotEmpty()){

                                        onSelect(dateString)

                                    }


                                }


                        ){



                            Column(

                                modifier =
                                    Modifier.padding(5.dp)

                            ){



                                Text(

                                    "$currentDay"

                                )





                                if(status.isNotEmpty()){


                                    Text(

                                        status,

                                        style =
                                            MaterialTheme.typography.labelSmall

                                    )


                                }



                                if(dayAttendance.size > 1){


                                    Text(

                                        "+${dayAttendance.size - 1}",

                                        style =
                                            MaterialTheme.typography.labelSmall

                                    )


                                }



                            }



                        }



                        currentDay++



                    }

                    else {



                        Spacer(

                            modifier =
                                Modifier

                                    .weight(1f)

                                    .height(55.dp)

                        )


                    }



                }



            }



        }



    }



}