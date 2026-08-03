package com.example.attendanceappfinal.navigation


import android.widget.Toast

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext

import com.example.attendanceappfinal.admin.AdminAttendanceAllPage
import com.example.attendanceappfinal.admin.AdminDataIntegrityPage
import com.example.attendanceappfinal.admin.AdminAcademySettingsPage
import com.example.attendanceappfinal.admin.HolidayManagePage
import com.example.attendanceappfinal.admin.AdminAttendanceLogPage
import com.example.attendanceappfinal.admin.AdminPage
import com.example.attendanceappfinal.admin.AdminStudentAttendancePage
import com.example.attendanceappfinal.admin.AdminStudentAttendanceDetailPage
import com.example.attendanceappfinal.admin.AdminStudentProfileEditPage
import com.example.attendanceappfinal.admin.AdminStudentScoreHistoryPage
import com.example.attendanceappfinal.admin.ParentCommunicationPage
import com.example.attendanceappfinal.admin.AdminUserManagePage
import com.example.attendanceappfinal.admin.StudentPreRegisterPage
import com.example.attendanceappfinal.admin.TeacherRegisterPage
import com.example.attendanceappfinal.admin.AdminPreStudentPage
import com.example.attendanceappfinal.auth.LoginPage
import com.example.attendanceappfinal.auth.RegisterPage
import com.example.attendanceappfinal.auth.ParentRegisterPage
import com.example.attendanceappfinal.parent.ParentPage

import com.example.attendanceappfinal.model.User

import com.example.attendanceappfinal.student.StudentAttendancePage
import com.example.attendanceappfinal.student.StudentNotificationPage
import com.example.attendanceappfinal.student.StudentPage

import com.example.attendanceappfinal.teacher.TeacherPage
import com.example.attendanceappfinal.teacher.TeacherAnnouncementPage
import com.example.attendanceappfinal.teacher.TeacherStudentSelectPage
import com.example.attendanceappfinal.teacher.TeacherFirstLoginPage
import android.content.Context
import com.google.gson.Gson
import androidx.compose.runtime.LaunchedEffect

