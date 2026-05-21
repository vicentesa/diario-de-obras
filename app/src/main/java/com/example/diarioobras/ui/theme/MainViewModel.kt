package com.example.diarioobras.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.diarioobras.data.AbastecimentoItemEntity
import com.example.diarioobras.data.AppDatabase
import com.example.diarioobras.data.CarregamentoItemEntity
import com.example.diarioobras.data.DeslocamentoItemEntity
import com.example.diarioobras.data.DesvioItemEntity
import com.example.diarioobras.data.DiarioExportacao
import com.example.diarioobras.data.DiarioRelatorio
import com.example.diarioobras.data.DiarioRepository
import com.example.diarioobras.data.ServicoAreaEntity
import com.example.diarioobras.data.ServicoEntity
import com.example.diarioobras.data.StatusEtapa
import com.example.diarioobras.data.SubservicoEntity
import com.example.diarioobras.data.FirebaseUploadService
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class UploadEstado {
    object Ocioso : UploadEstado()
    object Enviando : UploadEstado()
    object Sucesso : UploadEstado()
    data class Erro(val mensagem: String) : UploadEstado()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DiarioRepository(AppDatabase.getInstance(application))
    private val uploadService = FirebaseUploadService()

    private val _uploadEstado = MutableStateFlow<UploadEstado>(UploadEstado.Ocioso)
    val uploadEstado: StateFlow<UploadEstado> = _uploadEstado.asStateFlow()

    fun resetarUploadEstado() { _uploadEstado.value = UploadEstado.Ocioso }

    // ── Leituras (suspend) ───────────────────────────────────────────────

    suspend fun buscarDiarioPorId(id: Long) = repository.buscarDiarioPorId(id)
    suspend fun buscarObraPorId(id: Long) = repository.buscarObraPorId(id)
    suspend fun buscarServicoPorId(id: Long) = repository.buscarServicoPorId(id)
    suspend fun buscarDiarioCompleto(id: Long) = repository.buscarDiarioCompleto(id)
    suspend fun buscarServicoComAreasPorId(id: Long) = repository.buscarServicoComAreasPorId(id)
    suspend fun listarServicoAreasDireto(id: Long) = repository.listarServicoAreasDireto(id)
    suspend fun buscarDesvioPorId(id: Long) = repository.buscarDesvioPorId(id)
    suspend fun substituirAreasDoServico(servicoId: Long, areas: List<ServicoAreaEntity>) =
        repository.substituirAreasDoServico(servicoId, areas)
    suspend fun salvarServicoCompleto(servico: ServicoEntity, areas: List<ServicoAreaEntity>) =
        repository.salvarServicoCompleto(servico, areas)
    suspend fun salvarServicoRetornandoId(servico: ServicoEntity) =
        repository.salvarServicoRetornandoId(servico)
    suspend fun atualizarServicoSuspend(servico: ServicoEntity) =
        repository.atualizarServicoSuspend(servico)
    suspend fun montarServicosParaExportacao(diarioId: Long) =
        repository.montarServicosParaExportacao(diarioId)
    suspend fun montarDiarioParaExportacao(diarioId: Long) =
        repository.montarDiarioParaExportacao(diarioId)
    suspend fun montarDiarioParaRelatorio(diarioId: Long) =
        repository.montarDiarioParaRelatorio(diarioId)

    // ── StateFlows ───────────────────────────────────────────────────────

    val obras = repository.listarObras()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun diariosDaObra(obraId: Long) =
        repository.listarDiariosDaObra(obraId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deslocamentosDoDiario(diarioId: Long) =
        repository.listarDeslocamentos(diarioId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun carregamentosDoDiario(diarioId: Long) =
        repository.listarCarregamentos(diarioId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun servicosDoDiario(diarioId: Long) =
        repository.listarServicos(diarioId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun desviosDoDiario(diarioId: Long) =
        repository.listarDesvios(diarioId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun subservicosDoServico(servicoId: Long) =
        repository.listarSubservicosDoServico(servicoId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun servicoAreasDoServico(servicoId: Long) =
        repository.listarServicoAreas(servicoId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun buscarDiarioFlow(diarioId: Long) = repository.buscarDiarioFlow(diarioId)

    fun abastecimentosDoDiario(diarioId: Long) =
        repository.listarAbastecimentos(diarioId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Wrappers fire-and-forget ─────────────────────────────────────────

    fun adicionarObra(
        nome: String, local: String, contratante: String,
        contrato: String, dataInicioContrato: String, prazoContratoDias: Int,
        espessuraContratoCm: Double = 0.0
    ) {
        if (nome.isBlank()) return
        viewModelScope.launch {
            repository.inserirObra(nome, local, contratante, contrato, dataInicioContrato, prazoContratoDias, espessuraContratoCm)
        }
    }

    fun atualizarObra(obra: com.example.diarioobras.data.ObraEntity) {
        viewModelScope.launch { repository.atualizarObra(obra) }
    }

    fun criarNovoDiario(obraId: Long, onCriado: (Long) -> Unit, onJaExiste: (String) -> Unit) {
        viewModelScope.launch {
            val diarioId = repository.criarNovoDiario(obraId)
            onCriado(diarioId)
        }
    }

    fun excluirServico(servico: ServicoEntity) {
        viewModelScope.launch { repository.excluirServico(servico) }
    }

    fun encerrarServicos(diarioId: Long, proximoDestino: String) {
        viewModelScope.launch { repository.encerrarServicos(diarioId, proximoDestino) }
    }

    fun atualizarEquipeDiario(
        diarioId: Long, encarregado: String, equipe: List<String>,
        veiculo: String, equipamentosAuxiliares: List<String>
    ) {
        viewModelScope.launch {
            repository.atualizarEquipeDiario(diarioId, encarregado, equipe, veiculo, equipamentosAuxiliares)
        }
    }

    fun atualizarEquipamentoDiario(
        diarioId: Long, veiculo: String,
        equipamentosAuxiliares: List<String>, equipamentosCompactacao: List<String>
    ) {
        viewModelScope.launch {
            repository.atualizarEquipamentoDiario(diarioId, veiculo, equipamentosAuxiliares, equipamentosCompactacao)
        }
    }

    fun adicionarCarregamento(
        diarioId: Long, veiculo: String,
        chegadaUsina: String?, inicioCarregamento: String?,
        fimCarregamento: String?, horarioPesagem: String?,
        saidaUsinaTrecho: String?, localCarregamento: String,
        pesoLiquidoTon: String, fotoTicketUri: String
    ) {
        viewModelScope.launch {
            repository.adicionarCarregamento(
                diarioId, veiculo, chegadaUsina, inicioCarregamento,
                fimCarregamento, horarioPesagem, saidaUsinaTrecho,
                localCarregamento, pesoLiquidoTon, fotoTicketUri
            )
        }
    }

    fun excluirCarregamento(item: CarregamentoItemEntity) {
        viewModelScope.launch { repository.excluirCarregamento(item) }
    }

    fun salvarCarregamentoEtapa3(
        diarioId: Long, veiculo: String, pesoLiquidoTon: String, fotoTicketUri: String,
        latitude: Double, longitude: Double
    ) {
        viewModelScope.launch {
            repository.salvarCarregamentoEtapa3(diarioId, veiculo, pesoLiquidoTon, fotoTicketUri, latitude, longitude)
        }
    }

    fun marcarInicio(item: DeslocamentoItemEntity) {
        viewModelScope.launch { repository.marcarInicio(item) }
    }

    fun marcarFim(item: DeslocamentoItemEntity) {
        viewModelScope.launch { repository.marcarFim(item) }
    }

    fun atualizarHorarioManual(item: DeslocamentoItemEntity, novoInicio: String?, novoFim: String?) {
        viewModelScope.launch { repository.atualizarHorarioManual(item, novoInicio, novoFim) }
    }

    fun adicionarServico(
        diarioId: Long, ordemServico: Int, numeroProtocolo: String, endereco: String,
        comprimento: Double, largura: Double, altura: Double,
        inicio: String? = null, fim: String? = null,
        latitude: Double? = null, longitude: Double? = null, fotoUri: String? = null
    ) {
        viewModelScope.launch {
            repository.adicionarServico(
                diarioId, ordemServico, numeroProtocolo, endereco,
                comprimento, largura, altura, inicio, fim, latitude, longitude, fotoUri
            )
        }
    }

    fun concluirFechamentoServicos(
        diarioId: Long, inicioIntervalo: String?, fimIntervalo: String?,
        observacaoIntervalo: String, intervaloRegistrado: Boolean,
        horarioFechamentoServicos: String?, observacaoFechamentoServicos: String
    ) {
        viewModelScope.launch {
            repository.concluirFechamentoServicos(
                diarioId, inicioIntervalo, fimIntervalo, observacaoIntervalo,
                intervaloRegistrado, horarioFechamentoServicos, observacaoFechamentoServicos
            )
        }
    }

    fun marcarSaidaEtapa6(diarioId: Long) {
        viewModelScope.launch { repository.marcarSaidaEtapa6(diarioId) }
    }

    fun marcarChegadaEtapa6(diarioId: Long) {
        viewModelScope.launch { repository.marcarChegadaEtapa6(diarioId) }
    }

    fun concluirRetornoBase(
        diarioId: Long, saidaRetornoBase: String?,
        chegadaBase: String?, observacaoRetornoBase: String
    ) {
        viewModelScope.launch {
            repository.concluirRetornoBase(diarioId, saidaRetornoBase, chegadaBase, observacaoRetornoBase)
        }
    }

    fun atualizarObservacaoDesvio(id: Long, texto: String) {
        viewModelScope.launch { repository.atualizarObservacaoDesvio(id, texto) }
    }

    fun atualizarFotoDesvio(id: Long, fotoUri: String) {
        viewModelScope.launch { repository.atualizarFotoDesvio(id, fotoUri) }
    }

    fun concluirFechamentoDo(
        diarioId: Long, observacaoFinalDo: String, horarioPontoCidade: String? = null
    ) {
        viewModelScope.launch {
            repository.concluirFechamentoDo(diarioId, observacaoFinalDo, horarioPontoCidade)
            val jsonExport = repository.montarDiarioParaJson(diarioId)
            if (jsonExport == null) {
                _uploadEstado.value = UploadEstado.Erro("Não foi possível montar o JSON do diário")
                return@launch
            }
            _uploadEstado.value = UploadEstado.Enviando
            val resultado = uploadService.enviarDiario(getApplication(), jsonExport)
            if (resultado.isSuccess) {
                _uploadEstado.value = UploadEstado.Sucesso
                repository.marcarDiarioComoSincronizado(diarioId)
            } else {
                _uploadEstado.value = UploadEstado.Erro(resultado.exceptionOrNull()?.message ?: "Erro ao enviar")
                com.example.diarioobras.data.SyncScheduler.agendarUmaVez(getApplication())
            }
        }
    }

    fun adicionarDesvioCompleto(
        diarioId: Long, codigo: String, descricao: String,
        inicio: String, fim: String, observacao: String,
        litros: Double = 0.0, fotoTicketUri: String = "",
        latitude: Double = 0.0, longitude: Double = 0.0,
        endereco: String = ""
    ) {
        viewModelScope.launch {
            repository.adicionarDesvioCompleto(
                diarioId, codigo, descricao, inicio, fim, observacao,
                litros, fotoTicketUri, latitude, longitude, endereco
            )
        }
    }

    fun marcarInicioDesvio(item: DesvioItemEntity) {
        viewModelScope.launch { repository.marcarInicioDesvio(item) }
    }

    fun marcarFimDesvio(item: DesvioItemEntity) {
        viewModelScope.launch { repository.marcarFimDesvio(item) }
    }

    fun atualizarHorarioDesvio(item: DesvioItemEntity, novoInicio: String?, novoFim: String?) {
        viewModelScope.launch { repository.atualizarHorarioDesvio(item, novoInicio, novoFim) }
    }

    fun atualizarCarregamentoDiario(
        diarioId: Long, localCarregamento: String,
        pesoLiquidoTon: String, fotoTicketUri: String
    ) {
        viewModelScope.launch {
            repository.atualizarCarregamentoDiario(diarioId, localCarregamento, pesoLiquidoTon, fotoTicketUri)
        }
    }

    fun atualizarServico(servico: ServicoEntity) {
        viewModelScope.launch { repository.atualizarServico(servico) }
    }

    fun adicionarSubservico(
        servicoId: Long, tipo: String,
        comprimento: Double? = null, largura: Double? = null, altura: Double? = null
    ) {
        viewModelScope.launch {
            repository.adicionarSubservico(servicoId, tipo, comprimento, largura, altura)
        }
    }

    fun atualizarSubservico(subservico: SubservicoEntity) {
        viewModelScope.launch { repository.atualizarSubservico(subservico) }
    }

    fun atualizarStatusEtapasDiario(
        diarioId: Long, etapaAtual: Int,
        statusEquipe: StatusEtapa, statusEquipamento: StatusEtapa,
        statusCarregamento: StatusEtapa, statusServicos: StatusEtapa,
        statusFechamentoServicos: StatusEtapa, statusRetornoBase: StatusEtapa,
        statusFechamentoDo: StatusEtapa, diarioFechado: Boolean = false
    ) {
        viewModelScope.launch {
            repository.atualizarStatusEtapasDiario(
                diarioId, etapaAtual,
                statusEquipe, statusEquipamento, statusCarregamento,
                statusServicos, statusFechamentoServicos, statusRetornoBase,
                statusFechamentoDo, diarioFechado
            )
        }
    }

    fun atualizarIntervaloEFechamentoServicos(
        diarioId: Long, inicioIntervalo: String?, fimIntervalo: String?,
        observacaoIntervalo: String, intervaloRegistrado: Boolean,
        horarioFechamentoServicos: String?, observacaoFechamentoServicos: String,
        fechamentoServicosConcluido: Boolean
    ) {
        viewModelScope.launch {
            repository.atualizarIntervaloEFechamentoServicos(
                diarioId, inicioIntervalo, fimIntervalo, observacaoIntervalo,
                intervaloRegistrado, horarioFechamentoServicos,
                observacaoFechamentoServicos, fechamentoServicosConcluido
            )
        }
    }

    fun atualizarRetornoEFechamentoDo(
        diarioId: Long, saidaRetornoBase: String?, chegadaBase: String?,
        observacaoRetornoBase: String, retornoBaseConcluido: Boolean,
        observacaoFinalDo: String, diarioFechado: Boolean
    ) {
        viewModelScope.launch {
            repository.atualizarRetornoEFechamentoDo(
                diarioId, saidaRetornoBase, chegadaBase, observacaoRetornoBase,
                retornoBaseConcluido, observacaoFinalDo, diarioFechado
            )
        }
    }

    fun salvarFotoHospedagem(diarioId: Long, caminhoFoto: String, endereco: String) {
        viewModelScope.launch { repository.salvarFotoHospedagem(diarioId, caminhoFoto, endereco) }
    }

    fun concluirEtapa6ECriarDiarioDestino(
        diarioOrigemId: Long, obraDestinoId: Long,
        contratoDestinoDescricao: String, onNovoDiarioCriado: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val novoDiarioId = repository.concluirEtapa6ECriarDiarioDestino(
                diarioOrigemId, obraDestinoId, contratoDestinoDescricao
            )
            if (novoDiarioId != null) onNovoDiarioCriado(novoDiarioId)
        }
    }

    // ── Abastecimento ────────────────────────────────────────────────────

    fun salvarAbastecimento(
        diarioId: Long, veiculo: String, litros: Double,
        fotoTicketUri: String, horario: String
    ) {
        viewModelScope.launch {
            repository.salvarAbastecimento(diarioId, veiculo, litros, fotoTicketUri, horario)
        }
    }

    fun excluirAbastecimento(item: AbastecimentoItemEntity) {
        viewModelScope.launch { repository.excluirAbastecimento(item) }
    }

    // ── Exportação / relatório ───────────────────────────────────────────

    suspend fun salvarJsonDiarioNoApp(context: Context, diarioId: Long): String? {
        val jsonExport = repository.montarDiarioParaJson(diarioId) ?: return null
        val dataLimpa = jsonExport.data.replace("/", "-")
        val nomeArquivo = "diario_${dataLimpa}_${diarioId}.json"
        val gson = GsonBuilder().setPrettyPrinting().create()
        val conteudo = gson.toJson(jsonExport)
        context.openFileOutput(nomeArquivo, Context.MODE_PRIVATE).use { it.write(conteudo.toByteArray()) }
        return nomeArquivo
    }

    fun obterUriJsonSalvo(context: Context, nomeArquivo: String): Uri {
        val arquivo = File(context.filesDir, nomeArquivo)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
    }

    suspend fun gerarCsvDiarioPorId(diarioId: Long): String? {
        val exportacao = repository.montarDiarioParaExportacao(diarioId) ?: return null
        return gerarCsvDiario(exportacao)
    }

    suspend fun testarGeracaoCsv(diarioId: Long): String =
        gerarCsvDiarioPorId(diarioId) ?: "CSV não gerado"

    fun gerarNomeArquivoCsv(diario: DiarioExportacao): String {
        val dataLimpa = diario.diario.data.replace("/", "-")
        return "diario_obra_${diario.obra.id}_${dataLimpa}.csv"
    }

    fun obterUriCsvSalvo(context: Context, nomeArquivo: String): Uri {
        val arquivo = File(context.filesDir, nomeArquivo)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            arquivo
        )
    }

    suspend fun salvarCsvDiarioNoApp(context: Context, diarioId: Long): String? {
        val exportacao = repository.montarDiarioParaExportacao(diarioId) ?: return null
        val nomeArquivo = gerarNomeArquivoCsv(exportacao)
        val conteudo = gerarCsvDiario(exportacao)
        context.openFileOutput(nomeArquivo, Context.MODE_PRIVATE).use { it.write(conteudo.toByteArray()) }
        return nomeArquivo
    }

    suspend fun gerarTextoRelatorioPorId(diarioId: Long): String? {
        val relatorio = repository.montarDiarioParaRelatorio(diarioId) ?: return null
        return gerarTextoRelatorio(relatorio)
    }

    fun gerarCsvDiario(diario: DiarioExportacao): String {
        val sb = StringBuilder()

        sb.appendLine("secao,campo,valor")
        sb.appendLine("diario,id,${csv(diario.diario.id)}")
        sb.appendLine("diario,data,${csv(diario.diario.data)}")
        sb.appendLine("diario,obra_id,${csv(diario.obra.id)}")
        sb.appendLine("diario,obra_nome,${csv(diario.obra.nome)}")
        sb.appendLine("diario,local,${csv(diario.obra.local)}")
        sb.appendLine("diario,contratante,${csv(diario.obra.contratante)}")
        sb.appendLine("diario,contrato,${csv(diario.obra.contrato)}")
        sb.appendLine("diario,encarregado,${csv(diario.diario.encarregado)}")
        sb.appendLine("diario,equipe,${csv(diario.diario.equipe)}")
        sb.appendLine("diario,veiculo,${csv(diario.diario.veiculo)}")
        sb.appendLine("diario,equipamentos_auxiliares,${csv(diario.diario.equipamentosAuxiliares)}")

        sb.appendLine()
        sb.appendLine("secao,ordem,titulo,inicio,fim")

        diario.deslocamentos.forEach { deslocamento ->
            sb.appendLine(
                "deslocamento,${csv(deslocamento.ordem)},${csv(deslocamento.titulo)},${csv(deslocamento.inicio)},${csv(deslocamento.fim)}"
            )
        }

        sb.appendLine()
        sb.appendLine("secao,codigo,descricao,inicio,fim")

        diario.desvios.forEach { desvio ->
            sb.appendLine(
                "desvio,${csv(desvio.codigo)},${csv(desvio.descricao)},${csv(desvio.inicio)},${csv(desvio.fim)}"
            )
        }

        sb.appendLine()
        sb.appendLine("secao,ordem_servico,tipo,numero_protocolo,endereco,comprimento,largura,altura,inicio,fim,latitude,longitude,nome_rua,foto_uri,foto_conclusao_uri")

        diario.servicos.forEach { item ->
            val servico = item.servico
            sb.appendLine(
                "servico,${csv(servico.ordemServico)},${csv(servico.tipo)},${csv(servico.numeroProtocolo)},${csv(servico.endereco)},${csv(servico.comprimento)},${csv(servico.largura)},${csv(servico.altura)},${csv(servico.inicio)},${csv(servico.fim)},${csv(servico.latitude)},${csv(servico.longitude)},${csv(servico.nomeRua)},${csv(servico.fotoUri)},${csv(servico.fotoConclusaoUri)}"
            )
        }

        sb.appendLine()
        sb.appendLine("secao,ordem_servico,tipo_subservico,comprimento,largura,altura")

        diario.servicos.forEach { item ->
            item.subservicos.forEach { subservico ->
                sb.appendLine(
                    "subservico,${csv(item.servico.ordemServico)},${csv(subservico.tipo)},${csv(subservico.comprimento)},${csv(subservico.largura)},${csv(subservico.altura)}"
                )
            }
        }

        return sb.toString()
    }

    fun gerarTextoRelatorio(relatorio: DiarioRelatorio): String {
        val sb = StringBuilder()

        sb.appendLine("RELATÓRIO DIÁRIO DE OBRAS")
        sb.appendLine()

        sb.appendLine("DADOS GERAIS")
        sb.appendLine("Obra: ${relatorio.obra.nome}")
        sb.appendLine("Data: ${relatorio.diario.data}")
        sb.appendLine("Local: ${relatorio.obra.local}")
        sb.appendLine("Contratante: ${relatorio.obra.contratante}")
        sb.appendLine("Contrato: ${relatorio.obra.contrato}")
        sb.appendLine()

        sb.appendLine("EQUIPE E EQUIPAMENTOS")
        sb.appendLine("Encarregado: ${relatorio.diario.encarregado}")
        sb.appendLine("Equipe: ${relatorio.diario.equipe}")
        sb.appendLine("Veículo: ${relatorio.diario.veiculo}")
        sb.appendLine("Equipamentos auxiliares: ${relatorio.diario.equipamentosAuxiliares}")
        sb.appendLine()

        sb.appendLine("CARREGAMENTOS")

        if (relatorio.carregamentos.isEmpty()) {
            sb.appendLine("Nenhum carregamento registrado.")
        } else {
            relatorio.carregamentos.forEach { item ->
                sb.appendLine("Carregamento ${item.ordem}")
                sb.appendLine("Local de carregamento: ${item.localCarregamento}")
                sb.appendLine("Peso líquido (ton): ${item.pesoLiquidoTon}")
                sb.appendLine("Foto do ticket: ${item.fotoTicketUri}")
                sb.appendLine()
            }
        }

        sb.appendLine("DESLOCAMENTOS")
        relatorio.deslocamentos.forEach { item ->
            sb.appendLine("- ${item.ordem} | ${item.titulo} | início: ${item.inicio.orEmpty()} | fim: ${item.fim.orEmpty()}")
        }
        sb.appendLine()

        sb.appendLine("SERVIÇOS EXECUTADOS")
        relatorio.servicos.forEach { item ->
            val servico = item.servico
            sb.appendLine("Serviço ${servico.ordemServico}")
            sb.appendLine("Tipo: ${servico.tipo}")
            sb.appendLine("Protocolo: ${servico.numeroProtocolo}")
            sb.appendLine("Endereço: ${servico.endereco}")
            sb.appendLine("Medidas: C=${servico.comprimento} | L=${servico.largura} | A=${servico.altura}")
            sb.appendLine("Horário: início=${servico.inicio.orEmpty()} | fim=${servico.fim.orEmpty()}")
            sb.appendLine("Coordenadas: lat=${servico.latitude ?: ""} | long=${servico.longitude ?: ""}")
            sb.appendLine("Rua: ${servico.nomeRua.orEmpty()}")
            sb.appendLine("Foto antes: ${servico.fotoUri.orEmpty()}")
            sb.appendLine("Foto conclusão: ${servico.fotoConclusaoUri.orEmpty()}")

            if (item.subservicos.isNotEmpty()) {
                sb.appendLine("Subserviços:")
                item.subservicos.forEach { sub ->
                    sb.appendLine("  - ${sub.tipo} | C=${sub.comprimento ?: ""} | L=${sub.largura ?: ""} | A=${sub.altura ?: ""}")
                }
            }

            sb.appendLine()
        }

        sb.appendLine("DESVIOS")
        relatorio.desvios.forEach { item ->
            sb.appendLine("- ${item.codigo} | ${item.descricao} | início: ${item.inicio.orEmpty()} | fim: ${item.fim.orEmpty()}")
        }

        return sb.toString()
    }

    private fun csv(valor: Any?): String {
        val texto = valor?.toString().orEmpty()
        val escapado = texto.replace("\"", "\"\"")
        return "\"$escapado\""
    }

    //suspend fun marcarDiarioComoSincronizado(diarioId: Long) {
    //    val diario = dao.buscarDiarioPorId(diarioId) ?: return
    //    dao.atualizarDiario(diario.copy(sincronizado = true))
    //}

    fun reenviarDiario(diarioId: Long) {
        viewModelScope.launch {
            val jsonExport = repository.montarDiarioParaJson(diarioId)
            if (jsonExport == null) {
                _uploadEstado.value = UploadEstado.Erro("Não foi possível montar o JSON do diário")
                com.example.diarioobras.data.SyncScheduler.agendarUmaVez(getApplication())
                return@launch
            }
            _uploadEstado.value = UploadEstado.Enviando
            val resultado = uploadService.enviarDiario(getApplication(), jsonExport)
            if (resultado.isSuccess) {
                _uploadEstado.value = UploadEstado.Sucesso
                repository.marcarDiarioComoSincronizado(diarioId)
            } else {
                _uploadEstado.value = UploadEstado.Erro(resultado.exceptionOrNull()?.message ?: "Erro ao enviar")
                com.example.diarioobras.data.SyncScheduler.agendarUmaVez(getApplication())
            }
        }
    }
}
