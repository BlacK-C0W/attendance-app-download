package com.example.attendanceappfinal.teacher


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.Timetable
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalTime
import java.time.format.DateTimeFormatter



@Composable
fun TeacherTimetablePage(

    student: User,

    teacherUid: String,

    teacherName: String

){


    val database =
        FirebaseDatabase.getInstance()



    val days =
        listOf(

            "월",
            "화",
            "수",
            "목",
            "금",
            "토"

        )



    val subjects =
        listOf(

            "수학",
            "영어",
            "과학",
            "자습"

        )





    var selectedDay by remember {

        mutableStateOf("월")

    }



    var subject by remember {

        mutableStateOf("")

    }



    var subjectExpanded by remember {

        mutableStateOf(false)

    }





    var startTime by remember {

        mutableStateOf("")

    }



    var endTime by remember {

        mutableStateOf("")

    }





    var timetableList by remember {

        mutableStateOf(
            listOf<Timetable>()
        )

    }



    var message by remember {

        mutableStateOf("")

    }







    fun loadTimetable(){


        database

            .getReference("timetable")

            .child(student.uid)

            .get()

            .addOnSuccessListener { snapshot ->



                val list =
                    mutableListOf<Timetable>()



                for(child in snapshot.children){



                    child.getValue(
                        Timetable::class.java
                    )
                        ?.let {


                            list.add(it)


                        }



                }




                timetableList =

                    list.sortedBy {


                        it.startTime


                    }





            }



    }







    LaunchedEffect(student.uid){


        loadTimetable()


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





        Text(

            "${student.name} 시간표",

            style =
                MaterialTheme.typography.headlineMedium

        )





        Spacer(
            Modifier.height(20.dp)
        )







        Text("요일 선택")




        Spacer(
            Modifier.height(10.dp)
        )






        Row(

            modifier =
                Modifier.fillMaxWidth(),


            horizontalArrangement =
                Arrangement.SpaceBetween


        ){



            days.forEach { day ->




                Button(

                    modifier =
                        Modifier.width(45.dp),


                    contentPadding =
                        PaddingValues(0.dp),



                    onClick = {


                        selectedDay = day


                    }


                ){



                    Text(day)



                }



            }



        }







        Spacer(
            Modifier.height(20.dp)
        )









        Box(

            modifier =
                Modifier.fillMaxWidth()


        ){



            OutlinedButton(

                modifier =
                    Modifier.fillMaxWidth(),



                onClick = {


                    subjectExpanded = true


                }



            ){



                Text(

                    if(subject.isEmpty())

                        "과목 선택"

                    else

                        subject


                )



            }







            DropdownMenu(

                expanded =
                    subjectExpanded,


                onDismissRequest = {


                    subjectExpanded = false


                }



            ){



                subjects.forEach { item ->



                    DropdownMenuItem(


                        text = {


                            Text(item)


                        },


                        onClick = {



                            subject = item


                            subjectExpanded = false



                        }


                    )



                }



            }




        }








        Spacer(
            Modifier.height(10.dp)
        )









        OutlinedTextField(

            value = startTime,


            onValueChange = {


                startTime = it



                try{


                    val formatter =

                        DateTimeFormatter.ofPattern(
                            "HH:mm"
                        )



                    val time =

                        LocalTime.parse(

                            it,

                            formatter

                        )



                    endTime =

                        time

                            .plusMinutes(90)

                            .format(formatter)




                }catch(e:Exception){



                    endTime = ""



                }



            },


            modifier =
                Modifier.fillMaxWidth(),



            label = {


                Text("시작 시간")


            },



            placeholder = {


                Text("09:00")


            }



        )








        Spacer(
            Modifier.height(10.dp)
        )









        OutlinedTextField(

            value = endTime,


            onValueChange = {},


            enabled = false,


            modifier =
                Modifier.fillMaxWidth(),



            label = {


                Text("종료 시간 (자동)")


            }



        )








        Spacer(
            Modifier.height(20.dp)
        )






        Button(

            modifier =
                Modifier.fillMaxWidth(),



            onClick = {



                val timeRegex = Regex(

                    "^([01]\\d|2[0-3]):[0-5]\\d$"

                )





                if(

                    subject.isBlank()

                    ||

                    startTime.isBlank()

                    ||

                    endTime.isBlank()

                ){


                    message =
                        "과목과 시작 시간을 입력하세요"



                    return@Button


                }





                if(

                    !startTime.matches(timeRegex)

                ){



                    message =
                        "시간 형식은 HH:mm 입니다"



                    return@Button



                }







                val ref =

                    database

                        .getReference("timetable")

                        .child(student.uid)

                        .push()







                val data = Timetable(

                    id =
                        ref.key ?: "",

                    day =
                        selectedDay,

                    subject =
                        subject,

                    teacher =
                        teacherName,

                    teacherUid = teacherUid,

                    grade = student.grade,

                    className =
                        student.className,

                    startTime =
                        startTime,

                    endTime =
                        endTime

                )





                ref.setValue(data)

                    .addOnSuccessListener {

                        syncStudentScheduleToTeacherTimetable(database, data)



                        message =
                            "시간표 저장 완료"



                        subject = ""

                        startTime = ""

                        endTime = ""



                        loadTimetable()



                    }





            }



        ){


            Text("저장")


        }
        Spacer(
            Modifier.height(20.dp)
        )



        Text(

            message

        )





        Spacer(
            Modifier.height(20.dp)
        )





        Text(

            "등록된 시간표",

            style =
                MaterialTheme.typography.titleLarge

        )







        timetableList.forEach { item ->





            Card(

                modifier =
                    Modifier

                        .fillMaxWidth()

                        .padding(

                            vertical = 5.dp

                        )

            ){





                Row(

                    modifier =
                        Modifier

                            .fillMaxWidth()

                            .padding(15.dp),


                    horizontalArrangement =
                        Arrangement.SpaceBetween


                ){





                    Column {



                        Text(

                            "${item.day} ${item.startTime} ~ ${item.endTime}"

                        )



                        Text(

                            item.subject

                        )



                    }








                    Button(

                        onClick = {



                            deleteTimetable(

                                database,

                                student.uid,

                                item

                            ){

                                loadTimetable()

                            }



                        }



                    ){



                        Text("삭제")



                    }





                }





            }





        }





    }



}









