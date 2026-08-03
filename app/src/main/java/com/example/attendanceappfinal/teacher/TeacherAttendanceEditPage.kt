package com.example.attendanceappfinal.teacher


import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.model.AttendanceLog
import com.example.attendanceappfinal.model.Notification
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.repository.NotificationRepository
import com.example.attendanceappfinal.repository.attendanceStoragePath
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*



@Composable
fun TeacherAttendanceEditPage(

    onBack: () -> Unit = {}

){


    BackHandler {

        onBack()

    }



    val database =
        FirebaseDatabase.getInstance()





    var attendanceList by remember {

        mutableStateOf(
            emptyList<Attendance>()
        )

    }



    var selectedAttendance by remember {

        mutableStateOf<Attendance?>(null)

    }



    var searchName by remember {

        mutableStateOf("")

    }



    var searchDate by remember {

        mutableStateOf("")

    }



    var subject by remember {

        mutableStateOf("전체")

    }



    var newStatus by remember {

        mutableStateOf("출석")

    }



    var reason by remember {

        mutableStateOf("")

    }



    var message by remember {

        mutableStateOf("")

    }







    fun loadLegacy(){



        database

            .getReference("users")

            .get()

            .addOnSuccessListener { userSnap ->



                val names =
                    mutableMapOf<String,String>()





                userSnap.children.forEach { child ->



                    val user =

                        child.getValue(

                            User::class.java

                        )



                    if(user != null){



                        names[child.key ?: ""] =

                            user.name



                    }



                }







                database

                    .getReference("attendance")

                    .get()

                    .addOnSuccessListener { attendanceRoot ->




                        val list =

                            mutableListOf<Attendance>()





                        // attendance/{studentUid}/{attendanceId}

                        attendanceRoot.children.forEach { studentNode ->





                            val studentUid =

                                studentNode.key ?: ""







                            studentNode.children.forEach { item ->



                                try {



                                    val data =

                                        item.getValue(

                                            Attendance::class.java

                                        )





                                    if(data != null){



                                        list.add(



                                            data.copy(


                                                id =

                                                    item.key ?: "",



                                                studentUid =

                                                    if(data.studentUid.isBlank())

                                                        studentUid

                                                    else

                                                        data.studentUid,



                                                studentName =


                                                    if(data.studentName.isNotBlank())

                                                        data.studentName

                                                    else

                                                        names[studentUid]
                                                            ?: "이름없음"



                                            )



                                        )



                                    }



                                }catch(e:Exception){



                                    Log.e(

                                        "ATTENDANCE_LOAD",

                                        "출결 변환 실패 : ${item.key}"

                                    )



                                }



                            }





                        }





                        attendanceList =

                            list.sortedByDescending {


                                it.timestamp


                            }





                    }



            }



    }







    fun load() {
        val list = mutableListOf<Attendance>()

        fun appendRecords(rootSnapshot: com.google.firebase.database.DataSnapshot, prefix: String) {
            rootSnapshot.children.forEach { studentNode ->
                val storageId = studentNode.key ?: ""
                studentNode.children.forEach { item ->
                    item.getValue(Attendance::class.java)?.let { data ->
                        list.add(
                            data.copy(
                                id = item.key ?: "",
                                studentUid = data.studentUid.ifBlank { "$prefix$storageId" },
                                studentName = data.studentName.ifBlank { "이름 없음" }
                            )
                        )
                    }
                }
            }
        }

        fun finish() {
            attendanceList = list.sortedByDescending { it.timestamp }
            if (attendanceList.isEmpty()) message = "출결 기록이 없습니다."
        }

        database.getReference("attendance").get().addOnSuccessListener { registered ->
            appendRecords(registered, "")
            database.getReference("preAttendance").get().addOnSuccessListener { pre ->
                appendRecords(pre, "pre_")
                database.getReference("unregisteredAttendance").get().addOnSuccessListener { unregistered ->
                    appendRecords(unregistered, "un_")
                    finish()
                }.addOnFailureListener { finish() }
            }.addOnFailureListener { finish() }
        }.addOnFailureListener {
            message = "출결 기록을 불러오지 못했습니다: ${it.message ?: "권한 또는 네트워크 오류"}"
            finish()
        }
    }

    LaunchedEffect(Unit){


        load()


    }
    val subjects =

        listOf("전체") +

                attendanceList

                    .map {

                        it.subject

                    }

                    .distinct()





    val filtered =

        attendanceList.filter {



            val nameCheck =

                searchName.isBlank()
                        ||
                        it.studentName.contains(searchName)



            val dateCheck =

                searchDate.isBlank()
                        ||
                        it.date.contains(searchDate)



            val subjectCheck =

                subject == "전체"
                        ||
                        it.subject == subject




            nameCheck &&
                    dateCheck &&
                    subjectCheck



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

            "📝 출결 수정 관리",

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

                    "검색",

                    style =

                        MaterialTheme.typography.titleLarge

                )





                Spacer(

                    Modifier.height(10.dp)

                )





                OutlinedTextField(

                    modifier =

                        Modifier.fillMaxWidth(),


                    value =

                        searchName,


                    onValueChange = {

                        searchName = it

                    },


                    label = {

                        Text("학생 이름")

                    }


                )






                Spacer(

                    Modifier.height(10.dp)

                )






                OutlinedTextField(

                    modifier =

                        Modifier.fillMaxWidth(),


                    value =

                        searchDate,


                    onValueChange = {

                        searchDate = it

                    },


                    label = {

                        Text("날짜 (예: 2026-07-29)")

                    }


                )



            }


        }






        Spacer(

            Modifier.height(15.dp)

        )







        Row(

            modifier =

                Modifier

                    .fillMaxWidth()

                    .horizontalScroll(

                        rememberScrollState()

                    )


        ){


            subjects.forEach { item ->



                Button(

                    modifier =

                        Modifier.padding(end = 8.dp),


                    onClick = {

                        subject = item

                    },


                    shape =

                        RoundedCornerShape(14.dp)

                ){


                    Text(item)


                }


            }


        }






        Spacer(

            Modifier.height(20.dp)

        )







        if(selectedAttendance == null){



            if(filtered.isEmpty()){


                Text(

                    "출결 기록이 없습니다."

                )


            }





            filtered.forEach { item ->




                Card(

                    modifier =

                        Modifier

                            .fillMaxWidth()

                            .padding(vertical = 6.dp),


                    shape =

                        RoundedCornerShape(18.dp)

                ){



                    Column(

                        modifier =

                            Modifier.padding(16.dp)

                    ){



                        Text(

                            "👤 ${item.studentName}",

                            style =

                                MaterialTheme.typography.titleLarge

                        )



                        Text(

                            "📅 ${item.date}"

                        )



                        Text(

                            "📚 ${item.subject}"

                        )



                        Text(

                            "상태 : ${item.status}"

                        )





                        Spacer(

                            Modifier.height(10.dp)

                        )





                        Button(

                            modifier =

                                Modifier.fillMaxWidth(),


                            onClick = {


                                selectedAttendance = item


                                newStatus = item.status


                                reason = item.reason


                            }


                        ){


                            Text("수정하기")


                        }



                    }



                }



            }





        }







        selectedAttendance?.let { item ->




            Card(

                modifier =

                    Modifier.fillMaxWidth(),


                shape =

                    RoundedCornerShape(20.dp)

            ){



                Column(

                    modifier =

                        Modifier.padding(18.dp)

                ){



                    Text(

                        "✏️ 출결 수정",

                        style =

                            MaterialTheme.typography.titleLarge

                    )





                    Spacer(

                        Modifier.height(15.dp)

                    )





                    Text("학생 : ${item.studentName}")

                    Text("과목 : ${item.subject}")

                    Text("날짜 : ${item.date}")







                    Spacer(

                        Modifier.height(15.dp)

                    )







                    listOf(

                        "출석",

                        "지각",

                        "결석"

                    ).forEach { status ->



                        Button(

                            modifier =

                                Modifier.fillMaxWidth()

                                    .padding(vertical = 3.dp),


                            onClick = {

                                newStatus = status

                            }


                        ){


                            Text(status)


                        }



                    }







                    OutlinedTextField(

                        modifier =

                            Modifier.fillMaxWidth(),


                        value = reason,


                        onValueChange = {

                            reason = it

                        },


                        label = {

                            Text("수정 사유")

                        }


                    )






                    Spacer(

                        Modifier.height(15.dp)

                    )






                    Button(

                        modifier =

                            Modifier.fillMaxWidth(),


                        onClick = {


                            updateAttendance(

                                database,

                                item,

                                newStatus,

                                reason

                            ){


                                message = "수정 완료"


                                selectedAttendance = null


                                load()


                            }


                        }


                    ){


                        Text("💾 저장")


                    }

                    Spacer(
                        Modifier.height(10.dp)
                    )


                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = {

                            deleteAttendance(
                                database,
                                item
                            ){

                                message = "삭제 완료"

                                selectedAttendance = null

                                load()

                            }

                        }
                    ){

                        Text("🗑 출결 삭제")

                    }



                    OutlinedButton(

                        modifier =

                            Modifier.fillMaxWidth(),


                        onClick = {


                            selectedAttendance = null


                        }


                    ){


                        Text("취소")


                    }



                }



            }




        }






        if(message.isNotBlank()){


            Spacer(

                Modifier.height(15.dp)

            )


            Text(message)


        }





    }



}









