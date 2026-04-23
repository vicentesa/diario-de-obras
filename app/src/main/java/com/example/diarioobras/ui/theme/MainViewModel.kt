package com.example.diarioobras.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.diarioobras.data.AppDatabase
import com.example.diarioobras.data.CarregamentoItemEntity
import com.example.diarioobras.data.DeslocamentoItemEntity
import com.example.diarioobras.data.DesvioItemEntity
import com.example.diarioobras.data.DiarioCompleto
import com.example.diarioobras.data.DiarioEntity
import com.example.diarioobras.data.DiarioExportacao
import com.example.diarioobras.data.DiarioRelatorio
import com.example.diarioobras.data.ObraEntity
import com.example.diarioobras.data.ServicoAreaEntity
import com.example.diarioobras.data.ServicoComAreas
import com.example.diarioobras.data.ServicoEntity
import com.example.diarioobras.data.ServicoExportacao
import com.example.diarioobras.data.SubservicoEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).obrasDao()

    suspend fun buscarDiarioPorId(diarioId: Long) = dao.buscarDiarioPorId(diarioId)
    suspend fun buscarObraPorId(obraId: Long) = dao.buscarObraPorId(obraId)
    suspend fun buscarServicoPorId(servicoId: Long) = dao.buscarServicoPorId(servicoId)

    suspend fun buscarDiarioCompleto(diarioId: Long): DiarioCompleto? {
        return dao.buscarDiarioCompletoPorId(diarioId)
    }

    suspend fun buscarServicoComAreasPorId(servicoId: Long): ServicoComAreas? {
        return dao.buscarServicoComAreasPorId(servicoId)
    }

    suspend fun listarServicoAreasDireto(servicoId: Long): List<ServicoAreaEntity> {
        return dao.listarServicoAreasDireto(servicoId)
    }

    fun servicoAreasDoServico(servicoId: Long) =
        dao.listarServicoAreas(servicoId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun substituirAreasDoServico(
        servicoId: Long,
        areas: List<ServicoAreaEntity>
    ) {
        dao.excluirServicoAreasPorServicoId(servicoId)

        if (areas.isNotEmpty()) {
            dao.inserirServicoAreas(
                areas.mapIndexed { index, area ->
                    area.copy(
                        id = 0,
                        servicoId = servicoId,
                        ordem = index + 1
                    )
                }
            )
        }
    }

    suspend fun salvarServicoCompleto(
        servico: ServicoEntity,
        areas: List<ServicoAreaEntity>
    ): Long {
        val servicoId = if (servico.id == 0L) {
            dao.inserirServico(servico.copy(id = 0))
        } else {
            dao.atualizarServico(servico)
            servico.id
        }

        substituirAreasDoServico(
            servicoId = servicoId,
            areas = areas.map { it.copy(servicoId = servicoId) }
        )

        val diarioAtual = dao.buscarDiarioPorId(servico.diarioId)
        if (diarioAtual != null && diarioAtual.statusServicos != "CONCLUIDA") {
            dao.atualizarStatusEtapasDiario(
                diarioId = servico.diarioId,
                etapaAtual = 4,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = "EM_ANDAMENTO",
                statusFechamentoServicos = "DISPONIVEL",
                statusRetornoBase = diarioAtual.statusRetornoBase,
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }

        return servicoId
    }

    fun excluirServico(servico: ServicoEntity) {
        viewModelScope.launch {
            dao.excluirServico(servico)
        }
    }

    fun encerrarServicos(diarioId: Long, proximoDestino: String) {
        viewModelScope.launch {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@launch
            val horaAtual = horaAtual()

            dao.atualizarIntervaloEFechamentoServicos(
                diarioId = diarioId,
                inicioIntervalo = diarioAtual.inicioIntervalo,
                fimIntervalo = diarioAtual.fimIntervalo,
                observacaoIntervalo = diarioAtual.observacaoIntervalo,
                intervaloRegistrado = diarioAtual.intervaloRegistrado,
                horarioFechamentoServicos = horaAtual,
                observacaoFechamentoServicos = diarioAtual.observacaoFechamentoServicos,
                proximoDestino = proximoDestino,
                fechamentoServicosConcluido = true
            )

            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId,
                etapaAtual = 6,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = "CONCLUIDA",
                statusFechamentoServicos = "CONCLUIDA",
                statusRetornoBase = "DISPONIVEL",
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    suspend fun montarServicosParaExportacao(diarioId: Long): List<ServicoExportacao> {
        val diarioCompleto = dao.buscarDiarioCompletoPorId(diarioId) ?: return emptyList()

        return diarioCompleto.servicos.map { servico ->
            val subservicos = dao.listarSubservicosDoServicoDireto(servico.id)
            ServicoExportacao(
                servico = servico,
                subservicos = subservicos
            )
        }
    }

    suspend fun montarDiarioParaExportacao(diarioId: Long): DiarioExportacao? {
        val diarioCompleto = dao.buscarDiarioCompletoPorId(diarioId) ?: return null
        val servicos = montarServicosParaExportacao(diarioId)
        val carregamentos = dao.listarCarregamentosDireto(diarioId)

        return DiarioExportacao(
            diario = diarioCompleto.diario,
            obra = diarioCompleto.obra,
            deslocamentos = diarioCompleto.deslocamentos,
            carregamentos = carregamentos,
            desvios = diarioCompleto.desvios,
            servicos = servicos
        )
    }

    suspend fun montarDiarioParaRelatorio(diarioId: Long): DiarioRelatorio? {
        val diarioCompleto = dao.buscarDiarioCompletoPorId(diarioId) ?: return null
        val servicos = montarServicosParaExportacao(diarioId)
        val carregamentos = dao.listarCarregamentosDireto(diarioId)

        return DiarioRelatorio(
            diario = diarioCompleto.diario,
            obra = diarioCompleto.obra,
            deslocamentos = diarioCompleto.deslocamentos,
            carregamentos = carregamentos,
            desvios = diarioCompleto.desvios,
            servicos = servicos
        )
    }

    suspend fun gerarCsvDiarioPorId(diarioId: Long): String? {
        val diarioExportacao = montarDiarioParaExportacao(diarioId) ?: return null
        return gerarCsvDiario(diarioExportacao)
    }

    suspend fun testarGeracaoCsv(diarioId: Long): String {
        return gerarCsvDiarioPorId(diarioId) ?: "CSV não gerado"
    }

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
        val diarioExportacao = montarDiarioParaExportacao(diarioId) ?: return null
        val nomeArquivo = gerarNomeArquivoCsv(diarioExportacao)
        val conteudoCsv = gerarCsvDiario(diarioExportacao)

        context.openFileOutput(nomeArquivo, Context.MODE_PRIVATE).use { output ->
            output.write(conteudoCsv.toByteArray())
        }

        return nomeArquivo
    }

    val obras = dao.listarObras()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun atualizarEquipeDiario(
        diarioId: Long,
        encarregado: String,
        equipe: List<String>,
        veiculo: String,
        equipamentosAuxiliares: List<String>
    ) {
        viewModelScope.launch {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@launch

            dao.atualizarDiario(
                diarioAtual.copy(
                    encarregado = encarregado,
                    equipe = equipe.joinToString(" / "),
                    veiculo = veiculo,
                    equipamentosAuxiliares = equipamentosAuxiliares.joinToString(" / "),
                    localCarregamento = diarioAtual.localCarregamento,
                    pesoLiquidoTon = diarioAtual.pesoLiquidoTon,
                    fotoTicketUri = diarioAtual.fotoTicketUri
                )
            )

            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId,
                etapaAtual = 2,
                statusEquipe = "CONCLUIDA",
                statusEquipamento = "DISPONIVEL",
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = diarioAtual.statusServicos,
                statusFechamentoServicos = diarioAtual.statusFechamentoServicos,
                statusRetornoBase = diarioAtual.statusRetornoBase,
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    fun atualizarEquipamentoDiario(
        diarioId: Long,
        veiculo: String,
        equipamentosAuxiliares: List<String>,
        equipamentosCompactacao: List<String>
    ) {
        viewModelScope.launch {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@launch

            dao.atualizarDiario(
                diarioAtual.copy(
                    veiculo = veiculo,
                    equipamentosAuxiliares = equipamentosAuxiliares.joinToString(" / "),
                    equipamentosCompactacao = equipamentosCompactacao.joinToString(" / ")
                )
            )

            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId,
                etapaAtual = 3,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = "CONCLUIDA",
                statusCarregamento = "DISPONIVEL",
                statusServicos = diarioAtual.statusServicos,
                statusFechamentoServicos = diarioAtual.statusFechamentoServicos,
                statusRetornoBase = diarioAtual.statusRetornoBase,
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    fun diariosDaObra(obraId: Long) =
        dao.listarDiariosDaObra(obraId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deslocamentosDoDiario(diarioId: Long) =
        dao.listarDeslocamentos(diarioId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun carregamentosDoDiario(diarioId: Long) =
        dao.listarCarregamentos(diarioId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun adicionarCarregamento(
        diarioId: Long,
        veiculo: String,
        chegadaUsina: String?,
        inicioCarregamento: String?,
        fimCarregamento: String?,
        horarioPesagem: String?,
        saidaUsinaTrecho: String?,
        localCarregamento: String,
        pesoLiquidoTon: String,
        fotoTicketUri: String
    ) {
        viewModelScope.launch {
            val carregamentosExistentes = dao.listarCarregamentosDireto(diarioId)
            val proximaOrdem = (carregamentosExistentes.maxOfOrNull { it.ordem } ?: 0) + 1

            dao.inserirCarregamento(
                CarregamentoItemEntity(
                    diarioId = diarioId,
                    ordem = proximaOrdem,
                    veiculo = veiculo,
                    chegadaUsina = chegadaUsina,
                    inicioCarregamento = inicioCarregamento,
                    fimCarregamento = fimCarregamento,
                    horarioPesagem = horarioPesagem,
                    saidaUsinaTrecho = saidaUsinaTrecho,
                    localCarregamento = localCarregamento,
                    pesoLiquidoTon = pesoLiquidoTon,
                    fotoTicketUri = fotoTicketUri
                )
            )
        }
    }

    fun excluirCarregamento(item: CarregamentoItemEntity) {
        viewModelScope.launch {
            dao.excluirCarregamento(item)
        }
    }

    fun servicosDoDiario(diarioId: Long) =
        dao.listarServicos(diarioId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun adicionarObra(
        nome: String,
        local: String,
        contratante: String,
        contrato: String,
        dataInicioContrato: String,
        prazoContratoDias: Int
    ) {
        if (nome.isBlank()) return
        viewModelScope.launch {
            dao.inserirObra(
                ObraEntity(
                    nome = nome,
                    local = local,
                    contratante = contratante,
                    contrato = contrato,
                    dataInicioContrato = dataInicioContrato,
                    prazoContratoDias = prazoContratoDias
                )
            )
        }
    }

    fun criarNovoDiario(
        obraId: Long,
        onCriado: (Long) -> Unit,
        onJaExiste: (String) -> Unit
    ) {
        viewModelScope.launch {
            val dataHoje = dataAtual()

            val diarioId = dao.inserirDiario(
                DiarioEntity(
                    obraId = obraId,
                    data = dataHoje
                )
            )

            dao.inserirDeslocamentos(
                listOf(
                    DeslocamentoItemEntity(
                        diarioId = diarioId,
                        ordem = 1,
                        titulo = "Batendo ponto na entrada"
                    ),
                    DeslocamentoItemEntity(
                        diarioId = diarioId,
                        ordem = 2,
                        titulo = "Organizando materiais e ferramentas para o trabalho (Manhã)"
                    ),
                    DeslocamentoItemEntity(
                        diarioId = diarioId,
                        ordem = 3,
                        titulo = "A caminho da Usina"
                    ),
                    DeslocamentoItemEntity(
                        diarioId = diarioId,
                        ordem = 4,
                        titulo = "Chegada na Usina"
                    ),
                    DeslocamentoItemEntity(
                        diarioId = diarioId,
                        ordem = 5,
                        titulo = "Carregando asfalto"
                    ),
                    DeslocamentoItemEntity(
                        diarioId = diarioId,
                        ordem = 6,
                        titulo = "Término do carregamento"
                    ),
                    DeslocamentoItemEntity(
                        diarioId = diarioId,
                        ordem = 7,
                        titulo = "Pesagem do caminhão carregado"
                    ),
                    DeslocamentoItemEntity(
                        diarioId = diarioId,
                        ordem = 8,
                        titulo = "Saída da Usina para o trecho"
                    ),
                    DeslocamentoItemEntity(
                        diarioId = diarioId,
                        ordem = 9,
                        titulo = "Chegada no trecho"
                    )
                )
            )

            onCriado(diarioId)
        }
    }

    fun marcarInicio(item: DeslocamentoItemEntity) {
        viewModelScope.launch {
            dao.atualizarDeslocamento(item.copy(inicio = horaAtual()))
        }
    }

    fun marcarFim(item: DeslocamentoItemEntity) {
        viewModelScope.launch {
            dao.atualizarDeslocamento(item.copy(fim = horaAtual()))
        }
    }

    fun atualizarHorarioManual(
        item: DeslocamentoItemEntity,
        novoInicio: String?,
        novoFim: String?
    ) {
        viewModelScope.launch {
            dao.atualizarDeslocamento(item.copy(inicio = novoInicio, fim = novoFim))
        }
    }

    fun adicionarServico(
        diarioId: Long,
        ordemServico: Int,
        numeroProtocolo: String,
        endereco: String,
        comprimento: Double,
        largura: Double,
        altura: Double,
        inicio: String? = null,
        fim: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        fotoUri: String? = null
    ) {
        viewModelScope.launch {
            dao.inserirServico(
                ServicoEntity(
                    diarioId = diarioId,
                    ordemServico = ordemServico,
                    numeroProtocolo = numeroProtocolo,
                    endereco = endereco,
                    comprimento = comprimento,
                    largura = largura,
                    altura = altura,
                    inicio = inicio,
                    fim = fim,
                    latitude = latitude,
                    longitude = longitude,
                    fotoUri = fotoUri
                )
            )

            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@launch

            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId,
                etapaAtual = 4,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = "EM_ANDAMENTO",
                statusFechamentoServicos = "DISPONIVEL",
                statusRetornoBase = diarioAtual.statusRetornoBase,
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    fun concluirFechamentoServicos(
        diarioId: Long,
        inicioIntervalo: String?,
        fimIntervalo: String?,
        observacaoIntervalo: String,
        intervaloRegistrado: Boolean,
        horarioFechamentoServicos: String?,
        observacaoFechamentoServicos: String
    ) {
        viewModelScope.launch {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@launch

            dao.atualizarIntervaloEFechamentoServicos(
                diarioId = diarioId,
                inicioIntervalo = inicioIntervalo,
                fimIntervalo = fimIntervalo,
                observacaoIntervalo = observacaoIntervalo,
                intervaloRegistrado = intervaloRegistrado,
                horarioFechamentoServicos = horarioFechamentoServicos,
                observacaoFechamentoServicos = observacaoFechamentoServicos,
                proximoDestino = diarioAtual.proximoDestino,
                fechamentoServicosConcluido = true
            )

            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId,
                etapaAtual = 6,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = "CONCLUIDA",
                statusFechamentoServicos = "CONCLUIDA",
                statusRetornoBase = "DISPONIVEL",
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    fun marcarSaidaEtapa6(diarioId: Long) {
        viewModelScope.launch {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@launch

            dao.atualizarRetornoEFechamentoDo(
                diarioId = diarioId,
                saidaRetornoBase = horaAtual(),
                chegadaBase = diarioAtual.chegadaBase,
                observacaoRetornoBase = diarioAtual.observacaoRetornoBase,
                retornoBaseConcluido = diarioAtual.retornoBaseConcluido,
                observacaoFinalDo = diarioAtual.observacaoFinalDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    fun marcarChegadaEtapa6(diarioId: Long) {
        viewModelScope.launch {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@launch

            dao.atualizarRetornoEFechamentoDo(
                diarioId = diarioId,
                saidaRetornoBase = diarioAtual.saidaRetornoBase,
                chegadaBase = horaAtual(),
                observacaoRetornoBase = diarioAtual.observacaoRetornoBase,
                retornoBaseConcluido = diarioAtual.retornoBaseConcluido,
                observacaoFinalDo = diarioAtual.observacaoFinalDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }
    fun concluirRetornoBase(
        diarioId: Long,
        saidaRetornoBase: String?,
        chegadaBase: String?,
        observacaoRetornoBase: String
    ) {
        viewModelScope.launch {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@launch

            dao.atualizarRetornoEFechamentoDo(
                diarioId = diarioId,
                saidaRetornoBase = saidaRetornoBase,
                chegadaBase = chegadaBase,
                observacaoRetornoBase = observacaoRetornoBase,
                retornoBaseConcluido = true,
                observacaoFinalDo = diarioAtual.observacaoFinalDo,
                diarioFechado = diarioAtual.diarioFechado
            )

            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId,
                etapaAtual = 7,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = diarioAtual.statusServicos,
                statusFechamentoServicos = diarioAtual.statusFechamentoServicos,
                statusRetornoBase = "CONCLUIDA",
                statusFechamentoDo = "DISPONIVEL",
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    fun concluirFechamentoDo(
        diarioId: Long,
        observacaoFinalDo: String
    ) {
        viewModelScope.launch {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@launch

            dao.atualizarRetornoEFechamentoDo(
                diarioId = diarioId,
                saidaRetornoBase = diarioAtual.saidaRetornoBase,
                chegadaBase = diarioAtual.chegadaBase,
                observacaoRetornoBase = diarioAtual.observacaoRetornoBase,
                retornoBaseConcluido = diarioAtual.retornoBaseConcluido,
                observacaoFinalDo = observacaoFinalDo,
                diarioFechado = true
            )

            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId,
                etapaAtual = 7,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = diarioAtual.statusServicos,
                statusFechamentoServicos = diarioAtual.statusFechamentoServicos,
                statusRetornoBase = diarioAtual.statusRetornoBase,
                statusFechamentoDo = "CONCLUIDA",
                diarioFechado = true
            )
        }
    }

    suspend fun salvarServicoRetornandoId(servico: ServicoEntity): Long {
        return dao.inserirServico(servico)
    }

    suspend fun atualizarServicoSuspend(servico: ServicoEntity) {
        dao.atualizarServico(servico)
    }

    fun desviosDoDiario(diarioId: Long) =
        dao.listarDesvios(diarioId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun adicionarDesvio(
        diarioId: Long,
        codigo: String,
        descricao: String
    ) {
        viewModelScope.launch {
            dao.inserirDesvio(
                DesvioItemEntity(
                    diarioId = diarioId,
                    codigo = codigo,
                    descricao = descricao
                )
            )
        }
    }

    fun marcarInicioDesvio(item: DesvioItemEntity) {
        viewModelScope.launch {
            dao.atualizarDesvio(item.copy(inicio = horaAtual()))
        }
    }

    fun marcarFimDesvio(item: DesvioItemEntity) {
        viewModelScope.launch {
            dao.atualizarDesvio(item.copy(fim = horaAtual()))
        }
    }

    fun atualizarHorarioDesvio(
        item: DesvioItemEntity,
        novoInicio: String?,
        novoFim: String?
    ) {
        viewModelScope.launch {
            dao.atualizarDesvio(item.copy(inicio = novoInicio, fim = novoFim))
        }
    }

    fun atualizarCarregamentoDiario(
        diarioId: Long,
        localCarregamento: String,
        pesoLiquidoTon: String,
        fotoTicketUri: String
    ) {
        viewModelScope.launch {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@launch

            dao.atualizarDiario(
                diarioAtual.copy(
                    localCarregamento = localCarregamento,
                    pesoLiquidoTon = pesoLiquidoTon,
                    fotoTicketUri = fotoTicketUri
                )
            )

            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId,
                etapaAtual = 4,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = "CONCLUIDA",
                statusServicos = "DISPONIVEL",
                statusFechamentoServicos = diarioAtual.statusFechamentoServicos,
                statusRetornoBase = diarioAtual.statusRetornoBase,
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    fun atualizarServico(servico: ServicoEntity) {
        viewModelScope.launch {
            dao.atualizarServico(servico)
        }
    }

    fun subservicosDoServico(servicoId: Long) =
        dao.listarSubservicosDoServico(servicoId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun adicionarSubservico(
        servicoId: Long,
        tipo: String,
        comprimento: Double? = null,
        largura: Double? = null,
        altura: Double? = null
    ) {
        viewModelScope.launch {
            dao.inserirSubservico(
                SubservicoEntity(
                    servicoId = servicoId,
                    tipo = tipo,
                    comprimento = comprimento,
                    largura = largura,
                    altura = altura
                )
            )
        }
    }

    fun atualizarSubservico(subservico: SubservicoEntity) {
        viewModelScope.launch {
            dao.atualizarSubservico(subservico)
        }
    }

    fun atualizarStatusEtapasDiario(
        diarioId: Long,
        etapaAtual: Int,
        statusEquipe: String,
        statusEquipamento: String,
        statusCarregamento: String,
        statusServicos: String,
        statusFechamentoServicos: String,
        statusRetornoBase: String,
        statusFechamentoDo: String,
        diarioFechado: Boolean = false
    ) {
        viewModelScope.launch {
            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId,
                etapaAtual = etapaAtual,
                statusEquipe = statusEquipe,
                statusEquipamento = statusEquipamento,
                statusCarregamento = statusCarregamento,
                statusServicos = statusServicos,
                statusFechamentoServicos = statusFechamentoServicos,
                statusRetornoBase = statusRetornoBase,
                statusFechamentoDo = statusFechamentoDo,
                diarioFechado = diarioFechado
            )
        }
    }

    fun atualizarIntervaloEFechamentoServicos(
        diarioId: Long,
        inicioIntervalo: String?,
        fimIntervalo: String?,
        observacaoIntervalo: String,
        intervaloRegistrado: Boolean,
        horarioFechamentoServicos: String?,
        observacaoFechamentoServicos: String,
        fechamentoServicosConcluido: Boolean
    ) {
        viewModelScope.launch {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@launch
            dao.atualizarIntervaloEFechamentoServicos(
                diarioId = diarioId,
                inicioIntervalo = inicioIntervalo,
                fimIntervalo = fimIntervalo,
                observacaoIntervalo = observacaoIntervalo,
                intervaloRegistrado = intervaloRegistrado,
                horarioFechamentoServicos = horarioFechamentoServicos,
                observacaoFechamentoServicos = observacaoFechamentoServicos,
                proximoDestino = diarioAtual.proximoDestino,
                fechamentoServicosConcluido = fechamentoServicosConcluido
            )
        }
    }

    fun atualizarRetornoEFechamentoDo(
        diarioId: Long,
        saidaRetornoBase: String?,
        chegadaBase: String?,
        observacaoRetornoBase: String,
        retornoBaseConcluido: Boolean,
        observacaoFinalDo: String,
        diarioFechado: Boolean
    ) {
        viewModelScope.launch {
            dao.atualizarRetornoEFechamentoDo(
                diarioId = diarioId,
                saidaRetornoBase = saidaRetornoBase,
                chegadaBase = chegadaBase,
                observacaoRetornoBase = observacaoRetornoBase,
                retornoBaseConcluido = retornoBaseConcluido,
                observacaoFinalDo = observacaoFinalDo,
                diarioFechado = diarioFechado
            )
        }
    }

    private fun horaAtual(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun dataAtual(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    private fun csv(valor: Any?): String {
        val texto = valor?.toString().orEmpty()
        val escapado = texto.replace("\"", "\"\"")
        return "\"$escapado\""
    }

    fun buscarDiarioFlow(diarioId: Long) = dao.buscarDiarioFlowPorId(diarioId)

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

    suspend fun gerarTextoRelatorioPorId(diarioId: Long): String? {
        val relatorio = montarDiarioParaRelatorio(diarioId) ?: return null
        return gerarTextoRelatorio(relatorio)
    }
}