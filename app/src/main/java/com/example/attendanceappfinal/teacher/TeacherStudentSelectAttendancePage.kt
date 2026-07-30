package com.example.attendanceappfinal.teacher

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.Notification
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.repository.AttendanceRepository
import com.example.attendanceappfinal.repository.NotificationRepository
import com.example.attendanceappfinal.ui.StudentTypeBadge
import com.google.firebase.database.FirebaseDatabase


@Composable
fun TeacherStudentSelectAttendancePage(

    onBack: () -> Unit = {}

) {

    BackHandler {
        onBack()
    }


    val database =
        FirebaseDatabase.getInstance()


    var students by remember {
        mutableStateOf(emptyList<User>())
    }


    var loading by remember {
        mutableStateOf(true)
    }


    var message by remember {
        mutableStateOf("")
    }


    val subjects =
        listOf(
            "수학",
            "영어",
            "국어",
            "과학"
        )


    var selectedSubject by remember {
        mutableStateOf("")
    }


    var dropdownOpen by remember {
        mutableStateOf(false)
    }



    val attendanceMap =
        remember {
            mutableStateMapOf<String,String>()
        }



    LaunchedEffect(Unit) {


        database
            .getReference("users")
            .get()
            .addOnSuccessListener { snapshot ->


                val list =
                    mutableListOf<User>()


                snapshot.children.forEach { child ->


                    val user =
                        child.getValue(User::class.java)
                            ?.copy(
                                uid = child.key ?: ""
                            )


                    if(
                        user != null &&
                        user.role == "student"
                    ){

                        list.add(user)

                        attendanceMap[user.uid] =
                            "출석"
                    }

                }


                database.getReference("preStudents")
                    .get()
                    .addOnSuccessListener { preSnapshot ->
                        preSnapshot.children.forEach { child ->
                            val pre = child.getValue(
                                com.example.attendanceappfinal.model.PreStudent::class.java
                            ) ?: return@forEach
                            val preId = child.key ?: return@forEach
                            val preUser = User(
                                uid = "pre_$preId",
                                name = pre.name,
                                phone = pre.phone,
                                role = "student",
                                grade = pre.grade,
                                className = "가입예정",
                                nfcId = pre.nfcTag,
                                isPreStudent = true,
                                preStudentId = preId
                            )
                            list.add(preUser)
                            attendanceMap[preUser.uid] = "출석"
                        }

                        database.getReference("unregisteredStudents")
                            .get()
                            .addOnSuccessListener { unSnapshot ->
                                unSnapshot.children.forEach { child ->
                                    val un = child.getValue(
                                        com.example.attendanceappfinal.model.UnregisteredStudent::class.java
                                    ) ?: return@forEach
                                    val unId = child.key ?: return@forEach
                                    val unUser = User(
                                        uid = "un_$unId",
                                        name = un.name,
                                        phone = un.phone,
                                        role = "student",
                                        grade = un.grade,
                                        className = un.className.ifBlank { "미가입" },
                                        nfcId = un.nfcTag,
                                        isUnregisteredStudent = true,
                                        unregisteredStudentId = unId
                                    )
                                    list.add(unUser)
                                    attendanceMap[unUser.uid] = "출석"
                                }
                                students = list
                                loading = false
                            }
                            .addOnFailureListener {
                                students = list
                                loading = false
                            }
                    }
                    .addOnFailureListener {
                        students = list
                        loading = false
                    }

            }
            .addOnFailureListener {


                message =
                    "학생 불러오기 실패 : ${it.message}"


                loading =
                    false

            }


    }



    fun saveAttendance(){


        if(selectedSubject.isBlank()){


            message =
                "과목을 선택해주세요"


            return

        }


        students.forEach { student ->


            val attendance =
                Attendance(

                    studentUid =
                        student.uid,

                    studentName =
                        student.name,

                    subject =
                        selectedSubject,

                    teacher =
                        "선생님",

                    date =
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date()),

                    time =
                        java.text.SimpleDateFormat(
                            "HH:mm",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date()),

                    status =
                        attendanceMap[student.uid]
                            ?: "출석",

                    timestamp =
                        System.currentTimeMillis()

                )


            AttendanceRepository.saveAttendance(

                attendance,

                onSuccess = {


                    saveLogAndNotification(

                        database,

                        student,

                        attendance

                    )


                },

                onFail = {

                    Log.e(
                        "ATTENDANCE",
                        it
                    )

                }

            )


        }


        message =
            "전체 출결 저장 완료"


    }



    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp)

    ){


        Text(

            "📋 오늘 출석 관리",

            style =
                MaterialTheme.typography.headlineMedium

        )


        Spacer(
            Modifier.height(20.dp)
        )



        Card(

            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(18.dp)

        ){

            Column(

                modifier =
                    Modifier.padding(16.dp)

            ){


                Text(
                    "과목 선택",
                    style =
                        MaterialTheme.typography.titleMedium
                )


                Spacer(
                    Modifier.height(10.dp)
                )


                Button(

                    modifier =
                        Modifier.fillMaxWidth(),

                    onClick = {

                        dropdownOpen =
                            true

                    }

                ){

                    Text(

                        if(selectedSubject.isEmpty())

                            "과목 선택"

                        else

                            selectedSubject

                    )

                }


                DropdownMenu(

                    expanded =
                        dropdownOpen,

                    onDismissRequest = {

                        dropdownOpen =
                            false

                    }

                ){

                    subjects.forEach { subject ->


                        DropdownMenuItem(

                            text = {

                                Text(subject)

                            },

                            onClick = {

                                selectedSubject =
                                    subject

                                dropdownOpen =
                                    false

                            }

                        )


                    }

                }


            }

        }


        Spacer(
            Modifier.height(20.dp)
        )
        when {


            loading -> {


                CircularProgressIndicator()


            }



            students.isEmpty() -> {


                Text(
                    "등록된 학생이 없습니다."
                )


            }



            else -> {


                students.forEach { student ->



                    val currentStatus =

                        attendanceMap[student.uid]
                            ?: "출석"




                    Card(

                        modifier =
                            Modifier

                                .fillMaxWidth()

                                .padding(
                                    vertical = 6.dp
                                ),


                        shape =
                            RoundedCornerShape(18.dp),


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

                                "👤 ${student.name}",

                                style =
                                    MaterialTheme.typography.titleLarge

                            )

                            Spacer(Modifier.height(6.dp))

                            StudentTypeBadge(student)



                            Spacer(
                                Modifier.height(12.dp)
                            )





                            Row(

                                modifier =
                                    Modifier.fillMaxWidth(),


                                horizontalArrangement =
                                    Arrangement.SpaceBetween


                            ){



                                listOf(

                                    "출석",
                                    "지각",
                                    "결석"

                                )
                                    .forEach { status ->




                                        Button(

                                            onClick = {


                                                attendanceMap[student.uid] =
                                                    status


                                            },


                                            colors =
                                                ButtonDefaults.buttonColors(


                                                    containerColor =

                                                        if(currentStatus == status)

                                                            MaterialTheme.colorScheme.primary

                                                        else

                                                            MaterialTheme.colorScheme.surfaceVariant


                                                ),


                                            shape =
                                                RoundedCornerShape(12.dp)

                                        ){



                                            Text(status)



                                        }




                                    }



                            }




                            Spacer(
                                Modifier.height(10.dp)
                            )



                            AssistChip(

                                onClick = {},


                                label = {

                                    Text(
                                        "현재 상태 : $currentStatus"
                                    )

                                }


                            )



                        }


                    }




                }





                Spacer(
                    Modifier.height(25.dp)
                )






                Button(

                    modifier =
                        Modifier.fillMaxWidth(),


                    shape =
                        RoundedCornerShape(16.dp),


                    onClick = {


                        saveAttendance()


                    }


                ){



                    Text(

                        "💾 전체 출결 저장"

                    )


                }




            }


        }






        Spacer(
            Modifier.height(20.dp)
        )





        if(message.isNotBlank()){


            Card(

                modifier =
                    Modifier.fillMaxWidth(),


                shape =
                    RoundedCornerShape(15.dp)


            ){


                Text(

                    message,

                    modifier =
                        Modifier.padding(15.dp)

                )


            }


        }




    }


}






private fun saveLogAndNotification(

    database: FirebaseDatabase,

    student: User,

    attendance: Attendance

){



    database

        .getReference("attendance_logs")

        .push()

        .setValue(

            mapOf(

                "studentUid" to student.uid,

                "studentName" to student.name,

                "subject" to attendance.subject,

                "beforeStatus" to "없음",

                "afterStatus" to attendance.status,

                "reason" to attendance.reason,

                "time" to attendance.date

            )

        )





    NotificationRepository.saveNotification(


        Notification(


            studentUid =
                student.uid,


            title =
                "출결 등록 알림",



            message =

                """
                과목 : ${attendance.subject}
                날짜 : ${attendance.date}
                상태 : ${attendance.status}
                """.trimIndent(),



            timestamp =
                System.currentTimeMillis()


        ),



        onSuccess = {


            Log.d(

                "ATTENDANCE",

                "알림 저장 성공"

            )


        },



        onFail = {


            Log.e(

                "ATTENDANCE",

                it

            )


        }


    )



}
