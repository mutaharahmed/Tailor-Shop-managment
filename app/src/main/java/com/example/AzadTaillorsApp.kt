package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.data.backup.AutoBackupWorker
import com.example.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AzadTaillorsApp : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            runCatching {
                val settings = container.preferencesRepository.settingsFlow.first()
                AutoBackupWorker.schedule(this@AzadTaillorsApp, settings.backupSchedule)
            }
        }
    }
}
