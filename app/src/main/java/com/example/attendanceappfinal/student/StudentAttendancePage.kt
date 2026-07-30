package com.example.attendanceappfinal.student


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.repository.AttendanceRepository
import androidx.compose.ui.unit.dp

@Composable
fun StudentAttendancePage(

    user: User,

    onBack: () -> Unit

) {


    BackHandler {

        onBack()

    }



    val attendance by AttendanceRepository.getAttendanceFlow(
        user.uid
    )
        .collectAsState(

            initial = emptyList()

        )





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

    ) {



        Text(

            "출결 확인",

            style =
                MaterialTheme.typography.headlineMedium

        )




        Spacer(

            Modifier.height(20.dp)

        )



        val total =
            attendance.size


        val present =
            attendance.count {

                it.status == "출석"

            }


        val late =
            attendance.count {

                it.status == "지각"

            }


        val absent =
            attendance.count {

                it.status == "결석"

            }


        val attendanceRate =

            if(total == 0)

                0

            else

                ((present + late) * 100) / total

        Card(

            modifier =
                Modifier.fillMaxWidth()

        ){

            Column(

                modifier =
                    Modifier.padding(18.dp)

            ){

                Text(

                    "📊 출석 현황",

                    style =
                        MaterialTheme.typography.titleLarge

                )


                Spacer(
                    Modifier.height(10.dp)
                )


                Text(

                    """
            출석률 : ${attendanceRate}%
            전체 수업 : ${total}회
            출석 : ${present}회
            지각 : ${late}회
            결석 : ${absent}회
            """.trimIndent()

                )


            }

        }

        StudentAttendanceCalendar(

            attendance = attendance

        )



    }


}