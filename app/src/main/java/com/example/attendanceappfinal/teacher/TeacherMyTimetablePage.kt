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
import com.example.attendanceappfinal.model.Timetable
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalTime
import java.time.format.DateTimeFormatter



@Composable
fun TeacherMyTimetablePage(

    teacherUid:String,

    teacherName:String,

    onBack:()->Unit = {},

    onClassOpen:(Timetable)->Unit = {}

){



    BackHandler {

        onBack()

    }



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



    val defaultClasses =
        listOf(
            "A반",
            "B반",
            "C반",
            "D반"
        )



    val defaultSubjects =
        listOf(
            "수학",
            "영어",
            "과학",
            "자습"
        )



    var selectedDay by remember {
        mutableStateOf("월")
    }


    var selectedClass by remember {
        mutableStateOf("A반")
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



    var list by remember {
        mutableStateOf(emptyList<Timetable>())
    }


    var message by remember {
        mutableStateOf("")
    }

    var classes by remember { mutableStateOf(defaultClasses) }

    var subjects by remember { mutableStateOf(defaultSubjects) }






    fun load(){


        database

            .getReference("teacherTimetable")

            .child(teacherUid)

            .get()

            .addOnSuccessListener { snapshot ->


                val result =
                    mutableListOf<Timetable>()


                snapshot.children.forEach { child ->


                    child.getValue(
                        Timetable::class.java
                    )?.let {


                        result.add(

                            it.copy(

                                id =
                                    child.key ?: ""

                            )

                        )


                    }


                }


                list =
                    result.sortedBy {

                        it.startTime

                    }


            }


    }







    LaunchedEffect(teacherUid){

        load()

        database.getReference("academySettings").get().addOnSuccessListener { snapshot ->
            snapshot.child("classes").children.mapNotNull { it.getValue(String::class.java) }
                .takeIf { it.isNotEmpty() }?.let { classes = it }
            snapshot.child("subjects").children.mapNotNull { it.getValue(String::class.java) }
                .takeIf { it.isNotEmpty() }?.let { subjects = it }
        }

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

            "📅 내 시간표",

            style =
                MaterialTheme.typography.headlineMedium

        )



        Spacer(
            Modifier.height(15.dp)
        )



        Button(

            modifier =
                Modifier.fillMaxWidth(),

            onClick = {

                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val now = LocalTime.now()
                val current = list.firstOrNull { item ->
                    try {
                        val start = LocalTime.parse(item.startTime, formatter)
                        val end = LocalTime.parse(item.endTime, formatter)
                        !now.isBefore(start) && !now.isAfter(end)
                    } catch (_: Exception) {
                        false
                    }
                }
                if (current != null) {
                    onClassOpen(current)
                } else {
                    message = "현재 진행 중인 수업이 없습니다. 아래 시간표에서 수업을 선택할 수 있습니다."
                }

            }

        ){

            Text(
                "현재 수업 확인"
            )

        }



        Spacer(
            Modifier.height(20.dp)
        )

        TeacherHolidayCalendar()

        Spacer(
            Modifier.height(20.dp)
        )





        Text("요일")



        days.chunked(3).forEach { weekRow ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ){
                weekRow.forEach { day ->
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { selectedDay = day },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDay == day) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (selectedDay == day) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    ){
                        Text(day)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }







        Spacer(
            Modifier.height(15.dp)
        )






        Text("반")



        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ){
            classes.forEach { item ->
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { selectedClass = item },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedClass == item) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (selectedClass == item) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                ){
                    Text(item)
                }
            }
        }






        Spacer(
            Modifier.height(15.dp)
        )






        Box {


            OutlinedButton(

                modifier =
                    Modifier.fillMaxWidth(),

                onClick = {

                    subjectExpanded = true

                }

            ){


                Text(

                    if(subject.isBlank())

                        "과목 선택"

                    else

                        subject

                )


            }





            DropdownMenu(

                expanded =
                    subjectExpanded,

                onDismissRequest = {

                    subjectExpanded=false

                }

            ){


                subjects.forEach { item ->


                    DropdownMenuItem(

                        text = {

                            Text(item)

                        },


                        onClick = {

                            subject=item

                            subjectExpanded=false

                        }

                    )


                }


            }


        }







        Spacer(
            Modifier.height(15.dp)
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


                    endTime =

                        LocalTime.parse(
                            it,
                            formatter
                        )

                            .plusMinutes(90)

                            .format(formatter)



                }catch(e:Exception){


                    endTime=""

                }


            },

            label = {

                Text("시작 시간")

            },

            modifier =
                Modifier.fillMaxWidth()

        )







        Spacer(
            Modifier.height(10.dp)
        )







        OutlinedTextField(

            value=endTime,

            onValueChange={},

            enabled=false,

            label={

                Text("종료 시간")

            },

            modifier =
                Modifier.fillMaxWidth()

        )







        Spacer(
            Modifier.height(20.dp)
        )






        Button(

            modifier =
                Modifier.fillMaxWidth(),

            onClick = {



                val ref =

                    database

                        .getReference("teacherTimetable")

                        .child(teacherUid)

                        .push()



                val data =

                    Timetable(

                        id =
                            ref.key ?: "",


                        day =
                            selectedDay,


                        subject =
                            subject,


                        teacher =
                            teacherName,


                        teacherUid =
                            teacherUid,


                        className =
                            selectedClass,


                        startTime =
                            startTime,


                        endTime =
                            endTime

                    )




                ref.setValue(data)

                    .addOnSuccessListener {


                        message =
                            "저장 완료"


                        subject=""


                        startTime=""


                        endTime=""


                        load()


                    }



            }

        ){


            Text("시간표 저장")


        }






        if(message.isNotBlank()){


            Text(message)


        }






        Spacer(
            Modifier.height(25.dp)
        )






        Text(

            "등록된 수업",

            style =
                MaterialTheme.typography.titleLarge

        )






        list.forEach { item ->



            Card(

                modifier =
                    Modifier

                        .fillMaxWidth()

                        .padding(5.dp)

            ){



                Column(

                    modifier =
                        Modifier.padding(15.dp)

                ){



                    Text(

                        "${item.day} ${item.startTime}~${item.endTime}"

                    )



                    Text(

                        "${item.className} ${item.subject}"

                    )

                    Spacer(Modifier.height(10.dp))

                    Button(

                        modifier = Modifier.fillMaxWidth(),

                        onClick = { onClassOpen(item) }

                    ){

                        Text("학생 목록 · 출결 관리")

                    }




                    Button(

                        onClick = {


                            database

                                .getReference("teacherTimetable")

                                .child(teacherUid)

                                .child(item.id)

                                .removeValue()

                                .addOnSuccessListener {


                                    load()


                                }



                        }

                    ){

                        Text("삭제")

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

            onClick = {

                onBack()

            }

        ){


            Text("뒤로가기")


        }




    }



}
