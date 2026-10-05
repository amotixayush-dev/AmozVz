package com.amozvz.app

import android.app.Application
import com.amozvz.app.data.PreferencesManager
import com.amozvz.app.engine.AmozVzEngine
import com.amozvz.app.utils.NotificationHelper

class AmozVzApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var engine: AmozVzEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
        engine = AmozVzEngine(this)
        NotificationHelper.createNotificationChannel(this)
    }

    companion object {
        lateinit var instance: AmozVzApplication
            private set
    }
}
