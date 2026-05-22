package com.example.diarioobras.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
private const val TAG = "FirebaseUpload"

class FirebaseUploadService {

    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun enviarDiario(context: Context, jsonExport: DiarioJsonExport): Result<Unit> = runCatching {
        if (!isNetworkAvailable(context)) {
            throw Exception("Sem conexão com a internet")
        }
        withTimeout(120_000L) {
            val obraNome = jsonExport.obra.nome.normalizar()
            val data = jsonExport.data.replace("/", "-")
            val docId = "${obraNome}_${data}"
            val pasta = "fotos/$obraNome/$data"

            val todosCaminhos = coletarCaminhos(jsonExport)
            Log.d(TAG, "=== INÍCIO DO UPLOAD ===")
            Log.d(TAG, "docId=$docId | pasta=$pasta")
            Log.d(TAG, "Total de entradas de foto (antes do distinct/filter): ${todosCaminhos.size}")
            todosCaminhos.forEachIndexed { i, c ->
                Log.d(TAG, "  [$i] \"$c\"")
            }

            val caminhos = todosCaminhos.distinct().filter { it.isNotBlank() }
            Log.d(TAG, "Caminhos únicos não-vazios: ${caminhos.size}")

            val urlMap = mutableMapOf<String, String>()
            caminhos.forEach { caminho ->
                val url = uploadFoto(context, caminho, pasta)
                if (url != null) urlMap[caminho] = url
            }

            Log.d(TAG, "URLs geradas com sucesso: ${urlMap.size}/${caminhos.size}")

            val jsonAtualizado = substituirCaminhos(jsonExport, urlMap)

            @Suppress("UNCHECKED_CAST")
            val mapa = Gson().let { gson ->
                gson.fromJson(gson.toJson(jsonAtualizado), Map::class.java) as Map<String, Any>
            }
            firestore.collection("diarios").document(docId).set(mapa).await()
            Log.d(TAG, "=== Firestore OK: $docId ===")
        }
    }

    private suspend fun uploadFoto(context: Context, caminho: String, pasta: String): String? {
        Log.d(TAG, "uploadFoto: caminho=\"$caminho\"")

        val uri = runCatching { Uri.parse(caminho) }.getOrElse {
            Log.e(TAG, "  ERRO ao fazer parse do URI: ${it.message}")
            return null
        }
        Log.d(TAG, "  Uri.scheme=${uri.scheme} | Uri.path=${uri.path}")

        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.w(TAG, "  SKIP: ContentResolver.openInputStream retornou null")
                return null
            }
            val nomeArquivo = uri.lastPathSegment?.substringAfterLast('/')
                ?.ifBlank { null }
                ?: "foto_${System.currentTimeMillis()}.jpg"
            val ref = storage.reference.child("$pasta/$nomeArquivo")
            Log.d(TAG, "  Storage ref: ${ref.path}")
            inputStream.use { ref.putStream(it).await() }
            val url = ref.downloadUrl.await().toString()
            Log.d(TAG, "  SUCESSO: url=$url")
            url
        } catch (e: Exception) {
            Log.e(TAG, "  ERRO em uploadFoto: ${e::class.simpleName}: ${e.message}", e)
            null
        }
    }

    private fun coletarCaminhos(json: DiarioJsonExport): List<String> = buildList {
        json.carregamentos.forEach { add(it.fotoTicket) }
        json.abastecimentos.forEach { add(it.fotoTicket) }
        json.desvios.forEach { if (it.fotoTicket.isNotBlank()) add(it.fotoTicket) }
        json.servicos.forEach { s ->
            s.fotoAntes?.let { add(it) }
            s.fotoCavaAberta?.let { add(it) }
            s.fotoEspessura?.let { add(it) }
            s.fotoCompactacao?.let { add(it) }
            s.fotoConclusao?.let { add(it) }
            s.areas.forEach { a ->
                a.fotoDim1?.let { add(it) }
                a.fotoDim2?.let { add(it) }
            }
        }
        if (json.retornoBase.fotoHospedagem.isNotBlank()) add(json.retornoBase.fotoHospedagem)
    }

    private fun substituirCaminhos(
        json: DiarioJsonExport,
        urlMap: Map<String, String>
    ): DiarioJsonExport {
        fun String?.sub() = this?.let { urlMap[it] ?: it }
        return json.copy(
            carregamentos = json.carregamentos.map { c ->
                c.copy(fotoTicket = urlMap[c.fotoTicket] ?: c.fotoTicket)
            },
            servicos = json.servicos.map { s ->
                s.copy(
                    fotoAntes = s.fotoAntes.sub(),
                    fotoCavaAberta = s.fotoCavaAberta.sub(),
                    fotoEspessura = s.fotoEspessura.sub(),
                    fotoCompactacao = s.fotoCompactacao.sub(),
                    fotoConclusao = s.fotoConclusao.sub(),
                    areas = s.areas.map { a ->
                        a.copy(
                            fotoDim1 = a.fotoDim1.sub(),
                            fotoDim2 = a.fotoDim2.sub()
                        )
                    }
                )
            },
            retornoBase = json.retornoBase.copy(
                fotoHospedagem = urlMap[json.retornoBase.fotoHospedagem]
                    ?: json.retornoBase.fotoHospedagem
            )
        )
    }

    private fun String.normalizar(): String =
        lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
}
