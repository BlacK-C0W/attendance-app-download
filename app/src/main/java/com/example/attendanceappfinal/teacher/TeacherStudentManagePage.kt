package com.example.attendanceappfinal.teacher


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig
import com.example.attendanceappfinal.admin.UnregisteredStudentRegisterPage
import com.example.attendanceappfinal.model.User
import com.example.attendanceappfinal.nfc.NfcRegisterPage



@Composable
fun TeacherStudentManagePage(

    teacherUid:String,

    teacherName:String,

    nfcTag:String?,

    onBack:()->Unit

){


    var page by remember {

        mutableStateOf("")

    }


    var selectedStudent by remember {

        mutableStateOf<User?>(null)

    }





    BackHandler {


        when {


            selectedStudent != null -> {


                selectedStudent = null


            }


            page.isNotBlank() -> {


                page=""


            }


            else -> {


                onBack()


            }


        }


    }






    // 학생 상세 관리 페이지

    selectedStudent?.let {


        TeacherStudentDetailPage(

            student = it

        )


        return

    }







    // 학생 정보 관리

    if(page=="student"){



        TeacherStudentSelectPage(


            mode = "student",


            teacherUid = teacherUid,


            teacherName = teacherName,


            onBack = {


                page=""


            },


            onStudentClick = {


                selectedStudent = it


            }


        )



        return


    }










    // 학생 시간표 관리

    if(page=="timetable"){



        TeacherStudentSelectPage(


            mode = "timetable",


            teacherUid = teacherUid,


            teacherName = teacherName,


            onBack = {


                page=""


            }


        )



        return


    }









    // NFC 등록

    if(page=="nfc"){



        NfcRegisterPage(

            nfcTag = nfcTag,

            onBack = {

                page=""

            }

        )



        return


    }









    // 미가입학생 등록

    if(page=="register"){



        UnregisteredStudentRegisterPage(


            onBack = {


                page=""


            }


        )



        return


    }

    if(page=="announcement"){

        TeacherAnnouncementPage(
            onBack = { page="" }
        )

        return

    }









    Column(


        modifier = Modifier


            .fillMaxSize()


            .padding(


                top = UiConfig.topPadding,


                start = UiConfig.sidePadding,


                end = UiConfig.sidePadding,


                bottom = UiConfig.bottomPadding


            )


    ){



        Text(


            "👥 학생 관리",


            style = MaterialTheme.typography.headlineMedium


        )





        Spacer(

            Modifier.height(25.dp)

        )








        TeacherManageCard(


            title = "👤 학생 정보 관리",


            desc = "학생 정보 수정 및 출결 확인"


        ){


            page="student"


        }







        Spacer(

            Modifier.height(15.dp)

        )








        TeacherManageCard(


            title = "📅 학생 시간표 관리",


            desc = "학생별 수업 시간표 등록"


        ){


            page="timetable"


        }







        Spacer(

            Modifier.height(15.dp)

        )








        TeacherManageCard(


            title = "📱 NFC 등록",


            desc = "학생 출석용 NFC 등록"


        ){


            page="nfc"


        }







        Spacer(

            Modifier.height(15.dp)

        )








        TeacherManageCard(


            title = "➕ 미가입학생 등록",


            desc = "가입 전 미가입학생 등록"


        ){


            page="register"


        }

        Spacer(Modifier.height(15.dp))

        TeacherManageCard(
            title = "📢 학생 공지",
            desc = "선택한 가입 완료 학생에게 알림 전송"
        ){
            page="announcement"
        }







        Spacer(

            Modifier.weight(1f)

        )








        OutlinedButton(


            modifier = Modifier.fillMaxWidth(),


            onClick = {


                onBack()


            }


        ){


            Text("뒤로가기")


        }





    }



}









@Composable
private fun TeacherManageCard(


    title:String,


    desc:String,


    onClick:()->Unit


){



    Card(


        modifier = Modifier.fillMaxWidth(),


        shape = RoundedCornerShape(18.dp),


        onClick = onClick,


        elevation = CardDefaults.cardElevation(

            defaultElevation = 5.dp

        )


    ){



        Column(


            modifier = Modifier.padding(20.dp)


        ){



            Text(


                title,


                style = MaterialTheme.typography.titleLarge


            )




            Spacer(

                Modifier.height(5.dp)

            )




            Text(desc)



        }



    }



}
