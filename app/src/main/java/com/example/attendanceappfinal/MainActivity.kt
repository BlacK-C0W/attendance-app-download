package com.example.attendanceappfinal

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.attendanceappfinal.navigation.AppNavigation
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity() {


    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {

        }



    private var nfcAdapter: NfcAdapter? = null



    private var nfcTagId by mutableStateOf<String?>(null)



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)



        if(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ){

            if(
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                != PackageManager.PERMISSION_GRANTED
            ){

                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )

            }

        }





        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { task ->


                if(!task.isSuccessful){

                    println("FCM TOKEN ERROR")

                    return@addOnCompleteListener

                }


                println(
                    "FCM TOKEN : ${task.result}"
                )


            }





        nfcAdapter =
            NfcAdapter.getDefaultAdapter(this)



        handleNfc(intent)





        setContent {


            AppNavigation(

                nfcTag = nfcTagId,

                clearNfc = {

                    nfcTagId = null

                }

            )


        }



    }





    override fun onResume(){

        super.onResume()



        val intent = Intent(

            this,

            javaClass

        ).addFlags(

            Intent.FLAG_ACTIVITY_SINGLE_TOP

        )




        val pendingIntent =

            PendingIntent.getActivity(

                this,

                0,

                intent,

                PendingIntent.FLAG_MUTABLE

            )




        nfcAdapter?.enableForegroundDispatch(

            this,

            pendingIntent,

            null,

            null

        )


    }





    override fun onPause(){

        super.onPause()


        nfcAdapter?.disableForegroundDispatch(this)


    }





    override fun onNewIntent(intent: Intent){


        super.onNewIntent(intent)


        handleNfc(intent)


    }





    private fun handleNfc(intent: Intent){


        when(intent.action){



            NfcAdapter.ACTION_TAG_DISCOVERED,

            NfcAdapter.ACTION_TECH_DISCOVERED,

            NfcAdapter.ACTION_NDEF_DISCOVERED -> {



                val tag =

                    intent.getParcelableExtra<Tag>(

                        NfcAdapter.EXTRA_TAG

                    )



                nfcTagId =

                    tag?.id?.joinToString("") {


                        "%02X".format(it)

                    }



                println(

                    "NFC READ : $nfcTagId"

                )



            }


        }



    }



}