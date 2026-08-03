package com.example.attendanceappfinal.admin


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.AttendanceLog
import com.example.attendanceappfinal.repository.AttendanceRepository
import com.google.firebase.database.FirebaseDatabase



@Composable
fun AdminAttendanceLogPage(

    onBack: () -> Unit

){


    BackHandler {

        onBack()

    }



    val logs by AttendanceRepository.getAttendanceLogFlow()

        .collectAsState(

            initial = emptyList()

        )



    var searchText by remember {

        mutableStateOf("")

    }



    var selectedFilter by remember {

        mutableStateOf("전체")

    }



    val filters = listOf(

        "전체",
        "출석",
        "결석",
        "지각"

    )




    val database = FirebaseDatabase.getInstance()
    var showAutomaticLogDeleteConfirm by remember { mutableStateOf(false) }
    var deletingAutomaticLogs by remember { mutableStateOf(false) }
    var deleteMessage by remember { mutableStateOf("") }

    fun deleteAutomaticLogs() {
        deletingAutomaticLogs = true
        database.getReference("attendance_logs").get()
            .addOnSuccessListener { snapshot ->
                val updates = mutableMapOf<String, Any?>()
                snapshot.children.forEach { item ->
                    if (item.getValue(AttendanceLog::class.java)?.automatic == true) {
                        item.key?.let { updates[it] = null }
                    }
                }
                if (updates.isEmpty()) {
                    deletingAutomaticLogs = false
                    deleteMessage = "삭제할 자동 결석 처리 내역이 없습니다."
                    return@addOnSuccessListener
                }
                database.getReference("attendance_logs").updateChildren(updates)
                    .addOnSuccessListener {
                        deletingAutomaticLogs = false
                        deleteMessage = "자동 결석 처리 내역 ${updates.size}건을 삭제했습니다."
                    }
                    .addOnFailureListener {
                        deletingAutomaticLogs = false
                        deleteMessage = "자동 결석 처리 내역을 삭제하지 못했습니다."
                    }
            }
            .addOnFailureListener {
                deletingAutomaticLogs = false
                deleteMessage = "자동 결석 처리 내역을 불러오지 못했습니다."
            }
    }

    if (showAutomaticLogDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!deletingAutomaticLogs) showAutomaticLogDeleteConfirm = false },
            title = { Text("자동 결석 처리 내역 삭제") },
            text = { Text("자동 결석 처리 목록을 모두 삭제할까요? 실제 학생 출결 기록은 삭제되지 않습니다.") },
            confirmButton = {
                TextButton(
                    enabled = !deletingAutomaticLogs,
                    onClick = {
                        showAutomaticLogDeleteConfirm = false
                        deleteAutomaticLogs()
                    }
                ) { Text("전체 삭제") }
            },
            dismissButton = {
                TextButton(
                    enabled = !deletingAutomaticLogs,
                    onClick = { showAutomaticLogDeleteConfirm = false }
                ) { Text("취소") }
            }
        )
    }

    val filteredLogs =

        logs

            .filter { log ->



                val searchMatch =


                    searchText.isBlank()

                            ||

                            log.studentName.contains(

                                searchText,

                                ignoreCase = true

                            )

                            ||

                            log.subject.contains(

                                searchText,

                                ignoreCase = true

                            )

                            ||

                            log.reason.contains(

                                searchText,

                                ignoreCase = true

                            )




                val statusMatch =

                    when(selectedFilter){


                        "전체" -> true


                        else ->

                            log.afterStatus == selectedFilter


                    }



                searchMatch && statusMatch



            }

            .reversed()





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





        Card(

            modifier = Modifier

                .fillMaxWidth()

        ){


            Column(

                modifier =
                    Modifier.padding(18.dp)

            ){



                Text(

                    "출결 수정 기록",

                    style =
                        MaterialTheme.typography.titleLarge

                )



                Spacer(

                    Modifier.height(5.dp)

                )



                Text(

                    "관리자가 수정한 출결 내역 확인",

                    style =
                        MaterialTheme.typography.bodyMedium

                )



            }


        }




        Spacer(

            Modifier.height(20.dp)

        )






        OutlinedTextField(

            modifier =
                Modifier.fillMaxWidth(),


            value = searchText,


            onValueChange = {

                searchText = it

            },


            label = {

                Text(

                    "학생명 / 과목 / 사유 검색"

                )

            }


        )





        Spacer(

            Modifier.height(15.dp)

        )






        Row(

            modifier =
                Modifier.fillMaxWidth(),


            horizontalArrangement =
                Arrangement.spacedBy(6.dp)

        ){



            filters.forEach { filter ->



                FilterChip(

                    selected =
                        selectedFilter == filter,


                    onClick = {

                        selectedFilter = filter

                    },


                    label = {

                        Text(filter)

                    }


                )



            }



        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !deletingAutomaticLogs,
            onClick = { showAutomaticLogDeleteConfirm = true }
        ) {
            Text(if (deletingAutomaticLogs) "삭제 중..." else "자동 결석 처리 내역 전체 삭제")
        }

        Text(
            "실제 출결 기록은 유지하고 자동 결석 처리 목록만 삭제합니다.",
            style = MaterialTheme.typography.bodySmall
        )

        if (deleteMessage.isNotBlank()) Text(deleteMessage)






        Spacer(

            Modifier.height(15.dp)

        )






        Text(

            "검색 결과 ${filteredLogs.size}건",

            style =
                MaterialTheme.typography.titleMedium

        )



        Spacer(

            Modifier.height(10.dp)

        )
        if(filteredLogs.isEmpty()){



            Card(

                modifier =
                    Modifier.fillMaxWidth()

            ){



                Text(

                    "출결 수정 기록이 없습니다.",

                    modifier =
                        Modifier.padding(20.dp)

                )



            }



        }else{



            filteredLogs.forEach { log ->



                AttendanceLogCard(

                    log = log

                )



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


            Text(

                "뒤로가기"

            )


        }





    }



}









@Composable
fun AttendanceLogCard(

    log: AttendanceLog

){



    Card(

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 6.dp),


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

                log.studentName,

                style =
                    MaterialTheme.typography.titleLarge

            )





            Spacer(

                Modifier.height(8.dp)

            )





            Text(

                "📚 과목 : ${log.subject}"

            )





            Spacer(

                Modifier.height(8.dp)

            )





            Row{



                Text(

                    "변경 : "

                )



                Text(

                    log.beforeStatus,

                    color = Color.Gray

                )



                Text(

                    "  →  "

                )



                Text(

                    log.afterStatus,

                    color = MaterialTheme.colorScheme.primary

                )



            }







            if(log.reason.isNotBlank()){



                Spacer(

                    Modifier.height(8.dp)

                )



                Text(

                    "📝 사유 : ${log.reason}"

                )



            }







            Spacer(

                Modifier.height(8.dp)

            )






            Text(

                "🕒 수정 시간 : ${log.time}"

            )





        }



    }



}
