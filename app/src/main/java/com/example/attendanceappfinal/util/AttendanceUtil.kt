package com.example.attendanceappfinal.util

import com.example.attendanceappfinal.model.Attendance


fun calculateAttendanceRate(
    list:List<Attendance>
):Int{


    if(list.isEmpty())
        return 0



    val present =

        list.count {

            it.status == "출석" ||
                    it.status == "지각"

        }



    return (

            present * 100

                    /

                    list.size

            )


}





fun countStatus(
    list:List<Attendance>,
    status:String
):Int{


    return list.count {

        it.status == status

    }

}