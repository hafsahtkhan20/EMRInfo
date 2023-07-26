package com.example.emrinfo
import android.app.Application


object MyApplication : Application() {
     var token = ""
        get() = field
        set(value) {field = value}

    var jwt = ""
        get() = field
        set(value) {field = value }

    var patientid = ""
        get() = field
        set(value) {field = value}
}