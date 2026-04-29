package com.smartvoice.assistant

import android.app.Application
import com.smartvoice.assistant.data.local.AppDatabase

/**
 * Application class for Smart Voice Assistant.
 * Initializes global singletons and app-wide resources.
 */
class SmartVoiceApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: SmartVoiceApp
            private set
    }
}