@Composable
fun AppNavigation(

    nfcTag:String?,

    clearNfc: () -> Unit = {}

){


    val context = LocalContext.current


    val prefs =
        context.getSharedPreferences(
            "login",
            Context.MODE_PRIVATE
        )


    var currentScreen by remember {

        mutableStateOf("login")

    }



    var currentUser by remember {

        mutableStateOf<User?>(null)

    }

    LaunchedEffect(Unit){

        val savedUser =
            prefs.getString(
                "user",
                null
            )


        if(savedUser != null){

            val user =
                Gson().fromJson(
                    savedUser,
                    User::class.java
                )


            currentUser = user


            currentScreen =
                when(user.role){

                    "student" -> "student"

                    "teacher" -> "teacher"

                    "admin" -> "admin"

                    else -> "login"

                }

        }

    }


    var selectedStudent by remember {

        mutableStateOf<User?>(null)

    }

    var selectedAttendanceGrade by remember {

        mutableStateOf<String?>(null)

    }

    var previousAdminScreen by remember {

        mutableStateOf("admin")

    }

    var lastParentBackTime by remember {
        mutableLongStateOf(0L)
    }

    var lastAdminBackTime by remember {

        mutableLongStateOf(0L)

    }


    when(currentScreen){



        "login" -> {


            LoginPage(

                onStudentLogin = {

                    currentUser = it

                    prefs.edit()
                        .putString(
                            "user",
                            Gson().toJson(it)
                        )
                        .apply()

                    currentScreen = "student"

                },


                onTeacherLogin = { user ->

                    currentUser = user

                    prefs.edit()
                        .putString(
                            "user",
                            Gson().toJson(user)
                        )
                        .apply()

                    currentScreen = "teacher"

                },


                onAdminLogin = { user ->

                    currentUser = user

                    prefs.edit()
                        .putString(
                            "user",
                            Gson().toJson(user)
                        )
                        .apply()

                    currentScreen = "admin"

                },


                onParentLogin = { user ->

                    currentUser = user

                    prefs.edit().putString("user", Gson().toJson(user)).apply()

                    currentScreen = "parent"

                },

                onRegisterClick = {

                    currentScreen = "register"

                },

                onParentRegisterClick = {

                    currentScreen = "parentRegister"

                }

            )


        }


        "register" -> {


            RegisterPage(

                onBack = {

                    currentScreen = "login"

                }

            )


        }

        "parentRegister" -> {

            ParentRegisterPage(onBack = { currentScreen = "login" })

        }

        "parent" -> {

            BackHandler {
                val now = System.currentTimeMillis()
                if (now - lastParentBackTime < 2_000) {
                    currentUser = null
                    prefs.edit().clear().apply()
                    currentScreen = "login"
                } else {
                    lastParentBackTime = now
                    Toast.makeText(context, "한 번 더 누르면 로그아웃합니다.", Toast.LENGTH_SHORT).show()
                }
            }

            currentUser?.let { user ->
                ParentPage(user = user, onLogout = {
                    currentUser = null
                    prefs.edit().clear().apply()
                    currentScreen = "login"
                })
            }

        }

        "student" -> {


            currentUser?.let {


                StudentPage(

                    user = it,


                    onAttendanceClick = {

                        currentScreen = "attendance"

                    },


                    onNotificationClick = {

                        currentScreen = "notification"

                    },


                    onLogout = {

                        currentUser = null

                        prefs.edit()
                            .clear()
                            .apply()

                        currentScreen = "login"

                    }

                )


            }


        }

        "attendance" -> {


            currentUser?.let {


                StudentAttendancePage(

                    user = it,


                    onBack = {

                        currentScreen = "student"

                    }

                )


            }


        }

        "notification" -> {


            currentUser?.let {


                StudentNotificationPage(

                    user = it,


                    onBack = {

                        currentScreen = "student"

                    }

                )


            }


        }


        "teacher" -> {

            currentUser?.let { user ->


                if(user.firstLogin){

                    TeacherFirstLoginPage(

                        user = user,

                        onComplete = {

                            currentUser = it


                            prefs.edit()
                                .putString(
                                    "user",
                                    Gson().toJson(it)
                                )
                                .apply()


                            currentScreen = "teacher"

                        }

                    )


                }else{


                    TeacherPage(

                        nfcTag = nfcTag,

                        clearNfc = clearNfc,

                        teacherUid = user.uid,

                        teacherName = user.name,

                        onLogout = {

                            currentUser = null

                            prefs.edit()
                                .clear()
                                .apply()

                            currentScreen = "login"

                        }

                    )


                }


            }

        }

        "admin" -> {



            BackHandler {


                val now =
                    System.currentTimeMillis()



                if(now - lastAdminBackTime < 2000){


                    currentUser = null

                    selectedStudent = null


                    prefs.edit()
                        .clear()
                        .apply()


                    currentScreen = "login"


                }else{


                    lastAdminBackTime = now



                    Toast.makeText(

                        context,

                        "뒤로가기를 한 번 더 누르면 로그인 화면으로 이동합니다",

                        Toast.LENGTH_SHORT

                    ).show()


                }



            }


            AdminPage(



                onStudentRegisterClick = {

                    currentScreen = "studentPreRegister"

                },


                onUserManageClick = {

                    currentScreen = "adminUserManage"

                },


                onStudentManageClick = {

                    currentScreen = "adminStudentManage"

                },


                onTeacherRegisterClick = {

                    currentScreen = "teacherRegister"

                },


                onLogClick = {

                    currentScreen = "adminLog"

                },


                onAttendanceAllClick = {

                    currentScreen = "adminAttendance"

                },


                onStudentAttendanceClick = {

                    currentScreen = "adminStudentAttendance"

                },

                onPreStudentClick = {

                    currentScreen = "adminPreStudent"

                },

                onDataAuditClick = {

                    currentScreen = "adminDataAudit"

                },

                onHolidayManageClick = {

                    currentScreen = "holidayManage"

                },

                onAnnouncementClick = {

                    currentScreen = "adminAnnouncement"

                },

                onAcademySettingsClick = {

                    currentScreen = "academySettings"

                },

                onLogout = {


                    currentUser = null

                    selectedStudent = null


                    prefs.edit()
                        .clear()
                        .apply()


                    currentScreen = "login"


                }


            )


        }
        "adminUserManage" -> {


            AdminUserManagePage(


                onBack = {

                    currentScreen = "admin"

                },


                onStudentManageClick = { student ->


                    selectedStudent = student

                    previousAdminScreen = "adminUserManage"

                    currentScreen = "studentProfileEdit"


                }


            )


        }

        "studentDetail" -> {


            selectedStudent?.let { student ->



                AdminStudentAttendanceDetailPage(


                    student = student,


                    onBack = {


                        selectedStudent = null

                        currentScreen = previousAdminScreen


                    }


                )


            }


        }


        "adminStudentManage" -> {


            TeacherStudentSelectPage(


                mode = "student",

                teacherUid = "",

                teacherName = "",


                onBack = {

                    currentScreen = "admin"

                },


                onStudentClick = { student ->


                    selectedStudent = student

                    previousAdminScreen = "adminStudentManage"

                    currentScreen = "studentProfileEdit"


                }


            )


        }
        "studentPreRegister" -> {


            StudentPreRegisterPage(

                onBack = {

                    currentScreen = "admin"

                }

            )


        }

        "adminPreStudent" -> {


            AdminPreStudentPage(

                nfcTag = nfcTag,

                clearNfc = clearNfc,


                onBack = {

                    currentScreen = "admin"

                }

            )


        }

        "studentProfileEdit" -> {

            selectedStudent?.let { student ->

                AdminStudentProfileEditPage(

                    student = student,

                    onBack = {

                        selectedStudent = null

                        currentScreen = previousAdminScreen

                    },

                    onScoreHistoryClick = {

                        currentScreen = "studentScoreHistory"

                    },

                    onParentCommunicationClick = {

                        currentScreen = "parentCommunication"

                    }

                )

            }

        }

        "adminDataAudit" -> {

            AdminDataIntegrityPage(
                onBack = { currentScreen = "admin" }
            )

        }

        "academySettings" -> {

            AdminAcademySettingsPage(

                onBack = { currentScreen = "admin" }

            )

        }

        "studentScoreHistory" -> {

            selectedStudent?.let { student ->

                AdminStudentScoreHistoryPage(

                    student = student,

                    onBack = { currentScreen = "studentProfileEdit" }

                )

            }

        }

        "parentCommunication" -> {

            selectedStudent?.let { student ->

                ParentCommunicationPage(

                    student = student,

                    onBack = { currentScreen = "studentProfileEdit" }

                )

            }

        }

        "holidayManage" -> {

            HolidayManagePage(
                onBack = { currentScreen = "admin" }
            )

        }

        "adminAnnouncement" -> {

            TeacherAnnouncementPage(
                onBack = { currentScreen = "admin" }
            )

        }









        "teacherRegister" -> {


            TeacherRegisterPage(


                onBack = {


                    currentScreen = "admin"


                }


            )


        }









        "adminLog" -> {


            AdminAttendanceLogPage(


                onBack = {


                    currentScreen = "admin"


                }


            )


        }









        "adminAttendance" -> {


            AdminAttendanceAllPage(


                onBack = {


                    currentScreen = "admin"


                }


            )


        }









        "adminStudentAttendance" -> {


            AdminStudentAttendancePage(

                onBack = {

                    selectedAttendanceGrade = null

                    currentScreen = "admin"

                },

                initialGrade = selectedAttendanceGrade,

                onGradeChange = { selectedAttendanceGrade = it },

                onStudentClick = { student ->


                    selectedStudent = student

                    previousAdminScreen = "adminStudentAttendance"

                    currentScreen = "studentDetail"


                }


            )


        }



        else -> {


            Text("페이지 준비중")


        }



    }


}
