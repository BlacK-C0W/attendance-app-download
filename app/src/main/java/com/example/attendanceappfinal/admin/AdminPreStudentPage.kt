package com.example.attendanceappfinal.admin


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.UnregisteredStudent
import com.google.firebase.database.FirebaseDatabase


@Composable
fun AdminPreStudentPage(

    nfcTag: String?,

    clearNfc: () -> Unit,

    onBack: () -> Unit

) {


    BackHandler {

        onBack()

    }


    val database =
        FirebaseDatabase.getInstance()


    var students by remember {

        mutableStateOf(
            emptyList<UnregisteredStudent>()
        )

    }


    var name by remember {

        mutableStateOf("")

    }


    var grade by remember {

        mutableStateOf("")

    }


    var className by remember {

        mutableStateOf("")

    }


    var phone by remember {

        mutableStateOf("")

    }


    var pendingStudent by remember {

        mutableStateOf<UnregisteredStudent?>(null)

    }


    var message by remember {

        mutableStateOf("")

    }


    fun loadStudents() {


        database

            .getReference("unregisteredStudents")

            .get()

            .addOnSuccessListener { snapshot ->


                val list =
                    mutableListOf<UnregisteredStudent>()


                snapshot.children.forEach { child ->


                    child.getValue(
                        UnregisteredStudent::class.java
                    )?.let { student ->


                        list.add(

                            student.copy(

                                id =
                                    child.key ?: student.id

                            )

                        )

                    }

                }


                students =

                    list.sortedWith(

                        compareBy<UnregisteredStudent> {

                            it.grade

                        }.thenBy {

                            it.className

                        }.thenBy {

                            it.name

                        }

                    )

            }

            .addOnFailureListener {


                message =
                    "학생 목록을 불러오지 못했습니다"

            }

    }


    LaunchedEffect(Unit) {

        loadStudents()

    }


    LaunchedEffect(nfcTag) {


        val student =
            pendingStudent


        if (

            !nfcTag.isNullOrBlank() &&

            student != null

        ) {


            database

                .getReference(
                    "unregisteredStudents/${student.id}"
                )

                .updateChildren(

                    mapOf(

                        "nfcTag" to nfcTag,

                        "hasNfc" to true

                    )

                )

                .addOnSuccessListener {


                    database

                        .getReference("nfc_tags")

                        .child(nfcTag)

                        .setValue(

                            mapOf(

                                "unregisteredStudentId" to student.id,

                                "preStudentId" to "",

                                "studentUid" to "",

                                "name" to student.name,

                                "type" to "unregistered"

                            )

                        )

                        .addOnSuccessListener {


                            message =
                                "${student.name} 학생 NFC 등록 완료"


                            pendingStudent = null


                            loadStudents()


                            clearNfc()

                        }

                        .addOnFailureListener {


                            message =
                                "NFC 정보 저장에 실패했습니다"

                        }

                }

                .addOnFailureListener {


                    message =
                        "학생 NFC 등록에 실패했습니다"

                }

        }

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

            "📵 미가입 학생 관리",

            style =
                MaterialTheme.typography.headlineMedium

        )


        Spacer(

            Modifier.height(5.dp)

        )


        Text(

            "앱을 사용하지 않는 학생을 등록하고 NFC 또는 수동으로 출결을 관리합니다",

            style =
                MaterialTheme.typography.bodyMedium

        )


        if (message.isNotBlank()) {


            Spacer(

                Modifier.height(14.dp)

            )


            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp),

                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )

            ) {


                Text(

                    message,

                    color = MaterialTheme.colorScheme.onPrimaryContainer,

                    modifier =
                        Modifier.padding(16.dp)

                )

            }

        }


        Spacer(

            Modifier.height(25.dp)

        )


        Card(

            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(20.dp),

            elevation =
                CardDefaults.cardElevation(

                    defaultElevation = 5.dp

                )

        ) {


            Column(

                modifier =
                    Modifier.padding(20.dp)

            ) {


                Text(

                    "미가입 학생 등록",

                    style =
                        MaterialTheme.typography.titleLarge

                )


                Spacer(

                    Modifier.height(15.dp)

                )


                OutlinedTextField(

                    value = name,

                    onValueChange = {

                        name = it

                    },

                    label = {

                        Text("학생 이름")

                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true

                )


                Spacer(

                    Modifier.height(10.dp)

                )


                OutlinedTextField(

                    value = grade,

                    onValueChange = {

                        grade = it

                    },

                    label = {

                        Text("학년 (예: 중1)")

                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true

                )


                Spacer(

                    Modifier.height(10.dp)

                )


                OutlinedTextField(

                    value = className,

                    onValueChange = {

                        className = it

                    },

                    label = {

                        Text("반 (예: A반)")

                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true

                )


                Spacer(

                    Modifier.height(10.dp)

                )


                OutlinedTextField(

                    value = phone,

                    onValueChange = {

                        phone = it

                    },

                    label = {

                        Text("전화번호")

                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true

                )


                Spacer(

                    Modifier.height(15.dp)

                )


                Text(

                    "NFC는 나중에 등록할 수 있습니다",

                    style =
                        MaterialTheme.typography.bodySmall

                )


                Spacer(

                    Modifier.height(15.dp)

                )


                Button(

                    modifier = Modifier

                        .fillMaxWidth()

                        .height(55.dp),

                    shape =
                        RoundedCornerShape(16.dp),

                    onClick = {


                        if (

                            name.isBlank() ||

                            grade.isBlank() ||

                            className.isBlank() ||

                            phone.isBlank()

                        ) {


                            message =
                                "이름, 학년, 반, 전화번호를 입력하세요"


                            return@Button

                        }


                        val ref =

                            database

                                .getReference(
                                    "unregisteredStudents"
                                )

                                .push()


                        val studentId =
                            ref.key ?: return@Button


                        val student =

                            UnregisteredStudent(

                                id = studentId,

                                name = name.trim(),

                                phone = phone.trim(),

                                grade = grade.trim(),

                                className = className.trim(),

                                nfcTag = "",

                                hasNfc = false,

                                createdAt =
                                    System.currentTimeMillis()

                            )


                        ref.setValue(student)

                            .addOnSuccessListener {


                                name = ""

                                grade = ""

                                className = ""

                                phone = ""


                                message =
                                    "미가입 학생 등록 완료"


                                loadStudents()

                            }

                            .addOnFailureListener {


                                message =
                                    "학생 등록에 실패했습니다"

                            }

                    }

                ) {


                    Text(

                        "미가입 학생 등록"

                    )

                }

            }

        }


        Spacer(

            Modifier.height(25.dp)

        )


        Text(

            "미가입 학생 목록",

            style =
                MaterialTheme.typography.titleLarge

        )


        Spacer(

            Modifier.height(5.dp)

        )


        Text(

            "총 ${students.size}명",

            style =
                MaterialTheme.typography.bodyMedium

        )


        Spacer(

            Modifier.height(10.dp)

        )


        if (students.isEmpty()) {


            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(18.dp)

            ) {


                Text(

                    "등록된 미가입 학생이 없습니다",

                    modifier =
                        Modifier.padding(20.dp)

                )

            }

        }


        students.forEach { student ->


            Card(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(vertical = 6.dp),

                shape =
                    RoundedCornerShape(18.dp),

                elevation =
                    CardDefaults.cardElevation(

                        defaultElevation = 4.dp

                    )

            ) {


                Column(

                    modifier =
                        Modifier.padding(18.dp)

                ) {


                    Text(

                        student.name,

                        style =
                            MaterialTheme.typography.titleMedium

                    )


                    Spacer(

                        Modifier.height(5.dp)

                    )


                    Text(

                        "학년 : ${student.grade}"

                    )


                    Text(

                        "반 : ${student.className}"

                    )


                    Text(

                        "전화번호 : ${

                            if (student.phone.isBlank())

                                "미등록"

                            else

                                student.phone

                        }"

                    )


                    Text(

                        "NFC : ${

                            if (student.hasNfc)

                                student.nfcTag

                            else

                                "미등록"

                        }"

                    )


                    Spacer(

                        Modifier.height(12.dp)

                    )


                    if (!student.hasNfc) {


                        Button(

                            modifier =
                                Modifier.fillMaxWidth(),

                            onClick = {


                                pendingStudent =
                                    student


                                message =
                                    "${student.name} 학생의 NFC 태그를 찍어주세요"

                            }

                        ) {


                            Text(

                                "NFC 등록"

                            )

                        }

                    } else {


                        OutlinedButton(

                            modifier =
                                Modifier.fillMaxWidth(),

                            onClick = {


                                pendingStudent =
                                    student


                                message =
                                    "${student.name} 학생의 새 NFC 태그를 찍어주세요"

                            }

                        ) {


                            Text(

                                "NFC 변경"

                            )

                        }

                    }


                    Spacer(

                        Modifier.height(8.dp)

                    )


                    OutlinedButton(

                        modifier =
                            Modifier.fillMaxWidth(),

                        onClick = {


                            if (student.nfcTag.isNotBlank()) {


                                database

                                    .getReference("nfc_tags")

                                    .child(student.nfcTag)

                                    .removeValue()

                            }


                            database

                                .getReference(
                                    "unregisteredStudents/${student.id}"
                                )

                                .removeValue()

                                .addOnSuccessListener {


                                    if (

                                        pendingStudent?.id ==
                                        student.id

                                    ) {


                                        pendingStudent = null

                                    }


                                    message =
                                        "${student.name} 학생 삭제 완료"


                                    loadStudents()

                                }

                                .addOnFailureListener {


                                    message =
                                        "학생 삭제에 실패했습니다"

                                }

                        }

                    ) {


                        Text(

                            "삭제"

                        )

                    }

                }

            }

        }
        Spacer(

            Modifier.height(20.dp)

        )


        OutlinedButton(

            modifier = Modifier

                .fillMaxWidth()

                .height(55.dp),

            shape =
                RoundedCornerShape(16.dp),

            onClick = onBack

        ) {


            Text(

                "뒤로가기"

            )

        }

    }

}
