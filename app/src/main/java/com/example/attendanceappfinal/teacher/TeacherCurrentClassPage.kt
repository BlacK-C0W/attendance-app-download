package com.example.attendanceappfinal.teacher


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
import com.example.attendanceappfinal.model.Timetable
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.*



@Composable
fun TeacherCurrentClassPage(

    teacherUid:String,

    onBack:()->Unit

){


    BackHandler {

        onBack()

    }



    val database =
        FirebaseDatabase.getInstance()



    var timetableList by remember {

        mutableStateOf(
            emptyList<Timetable>()
        )

    }

    var allTimetableList by remember { mutableStateOf(emptyList<Timetable>()) }
    var selectedTimetable by remember { mutableStateOf<Timetable?>(null) }
    var showManualClassPicker by remember { mutableStateOf(false) }



    var students by remember {

        mutableStateOf(
            emptyList<User>()
        )

    }



    var statusMap =
        remember {

            mutableStateMapOf<String,String>()

        }



    var message by remember {

        mutableStateOf("")

    }





    fun todayKor():String{


        return when(

            Calendar.getInstance()
                .get(Calendar.DAY_OF_WEEK)

        ){

            Calendar.MONDAY ->
                "월"

            Calendar.TUESDAY ->
                "화"

            Calendar.WEDNESDAY ->
                "수"

            Calendar.THURSDAY ->
                "목"

            Calendar.FRIDAY ->
                "금"

            Calendar.SATURDAY ->
                "토"

            else ->
                "일"

        }


    }





    fun selectTimetable(timetable: Timetable) {
        selectedTimetable = timetable
        students = emptyList()
        statusMap.clear()
        loadStudents(listOf(timetable)) { loadedStudents ->
            students = loadedStudents
            loadedStudents.forEach { student -> statusMap[student.uid] = "출석" }
        }
    }

    fun load(){


        database

            .getReference("teacherTimetable")

            .child(teacherUid)

            .get()

            .addOnSuccessListener { snapshot ->



                val list =
                    mutableListOf<Timetable>()


                snapshot.children.forEach { child ->



                    child.getValue(
                        Timetable::class.java
                    )
                        ?.let {

                            list.add(it)

                        }


                }


                allTimetableList = list.sortedWith(
                    compareBy<Timetable> { currentClassDayOrder(it.day) }.thenBy { it.startTime }
                )
                timetableList = allTimetableList.filter { isSameCurrentClassDay(it.day, todayKor()) }
                timetableList.firstOrNull()?.let(::selectTimetable) ?: run {
                    selectedTimetable = null
                    students = emptyList()
                    statusMap.clear()
                }


            }



    }







    fun saveAttendance(){



        students.forEach { student ->



            val attendance = Attendance(


                studentUid =
                    student.uid,


                studentName =
                    student.name,


                subject =
                    selectedTimetable
                        ?.subject
                        ?: "",


                teacher =
                    "선생님",



                date =
                    SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    )
                        .format(Date()),



                time =
                    SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                    )
                        .format(Date()),



                status =
                    statusMap[student.uid]
                        ?: "출석",



                timestamp =
                    System.currentTimeMillis()


            )



            database

                .getReference("attendance")

                .child(student.uid)

                .push()

                .setValue(attendance)



        }



        message =
            "출결 저장 완료"



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

                .padding(

                    top = UiConfig.topPadding,

                    start = UiConfig.sidePadding,

                    end = UiConfig.sidePadding,

                    bottom = UiConfig.bottomPadding

                )

    ){



        Text(

            "📅 내 현재 수업",

            style =
                MaterialTheme.typography.headlineMedium

        )



        Spacer(
            Modifier.height(20.dp)
        )




        if(timetableList.isEmpty()){


            Text(
                "현재 진행중인 수업이 없습니다."
            )

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showManualClassPicker = true },
                enabled = allTimetableList.isNotEmpty()
            ) {
                Text("다른 수업 찾아서 선택")
            }


        }
        else{


            timetableList.forEach { item ->



                Card(

                    modifier =
                        Modifier.fillMaxWidth()

                ){


                    Column(

                        modifier =
                            Modifier.padding(15.dp)

                    ){


                        Text(
                            "${item.grade.ifBlank { "학년 미지정" }} ${item.className} ${item.subject}",
                            style =
                                MaterialTheme.typography.titleLarge
                        )


                        Text(
                            "${item.startTime} ~ ${item.endTime}"
                        )

                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { selectTimetable(item) }
                        ) {
                            Text(if (selectedTimetable == item) "선택된 수업" else "이 수업 선택")
                        }


                    }


                }



            }




            Spacer(
                Modifier.height(20.dp)
            )



            Text(

                "학생 출결",

                style =
                    MaterialTheme.typography.titleLarge

            )





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
                            student.name,
                            style =
                                MaterialTheme.typography.titleMedium
                        )



                        Row(

                            horizontalArrangement =
                                Arrangement.SpaceEvenly,

                            modifier =
                                Modifier.fillMaxWidth()

                        ){


                            listOf(

                                "출석",
                                "지각",
                                "결석"

                            )
                                .forEach { status ->



                                    Button(

                                        onClick = {


                                            statusMap[student.uid] =
                                                status


                                        }

                                    ){


                                        Text(

                                            if(
                                                statusMap[student.uid]
                                                == status
                                            )

                                                "✓ $status"

                                            else

                                                status

                                        )


                                    }



                                }



                        }



                    }



                }



            }





            Spacer(
                Modifier.height(20.dp)
            )




            Button(

                modifier =
                    Modifier.fillMaxWidth(),

                onClick = {

                    saveAttendance()

                }

            ){

                Text("💾 출결 저장")

            }



        }





        Spacer(
            Modifier.height(20.dp)
        )



        if(message.isNotBlank()){

            Text(message)

        }





        OutlinedButton(

            modifier =
                Modifier.fillMaxWidth(),

            onClick = {

                onBack()

            }

        ){

            Text("뒤로가기")

        }



    }

    if (showManualClassPicker) {
        AlertDialog(
            onDismissRequest = { showManualClassPicker = false },
            title = { Text("수업 찾아서 선택") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    allTimetableList.forEach { item ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = {
                                showManualClassPicker = false
                                selectTimetable(item)
                            }
                        ) {
                            Text("${item.day} · ${item.grade} ${item.className} · ${item.subject} (${item.startTime}~${item.endTime})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManualClassPicker = false }) { Text("닫기") }
            }
        )
    }



}







private fun isSameCurrentClassDay(day: String, today: String): Boolean {
    val normalized = day.trim()
    return normalized == today || normalized == "${today}요일"
}

private fun currentClassDayOrder(day: String): Int = when (day.trim().removeSuffix("요일")) {
    "월" -> 0
    "화" -> 1
    "수" -> 2
    "목" -> 3
    "금" -> 4
    "토" -> 5
    "일" -> 6
    else -> Int.MAX_VALUE
}

private fun loadStudents(

    timetableList:List<Timetable>,

    callback:(List<User>)->Unit = {}

){



    if(timetableList.isEmpty())
        return




    val database =
        FirebaseDatabase.getInstance()



    val targetClass =
        timetableList.first().className

    val targetGrade =
        timetableList.first().grade





    database

        .getReference("users")

        .get()

        .addOnSuccessListener { snapshot ->



            val list =
                mutableListOf<User>()



            snapshot.children.forEach { child ->



                val user =

                    child.getValue(
                        User::class.java
                    )
                        ?.copy(
                            uid =
                                child.key ?: ""
                        )



                if(
                    user != null
                    &&
                    user.className.trim() == targetClass.trim()

                    &&

                    (targetGrade.isBlank() || normalizeCurrentGrade(user.grade) == normalizeCurrentGrade(targetGrade))
                    &&
                    user.role == "student"
                ){

                    list.add(user)

                }



            }



            callback(list)


        }



}

private fun normalizeCurrentGrade(value: String): String =
    value.replace(" ", "").replace("학년", "").trim()
