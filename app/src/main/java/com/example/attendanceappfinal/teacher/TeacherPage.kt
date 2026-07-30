package com.example.attendanceappfinal.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.Timetable
import com.example.attendanceappfinal.nfc.NfcAttendance
import com.example.attendanceappfinal.repository.AutoAbsenceRepository
import kotlinx.coroutines.delay


@Composable
fun TeacherPage(

    nfcTag:String?,

    clearNfc:()->Unit = {},

    teacherUid:String,

    teacherName:String,

    onLogout:()->Unit = {}

){

    val context = LocalContext.current


    var page by remember {
        mutableStateOf("")
    }

    var selectedClass by remember {
        mutableStateOf<Timetable?>(null)
    }


    var message by remember {
        mutableStateOf("")
    }


    var backTime by remember {
        mutableLongStateOf(0L)
    }

    // Keep timetable/holiday data in memory. Firebase is only contacted when those
    // values change, not by the one-minute local timer below.
    DisposableEffect(teacherUid) {
        AutoAbsenceRepository.startMonitoring(teacherUid)
        onDispose { AutoAbsenceRepository.stopMonitoring(teacherUid) }
    }

    // The check itself is local. Student and attendance data are read only once
    // for a lesson after its end time, then that lesson is marked as processed.
    LaunchedEffect(teacherUid) {
        while (true) {
            AutoAbsenceRepository.checkExpiredClasses(teacherUid)
            delay(60_000)
        }
    }



    BackHandler {


        if(selectedClass != null){

            selectedClass = null

        } else if(page.isNotBlank()){

            page=""

        }else{


            val now =
                System.currentTimeMillis()


            if(now-backTime < 2000){

                onLogout()

            }else{

                backTime = now

                Toast.makeText(
                    context,
                    "한 번 더 누르면 로그아웃",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    }



    if(page=="student"){


        TeacherStudentManagePage(

            teacherUid = teacherUid,

            teacherName = teacherName,

            nfcTag = nfcTag,

            onBack = {

                page=""

            }

        )

        return

    }

    selectedClass?.let { timetable ->

        TeacherClassAttendancePage(

            timetable = timetable,

            teacherName = teacherName,

            onBack = {

                selectedClass = null

            }

        )

        return

    }




    if(page=="myTimetable"){


        TeacherMyTimetablePage(

            teacherUid = teacherUid,

            teacherName = teacherName,

            onBack = {

                page=""

            },

            onClassOpen = { timetable ->

                selectedClass = timetable

            }

        )

        return

    }




    if(page=="attendance"){


        TeacherAttendanceEditPage(

            onBack = {

                page=""

            }

        )

        return

    }

    if(page=="autoAbsence"){

        TeacherAutoAbsencePage(
            onBack = { page="" }
        )

        return

    }





    LaunchedEffect(nfcTag){


        nfcTag?.let {


            NfcAttendance.processNfcAttendance(

                nfcTag = it,

                teacherUid = teacherUid

            ){ result ->

                message=result

                clearNfc()

            }


        }


    }






    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    top = UiConfig.topPadding,
                    start = UiConfig.sidePadding,
                    end = UiConfig.sidePadding,
                    bottom = UiConfig.bottomPadding
                )

    ){


        Text(
            "👨‍🏫 선생님 페이지",
            style =
                MaterialTheme.typography.headlineMedium
        )


        Spacer(
            Modifier.height(25.dp)
        )



        if(message.isNotBlank()){


            Text(message)


        }



        MenuCard(
            "👥 학생 관리",
            "학생 정보 / 시간표 / NFC"
        ){

            page="student"

        }



        Spacer(
            Modifier.height(15.dp)
        )



        MenuCard(
            "📅 내 시간표",
            "내 수업 관리"
        ){

            page="myTimetable"

        }



        Spacer(
            Modifier.height(15.dp)
        )



        MenuCard(
            "📝 출결 수정",
            "출결 기록 수정"
        ){

            page="attendance"

        }

        Spacer(
            Modifier.height(15.dp)
        )

        MenuCard(
            "자동 결석 처리 내역",
            "학년 · 반 · 학생별 자동 결석 확인"
        ){

            page="autoAbsence"

        }



        Spacer(
            Modifier.weight(1f)
        )



        Button(

            modifier =
                Modifier.fillMaxWidth(),

            onClick = {

                onLogout()

            }

        ){

            Text("로그아웃")

        }


    }


}



@Composable
private fun MenuCard(

    title:String,

    desc:String,

    click:()->Unit

){

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    click()
                },

        shape =
            RoundedCornerShape(18.dp)

    ){


        Column(

            modifier =
                Modifier.padding(20.dp)

        ){

            Text(
                title,
                style =
                    MaterialTheme.typography.titleLarge
            )

            Text(desc)

        }

    }

}
