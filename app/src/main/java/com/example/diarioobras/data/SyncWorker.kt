package com.example.diarioobras.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val repository = DiarioRepository(db)
        val uploadService = FirebaseUploadService()

        val pendentes = db.obrasDao().listarDiariosPendentesSincronizacao()
        for (diario in pendentes) {
            val json = repository.montarDiarioParaJson(diario.id) ?: continue
            val resultado = uploadService.enviarDiario(applicationContext, json)
            if (resultado.isSuccess) {
                repository.marcarDiarioComoSincronizado(diario.id)
            } else {
                return Result.retry()
            }
        }
        return Result.success()
    }
}
