package com.example.attendanceappfinal.student


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.Timetable
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.util.getClassStatus
import com.example.attendanceappfinal.util.getToday
import com.example.attendanceappfinal.util.rememberCurrentTime
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.attendanceappfinal.notification.AttendanceListener
import com.example.attendanceappfinal.repository.AttendanceRepository

@Composable
fun StudentPage(

    user: User,

    onAttendanceClick: () -> Unit,

    onNotificationClick: () -> Unit,

    onLogout: () -> Unit = {}

){

    val context = LocalContext.current

    var backPressedOnce by remember {

        mutableStateOf(false)

    }



    BackHandler {

        if(backPressedOnce){

            onLogout()

        }else{

            backPressedOnce = true

            Toast.makeText(
                context,
                "뒤로가기를 한 번 더 누르면 로그아웃됩니다",
                Toast.LENGTH_SHORT
            ).show()

        }

    }



    LaunchedEffect(backPressedOnce){


        if(backPressedOnce){


            delay(2000)


            backPressedOnce = false


        }


    }







    val database =
        FirebaseDatabase.getInstance()



    var timetable by remember {

        mutableStateOf(
            emptyList<Timetable>()
        )

    }




    val currentTime =
        rememberCurrentTime()


    val attendance by AttendanceRepository.getAttendanceFlow(
        user.uid
    )
        .collectAsState(
            initial = emptyList()
        )



    fun loadTimetable(){

        println("학생 UID 확인 : ${user.uid}")


        database

            .getReference("timetable")

            .child(user.uid)

            .get()

            .addOnSuccessListener { snapshot ->


                println("시간표 존재 여부 : ${snapshot.exists()}")
                println("시간표 개수 : ${snapshot.childrenCount}")



                val list =
                    mutableListOf<Timetable>()


                snapshot.children.forEach { child ->


                    println("시간표 키 : ${child.key}")

                    println("시간표 데이터 : ${child.value}")



                    child.getValue(
                        Timetable::class.java
                    )
                        ?.let {

                            println("변환 성공 : $it")

                            list.add(it)

                        }
                        ?: println("Timetable 변환 실패")



                }


                database
                    .getReference("teacherTimetable")
                    .get()
                    .addOnSuccessListener { teacherSnapshot ->
                        val classSchedules = teacherSnapshot.children.flatMap { teacherNode ->
                            teacherNode.children.mapNotNull { child ->
                                child.getValue(Timetable::class.java)?.copy(id = child.key ?: "")
                            }
                        }.filter { schedule ->
                            schedule.className.trim() == user.className.trim() &&
                                (schedule.grade.isBlank() || schedule.grade.trim() == user.grade.trim())
                        }
                        timetable = (list + classSchedules)
                            .distinctBy { it.studentLessonKey() }
                            .sortedWith(compareBy<Timetable> { studentDayOrder(it.day) }.thenBy { it.startTime })
                    }
                    .addOnFailureListener {
                        timetable = list
                            .distinctBy { it.studentLessonKey() }
                            .sortedWith(compareBy<Timetable> { studentDayOrder(it.day) }.thenBy { it.startTime })
                    }


            }


    }







    LaunchedEffect(user.uid){


        loadTimetable()



        AttendanceListener(

            context = context,

            studentUid = user.uid

        ).start()


    }







    val today =
        getToday()


    println("현재 요일 : $today")


    timetable.forEach {

        println(
            "시간표 요일 : ${it.day} / 과목 : ${it.subject}"
        )

    }



    val todayClass =
        timetable.filter {


            isSameStudentDay(it.day, today)


        }

    val todayDate = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString()

    fun displayedClassStatus(item: Timetable): String {
        val matchingAttendance = attendance
            .filter { record ->
                record.date == todayDate &&
                    record.subject.trim() == item.subject.trim() &&
                    (record.teacherUid.isBlank() || item.teacherUid.isBlank() || record.teacherUid == item.teacherUid)
            }
        // If an older automatic-absence record remains beside a later manual
        // present/late entry, the teacher's manual entry must take precedence.
        val savedAttendance = matchingAttendance
            .filter { it.status == "출석" || it.status == "지각" }
            .maxByOrNull { it.timestamp }
            ?: matchingAttendance.maxByOrNull { it.timestamp }
        return savedAttendance?.let { "${it.status} 처리 완료" } ?: getClassStatus(item.startTime)
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






        Row(

            modifier =
                Modifier.fillMaxWidth(),


            horizontalArrangement =
                Arrangement.SpaceBetween


        ){





            Column {


                Text(

                    "${user.name} 학생",

                    style =
                        MaterialTheme.typography.headlineMedium

                )


                Spacer(
                    Modifier.height(5.dp)
                )


                Text(

                    "출결 관리 시스템",

                    style =
                        MaterialTheme.typography.bodyMedium

                )


            }








            Card(

                modifier =
                    Modifier.clickable {

                        onNotificationClick()

                    },


                shape =
                    RoundedCornerShape(50.dp)

            ){


                IconButton(

                    onClick = {

                        onNotificationClick()

                    }

                ){


                    Icon(

                        imageVector =
                            Icons.Default.Notifications,


                        contentDescription =
                            "알림"

                    )


                }


            }



        }









        Spacer(
            Modifier.height(25.dp)
        )







        Card(

            modifier =
                Modifier.fillMaxWidth(),


            shape =
                RoundedCornerShape(18.dp)

        ){


            Column(

                modifier =
                    Modifier.padding(18.dp)

            ){


                Text(
                    "현재 시간",

                    style =
                        MaterialTheme.typography.titleMedium
                )


                Spacer(
                    Modifier.height(8.dp)
                )


                Text(

                    currentTime,

                    style =
                        MaterialTheme.typography.headlineSmall

                )


            }


        }









        Spacer(
            Modifier.height(18.dp)
        )




        Text(

            "📅 오늘 시간표",

            style =
                MaterialTheme.typography.titleLarge

        )





        Spacer(
            Modifier.height(10.dp)
        )







        if(todayClass.isEmpty()){


            StudentInfoCard(

                title = "오늘 수업",

                content = "오늘 수업 없음"

            )


        }else{


            todayClass.forEach { item ->


                val status = displayedClassStatus(item)


                StudentInfoCard(

                    title =
                        "${item.startTime} ~ ${item.endTime}",


                    content =

                        """
            과목 : ${item.subject}
            담당 : ${item.teacher}
            상태 : $status
            """.trimIndent()


                )


            }


        }








        Spacer(
            Modifier.height(30.dp)
        )








        Button(

            modifier =
                Modifier.fillMaxWidth(),


            onClick =
                onAttendanceClick,


            shape =
                RoundedCornerShape(15.dp)

        ){

            Text(
                "출결 확인"
            )

        }







        Spacer(
            Modifier.height(15.dp)
        )







        Button(

            modifier =
                Modifier.fillMaxWidth(),


            onClick =
                onLogout,


            shape =
                RoundedCornerShape(15.dp)

        ){


            Text(
                "로그아웃"
            )


        }







    }


}









