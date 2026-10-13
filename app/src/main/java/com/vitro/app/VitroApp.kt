package com.vitro.app

import android.app.Application
import com.vitro.app.data.Repository

class VitroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
    }
}
