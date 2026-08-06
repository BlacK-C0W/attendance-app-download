package com.example.attendanceappfinal.teacher

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.AttendanceLog
import com.example.attendanceappfinal.model.Notification
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.repository.NotificationRepository
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeacherStudentDetailPage(
    student: User
) {

    val database =
        FirebaseDatabase.getInstance()

    var attendanceList by remember {
        mutableStateOf(emptyList<Attendance>())
    }

    var grade by remember {
        mutableStateOf(student.grade)
    }

    var className by remember {
        mutableStateOf(student.className)
    }

    var phone by remember {
        mutableStateOf(student.phone)
    }

    var gradeOpen by remember {
        mutableStateOf(false)
    }

    var classOpen by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf("")
    }

    var selectedSubject by remember {
        mutableStateOf("")
    }

    var subjectOpen by remember {
        mutableStateOf(false)
    }

    var selectedStatus by remember {
        mutableStateOf("")
    }

    var reason by remember {
        mutableStateOf("")
    }


    val grades =
        listOf(
            "초5",
            "초6",
            "중1",
            "중2",
            "중3",
            "고1",
            "고2",
            "고3"
        )


    val classes =
        listOf(
            "A반",
            "B반",
            "C반",
            "D반"
        )


    fun studentId(): String {

        return when {

            student.isPreStudent ->
                if(student.preStudentId.isNotBlank())
                    student.preStudentId
                else
                    student.uid.removePrefix("pre_")


            student.isUnregisteredStudent ->
                if(student.unregisteredStudentId.isNotBlank())
                    student.unregisteredStudentId
                else
                    student.uid.removePrefix("un_")


            else ->
                student.uid

        }

    }


    fun getStudentRoot() =
        when {

            student.isPreStudent ->

                database
                    .getReference("preStudents")
                    .child(studentId())


            student.isUnregisteredStudent ->

                database
                    .getReference("unregisteredStudents")
                    .child(studentId())


            else ->

                database
                    .getReference("users")
                    .child(studentId())

        }



    fun getAttendanceRoot() =
        when {

            student.isPreStudent ->

                database
                    .getReference("preAttendance")
                    .child(studentId())


            student.isUnregisteredStudent ->

                database
                    .getReference("unregisteredAttendance")
                    .child(studentId())


            else ->

                database
                    .getReference("attendance")
                    .child(studentId())

        }



    fun load(){

        getAttendanceRoot()
            .get()
            .addOnSuccessListener { snapshot ->


                val list =
                    mutableListOf<Attendance>()


                snapshot.children.forEach { child ->


                    child.getValue(
                        Attendance::class.java
                    )
                        ?.copy(
                            id = child.key ?: ""
                        )
                        ?.let {

                            list.add(it)

                        }

                }


                attendanceList =
                    list.sortedByDescending {
                        it.timestamp
                    }

            }

    }
    fun saveStudentInfo(){

        getStudentRoot()

            .updateChildren(

                mapOf(

                    "grade" to grade,

                    "className" to className,

                    "phone" to phone.trim()

                )

            )

            .addOnSuccessListener {

                message =
                    "학생 정보 저장 완료"

            }

            .addOnFailureListener {

                message =
                    "저장 실패 : ${it.message}"

            }

    }




    fun saveNotification(

        attendance: Attendance

    ){

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
                    사유 : ${attendance.reason.ifBlank { "없음" }}
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




    fun saveLog(

        attendance: Attendance

    ){

        database

            .getReference("attendance_logs")

            .push()

            .setValue(

                AttendanceLog(

                    studentUid =
                        student.uid,

                    studentName =
                        student.name,

                    subject =
                        attendance.subject,

                    beforeStatus =
                        "없음",

                    afterStatus =
                        attendance.status,

                    reason =
                        attendance.reason,

                    time =
                        attendance.date

                )

            )

    }





    LaunchedEffect(student.uid){

        load()

    }





    fun saveAttendance(){


        if(selectedSubject.isBlank()){

            message =
                "과목을 선택하세요"

            return

        }


        if(selectedStatus.isBlank()){

            message =
                "출결 상태를 선택하세요"

            return

        }



        val now =
            Date()



        val attendance = Attendance(

            studentUid =
                student.uid,

            studentName =
                student.name,

            subject =
                selectedSubject,

            teacher =
                "선생님",

            date =
                SimpleDateFormat(

                    "yyyy-MM-dd",

                    Locale.getDefault()

                ).format(now),


            time =
                SimpleDateFormat(

                    "HH:mm",

                    Locale.getDefault()

                ).format(now),


            status =
                selectedStatus,


            reason =
                reason,


            timestamp =
                System.currentTimeMillis()

        )




        val ref =

            getAttendanceRoot()

                .push()



        ref.setValue(

            attendance.copy(

                id =
                    ref.key ?: ""

            )

        )

            .addOnSuccessListener {


                saveNotification(

                    attendance

                )


                saveLog(

                    attendance

                )


                message =
                    "출결 저장 완료"



                selectedSubject = ""

                selectedStatus = ""

                reason = ""



                load()


            }



    }
    Column(

        modifier = Modifier

            .fillMaxSize()

            .verticalScroll(

                rememberScrollState()

            )

            .padding(20.dp)

    ){



        Text(

            "${student.name} 학생 관리",

            style =
                MaterialTheme.typography.headlineMedium

        )



        Spacer(
            Modifier.height(20.dp)
        )




        Card(

            modifier =
                Modifier.fillMaxWidth()

        ){

            Column(

                modifier =
                    Modifier.padding(15.dp)

            ){

                Text(

                    "학생 정보",

                    style =
                        MaterialTheme.typography.titleLarge

                )


                Spacer(
                    Modifier.height(10.dp)
                )



                Text(
                    "이름 : ${student.name}"
                )

                Text(
                    "전화번호 : ${student.phone.ifBlank { "미등록" }}"
                )



                Text(

                    when {

                        student.isPreStudent ->

                            "상태 : 앱 가입 예정 학생"


                        student.isUnregisteredStudent ->

                            "상태 : 미가입 학생 (NFC/수동 출결)"


                        else ->

                            "상태 : 앱 가입 완료"

                    }

                )



                Spacer(
                    Modifier.height(10.dp)
                )


                OutlinedTextField(

                    modifier = Modifier.fillMaxWidth(),

                    value = phone,

                    onValueChange = {
                        phone = it
                    },

                    label = {
                        Text("학생 전화번호")
                    },

                    singleLine = true,

                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    )

                )


                Spacer(
                    Modifier.height(10.dp)
                )




                Button(

                    modifier =
                        Modifier.fillMaxWidth(),

                    onClick = {

                        gradeOpen = true

                    }

                ){

                    Text(

                        if(grade.isBlank())

                            "학년 선택"

                        else

                            grade

                    )

                }




                DropdownMenu(

                    expanded =
                        gradeOpen,

                    onDismissRequest = {

                        gradeOpen = false

                    }

                ){

                    grades.forEach { item ->


                        DropdownMenuItem(

                            text = {

                                Text(item)

                            },

                            onClick = {

                                grade = item

                                gradeOpen = false

                            }

                        )

                    }

                }




                Spacer(
                    Modifier.height(10.dp)
                )





                Button(

                    modifier =
                        Modifier.fillMaxWidth(),

                    onClick = {

                        classOpen = true

                    }

                ){

                    Text(

                        if(className.isBlank())

                            "반 선택"

                        else

                            className

                    )

                }





                DropdownMenu(

                    expanded =
                        classOpen,

                    onDismissRequest = {

                        classOpen = false

                    }

                ){

                    classes.forEach { item ->


                        DropdownMenuItem(

                            text = {

                                Text(item)

                            },

                            onClick = {

                                className = item

                                classOpen = false

                            }

                        )


                    }

                }




                Spacer(
                    Modifier.height(10.dp)
                )




                Button(

                    modifier =
                        Modifier.fillMaxWidth(),

                    onClick = {

                        saveStudentInfo()

                    }

                ){

                    Text(
                        "학생 정보 저장"
                    )

                }


            }

        }




        Spacer(
            Modifier.height(25.dp)
        )




        Text(

            "출결 등록",

            style =
                MaterialTheme.typography.titleLarge

        )




        Spacer(
            Modifier.height(10.dp)
        )





        Button(

            modifier =
                Modifier.fillMaxWidth(),

            onClick = {

                subjectOpen = true

            }

        ){

            Text(

                if(selectedSubject.isBlank())

                    "과목 선택"

                else

                    selectedSubject

            )

        }





        DropdownMenu(

            expanded =
                subjectOpen,

            onDismissRequest = {

                subjectOpen = false

            }

        ){

            listOf(

                "수학",
                "영어",
                "국어",
                "과학",
                "자습"

            ).forEach { item ->


                DropdownMenuItem(

                    text = {

                        Text(item)

                    },

                    onClick = {

                        selectedSubject = item

                        subjectOpen = false

                    }

                )

            }

        }





        Spacer(
            Modifier.height(10.dp)
        )




        OutlinedTextField(

            modifier =
                Modifier.fillMaxWidth(),

            value =
                reason,

            onValueChange = {

                reason = it

            },

            label = {

                Text("사유")

            }

        )





        Spacer(
            Modifier.height(15.dp)
        )





        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceEvenly

        ){

            listOf(

                "출석",
                "지각",
                "결석"

            ).forEach { status ->


                Button(

                    onClick = {

                        selectedStatus = status

                    }

                ){

                    Text(

                        if(selectedStatus == status)

                            "✓ $status"

                        else

                            status

                    )

                }

            }

        }





        Spacer(
            Modifier.height(15.dp)
        )





        Button(

            modifier =
                Modifier.fillMaxWidth(),

            onClick = {

                saveAttendance()

            }

        ){

            Text(
                "출결 저장"
            )

        }





        if(message.isNotBlank()){


            Spacer(
                Modifier.height(15.dp)
            )


            Text(message)

        }





        Spacer(
            Modifier.height(25.dp)
        )





        Text(

            "출결 기록",

            style =
                MaterialTheme.typography.titleLarge

        )





        Spacer(
            Modifier.height(10.dp)
        )





        attendanceList.forEach { item ->


            Card(

                modifier =
                    Modifier

                        .fillMaxWidth()

                        .padding(vertical = 5.dp)

            ){

                Column(

                    modifier =
                        Modifier.padding(15.dp)

                ){


                    Text(
                        "과목 : ${item.subject}"
                    )


                    Text(
                        "날짜 : ${item.date}"
                    )


                    Text(
                        "상태 : ${item.status}"
                    )


                    Text(
                        "시간 : ${item.time}"
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
