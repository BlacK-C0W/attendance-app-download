package com.example.attendanceappfinal.teacher


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
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.model.PreStudent
import com.example.attendanceappfinal.model.UnregisteredStudent
import com.example.attendanceappfinal.ui.StudentTypeBadge
import com.google.firebase.database.FirebaseDatabase



@Composable
fun TeacherStudentSelectPage(

    mode:String,

    teacherUid:String,

    teacherName:String,

    onBack:()->Unit,

    onStudentClick:(User)->Unit = {}

){



    val database =
        FirebaseDatabase.getInstance()



    var studentList by remember {

        mutableStateOf(
            emptyList<User>()
        )

    }



    var selectedStudent by remember {

        mutableStateOf<User?>(null)

    }



    var expandedGrade by remember {

        mutableStateOf<String?>(null)

    }



    var expandedClass by remember {

        mutableStateOf<Pair<String,String>?>(null)

    }



    var searchText by remember {

        mutableStateOf("")

    }





    BackHandler {


        if(selectedStudent != null){


            selectedStudent = null


        }else{


            onBack()


        }


    }





    fun loadStudents(){



        val list =
            mutableListOf<User>()





        // 가입 완료 학생

        database

            .getReference("users")

            .get()

            .addOnSuccessListener { snapshot ->



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

                        user != null &&

                        user.role == "student"

                    ){


                        list.add(user)


                    }


                }






                // 앱 가입 예정 학생

                database

                    .getReference("preStudents")

                    .get()

                    .addOnSuccessListener { preSnapshot ->



                        preSnapshot.children.forEach { child ->



                            val pre =

                                child.getValue(
                                    PreStudent::class.java
                                )



                            if(pre != null){



                                list.add(


                                    User(

                                        uid =
                                            "pre_${child.key}",


                                        name =
                                            pre.name,


                                        phone =
                                            pre.phone,


                                        role =
                                            "student",


                                        grade =
                                            pre.grade,


                                        className =
                                            "가입예정",


                                        nfcId =
                                            "",


                                        createdAt =
                                            0L,


                                        isPreStudent =
                                            true,


                                        preStudentId =
                                            child.key ?: ""

                                    )



                                )



                            }



                        }






                        // 미가입 학생 추가

                        database

                            .getReference(
                                "unregisteredStudents"
                            )

                            .get()

                            .addOnSuccessListener { unSnapshot ->



                                unSnapshot.children.forEach { child ->




                                    val un =

                                        child.getValue(
                                            UnregisteredStudent::class.java
                                        )



                                    if(un != null){



                                        list.add(


                                            User(


                                                uid =
                                                    "un_${child.key}",



                                                name =
                                                    un.name,



                                                phone =
                                                    un.phone,



                                                role =
                                                    "student",



                                                grade =
                                                    un.grade,



                                                className =
                                                    un.className.ifBlank {

                                                        "미등록"

                                                    },


                                                nfcId =
                                                    un.nfcTag,


                                                createdAt =
                                                    un.createdAt,


                                                isUnregisteredStudent =
                                                    true


                                            )


                                        )


                                    }




                                }



                                studentList =
                                    list



                            }




                    }



            }



    }





    LaunchedEffect(Unit){

        loadStudents()

    }
    selectedStudent?.let { student ->


        if(mode == "timetable"){


            TeacherTimetablePage(

                student = student,

                teacherUid = teacherUid,

                teacherName = teacherName

            )


        }else{


            TeacherStudentDetailPage(

                student = student

            )


        }


        return


    }





    val gradeOrder = listOf(

        "초5",
        "초6",

        "중1",
        "중2",
        "중3",

        "고1",
        "고2",
        "고3",

        "가입예정",

        "미등록"

    )





    val filteredStudents =

        studentList

            .filter {


                searchText.isBlank()

                        ||

                        it.name.contains(
                            searchText,
                            ignoreCase = true
                        )

                        ||

                        it.phone.contains(
                            searchText,
                            ignoreCase = true
                        )


            }

            .sortedBy {

                it.name

            }





    val gradeMap =

        filteredStudents

            .groupBy {


                if(it.grade.isBlank())

                    "미등록"

                else

                    it.grade


            }

            .toSortedMap(

                compareBy {


                    val index =

                        gradeOrder.indexOf(it)


                    if(index == -1)

                        999

                    else

                        index


                }

            )







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

            if(mode == "student") "학생 관리" else "학생 선택",

            style =
                MaterialTheme.typography.headlineMedium

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
                Text("전체 ${filteredStudents.size}명", style = MaterialTheme.typography.titleLarge)
                Text("학년과 반을 펼쳐 학생 정보를 확인하세요.", style = MaterialTheme.typography.bodyMedium)
            }
        }





        Spacer(

            Modifier.height(15.dp)

        )





        OutlinedTextField(

            value = searchText,

            onValueChange = {

                searchText = it

            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {

                Text(
                    "이름 / 전화번호 검색"
                )

            }

        )





        Spacer(

            Modifier.height(20.dp)

        )







        gradeMap.forEach { (grade, students) ->





            Card(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(vertical = 6.dp)

            ){



                Column(

                    modifier =
                        Modifier.padding(15.dp)

                ){



                    Button(

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape = RoundedCornerShape(14.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = if(expandedGrade == grade)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if(expandedGrade == grade)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        ),


                        onClick = {


                            expandedGrade =

                                if(expandedGrade == grade)

                                    null

                                else

                                    grade


                            expandedClass = null


                        }

                    ){


                        Text(

                            if(expandedGrade == grade)

                                "▼ $grade (${students.size}명)"

                            else

                                "▶ $grade (${students.size}명)"

                        )


                    }






                    if(expandedGrade == grade){



                        students

                            .groupBy {

                                if(it.className.isBlank())

                                    "반 미등록"

                                else

                                    it.className

                            }

                            .forEach { (className,classStudents) ->





                                Spacer(

                                    Modifier.height(8.dp)

                                )





                                Button(

                                    modifier =
                                        Modifier.fillMaxWidth(),


                                    onClick = {


                                        val target =

                                            Pair(
                                                grade,
                                                className
                                            )


                                        expandedClass =

                                            if(expandedClass == target)

                                                null

                                            else

                                                target


                                    }

                                ){


                                    Text(

                                        "▶ $className (${classStudents.size}명)"

                                    )


                                }







                                if(

                                    expandedClass ==
                                    Pair(
                                        grade,
                                        className
                                    )

                                ){



                                    classStudents.forEach { student ->




                                        Card(

                                            modifier =
                                                Modifier

                                                    .fillMaxWidth()

                                                    .padding(
                                                        vertical = 5.dp
                                                    ),

                                            shape = RoundedCornerShape(16.dp),

                                            elevation = CardDefaults.cardElevation(
                                                defaultElevation = 2.dp
                                            )

                                        ){



                                            Column(

                                                modifier =
                                                    Modifier.padding(15.dp)

                                            ){



                                                Text(

                                                    "👤 ${student.name}",

                                                    style =
                                                        MaterialTheme.typography.titleMedium

                                                )



                                                Text(

                                                    "학년 : ${student.grade}"

                                                )



                                                Text(

                                                    "반 : ${student.className}"

                                                )





                                                if(student.phone.isNotBlank()){


                                                    Text(

                                                        "📞 ${student.phone}"

                                                    )


                                                }

                                                if(student.parentPhone.isNotBlank()){

                                                    Text(

                                                        "학부모 연락처 : ${student.parentPhone}"

                                                    )

                                                }







                                                Text(

                                                    when{


                                                        student.isPreStudent ->

                                                            "상태 : 앱 가입 예정"



                                                        student.isUnregisteredStudent ->

                                                            "상태 : 미가입 학생"



                                                        else ->

                                                            "상태 : 가입 완료"


                                                    }

                                                )

                                                StudentTypeBadge(student)






                                                Spacer(

                                                    Modifier.height(10.dp)

                                                )







                                                Button(

                                                    modifier =
                                                        Modifier.fillMaxWidth(),


                                                    onClick = {


                                                        if(mode=="student"){


                                                            onStudentClick(student)


                                                        }else{


                                                            selectedStudent = student


                                                        }


                                                    }


                                                ){


                                                    Text(

                                                        when(mode){


                                                            "timetable" ->

                                                                "시간표 관리"


                                                            "student" ->

                                                                "학생 관리"


                                                            else ->

                                                                "학생 정보"


                                                        }

                                                    )


                                                }



                                            }



                                        }




                                    }



                                }



                            }





                    }



                }



            }



        }





        Spacer(

            Modifier.height(25.dp)

        )





        OutlinedButton(

            modifier =
                Modifier.fillMaxWidth(),

            onClick = onBack

        ){


            Text("뒤로가기")


        }



    }



}
