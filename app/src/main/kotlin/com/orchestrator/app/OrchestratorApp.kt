package com.orchestrator.app

import android.app.Application
import com.orchestrator.app.notification.TaskAlarmReceiver
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OrchestratorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TaskAlarmReceiver.createNotificationChannel(this)
    }
}
