package com.example.attendanceappfinal.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig

@Composable
fun TeacherHome(

    message: String,

    onStudent: () -> Unit,

    onTimetable: () -> Unit,

    onNfc: () -> Unit,

    onAttendance: () -> Unit

) {

    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = UiConfig.topPadding,
                start = UiConfig.sidePadding,
                end = UiConfig.sidePadding,
                bottom = UiConfig.bottomPadding
            )

    ) {

        Text(

            text = "선생님 페이지",

            style = MaterialTheme.typography.headlineMedium

        )

        Spacer(Modifier.height(20.dp))

        if (message.isNotEmpty()) {

            Text(message)

            Spacer(Modifier.height(20.dp))

        }

        Button(

            modifier = Modifier.fillMaxWidth(),

            onClick = onStudent

        ) {

            Text("학생 관리")

        }

        Spacer(Modifier.height(15.dp))

        Button(

            modifier = Modifier.fillMaxWidth(),

            onClick = onTimetable

        ) {

            Text("시간표 관리")

        }

        Spacer(Modifier.height(15.dp))

        Button(

            modifier = Modifier.fillMaxWidth(),

            onClick = onNfc

        ) {

            Text("NFC 등록")

        }

        Spacer(Modifier.height(15.dp))

        Button(

            modifier = Modifier.fillMaxWidth(),

            onClick = onAttendance

        ) {

            Text("출결 수정")

        }

        Spacer(Modifier.height(30.dp))

        Text("NFC 태그 대기중")

    }

}