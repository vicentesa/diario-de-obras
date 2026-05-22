package com.example.diarioobras.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun comprimirFoto(context: Context, uri: Uri, larguraMaxima: Int = 1280, qualidade: Int = 75) {
    withContext(Dispatchers.IO) {
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: return@withContext

            val bitmapFinal = if (bitmap.width > larguraMaxima) {
                val escala = larguraMaxima.toFloat() / bitmap.width
                val novaAltura = (bitmap.height * escala).toInt()
                Bitmap.createScaledBitmap(bitmap, larguraMaxima, novaAltura, true)
                    .also { if (it !== bitmap) bitmap.recycle() }
            } else {
                bitmap
            }

            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                bitmapFinal.compress(Bitmap.CompressFormat.JPEG, qualidade, out)
            }
            bitmapFinal.recycle()
        } catch (e: Exception) {
            Log.e("FotoUtils", "Erro ao comprimir foto: ${e.message}", e)
        }
    }
}

suspend fun salvarFotoNaGaleria(context: Context, uri: Uri) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Log.e("FotoUtils", "Não foi possível ler a foto: uri=$uri")
                return@withContext
            }

            val nomeArquivo = "DiarioObras_${System.currentTimeMillis()}.jpg"
            val valores = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, nomeArquivo)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Diario de Obras")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val colecao = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = context.contentResolver.insert(colecao, valores)
            if (itemUri == null) {
                Log.e("FotoUtils", "MediaStore insert retornou null")
                return@withContext
            }

            context.contentResolver.openOutputStream(itemUri)?.use { it.write(bytes) }

            valores.clear()
            valores.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(itemUri, valores, null, null)
        } catch (e: Exception) {
            Log.e("FotoUtils", "Erro ao salvar foto na galeria: ${e.message}", e)
        }
    }
}