@Composable
private fun StudentInfoCard(

    title:String,

    content:String

){



    Card(

        modifier =
            Modifier

                .fillMaxWidth()

                .padding(vertical = 5.dp),


        shape =
            RoundedCornerShape(18.dp),


        elevation =
            CardDefaults.cardElevation(

                defaultElevation = 4.dp

            )

    ){



        Column(

            modifier =
                Modifier.padding(18.dp)

        ){



            Text(

                title,

                style =
                    MaterialTheme.typography.titleLarge

            )



            Spacer(
                Modifier.height(8.dp)
            )



            Text(
                content
            )


        }


    }


}

private fun Timetable.studentLessonKey(): String = listOf(
    day.trim(), grade.trim(), className.trim(), subject.trim(), teacherUid.trim(), startTime.trim(), endTime.trim()
).joinToString("\u0001")

private fun isSameStudentDay(day: String, today: String): Boolean {
    val normalized = day.trim()
    return normalized == today || normalized == "${today}요일"
}

private fun studentDayOrder(day: String): Int = when (day.trim().removeSuffix("요일")) {
    "월" -> 0
    "화" -> 1
    "수" -> 2
    "목" -> 3
    "금" -> 4
    "토" -> 5
    "일" -> 6
    else -> Int.MAX_VALUE
}
