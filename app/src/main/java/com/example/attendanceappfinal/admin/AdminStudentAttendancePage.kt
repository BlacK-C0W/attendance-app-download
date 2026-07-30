package com.example.attendanceappfinal.admin


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.model.PreStudent
import com.example.attendanceappfinal.model.UnregisteredStudent
import com.google.firebase.database.FirebaseDatabase
import java.time.YearMonth
import com.example.attendanceappfinal.model.Attendance
import com.example.attendanceappfinal.ui.StudentTypeBadge
import com.example.attendanceappfinal.util.shareAttendanceCsv


@Composable
fun AdminStudentAttendancePage(

    onBack: () -> Unit,

    onStudentClick: (User) -> Unit,

    initialGrade: String? = null,

    onGradeChange: (String?) -> Unit = {}

) {

    val database =
        FirebaseDatabase.getInstance()

    val context = LocalContext.current


    var students by remember {

        mutableStateOf(
            listOf<User>()
        )

    }

    var attendanceList by remember {

        mutableStateOf(
            emptyList<Attendance>()
        )

    }




    var selectedGrade by remember {

        mutableStateOf(initialGrade)

    }

    var gradeMenuExpanded by remember {

        mutableStateOf(false)

    }

    var statsMonth by remember { mutableStateOf(YearMonth.now()) }

    BackHandler {

        if(selectedGrade != null){

            selectedGrade = null

            onGradeChange(null)


        }else{

            onBack()

        }

    }

    fun loadStudents() {


        database

            .getReference("users")

            .get()

            .addOnSuccessListener { snapshot ->


                val result =
                    mutableListOf<User>()



                for (child in snapshot.children) {


                    val user =

                        child.getValue(
                            User::class.java
                        )
                            ?.copy(

                                uid =
                                    child.key ?: ""

                            )


                    if (
                        user != null &&
                        user.role == "student"
                    ) {

                        result.add(user)

                    }

                }


                database.getReference("preStudents").get().addOnSuccessListener { preSnapshot ->
                    preSnapshot.children.mapNotNull { child ->
                        child.getValue(PreStudent::class.java)?.copy(id = child.key ?: "")
                    }.forEach { pre ->
                        result.add(User(
                            uid = "pre_${pre.id}", name = pre.name, phone = pre.phone,
                            grade = pre.grade, className = pre.className, role = "student",
                            isPreStudent = true, preStudentId = pre.id
                        ))
                    }
                    database.getReference("unregisteredStudents").get().addOnSuccessListener { unregisteredSnapshot ->
                        unregisteredSnapshot.children.mapNotNull { child ->
                            child.getValue(UnregisteredStudent::class.java)?.copy(id = child.key ?: "")
                        }.forEach { unregistered ->
                            result.add(User(
                                uid = "un_${unregistered.id}", name = unregistered.name,
                                phone = unregistered.phone, grade = unregistered.grade,
                                className = unregistered.className, role = "student",
                                isUnregisteredStudent = true,
                                unregisteredStudentId = unregistered.id
                            ))
                        }
                        students = result
                    }.addOnFailureListener { students = result }
                }.addOnFailureListener { students = result }


            }


    }





    fun loadAttendance(){


        database

            .getReference("attendance")

            .get()

            .addOnSuccessListener { snapshot ->


                val result =
                    mutableListOf<Attendance>()


                snapshot.children.forEach { parent ->


                    parent.children.forEach { child ->


                        child.getValue(
                            Attendance::class.java
                        )?.let {

                            result.add(it)

                        }


                    }


                }


                database.getReference("preAttendance").get().addOnSuccessListener { preAttendance ->
                    preAttendance.children.forEach { parent ->
                        parent.children.forEach { child ->
                            child.getValue(Attendance::class.java)?.let { result.add(it) }
                        }
                    }
                    database.getReference("unregisteredAttendance").get().addOnSuccessListener { unregisteredAttendance ->
                        unregisteredAttendance.children.forEach { parent ->
                            parent.children.forEach { child ->
                                child.getValue(Attendance::class.java)?.let { result.add(it) }
                            }
                        }
                        attendanceList = result
                    }.addOnFailureListener { attendanceList = result }
                }.addOnFailureListener { attendanceList = result }


            }


    }



    LaunchedEffect(Unit) {

        loadStudents()

        loadAttendance()

    }

    val grades = listOf(

        "초5",
        "초6",
        "중1",
        "중2",
        "중3",
        "고1",
        "고2",
        "고3"

    )


    val gradeStudents =
        students.filter {

            it.grade == selectedGrade

        }

    val gradeAttendance =

        attendanceList.filter { attendance ->


            gradeStudents.any {

                it.uid == attendance.studentUid

            }


        }



    val gradePresent =

        gradeAttendance.count {

            it.status == "출석"

        }



    val gradeLate =

        gradeAttendance.count {

            it.status == "지각"

        }



    val gradeAbsent =

        gradeAttendance.count {

            it.status == "결석"

        }



    val monthStats = gradeStudents.groupBy { it.className.ifBlank { "반 미등록" } }
        .toSortedMap()
        .map { (className, classStudents) ->
            val studentIds = classStudents.map { it.uid }.toSet()
            val records = gradeAttendance.filter {
                it.studentUid in studentIds && it.date.startsWith(statsMonth.toString())
            }
            ClassMonthlyStat(
                className = className,
                studentCount = classStudents.size,
                present = records.count { it.status == "출석" },
                late = records.count { it.status == "지각" },
                absent = records.count { it.status == "결석" }
            )
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

    ) {


        Text(

            "학생별 출결 통계",

            style =
                MaterialTheme.typography.headlineMedium

        )

        Text(
            "학년별 현황과 학생별 출결 기록을 확인하세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )



        Spacer(
            Modifier.height(20.dp)
        )





        if (selectedGrade == null) {


            if (grades.isEmpty()) {

                Text(
                    "학생 없음"
                )

            } else {


                Text(
                    "학년 선택",
                    style =
                        MaterialTheme.typography.titleLarge
                )


                Spacer(
                    Modifier.height(10.dp)
                )


                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "선택한 학년의 학생별 출결 통계를 확인합니다.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { gradeMenuExpanded = true }
                            ) {
                                Text("학년 선택")
                            }
                            DropdownMenu(
                                expanded = gradeMenuExpanded,
                                onDismissRequest = { gradeMenuExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                grades.forEach { grade ->
                                    DropdownMenuItem(
                                        text = { Text(grade) },
                                        onClick = {
                                            selectedGrade = grade
                                            onGradeChange(grade)
                                            gradeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }


            }


        } else {


            Button(

                onClick = {

                    selectedGrade = null

                }

            ) {

                Text(
                    "학년 선택으로 돌아가기"
                )

            }



            Spacer(
                Modifier.height(15.dp)
            )



            Text(

                "${selectedGrade}학년 학생",

                style =
                    MaterialTheme.typography.titleLarge

            )
            Spacer(
                Modifier.height(10.dp)
            )


            Card(

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(18.dp),

                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )

            ){


                Column(

                    modifier =
                        Modifier.padding(16.dp)

                ){


                    Text(
                        "${selectedGrade} 통계",
                        style = MaterialTheme.typography.titleLarge
                    )


                    Spacer(
                        Modifier.height(8.dp)
                    )


                    Text(
                        "학생 수 : ${gradeStudents.size}명"
                    )


                    Text(
                        "출석 : ${gradePresent}회"
                    )


                    Text(
                        "지각 : ${gradeLate}회"
                    )


                    Text(
                        "결석 : ${gradeAbsent}회"
                    )


                }


            }

            Spacer(Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { statsMonth = statsMonth.minusMonths(1) }) { Text("‹") }
                        Text("${statsMonth.year}년 ${statsMonth.monthValue}월 반별 통계", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { statsMonth = statsMonth.plusMonths(1) }) { Text("›") }
                    }
                    monthStats.forEach { stat ->
                        Text("${stat.className} · ${stat.studentCount}명  |  출석 ${stat.present} · 지각 ${stat.late} · 결석 ${stat.absent}")
                    }
                    if (monthStats.isEmpty()) Text("등록된 반이 없습니다.")
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    shareAttendanceCsv(
                        context = context,
                        grade = selectedGrade ?: "학생",
                        students = gradeStudents,
                        attendance = gradeAttendance
                    )
                }
            ) {
                Text("CSV로 내보내기")
            }

            Spacer(
                Modifier.height(10.dp)
            )



            if (gradeStudents.isEmpty()) {


                Text(
                    "해당 학년 학생 없음"
                )


            } else {


                gradeStudents.forEach { student ->


                    Card(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(vertical = 6.dp),

                        shape = RoundedCornerShape(18.dp),

                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

                    ) {


                        Column(

                            modifier =
                                Modifier.padding(16.dp)

                        ) {


                            Text(
                                student.name.ifBlank { "이름 없음" },
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(6.dp))

                            StudentTypeBadge(student)


                            Spacer(
                                Modifier.height(10.dp)
                            )


                            Button(

                                modifier = Modifier.fillMaxWidth(),

                                shape = RoundedCornerShape(12.dp),

                                onClick = {

                                    onStudentClick(student)

                                }

                            ) {

                                Text(
                                    "출결 달력 보기"
                                )

                            }


                        }


                    }


                }


            }
        }
    }

    val selectedMonthAttendance = gradeAttendance.filter {
        it.date.startsWith(statsMonth.toString())
    }

    val classMonthlyStats = gradeStudents.groupBy { it.className.ifBlank { "반 미등록" } }
        .toSortedMap()
        .map { (className, classStudents) ->
            val ids = classStudents.map { it.uid }.toSet()
            val records = selectedMonthAttendance.filter { it.studentUid in ids }
            ClassMonthlyStat(
                className = className,
                studentCount = classStudents.size,
                present = records.count { it.status == "출석" },
                late = records.count { it.status == "지각" },
                absent = records.count { it.status == "결석" }
            )
        }
}

private data class ClassMonthlyStat(
    val className: String,
    val studentCount: Int,
    val present: Int,
    val late: Int,
    val absent: Int
)
