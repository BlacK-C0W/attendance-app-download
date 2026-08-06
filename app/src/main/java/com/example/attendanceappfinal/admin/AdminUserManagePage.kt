package com.example.attendanceappfinal.admin



import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.model.User
import com.google.firebase.database.FirebaseDatabase
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(
    ExperimentalMaterial3Api::class
)

@Composable
fun AdminUserManagePage(

    onBack: () -> Unit,

    onStudentManageClick: (User) -> Unit = {}

){


    BackHandler {

        onBack()

    }



    val database =
        FirebaseDatabase.getInstance()



    var users by remember {

        mutableStateOf(
            emptyList<User>()
        )

    }



    var selectedGrade by remember {

        mutableStateOf("전체")

    }



    var gradeExpanded by remember {

        mutableStateOf(false)

    }



    var searchText by remember {

        mutableStateOf("")

    }



    var message by remember {

        mutableStateOf("")

    }





    val grades = listOf(

        "전체",
        "초5",
        "초6",
        "중1",
        "중2",
        "중3",
        "고1",
        "고2",
        "고3",
        "미가입"

    )





    fun loadUsers(){


        database

            .getReference("users")

            .get()

            .addOnSuccessListener { snapshot ->



                val list =
                    mutableListOf<User>()



                snapshot.children.forEach { child ->



                    child.getValue(User::class.java)

                        ?.let {

                            list.add(

                                it.copy(

                                    uid = child.key ?: ""

                                )

                            )

                        }



                }






                database

                    .getReference("preStudents")

                    .get()

                    .addOnSuccessListener { preSnapshot ->



                        preSnapshot.children.forEach { child ->



                            val preUser = User(


                                uid =
                                    "pre_${child.key}",


                                name =
                                    child.child("name")
                                        .getValue(String::class.java)
                                        ?: "",


                                phone =
                                    child.child("phone")
                                        .getValue(String::class.java)
                                        ?: "",


                                role =
                                    "student",


                                grade =
                                    child.child("grade")
                                        .getValue(String::class.java)
                                        ?: "",


                                className =
                                    "미가입",


                                isPreStudent =
                                    true


                            )


                            list.add(preUser)


                        }




                        database
                            .getReference("unregisteredStudents")
                            .get()
                            .addOnSuccessListener { unregisteredSnapshot ->
                                unregisteredSnapshot.children.forEach { child ->
                                    list.add(
                                        User(
                                            uid = "un_${child.key}",
                                            name = child.child("name").getValue(String::class.java) ?: "",
                                            phone = child.child("phone").getValue(String::class.java) ?: "",
                                            role = "student",
                                            grade = child.child("grade").getValue(String::class.java) ?: "",
                                            className = child.child("className").getValue(String::class.java) ?: "",
                                            isUnregisteredStudent = true,
                                            unregisteredStudentId = child.key ?: ""
                                        )
                                    )
                                }
                                users = list
                            }
                            .addOnFailureListener {
                                users = list
                            }



                    }



            }


            .addOnFailureListener {


                message =
                    "사용자 불러오기 실패"


            }



    }





    LaunchedEffect(Unit){

        loadUsers()

    }






    val filteredUsers = users.filter { user ->



        val searchMatch =

            searchText.isBlank()

                    ||

                    user.name.contains(
                        searchText,
                        ignoreCase = true
                    )

                    ||

                    user.phone.contains(
                        searchText,
                        ignoreCase = true
                    )

                    ||

                    when(searchText){

                        "관리자" ->
                            user.role == "admin"


                        "선생님" ->
                            user.role == "teacher"


                        "학생" ->
                            user.role == "student"


                        "가입대기",
                        "미가입" ->
                            user.isPreStudent ||
                                    user.isUnregisteredStudent ||
                                    user.className == "미가입"


                        else ->
                            false

                    }





        val gradeMatch = when(selectedGrade){



            "전체" ->

                true



            "미가입" ->

                user.isPreStudent || user.isUnregisteredStudent || user.className == "미가입"



            else ->

                user.grade == selectedGrade



        }




        searchMatch && gradeMatch



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

            "사용자 관리",

            style =
                MaterialTheme.typography.headlineMedium

        )



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

                Text("이름 / 전화번호 검색")

            }

        )





        Spacer(

            Modifier.height(15.dp)

        )





        Text(

            "학년 선택",

            style =
                MaterialTheme.typography.titleMedium

        )



        Spacer(

            Modifier.height(8.dp)

        )





        ExposedDropdownMenuBox(

            expanded = gradeExpanded,


            onExpandedChange = {

                gradeExpanded = !gradeExpanded

            }


        ){



            OutlinedTextField(

                value = selectedGrade,


                onValueChange = {},


                readOnly = true,


                label = {

                    Text("학년")

                },


                trailingIcon = {


                    ExposedDropdownMenuDefaults.TrailingIcon(

                        expanded = gradeExpanded

                    )


                },


                modifier = Modifier

                    .fillMaxWidth()

                    .menuAnchor()


            )



            ExposedDropdownMenu(

                expanded = gradeExpanded,


                onDismissRequest = {

                    gradeExpanded = false

                }


            ){



                grades.forEach { grade ->



                    DropdownMenuItem(

                        text = {

                            Text(grade)

                        },


                        onClick = {


                            selectedGrade = grade


                            gradeExpanded = false


                        }


                    )



                }


            }



        }
        Spacer(

            Modifier.height(15.dp)

        )




        Text(

            "검색 결과 : ${filteredUsers.size}명"

        )




        if(message.isNotBlank()){


            Spacer(

                Modifier.height(10.dp)

            )


            Text(message)


        }






        Spacer(

            Modifier.height(10.dp)

        )






        filteredUsers.forEach { user ->



            UserManageCard(

                user = user,


                onStudentManageClick = {

                    onStudentManageClick(user)

                },


                onDelete = {


                    if(user.uid.startsWith("pre_")){


                        val key =

                            user.uid.removePrefix(
                                "pre_"
                            )



                        database

                            .getReference(
                                "preStudents"
                            )

                            .child(key)

                            .removeValue()

                            .addOnSuccessListener {


                                message =

                                    "${user.name} 가입대기 학생 삭제 완료"


                                loadUsers()


                            }


                    }else if(user.uid.startsWith("un_")){


                        database

                            .getReference(
                                "unregisteredStudents"
                            )

                            .child(user.unregisteredStudentId.ifBlank { user.uid.removePrefix("un_") })

                            .removeValue()

                            .addOnSuccessListener {


                                message =
                                    "${user.name} 미가입학생 삭제 완료"


                                loadUsers()


                            }


                    }else{


                        database

                            .getReference(
                                "users"
                            )

                            .child(user.uid)

                            .removeValue()

                            .addOnSuccessListener {


                                message =

                                    "${user.name} 삭제 완료"


                                loadUsers()


                            }



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









@Composable
private fun UserManageCard(

    user: User,

    onStudentManageClick: () -> Unit,

    onDelete: () -> Unit

){



    Card(

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 5.dp),


        shape =

            MaterialTheme.shapes.large,


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

                user.name,

                style =

                    MaterialTheme.typography.titleLarge

            )





            Spacer(

                Modifier.height(8.dp)

            )





            Text(

                "전화번호 : ${user.phone}"

            )






            Text(

                "권한 : ${
                    when(user.role){

                        "admin" ->

                            "관리자"


                        "teacher" ->

                            "선생님"


                        "student" ->

                            if(user.isPreStudent || user.isUnregisteredStudent || user.className == "미가입")

                                "학생 (가입대기)"

                            else

                                "학생"



                        else ->

                            "미등록"


                    }

                }"

            )







            if(user.role == "student"){



                Text(

                    "학년 : ${
                        user.grade.ifBlank {

                            "미등록"

                        }

                    }"

                )





                Text(

                    "상태 : ${
                        if(user.isPreStudent || user.isUnregisteredStudent || user.className == "미가입")

                            "회원가입 대기"

                        else

                            "가입 완료"

                    }"

                )



            }







            Spacer(

                Modifier.height(12.dp)

            )







            if(

                user.role == "student"

                &&

                !user.isPreStudent

            ){



                Button(

                    modifier =

                        Modifier.fillMaxWidth(),


                    onClick =

                        onStudentManageClick


                ){

                    Text(

                        "학생 관리"

                    )

                }





                Spacer(

                    Modifier.height(8.dp)

                )



            }








            if(user.role == "admin"){



                Text(

                    "관리자 계정 보호됨"

                )



            }else{



                Button(

                    modifier =

                        Modifier.fillMaxWidth(),


                    colors =

                        ButtonDefaults.buttonColors(

                            containerColor =

                                MaterialTheme.colorScheme.error

                        ),


                    onClick =

                        onDelete


                ){



                    Text(

                        "삭제"

                    )



                }



            }





        }



    }



}
