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
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter



@Composable
fun TeacherMyTimetablePage(

    teacherUid:String,

    teacherName:String,

    onBack:()->Unit = {},

    onClassOpen:(Timetable)->Unit = {}

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



    var selectedDays by remember {
        mutableStateOf(setOf("월"))
    }

    var selectedGrade by remember {
        mutableStateOf("")
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

    var editingTimetable by remember {
        mutableStateOf<Timetable?>(null)
    }

    var viewingDay by remember {
        mutableStateOf("전체")
    }

    var showingRegisteredLessons by remember {
        mutableStateOf(false)
    }

    BackHandler {
        if (showingRegisteredLessons) {
            showingRegisteredLessons = false
        } else {
            onBack()
        }
    }


    var currentCandidates by remember {
        mutableStateOf(emptyList<Timetable>())
    }

    var timetableLoaded by remember {
        mutableStateOf(false)
    }

    var currentSelectionTitle by remember {
        mutableStateOf("")
    }

    var currentSelectionDescription by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    var classes by remember { mutableStateOf(defaultClasses) }

    var subjects by remember { mutableStateOf(defaultSubjects) }

    var gradesByClass by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    var registeredStudents by remember { mutableStateOf(emptyList<User>()) }

    var importingStudentSchedules by remember { mutableStateOf(false) }






    fun load(){

        timetableLoaded = false


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

                timetableLoaded = true

            }

            .addOnFailureListener {

                timetableLoaded = true

                message = "등록된 수업을 불러오지 못했습니다: ${it.message ?: "권한 또는 네트워크 오류"}"

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

        database.getReference("users").get().addOnSuccessListener { snapshot ->
            val students = snapshot.children.mapNotNull { child ->
                child.getValue(User::class.java)?.copy(uid = child.key ?: "")
            }.filter { it.role == "student" && it.grade.isNotBlank() && it.className.isNotBlank() }
            registeredStudents = students
            gradesByClass = students.groupBy { it.className }
                .mapValues { (_, students) -> students.map { it.grade }.distinct().sorted() }
        }

    }

    fun importStudentSchedules(){
        if (registeredStudents.isEmpty()) {
            message = "가져올 가입 완료 학생이 없습니다."
            return
        }
        importingStudentSchedules = true
        var completed = 0
        var imported = 0
        var failed = false
        val total = registeredStudents.size
        registeredStudents.forEach { student ->
            database.getReference("timetable").child(student.uid).get()
                .addOnSuccessListener { snapshot ->
                    snapshot.children.mapNotNull { it.getValue(Timetable::class.java) }
                        .filter { it.teacherUid == teacherUid }
                        .forEach { schedule ->
                            val key = "legacy_${schedule.day}_${schedule.grade}_${schedule.className}_${schedule.subject}_${schedule.startTime}_${schedule.endTime}"
                                .replace(".", "_")
                            database.getReference("teacherTimetable").child(teacherUid).child(key)
                                .setValue(schedule.copy(id = key))
                            imported++
                        }
                }
                .addOnFailureListener {
                    failed = true
                }
                .addOnCompleteListener {
                    completed++
                    if (completed == total) {
                        importingStudentSchedules = false
                        message = if (imported > 0) {
                            "학생별 시간표 ${imported}건을 내 시간표에 반영했습니다."
                        } else if (failed) {
                            "학생별 시간표를 읽지 못했습니다. Firebase timetable 읽기 권한을 확인하세요."
                        } else {
                            "현재 선생님으로 등록된 학생별 시간표가 없습니다."
                        }
                        load()
                    }
                }
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

            if (showingRegisteredLessons) "📚 등록된 수업" else "📅 내 시간표",

            style =
                MaterialTheme.typography.headlineMedium

        )



        Spacer(
            Modifier.height(15.dp)
        )



        if (!showingRegisteredLessons) {

        Button(

            modifier =
                Modifier.fillMaxWidth(),

            onClick = {

                if (!timetableLoaded) {
                    message = "시간표를 불러오는 중입니다. 잠시 후 다시 눌러주세요."
                    return@Button
                }

                val today = teacherTodayDay()
                val todayLessons = list
                    .filter { isSameTeacherDay(it.day, today) }
                    .sortedBy { it.startTime }

                if (todayLessons.isEmpty()) {
                    message = "${today}요일에 등록된 수업이 없습니다. 내 시간표에서 수업을 먼저 등록하세요."
                } else {
                    currentSelectionTitle = "오늘 수업 선택"
                    currentSelectionDescription = "${today}요일에 등록된 수업 전체입니다. 출결을 관리할 수업을 선택하세요."
                    currentCandidates = todayLessons
                }
                return@Button

            }

        ){

            Text(
                "현재 수업 확인"
            )

        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !importingStudentSchedules,
            onClick = { importStudentSchedules() }
        ) {
            Text(if (importingStudentSchedules) "학생별 시간표 가져오는 중..." else "학생별 시간표 가져오기")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showingRegisteredLessons = true }
        ) {
            Text("등록된 수업 관리")
        }

        }



        Spacer(
            Modifier.height(20.dp)
        )

        if (!showingRegisteredLessons) {

        TeacherHolidayCalendar()

        Spacer(
            Modifier.height(20.dp)
        )





        Text("요일 (여러 요일 선택 가능)")



        days.chunked(3).forEach { weekRow ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ){
                weekRow.forEach { day ->
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedDays = if (selectedDays.contains(day)) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDays.contains(day)) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (selectedDays.contains(day)) {
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






        OutlinedTextField(
            value = selectedGrade,
            onValueChange = { selectedGrade = it },
            label = { Text("학년") },
            placeholder = { Text("예: 중1, 고2") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        gradesByClass[selectedClass].orEmpty().takeIf { it.isNotEmpty() }?.let { candidates ->
            Text(
                "${selectedClass} 등록 학생 학년: ${candidates.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall
            )
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

                if (selectedDays.isEmpty() || selectedGrade.isBlank() || subject.isBlank() || startTime.isBlank() || endTime.isBlank()) {
                    message = "요일, 학년, 과목, 시작 시간을 모두 입력하세요."
                    return@Button
                }



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
                            selectedDays.first(),


                        subject =
                            subject,


                        teacher =
                            teacherName,


                        teacherUid =
                            teacherUid,


                        grade =
                            selectedGrade.trim(),


                        className =
                            selectedClass,


                        startTime =
                            startTime,


                        endTime =
                            endTime

                    )




                val schedules = selectedDays
                    .sortedBy { days.indexOf(it) }
                    .map { day ->
                        if (day == data.day) data else {
                            val extraRef = database
                                .getReference("teacherTimetable")
                                .child(teacherUid)
                                .push()
                            data.copy(id = extraRef.key ?: "", day = day)
                        }
                    }
                val updates = schedules.associate { schedule ->
                    "teacherTimetable/$teacherUid/${schedule.id}" to schedule
                }

                database.reference.updateChildren(updates)

                    .addOnSuccessListener {


                        message =
                            "${schedules.joinToString("·") { it.day }}요일 시간표 저장 완료"


                        subject=""


                        startTime=""


                        endTime=""


                        selectedDays = emptySet()


                        load()


                    }

                    .addOnFailureListener {

                        message = "수업 등록에 실패했습니다: ${it.message ?: "권한 또는 네트워크 오류"}"

                    }



            }

        ){


            Text("시간표 저장")


        }






        if(message.isNotBlank()){


            Text(message)


        }

        }






        Spacer(
            Modifier.height(25.dp)
        )






        if (showingRegisteredLessons) {

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showingRegisteredLessons = false }
        ) {
            Text("수업 등록으로 돌아가기")
        }

        Spacer(Modifier.height(16.dp))

        Text(

            "등록된 수업",

            style =
                MaterialTheme.typography.titleLarge

        )






        Text("요일별 보기", style = MaterialTheme.typography.titleMedium)

        (listOf("전체") + days).chunked(4).forEach { dayRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayRow.forEach { day ->
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { viewingDay = day },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewingDay == day) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (viewingDay == day) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) { Text(day) }
                }
                repeat(4 - dayRow.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(6.dp))
        }

        val visibleTimetables = if (viewingDay == "전체") list else list.filter { it.day == viewingDay }
        if (visibleTimetables.isEmpty()) {
            Text("${viewingDay}요일에 등록된 수업이 없습니다.")
        }

        visibleTimetables.forEach { item ->

            var legacyGrade by remember(item.id, gradesByClass) {
                mutableStateOf(item.grade.ifBlank { gradesByClass[item.className]?.singleOrNull().orEmpty() })
            }



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

                        "${item.grade.ifBlank { "학년 미지정" }} ${item.className} ${item.subject}"

                    )

                    run {
                        val candidates = gradesByClass[item.className].orEmpty()
                        Spacer(Modifier.height(8.dp))
                        Text("대상 학년을 학생 정보와 같게 확인해 주세요.", style = MaterialTheme.typography.bodySmall)
                        if (candidates.isNotEmpty()) {
                            Text("등록된 학생 학년: ${candidates.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedTextField(
                            value = legacyGrade,
                            onValueChange = { legacyGrade = it },
                            label = { Text("대상 학년") },
                            placeholder = { Text("예: 중1, 고2") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            enabled = legacyGrade.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                database.getReference("teacherTimetable").child(teacherUid).child(item.id)
                                    .child("grade").setValue(legacyGrade.trim())
                                    .addOnSuccessListener {
                                        message = "${item.className} 수업을 ${legacyGrade.trim()}로 지정했습니다."
                                        load()
                                    }
                                    .addOnFailureListener {
                                        message = "학년 지정에 실패했습니다: ${it.message ?: "권한 또는 네트워크 오류"}"
                                    }
                            }
                        ) { Text("이 학년으로 저장") }
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(

                        modifier = Modifier.fillMaxWidth(),

                        onClick = { onClassOpen(item) }

                    ){

                        Text("학생 목록 · 출결 관리")

                    }





                    OutlinedButton(

                        modifier = Modifier.fillMaxWidth(),

                        onClick = { editingTimetable = item }

                    ){

                        Text("수업 수정")

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

        }

        if (currentCandidates.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = {
                    currentCandidates = emptyList()
                    currentSelectionTitle = ""
                    currentSelectionDescription = ""
                },
                title = { Text(currentSelectionTitle.ifBlank { "수업 선택" }) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(currentSelectionDescription)
                        currentCandidates.forEach { item ->
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    currentCandidates = emptyList()
                                    currentSelectionTitle = ""
                                    currentSelectionDescription = ""
                                    onClassOpen(item)
                                }
                            ) {
                                Text("${item.grade} ${item.className} · ${item.subject} (${item.startTime}~${item.endTime})")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        currentCandidates = emptyList()
                        currentSelectionTitle = ""
                        currentSelectionDescription = ""
                    }) { Text("취소") }
                }
            )
        }

        editingTimetable?.let { item ->
            TeacherTimetableEditDialog(
                timetable = item,
                onDismiss = { editingTimetable = null },
                onSave = { updated ->
                    database.getReference("teacherTimetable").child(teacherUid).child(item.id)
                        .updateChildren(
                            mapOf(
                                "day" to updated.day,
                                "grade" to updated.grade,
                                "className" to updated.className,
                                "subject" to updated.subject,
                                "startTime" to updated.startTime,
                                "endTime" to updated.endTime
                            )
                        )
                        .addOnSuccessListener {
                            message = "수업 시간표를 수정했습니다."
                            editingTimetable = null
                            load()
                        }
                        .addOnFailureListener {
                            message = "수업 수정에 실패했습니다: ${it.message ?: "권한 또는 네트워크 오류"}"
                        }
                }
            )
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

private fun teacherTodayDay(): String = when (LocalDate.now().dayOfWeek) {
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
    DayOfWeek.SUNDAY -> "일"
}

private fun isSameTeacherDay(timetableDay: String, today: String): Boolean {
    val normalized = timetableDay.trim()
    return normalized == today || normalized == "${today}요일"
}

private fun parseTeacherTime(value: String): LocalTime? {
    val normalized = value.trim()
    val formats = listOf(
        DateTimeFormatter.ofPattern("HH:mm"),
        DateTimeFormatter.ofPattern("H:mm")
    )
    return formats.firstNotNullOfOrNull { format ->
        runCatching { LocalTime.parse(normalized, format) }.getOrNull()
    }
}

@Composable
private fun TeacherTimetableEditDialog(
    timetable: Timetable,
    onDismiss: () -> Unit,
    onSave: (Timetable) -> Unit
) {
    var day by remember(timetable.id) { mutableStateOf(timetable.day) }
    var grade by remember(timetable.id) { mutableStateOf(timetable.grade) }
    var className by remember(timetable.id) { mutableStateOf(timetable.className) }
    var subject by remember(timetable.id) { mutableStateOf(timetable.subject) }
    var startTime by remember(timetable.id) { mutableStateOf(timetable.startTime) }
    var endTime by remember(timetable.id) { mutableStateOf(timetable.endTime) }
    var error by remember { mutableStateOf("") }
    val days = setOf("월", "화", "수", "목", "금", "토", "일")
    val timeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("수업 시간표 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(day, { day = it }, Modifier.fillMaxWidth(), label = { Text("요일 (월~일)") }, singleLine = true)
                OutlinedTextField(grade, { grade = it }, Modifier.fillMaxWidth(), label = { Text("학년") }, singleLine = true)
                OutlinedTextField(className, { className = it }, Modifier.fillMaxWidth(), label = { Text("반") }, singleLine = true)
                OutlinedTextField(subject, { subject = it }, Modifier.fillMaxWidth(), label = { Text("과목") }, singleLine = true)
                OutlinedTextField(
                    startTime,
                    {
                        startTime = it
                        endTime = try {
                            LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
                                .plusMinutes(90).format(DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (_: Exception) { "" }
                    },
                    Modifier.fillMaxWidth(), label = { Text("시작 시간") }, singleLine = true
                )
                OutlinedTextField(endTime, {}, Modifier.fillMaxWidth(), enabled = false, label = { Text("종료 시간 (자동)") }, singleLine = true)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (day.trim() !in days || grade.isBlank() || className.isBlank() || subject.isBlank() || !startTime.matches(timeRegex)) {
                    error = "요일, 학년, 반, 과목과 HH:mm 형식의 시작 시간을 확인하세요."
                    return@Button
                }
                onSave(timetable.copy(
                    day = day.trim(), grade = grade.trim(), className = className.trim(),
                    subject = subject.trim(), startTime = startTime, endTime = endTime
                ))
            }) { Text("저장") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("취소") } }
    )
}
