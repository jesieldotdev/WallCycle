package com.wallcycle.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wallcycle.app.core.WallpaperChanger
import java.util.concurrent.TimeUnit

class ChangeWallpaperWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        if (WallpaperChanger.next(applicationContext)) Result.success() else Result.failure()
}

object Scheduler {
    private const val PERIODIC = "wallcycle_periodic"
    private const val ONE_SHOT = "wallcycle_now"

    /** Agenda (ou cancela, se minutes == 0) a troca automática. Mínimo do Android: 15 min. */
    fun schedule(context: Context, minutes: Int) {
        val wm = WorkManager.getInstance(context)
        if (minutes <= 0) {
            wm.cancelUniqueWork(PERIODIC)
            return
        }
        val request = PeriodicWorkRequestBuilder<ChangeWallpaperWorker>(
            minutes.coerceAtLeast(15).toLong(), TimeUnit.MINUTES
        ).setInitialDelay(minutes.coerceAtLeast(15).toLong(), TimeUnit.MINUTES).build()
        wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Troca imediatamente em segundo plano (usado pelo bloco de Configurações rápidas). */
    fun changeNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_SHOT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ChangeWallpaperWorker>().build()
        )
    }
}
