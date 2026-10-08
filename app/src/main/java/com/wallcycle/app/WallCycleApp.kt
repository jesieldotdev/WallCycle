package com.wallcycle.app

import android.app.Application
import com.wallcycle.app.data.Repository

class WallCycleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
    }
}
