package com.example.attendanceappfinal.nfc


import com.example.attendanceappfinal.model.Attendance
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale


object NfcAttendance {


    private val database =
        FirebaseDatabase.getInstance()

    private fun processPreStudentAttendance(

        preStudentId:String,

        studentName:String,

        nfcTag:String,

        teacherUid:String?,

        callback:(String)->Unit

    ){

        val database =
            FirebaseDatabase.getInstance()



        val date =

            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.KOREA
            ).format(Date())



        val time =

            SimpleDateFormat(
                "HH:mm:ss",
                Locale.KOREA
            ).format(Date())



        val attendanceId =
            "${date}_${preStudentId}"



        val attendance = Attendance(


            id = attendanceId,

            studentUid = "pre_$preStudentId",

            studentName = studentName.ifBlank { "이름 없음" },

            date = date,

            time = time,

            status = "출석",

            teacher = "",

            teacherUid = teacherUid ?: "",

            subject = "미가입학생",

            reason = "",

            nfcTag = nfcTag,

            timestamp =
                System.currentTimeMillis()


        )



        database

            .getReference("preAttendance")

            .child(preStudentId)

            .child(attendanceId)

            .setValue(attendance)

            .addOnSuccessListener {


                callback(
                    "${attendance.studentName} 출석 처리 완료(가입 예정 학생)"
                )


            }



    }

    private fun processUnregisteredStudentAttendance(
        unregisteredStudentId:String,
        studentName:String,
        nfcTag:String,
        teacherUid:String?,
        callback:(String)->Unit
    ){
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
        val time = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date())
        val attendanceId = "${date}_${unregisteredStudentId}"
        val attendance = Attendance(
            id = attendanceId,
            studentUid = "un_$unregisteredStudentId",
            studentName = studentName.ifBlank { "이름 없음" },
            date = date,
            time = time,
            status = "출석",
            teacherUid = teacherUid ?: "",
            subject = "미가입 학생",
            nfcTag = nfcTag,
            timestamp = System.currentTimeMillis()
        )

        database.getReference("unregisteredAttendance")
            .child(unregisteredStudentId)
            .child(attendanceId)
            .setValue(attendance)
            .addOnSuccessListener { callback("${attendance.studentName} 출석 처리 완료(미가입 학생)") }
            .addOnFailureListener { callback("출석 저장 실패: ${it.message ?: "알 수 없는 오류"}") }
    }

    fun processNfcAttendance(
        nfcTag:String,
        teacherUid:String? = null,
        subject:String = "",
        onResult:(String)->Unit
    ){


        if(nfcTag.isBlank()){

            onResult("NFC 태그가 없습니다")

            return

        }



        database

            .getReference("nfc_tags")

            .child(nfcTag)

            .get()

            .addOnSuccessListener { tagSnapshot ->



                if(!tagSnapshot.exists()){


                    onResult(
                        "등록되지 않은 NFC입니다"
                    )


                    return@addOnSuccessListener

                }





                val studentUid =

                    tagSnapshot.child("studentUid")
                        .getValue(String::class.java)
                        ?: ""


                val studentName =

                    tagSnapshot.child("name")
                        .getValue(String::class.java)
                        ?: ""


                val preStudentId =

                    tagSnapshot.child("preStudentId")
                        .getValue(String::class.java)
                        ?: ""

                val unregisteredStudentId =
                    tagSnapshot.child("unregisteredStudentId")
                        .getValue(String::class.java)
                        ?: ""



                if(studentUid.isBlank()){

                    if(unregisteredStudentId.isNotBlank()){
                        processUnregisteredStudentAttendance(
                            unregisteredStudentId,
                            studentName,
                            nfcTag,
                            teacherUid
                        ){ result ->
                            onResult(result)
                        }
                        return@addOnSuccessListener
                    }


                    if(preStudentId.isBlank()){


                        onResult(
                            "학생 정보를 찾을 수 없습니다"
                        )


                        return@addOnSuccessListener


                    }



                    processPreStudentAttendance(

                        preStudentId,

                        studentName,

                        nfcTag,

                        teacherUid

                    ){ result ->


                        onResult(result)


                    }



                    return@addOnSuccessListener


                }


                findTeacherName(

                    teacherUid

                ){ teacherName ->


                    findSubjectByTimetable(studentUid){ timetable ->

                        if(timetable.subject.isBlank()){

                            onResult("현재 시간표가 없습니다")

                        }else{

                            saveAttendance(

                                studentUid,
                                studentName,
                                nfcTag,
                                teacherUid,
                                teacherName,
                                timetable.subject,
                                timetable.startTime,
                                timetable.endTime

                            ){ result ->

                                onResult(result)

                            }

                        }

                    }



                }



            }



    }







    private fun findSubjectByTimetable(

        uid:String,

        callback:(TimetableInfo)->Unit

    ){



        val calendar =
            Calendar.getInstance()



        val day = when(

            calendar.get(Calendar.DAY_OF_WEEK)

        ){

            Calendar.MONDAY -> "월"
            Calendar.TUESDAY -> "화"
            Calendar.WEDNESDAY -> "수"
            Calendar.THURSDAY -> "목"
            Calendar.FRIDAY -> "금"
            Calendar.SATURDAY -> "토"

            else -> ""

        }





        val now =

            SimpleDateFormat(

                "HH:mm",

                Locale.KOREA

            )
                .format(Date())







        database

            .getReference("timetable")

            .child(uid)

            .get()

            .addOnSuccessListener { snapshot ->



                var result =

                    TimetableInfo(

                        subject = "",

                        startTime = ""

                    )





                for(child in snapshot.children){



                    val itemDay =

                        child.child("day")

                            .getValue(String::class.java)

                            ?: ""




                    val start =

                        child.child("startTime")

                            .getValue(String::class.java)

                            ?: ""





                    val end =

                        child.child("endTime")

                            .getValue(String::class.java)

                            ?: ""





                    val itemSubject =

                        child.child("subject")

                            .getValue(String::class.java)

                            ?: ""






                    if (itemDay == day && isNfcAttendanceTime(now, start, end)) {


                        result = TimetableInfo(

                            subject = itemSubject,

                            startTime = start,

                            endTime = end

                        )


                        break


                    }



                }




                callback(result)



            }

            .addOnFailureListener {


                callback(

                    TimetableInfo(

                        "자습",

                        ""

                    )

                )


            }



    }









    private fun saveAttendance(

        studentUid:String,

        studentName:String,

        nfcTag:String,

        teacherUid:String?,

        teacherName:String,

        subject:String,

        startTime:String,

        endTime:String,

        callback:(String)->Unit

    ){





        val date =

            SimpleDateFormat(

                "yyyy-MM-dd",

                Locale.KOREA

            )
                .format(Date())





        val time =

            SimpleDateFormat(

                "HH:mm:ss",

                Locale.KOREA

            )
                .format(Date())







        val attendanceId =

            "${date}_${studentUid}_${subject}"







        val ref =

            database

                .getReference("attendance")

                .child(studentUid)

                .child(attendanceId)







        ref.get()

            .addOnSuccessListener { old ->



                if(old.exists()){


                    callback(

                        "${studentName} 이미 출석 처리됨"

                    )


                    return@addOnSuccessListener


                }






                val status =

                    checkStatus(startTime)







                val attendance = Attendance(



                    id = attendanceId,


                    studentUid = studentUid,


                    studentName = studentName,


                    date = date,


                    time = time,


                    status = status,


                    teacher = teacherName,


                    teacherUid = teacherUid ?: "",


                    subject = subject,


                    reason = "",


                    nfcTag = nfcTag,


                    timestamp = System.currentTimeMillis()



                )







                ref.setValue(attendance)

                    .addOnSuccessListener {



                        callback(

                            "${studentName} ${subject} ${status} 처리 완료"

                        )


                    }



            }





    }


    private fun findTeacherName(

        uid:String?,

        callback:(String)->Unit

    ){

        if(uid.isNullOrBlank()){

            callback("")

            return

        }


        database

            .getReference("users")

            .child(uid)

            .get()

            .addOnSuccessListener { snapshot ->


                val name =

                    snapshot.child("name")

                        .getValue(String::class.java)

                        ?: ""


                callback(name)


            }

            .addOnFailureListener {


                callback("")


            }


    }





    private fun checkStatus(

        startTime:String

    ):String{



        if(startTime.isBlank()){

            return "출석"

        }



        try{


            val formatter =

                SimpleDateFormat(

                    "HH:mm",

                    Locale.KOREA

                )



            val start =

                formatter.parse(startTime)



            val now =

                formatter.parse(

                    formatter.format(Date())

                )



            if(start == null || now == null){
                return "출석"
            }

            val diff =

                (now.time - start.time) / 60000





            return if(diff <= 10){

                "출석"

            }else{

                "지각"

            }





        }catch(e:Exception){


            return "출석"


        }



    }




    private data class TimetableInfo(

        val subject:String,

        val startTime:String,

        val endTime:String = ""

    )

    /** NFC is available from 30 minutes before class until the lesson ends. */
    private fun isNfcAttendanceTime(now: String, start: String, end: String): Boolean = try {
        val nowTime = LocalTime.parse(now)
        val startTime = LocalTime.parse(start)
        val endTime = LocalTime.parse(end)
        !nowTime.isBefore(startTime.minusMinutes(30)) && nowTime.isBefore(endTime)
    } catch (_: Exception) {
        false
    }



}
