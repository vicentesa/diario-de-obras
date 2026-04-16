package com.example.diarioobras.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Aqui entraremos depois com a lógica real de sincronização:
        // 1. buscar registros pendentes no banco
        // 2. enviar para API quando houver internet
        // 3. marcar como sincronizado

        return Result.success()
    }
}