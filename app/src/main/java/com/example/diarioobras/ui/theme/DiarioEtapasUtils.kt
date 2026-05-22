package com.example.diarioobras.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale
import java.util.UUID

internal fun criarUriParaFotoEtapa(context: Context): Uri {
    val arquivo = File(
        context.cacheDir,
        "foto_${UUID.randomUUID()}.jpg"
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        arquivo
    )
}

internal fun criarUriParaFotoDiario(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val arquivo = File(imagesDir, "foto_diario_${System.currentTimeMillis()}.jpg")

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        arquivo
    )
}

internal fun formatarDecimalTruncado(valor: Double): String {
    val truncado = kotlin.math.floor(valor * 100.0) / 100.0
    return String.format(Locale.US, "%.2f", truncado).replace(".", ",")
}