private fun updateAttendance(

    database: FirebaseDatabase,

    attendance: Attendance,

    status: String,

    reason: String,

    callback: () -> Unit

){

    val now =

        SimpleDateFormat(

            "yyyy-MM-dd HH:mm:ss",

            Locale.getDefault()

        ).format(Date())



    val storagePath = attendanceStoragePath(attendance.studentUid)

    database

        .getReference(storagePath.root)

        .child(storagePath.studentId)

        .child(attendance.id)

        .updateChildren(

            mapOf(

                "status" to status,

                "reason" to reason,

                "modified" to true,

                "modifiedTime" to now

            )

        )

        .addOnSuccessListener {


            val log = AttendanceLog(

                studentUid = attendance.studentUid,

                studentName = attendance.studentName,

                subject = if(attendance.subject.isBlank()) {
                    "미정"
                } else {
                    attendance.subject
                },

                beforeStatus = attendance.status,

                afterStatus = status,

                reason = reason,

                time = now

            )




            database

                .getReference("attendance_logs")

                .push()

                .setValue(log)

                .addOnSuccessListener {


                    Log.d(

                        "ATTENDANCE_LOG",

                        "수정 기록 저장 성공"

                    )



                    NotificationRepository.saveNotification(


                        Notification(


                            studentUid = attendance.studentUid,


                            title = "출결 변경 알림",


                            message =

                                """
                    학생 : ${attendance.studentName}
                    과목 : ${attendance.subject}
                    날짜 : ${attendance.date}
                    변경 : ${attendance.status} → $status
                    사유 : ${reason.ifBlank { "없음" }}
                    """.trimIndent(),


                            timestamp = System.currentTimeMillis()


                        ),



                        onSuccess = {


                            Log.d(

                                "NOTIFICATION",

                                "알림 저장 성공"

                            )


                            callback()


                        },


                        onFail = {


                            Log.e(

                                "NOTIFICATION",

                                "알림 저장 실패 : $it"

                            )


                            callback()


                        }


                    )


                }


                .addOnFailureListener {


                    Log.e(

                        "ATTENDANCE_LOG",

                        "수정 기록 저장 실패 : ${it.message}"

                    )


                    callback()


                }

                .addOnFailureListener {


                    Log.e(

                        "ATTENDANCE_LOG",

                        "수정 기록 저장 실패 : ${it.message}"

                    )


                    callback()


                }




        }

        .addOnFailureListener {


            Log.e(

                "ATTENDANCE_EDIT",

                "출결 수정 실패 : ${it.message}"

            )


        }


}
private fun deleteAttendance(

    database: FirebaseDatabase,

    attendance: Attendance,

    callback: () -> Unit

){

    val storagePath = attendanceStoragePath(attendance.studentUid)

    database

        .getReference(storagePath.root)

        .child(storagePath.studentId)

        .child(attendance.id)

        .removeValue()

        .addOnSuccessListener {


            Log.d(

                "ATTENDANCE_DELETE",

                "출결 삭제 성공"

            )


            callback()


        }

        .addOnFailureListener {


            Log.e(

                "ATTENDANCE_DELETE",

                "출결 삭제 실패 : ${it.message}"

            )


        }


}