private fun syncStudentScheduleToTeacherTimetable(
    database: FirebaseDatabase,
    studentSchedule: Timetable
) {
    if (studentSchedule.teacherUid.isBlank()) return

    val teacherSchedules = database.getReference("teacherTimetable").child(studentSchedule.teacherUid)
    teacherSchedules.get().addOnSuccessListener { snapshot ->
        val alreadyRegistered = snapshot.children.any { child ->
            child.getValue(Timetable::class.java)?.let { schedule ->
                schedule.day == studentSchedule.day &&
                    schedule.grade == studentSchedule.grade &&
                    schedule.className == studentSchedule.className &&
                    schedule.subject == studentSchedule.subject &&
                    schedule.startTime == studentSchedule.startTime &&
                    schedule.endTime == studentSchedule.endTime
            } == true
        }
        if (!alreadyRegistered) {
            val ref = teacherSchedules.push()
            ref.setValue(studentSchedule.copy(id = ref.key ?: ""))
        }
    }
}

fun deleteTimetable(

    database: FirebaseDatabase,

    uid:String,

    target:Timetable,

    refresh:()->Unit

){



    database

        .getReference("timetable")

        .child(uid)

        .get()

        .addOnSuccessListener { snapshot ->





            for(child in snapshot.children){





                val data =

                    child.getValue(
                        Timetable::class.java
                    )







                if(

                    data != null

                    &&

                    data.id == target.id

                ){





                    child.ref

                        .removeValue()

                        .addOnSuccessListener {



                            refresh()



                        }





                    return@addOnSuccessListener



                }





            }





        }





}
