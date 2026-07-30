package com.example.attendanceappfinal.repository

import com.example.attendanceappfinal.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object FirebaseRepository {


    private val auth =
        FirebaseAuth.getInstance()


    private val database =
        FirebaseDatabase.getInstance()
            .reference





    // 로그인

    fun login(
        id:String,
        password:String,
        callback:(Boolean,String?)->Unit
    ){


        auth.signInWithEmailAndPassword(

            "$id@attendance.com",

            password

        )

            .addOnSuccessListener {


                callback(

                    true,

                    auth.currentUser!!.uid

                )


            }

            .addOnFailureListener {


                callback(

                    false,

                    null

                )


            }


    }





    // 학생 사전등록 확인

    fun checkStudentRegister(

        name:String,

        phone:String,

        callback:(Boolean)->Unit

    ){


        database

            .child("preStudents")

            .get()

            .addOnSuccessListener { snapshot ->



                var result=false



                for(child in snapshot.children){


                    val dbName =

                        child.child("name")
                            .value
                            ?.toString()
                            ?: ""



                    val dbPhone =

                        child.child("phone")
                            .value
                            ?.toString()
                            ?: ""




                    if(

                        dbName == name &&

                        dbPhone == phone

                    ){

                        result=true

                        break

                    }


                }



                callback(result)



            }



    }







    // 회원가입

    fun registerStudent(

        id:String,

        password:String,

        name:String,

        phone:String,

        callback:(Boolean,String)->Unit

    ){



        val email =

            "$id@attendance.com"





        checkStudentRegister(

            name,

            phone

        ){valid ->




            if(!valid){


                callback(

                    false,

                    "등록된 학생 정보가 없습니다"

                )


                return@checkStudentRegister


            }






            auth.createUserWithEmailAndPassword(

                email,

                password

            )

                .addOnSuccessListener {



                    val uid =

                        auth.currentUser!!.uid





                    val user = User(

                        uid = uid,

                        name = name,

                        phone = phone,

                        email = email,

                        role = "student"

                    )





                    database

                        .child("users")

                        .child(uid)

                        .setValue(user)

                        .addOnSuccessListener {



                            callback(

                                true,

                                "회원가입 완료"

                            )


                        }



                }


                .addOnFailureListener {


                    callback(

                        false,

                        "가입 실패"

                    )


                }





        }





    }



}
