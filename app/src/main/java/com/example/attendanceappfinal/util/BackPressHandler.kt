package com.example.attendanceappfinal.util


import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext


@Composable
fun DoubleBackToLogin(

    onLogin: () -> Unit

){


    val context = LocalContext.current


    var backPressed by remember {

        mutableStateOf(false)

    }



    LaunchedEffect(backPressed){

        if(backPressed){


            kotlinx.coroutines.delay(2000)


            backPressed = false


        }


    }





    BackHandler {


        if(backPressed){


            onLogin()


        }else{


            backPressed = true


            Toast.makeText(

                context,

                "한 번 더 누르면 로그인 화면으로 이동합니다",

                Toast.LENGTH_SHORT

            ).show()


        }



    }



}