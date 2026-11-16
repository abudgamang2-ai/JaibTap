package com.jaibtap.app

import android.app.Application
import com.jaibtap.app.data.DataManager

class JaibTapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // تهيئة مدير البيانات والمحاكاة
        DataManager.init(this)
    }
}
