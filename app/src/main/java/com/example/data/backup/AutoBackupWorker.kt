package com.example.data.backup

import android.content.Context
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.AzadTaillorsApp
import com.example.data.preferences.BackupSchedule
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AzadTaillorsApp ?: return Result.failure()
        val backupManager = app.container.backupManager
        val outcome = backupManager.createLocalBackup(BackupKind.AUTO)
        return if (outcome.isSuccess) {
            Result.success()
        } else {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "azad_taillors_auto_backup"

        fun schedule(context: Context, schedule: BackupSchedule) {
            val appCtx = context.applicationContext
            if (!WorkManager.isInitialized()) {
                val config = (appCtx as? Configuration.Provider)?.workManagerConfiguration
                    ?: Configuration.Builder().build()
                runCatching { WorkManager.initialize(appCtx, config) }
            }

            val workManager = WorkManager.getInstance(appCtx)
            if (schedule == BackupSchedule.OFF) {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                schedule.hours,
                TimeUnit.HOURS
            ).build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
