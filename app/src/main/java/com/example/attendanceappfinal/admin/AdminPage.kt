package com.example.attendanceappfinal.admin


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceappfinal.UiConfig



@Composable
fun AdminPage(

    onBack: () -> Unit = {},

    onLogClick: () -> Unit = {},

    onAttendanceAllClick: () -> Unit = {},

    onStudentAttendanceClick: () -> Unit = {},

    onUserManageClick: () -> Unit = {},

    onStudentManageClick: () -> Unit = {},

    onPreStudentClick: () -> Unit = {},

    onTeacherRegisterClick: () -> Unit = {},

    onHolidayManageClick: () -> Unit = {},

    onDataAuditClick: () -> Unit = {},

    onAnnouncementClick: () -> Unit = {},

    onAcademySettingsClick: () -> Unit = {},

    onLogout: () -> Unit = {}

){



    val menus = listOf(

        AdminMenu(
            "👥",
            "사용자 관리",
            onUserManageClick
        ),

        AdminMenu(
            "🎓",
            "학생 관리",
            onStudentManageClick
        ),

        AdminMenu(
            "⏳",
            "미가입학생 관리",
            onPreStudentClick
        ),

        AdminMenu(
            "👨‍🏫",
            "선생님 추가",
            onTeacherRegisterClick
        ),

        AdminMenu(
            "🗓",
            "휴일 관리",
            onHolidayManageClick
        ),

        AdminMenu(
            "📋",
            "출결 수정 기록",
            onLogClick
        ),

        AdminMenu(
            "📊",
            "전체 출결 조회",
            onAttendanceAllClick
        ),

        AdminMenu(
            "📈",
            "학생 출결 통계",
            onStudentAttendanceClick
        ),

        AdminMenu(
            "🔎",
            "데이터 점검",
            onDataAuditClick
        ),

        AdminMenu(
            "📢",
            "학생 공지",
            onAnnouncementClick
        ),

        AdminMenu(
            "⚙️",
            "반 · 과목 설정",
            onAcademySettingsClick
        )

    )





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

            "관리자 센터",

            style = MaterialTheme.typography.headlineMedium

        )



        Spacer(

            Modifier.height(5.dp)

        )



        Text(

            "출결 시스템 관리",

            style = MaterialTheme.typography.bodyMedium

        )





        Spacer(

            Modifier.height(25.dp)

        )







        LazyVerticalGrid(

            columns = GridCells.Fixed(2),

            modifier = Modifier

                .weight(1f),

            horizontalArrangement = Arrangement.spacedBy(12.dp),

            verticalArrangement = Arrangement.spacedBy(12.dp)

        ){



            items(menus){ menu ->


                AdminMenuCard(

                    icon = menu.icon,

                    title = menu.title,

                    onClick = menu.onClick

                )


            }


        }







        Spacer(

            Modifier.height(15.dp)

        )







        Button(

            modifier = Modifier

                .fillMaxWidth()

                .height(55.dp),

            shape = RoundedCornerShape(16.dp),

            onClick = {

                onLogout()

            }

        ){

            Text(

                "로그아웃"

            )


        }





    }



}





data class AdminMenu(

    val icon:String,

    val title:String,

    val onClick:()->Unit

)







@Composable
private fun AdminMenuCard(

    icon:String,

    title:String,

    onClick:()->Unit

){



    Card(

        modifier = Modifier

            .fillMaxWidth()

            .height(120.dp),

        shape = RoundedCornerShape(20.dp),

        onClick = onClick,

        elevation = CardDefaults.cardElevation(

            defaultElevation = 5.dp

        )

    ){



        Column(

            modifier = Modifier

                .fillMaxSize()

                .padding(15.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center

        ){



            Text(

                icon,

                style = MaterialTheme.typography.headlineMedium

            )



            Spacer(

                Modifier.height(8.dp)

            )



            Text(

                title,

                style = MaterialTheme.typography.titleMedium

            )



        }


    }



}
