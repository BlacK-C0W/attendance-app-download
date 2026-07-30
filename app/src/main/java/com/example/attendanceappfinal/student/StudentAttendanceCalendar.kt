package com.example.attendanceappfinal.student

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.Holiday
import com.example.attendanceappfinal.repository.KoreanHolidayRepository
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.*



@Composable
fun StudentAttendanceCalendar(

    attendance: List<Attendance>

){


    var currentCalendar by remember {

        mutableStateOf(
            Calendar.getInstance()
        )

    }

    var academyHolidays by remember { mutableStateOf(emptyList<Holiday>()) }

    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().getReference("holidays").get().addOnSuccessListener { snapshot ->
            academyHolidays = snapshot.children.mapNotNull { child ->
                child.getValue(Holiday::class.java)?.let { holiday ->
                    if (holiday.date.isBlank()) holiday.copy(date = child.key ?: "") else holiday
                }
            }
        }
    }





    fun changeMonth(amount:Int){


        val newCalendar =

            currentCalendar.clone()

                    as Calendar



        newCalendar.add(

            Calendar.MONTH,

            amount

        )



        newCalendar.set(

            Calendar.DAY_OF_MONTH,

            1

        )



        currentCalendar = newCalendar


    }







    val year =

        currentCalendar.get(Calendar.YEAR)



    val month =

        currentCalendar.get(Calendar.MONTH)







    val monthAttendance = remember(

        attendance,

        year,

        month

    ){



        val monthKey =

            String.format(

                "%04d-%02d",

                year,

                month + 1

            )



        attendance.filter {


            it.date.startsWith(monthKey)


        }


    }

    val publicHolidayDates = KoreanHolidayRepository.publicHolidays(year)
        .flatMap { holiday -> holidayDates(holiday.date, holiday.endDate.ifBlank { holiday.date }) }
        .toSet() - academyHolidays.filter { it.type == "publicHolidayClassDay" }.map { it.date }.toSet()
    val academyVacationDates = academyHolidays.filter { it.type == "academyVacation" }
        .flatMap { holiday -> holidayDates(holiday.date, holiday.endDate.ifBlank { holiday.date }) }.toSet()








    val present =

        monthAttendance.count {

            it.status == "출석"

        }




    val late =

        monthAttendance.count {

            it.status == "지각"

        }





    val absent =

        monthAttendance.count {

            it.status == "결석"

        }







    val total =

        monthAttendance.size






    val rate =

        if(total == 0)

            0

        else

            ((present + late) * 100) / total








    val abnormalAttendance = remember(

        monthAttendance

    ){


        monthAttendance.filter {


            it.status == "지각" ||

                    it.status == "결석"


        }


    }









    Column(

        modifier =
            Modifier.fillMaxWidth()

    ){





        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically

        ){



            Button(

                modifier =
                    Modifier.size(
                        50.dp,
                        35.dp
                    ),

                contentPadding =
                    PaddingValues(0.dp),


                onClick = {

                    changeMonth(-1)

                }

            ){

                Text("◀")

            }







            Text(

                "${year}년 ${month + 1}월 출결",

                style =
                    MaterialTheme.typography.titleLarge

            )







            Button(

                modifier =
                    Modifier.size(
                        50.dp,
                        35.dp
                    ),

                contentPadding =
                    PaddingValues(0.dp),


                onClick = {

                    changeMonth(1)

                }

            ){

                Text("▶")

            }




        }







        Spacer(
            Modifier.height(15.dp)
        )







        Card(

            modifier =
                Modifier.fillMaxWidth()

        ){


            Column(

                modifier =
                    Modifier.padding(15.dp)

            ){


                Text("출석 : ${present}회")

                Text("지각 : ${late}회")

                Text("결석 : ${absent}회")

                Text("출석률 : ${rate}%")



            }


        }







        Spacer(
            Modifier.height(15.dp)
        )







        StudentCalendarGrid(

            year = year,

            month = month,

            attendance = monthAttendance

            ,publicHolidayDates = publicHolidayDates

            ,academyVacationDates = academyVacationDates

        )








        Spacer(
            Modifier.height(20.dp)
        )








        if(abnormalAttendance.isNotEmpty()){



            Text(

                "지각 / 결석 기록",

                style =
                    MaterialTheme.typography.titleLarge

            )





            Spacer(
                Modifier.height(10.dp)
            )






            abnormalAttendance.forEach { item ->




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
                            item.subject,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "학생 : ${item.studentName}"
                        )

                        Text(
                            "날짜 : ${item.date}"
                        )

                        Text(
                            "시간 : ${item.time}"
                        )

                        Text(
                            "담당 : ${item.teacher}"
                        )

                        Text(
                            "상태 : ${item.status}",
                            color =
                                if (item.status == "결석")
                                    Color.Red
                                else
                                    Color(0xFFFF9800)
                        )





                        if(item.reason.isNotBlank()){


                            Text(
                                "사유 : ${item.reason}"
                            )


                        }





                    }



                }




            }




        }







    }



}









@Composable
fun StudentCalendarGrid(

    year:Int,

    month:Int,

    attendance:List<Attendance>,

    publicHolidayDates: Set<String> = emptySet(),

    academyVacationDates: Set<String> = emptySet()

){



    val calendar =

        Calendar.getInstance()



    calendar.set(

        year,

        month,

        1

    )





    val firstDay =

        calendar.get(Calendar.DAY_OF_WEEK) - 1





    val maxDay =

        calendar.getActualMaximum(

            Calendar.DAY_OF_MONTH

        )







    LazyVerticalGrid(

        columns =
            GridCells.Fixed(7),


        modifier =
            Modifier.height(300.dp)


    ){



        items(

            firstDay + maxDay

        ){ index ->



            if(index < firstDay){



                Box(

                    modifier =
                        Modifier.height(45.dp)

                )



            }else{



                val day =

                    index - firstDay + 1





                val dateText =

                    String.format(

                        "%04d-%02d-%02d",

                        year,

                        month + 1,

                        day

                    )





                val todayAttendance =

                    attendance.filter {

                        it.date == dateText

                    }





                val color =

                    when{


                        todayAttendance.any {

                            it.status == "결석"

                        } -> Color.Red



                        todayAttendance.any {

                            it.status == "지각"

                        } -> Color.Yellow



                        todayAttendance.any {

                            it.status == "출석"

                        } -> Color.Green

                        dateText in academyVacationDates -> Color(0xFFE8DEF8)

                        dateText in publicHolidayDates -> Color(0xFFFFDAD6)



                        else -> Color.LightGray


                    }







                Box(

                    modifier = Modifier

                        .padding(3.dp)

                        .height(45.dp)

                        .fillMaxWidth()

                        .background(

                            color.copy(

                                alpha = 0.5f

                            )

                        ),


                    contentAlignment =
                        Alignment.Center

                ){



                    Column(

                        horizontalAlignment =
                            Alignment.CenterHorizontally

                    ){



                        Text(

                            day.toString()

                        )





                        if(todayAttendance.isNotEmpty()){


                            Text(

                                todayAttendance.first().status,

                                style =
                                    MaterialTheme.typography.labelSmall

                            )


                        }




                    }



                }





            }




        }




    }



}

private fun holidayDates(start: String, end: String): List<String> = try {
    val finalDate = LocalDate.parse(end)
    generateSequence(LocalDate.parse(start)) { date -> date.plusDays(1).takeIf { !it.isAfter(finalDate) } }
        .map { it.toString() }.toList()
} catch (_: Exception) { emptyList() }
