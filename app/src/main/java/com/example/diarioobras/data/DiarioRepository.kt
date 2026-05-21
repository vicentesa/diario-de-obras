package com.example.diarioobras.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiarioRepository(
    private val db: AppDatabase,
    private val dao: ObrasDao = db.obrasDao()
) {

    // ── Flows ────────────────────────────────────────────────────────────

    fun listarObras(): Flow<List<ObraEntity>> = dao.listarObras()

    fun listarDiariosDaObra(obraId: Long): Flow<List<DiarioEntity>> =
        dao.listarDiariosDaObra(obraId)

    fun listarDeslocamentos(diarioId: Long): Flow<List<DeslocamentoItemEntity>> =
        dao.listarDeslocamentos(diarioId)

    fun listarCarregamentos(diarioId: Long): Flow<List<CarregamentoItemEntity>> =
        dao.listarCarregamentos(diarioId)

    fun listarServicos(diarioId: Long): Flow<List<ServicoEntity>> =
        dao.listarServicos(diarioId)

    fun listarDesvios(diarioId: Long): Flow<List<DesvioItemEntity>> =
        dao.listarDesvios(diarioId)

    fun listarSubservicosDoServico(servicoId: Long): Flow<List<SubservicoEntity>> =
        dao.listarSubservicosDoServico(servicoId)

    fun listarServicoAreas(servicoId: Long): Flow<List<ServicoAreaEntity>> =
        dao.listarServicoAreas(servicoId)

    fun buscarDiarioFlow(diarioId: Long): Flow<DiarioEntity?> =
        dao.buscarDiarioFlowPorId(diarioId)

    fun listarAbastecimentos(diarioId: Long): Flow<List<AbastecimentoItemEntity>> =
        dao.listarAbastecimentos(diarioId)

    // ── Leituras simples ─────────────────────────────────────────────────

    suspend fun buscarDiarioPorId(id: Long): DiarioEntity? = dao.buscarDiarioPorId(id)
    suspend fun buscarObraPorId(id: Long): ObraEntity? = dao.buscarObraPorId(id)
    suspend fun buscarServicoPorId(id: Long): ServicoEntity? = dao.buscarServicoPorId(id)
    suspend fun buscarDiarioCompleto(id: Long): DiarioCompleto? = dao.buscarDiarioCompletoPorId(id)
    suspend fun buscarServicoComAreasPorId(id: Long): ServicoComAreas? = dao.buscarServicoComAreasPorId(id)
    suspend fun listarServicoAreasDireto(id: Long): List<ServicoAreaEntity> = dao.listarServicoAreasDireto(id)
    suspend fun buscarDesvioPorId(id: Long): DesvioItemEntity? = dao.buscarDesvioPorId(id)

    // ── Escritas simples ─────────────────────────────────────────────────

    suspend fun inserirObra(
        nome: String, local: String, contratante: String,
        contrato: String, dataInicioContrato: String, prazoContratoDias: Int,
        espessuraContratoCm: Double = 0.0
    ) {
        dao.inserirObra(
            ObraEntity(
                nome = nome, local = local, contratante = contratante,
                contrato = contrato, dataInicioContrato = dataInicioContrato,
                prazoContratoDias = prazoContratoDias,
                espessuraContratoCm = espessuraContratoCm
            )
        )
    }

    suspend fun atualizarObra(obra: ObraEntity) = dao.atualizarObra(obra)

    suspend fun excluirServico(servico: ServicoEntity) = dao.excluirServico(servico)
    suspend fun excluirCarregamento(item: CarregamentoItemEntity) = dao.excluirCarregamento(item)
    suspend fun atualizarServico(servico: ServicoEntity) = dao.atualizarServico(servico)
    suspend fun salvarServicoRetornandoId(servico: ServicoEntity): Long = dao.inserirServico(servico)
    suspend fun atualizarServicoSuspend(servico: ServicoEntity) = dao.atualizarServico(servico)

    suspend fun adicionarSubservico(
        servicoId: Long, tipo: String,
        comprimento: Double?, largura: Double?, altura: Double?
    ) {
        dao.inserirSubservico(
            SubservicoEntity(
                servicoId = servicoId, tipo = tipo,
                comprimento = comprimento, largura = largura, altura = altura
            )
        )
    }

    suspend fun atualizarSubservico(subservico: SubservicoEntity) = dao.atualizarSubservico(subservico)

    suspend fun marcarInicio(item: DeslocamentoItemEntity) =
        dao.atualizarDeslocamento(item.copy(inicio = horaAtual()))

    suspend fun marcarFim(item: DeslocamentoItemEntity) =
        dao.atualizarDeslocamento(item.copy(fim = horaAtual()))

    suspend fun atualizarHorarioManual(
        item: DeslocamentoItemEntity,
        novoInicio: String?,
        novoFim: String?
    ) = dao.atualizarDeslocamento(item.copy(inicio = novoInicio, fim = novoFim))

    suspend fun atualizarStatusEtapasDiario(
        diarioId: Long, etapaAtual: Int,
        statusEquipe: StatusEtapa, statusEquipamento: StatusEtapa,
        statusCarregamento: StatusEtapa, statusServicos: StatusEtapa,
        statusFechamentoServicos: StatusEtapa, statusRetornoBase: StatusEtapa,
        statusFechamentoDo: StatusEtapa, diarioFechado: Boolean
    ) {
        dao.atualizarStatusEtapasDiario(
            diarioId = diarioId, etapaAtual = etapaAtual,
            statusEquipe = statusEquipe, statusEquipamento = statusEquipamento,
            statusCarregamento = statusCarregamento, statusServicos = statusServicos,
            statusFechamentoServicos = statusFechamentoServicos, statusRetornoBase = statusRetornoBase,
            statusFechamentoDo = statusFechamentoDo, diarioFechado = diarioFechado
        )
    }

    // ── Abastecimento ────────────────────────────────────────────────────

    suspend fun salvarAbastecimento(
        diarioId: Long, veiculo: String, litros: Double,
        fotoTicketUri: String, horario: String
    ) {
        dao.inserirAbastecimento(
            AbastecimentoItemEntity(
                diarioId = diarioId,
                veiculo = veiculo,
                litros = litros,
                fotoTicketUri = fotoTicketUri,
                horario = horario
            )
        )
    }

    suspend fun excluirAbastecimento(item: AbastecimentoItemEntity) =
        dao.excluirAbastecimento(item)

    // ── Operações transacionais ──────────────────────────────────────────

    suspend fun criarNovoDiario(obraId: Long): Long {
        return db.withTransaction {
            val diarioId = dao.inserirDiario(DiarioEntity(obraId = obraId, data = dataAtual()))
            dao.inserirDeslocamentos(
                listOf(
                    DeslocamentoItemEntity(diarioId = diarioId, ordem = 1, titulo = "Batendo ponto na entrada"),
                    DeslocamentoItemEntity(diarioId = diarioId, ordem = 2, titulo = "Organizando materiais e ferramentas para o trabalho (Manhã)"),
                    DeslocamentoItemEntity(diarioId = diarioId, ordem = 3, titulo = "A caminho da Usina"),
                    DeslocamentoItemEntity(diarioId = diarioId, ordem = 4, titulo = "Chegada na Usina"),
                    DeslocamentoItemEntity(diarioId = diarioId, ordem = 5, titulo = "Carregando asfalto"),
                    DeslocamentoItemEntity(diarioId = diarioId, ordem = 6, titulo = "Término do carregamento"),
                    DeslocamentoItemEntity(diarioId = diarioId, ordem = 7, titulo = "Pesagem do caminhão carregado"),
                    DeslocamentoItemEntity(diarioId = diarioId, ordem = 8, titulo = "Saída da Usina para o trecho"),
                    DeslocamentoItemEntity(diarioId = diarioId, ordem = 9, titulo = "Chegada no trecho")
                )
            )
            diarioId
        }
    }

    suspend fun adicionarCarregamento(
        diarioId: Long, veiculo: String,
        chegadaUsina: String?, inicioCarregamento: String?,
        fimCarregamento: String?, horarioPesagem: String?,
        saidaUsinaTrecho: String?, localCarregamento: String,
        pesoLiquidoTon: String, fotoTicketUri: String
    ) {
        db.withTransaction {
            val existentes = dao.listarCarregamentosDireto(diarioId)
            val proximaOrdem = (existentes.maxOfOrNull { it.ordem } ?: 0) + 1
            dao.inserirCarregamento(
                CarregamentoItemEntity(
                    diarioId = diarioId, ordem = proximaOrdem, veiculo = veiculo,
                    chegadaUsina = chegadaUsina, inicioCarregamento = inicioCarregamento,
                    fimCarregamento = fimCarregamento, horarioPesagem = horarioPesagem,
                    saidaUsinaTrecho = saidaUsinaTrecho, localCarregamento = localCarregamento,
                    pesoLiquidoTon = pesoLiquidoTon, fotoTicketUri = fotoTicketUri
                )
            )
        }
    }

    suspend fun salvarCarregamentoEtapa3(
        diarioId: Long, veiculo: String, pesoLiquidoTon: String, fotoTicketUri: String,
        latitude: Double, longitude: Double
    ) {
        db.withTransaction {
            val existentes = dao.listarCarregamentosDireto(diarioId)
            val existente = existentes.firstOrNull()
            if (existente == null) {
                dao.inserirCarregamento(
                    CarregamentoItemEntity(
                        diarioId = diarioId, ordem = 1,
                        veiculo = veiculo, pesoLiquidoTon = pesoLiquidoTon,
                        fotoTicketUri = fotoTicketUri,
                        latitude = latitude, longitude = longitude
                    )
                )
            } else {
                dao.atualizarCarregamento(
                    existente.copy(
                        veiculo = veiculo, pesoLiquidoTon = pesoLiquidoTon,
                        fotoTicketUri = fotoTicketUri,
                        latitude = latitude, longitude = longitude
                    )
                )
            }
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction
            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId, etapaAtual = 4,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = StatusEtapa.CONCLUIDA,
                statusServicos = StatusEtapa.DISPONIVEL,
                statusFechamentoServicos = diarioAtual.statusFechamentoServicos,
                statusRetornoBase = diarioAtual.statusRetornoBase,
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    suspend fun substituirAreasDoServico(servicoId: Long, areas: List<ServicoAreaEntity>) {
        db.withTransaction {
            dao.excluirServicoAreasPorServicoId(servicoId)
            if (areas.isNotEmpty()) {
                dao.inserirServicoAreas(
                    areas.mapIndexed { index, area ->
                        area.copy(id = 0, servicoId = servicoId, ordem = index + 1)
                    }
                )
            }
        }
    }

    suspend fun salvarServicoCompleto(servico: ServicoEntity, areas: List<ServicoAreaEntity>): Long {
        return db.withTransaction {
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
            if (diarioAtual != null && diarioAtual.statusServicos != StatusEtapa.CONCLUIDA) {
                dao.atualizarStatusEtapasDiario(
                    diarioId = servico.diarioId, etapaAtual = 4,
                    statusEquipe = diarioAtual.statusEquipe,
                    statusEquipamento = diarioAtual.statusEquipamento,
                    statusCarregamento = diarioAtual.statusCarregamento,
                    statusServicos = StatusEtapa.EM_ANDAMENTO,
                    statusFechamentoServicos = StatusEtapa.DISPONIVEL,
                    statusRetornoBase = diarioAtual.statusRetornoBase,
                    statusFechamentoDo = diarioAtual.statusFechamentoDo,
                    diarioFechado = diarioAtual.diarioFechado
                )
            }

            servicoId
        }
    }

    suspend fun encerrarServicos(diarioId: Long, proximoDestino: String) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction
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
                diarioId = diarioId, etapaAtual = 6,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = StatusEtapa.CONCLUIDA,
                statusFechamentoServicos = StatusEtapa.CONCLUIDA,
                statusRetornoBase = StatusEtapa.DISPONIVEL,
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    suspend fun atualizarEquipeDiario(
        diarioId: Long, encarregado: String, equipe: List<String>,
        veiculo: String, equipamentosAuxiliares: List<String>
    ) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction

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
                diarioId = diarioId, etapaAtual = 2,
                statusEquipe = StatusEtapa.CONCLUIDA,
                statusEquipamento = StatusEtapa.DISPONIVEL,
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = diarioAtual.statusServicos,
                statusFechamentoServicos = diarioAtual.statusFechamentoServicos,
                statusRetornoBase = diarioAtual.statusRetornoBase,
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    suspend fun atualizarEquipamentoDiario(
        diarioId: Long, veiculo: String,
        equipamentosAuxiliares: List<String>, equipamentosCompactacao: List<String>
    ) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction

            dao.atualizarDiario(
                diarioAtual.copy(
                    veiculo = veiculo,
                    equipamentosAuxiliares = equipamentosAuxiliares.joinToString(" / "),
                    equipamentosCompactacao = equipamentosCompactacao.joinToString(" / ")
                )
            )
            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId, etapaAtual = 3,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = StatusEtapa.CONCLUIDA,
                statusCarregamento = StatusEtapa.DISPONIVEL,
                statusServicos = diarioAtual.statusServicos,
                statusFechamentoServicos = diarioAtual.statusFechamentoServicos,
                statusRetornoBase = diarioAtual.statusRetornoBase,
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    suspend fun adicionarServico(
        diarioId: Long, ordemServico: Int, numeroProtocolo: String, endereco: String,
        comprimento: Double, largura: Double, altura: Double,
        inicio: String?, fim: String?, latitude: Double?, longitude: Double?, fotoUri: String?
    ) {
        db.withTransaction {
            dao.inserirServico(
                ServicoEntity(
                    diarioId = diarioId, ordemServico = ordemServico,
                    numeroProtocolo = numeroProtocolo, endereco = endereco,
                    comprimento = comprimento, largura = largura, altura = altura,
                    inicio = inicio, fim = fim, latitude = latitude,
                    longitude = longitude, fotoUri = fotoUri
                )
            )
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction
            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId, etapaAtual = 4,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = StatusEtapa.EM_ANDAMENTO,
                statusFechamentoServicos = StatusEtapa.DISPONIVEL,
                statusRetornoBase = diarioAtual.statusRetornoBase,
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    suspend fun concluirFechamentoServicos(
        diarioId: Long, inicioIntervalo: String?, fimIntervalo: String?,
        observacaoIntervalo: String, intervaloRegistrado: Boolean,
        horarioFechamentoServicos: String?, observacaoFechamentoServicos: String
    ) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction

            dao.atualizarIntervaloEFechamentoServicos(
                diarioId = diarioId, inicioIntervalo = inicioIntervalo,
                fimIntervalo = fimIntervalo, observacaoIntervalo = observacaoIntervalo,
                intervaloRegistrado = intervaloRegistrado,
                horarioFechamentoServicos = horarioFechamentoServicos,
                observacaoFechamentoServicos = observacaoFechamentoServicos,
                proximoDestino = diarioAtual.proximoDestino, fechamentoServicosConcluido = true
            )
            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId, etapaAtual = 6,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = StatusEtapa.CONCLUIDA,
                statusFechamentoServicos = StatusEtapa.CONCLUIDA,
                statusRetornoBase = StatusEtapa.DISPONIVEL,
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    suspend fun marcarSaidaEtapa6(diarioId: Long) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction

            dao.atualizarRetornoEFechamentoDo(
                diarioId = diarioId, saidaRetornoBase = horaAtual(),
                chegadaBase = diarioAtual.chegadaBase,
                observacaoRetornoBase = diarioAtual.observacaoRetornoBase,
                retornoBaseConcluido = diarioAtual.retornoBaseConcluido,
                observacaoFinalDo = diarioAtual.observacaoFinalDo,
                horarioPontoCidade = diarioAtual.horarioPontoCidade,
                fotoHospedagemPath = diarioAtual.fotoHospedagemPath,
                enderecoHospedagem = diarioAtual.enderecoHospedagem,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    suspend fun marcarChegadaEtapa6(diarioId: Long) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction

            dao.atualizarRetornoEFechamentoDo(
                diarioId = diarioId, saidaRetornoBase = diarioAtual.saidaRetornoBase,
                chegadaBase = horaAtual(),
                observacaoRetornoBase = diarioAtual.observacaoRetornoBase,
                retornoBaseConcluido = diarioAtual.retornoBaseConcluido,
                observacaoFinalDo = diarioAtual.observacaoFinalDo,
                horarioPontoCidade = diarioAtual.horarioPontoCidade,
                fotoHospedagemPath = diarioAtual.fotoHospedagemPath,
                enderecoHospedagem = diarioAtual.enderecoHospedagem,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    suspend fun concluirRetornoBase(
        diarioId: Long, saidaRetornoBase: String?,
        chegadaBase: String?, observacaoRetornoBase: String
    ) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction

            dao.atualizarRetornoEFechamentoDo(
                diarioId = diarioId, saidaRetornoBase = saidaRetornoBase,
                chegadaBase = chegadaBase, observacaoRetornoBase = observacaoRetornoBase,
                retornoBaseConcluido = true, observacaoFinalDo = diarioAtual.observacaoFinalDo,
                horarioPontoCidade = diarioAtual.horarioPontoCidade,
                fotoHospedagemPath = diarioAtual.fotoHospedagemPath,
                enderecoHospedagem = diarioAtual.enderecoHospedagem,
                diarioFechado = diarioAtual.diarioFechado
            )
            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId, etapaAtual = 7,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = diarioAtual.statusServicos,
                statusFechamentoServicos = diarioAtual.statusFechamentoServicos,
                statusRetornoBase = StatusEtapa.CONCLUIDA,
                statusFechamentoDo = StatusEtapa.DISPONIVEL,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    suspend fun atualizarObservacaoDesvio(id: Long, texto: String) {
        db.withTransaction {
            val desvio = dao.buscarDesvioPorId(id) ?: return@withTransaction
            val diario = dao.buscarDiarioPorId(desvio.diarioId) ?: return@withTransaction
            if (diario.diarioFechado || diario.statusFechamentoDo == StatusEtapa.CONCLUIDA) {
                return@withTransaction
            }
            dao.atualizarObservacaoDesvio(id, texto)
        }
    }

    suspend fun atualizarFotoDesvio(id: Long, fotoUri: String) = dao.atualizarFotoDesvio(id, fotoUri)

    suspend fun concluirFechamentoDo(
        diarioId: Long, observacaoFinalDo: String, horarioPontoCidade: String? = null
    ) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction

            dao.atualizarRetornoEFechamentoDo(
                diarioId = diarioId, saidaRetornoBase = diarioAtual.saidaRetornoBase,
                chegadaBase = diarioAtual.chegadaBase,
                observacaoRetornoBase = diarioAtual.observacaoRetornoBase,
                retornoBaseConcluido = diarioAtual.retornoBaseConcluido,
                observacaoFinalDo = observacaoFinalDo,
                horarioPontoCidade = horarioPontoCidade,
                fotoHospedagemPath = diarioAtual.fotoHospedagemPath,
                enderecoHospedagem = diarioAtual.enderecoHospedagem,
                diarioFechado = true
            )
            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId, etapaAtual = 7,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = diarioAtual.statusCarregamento,
                statusServicos = diarioAtual.statusServicos,
                statusFechamentoServicos = diarioAtual.statusFechamentoServicos,
                statusRetornoBase = diarioAtual.statusRetornoBase,
                statusFechamentoDo = StatusEtapa.CONCLUIDA,
                diarioFechado = true
            )
        }
    }

    suspend fun adicionarDesvioCompleto(
        diarioId: Long, codigo: String, descricao: String,
        inicio: String, fim: String, observacao: String,
        litros: Double = 0.0, fotoTicketUri: String = "",
        latitude: Double = 0.0, longitude: Double = 0.0,
        endereco: String = ""
    ) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction
            if (diarioAtual.diarioFechado || diarioAtual.statusFechamentoDo == StatusEtapa.CONCLUIDA) {
                return@withTransaction
            }
            dao.inserirDesvio(
                DesvioItemEntity(
                    diarioId = diarioId, codigo = codigo, descricao = descricao,
                    inicio = inicio, fim = fim, observacao = observacao,
                    litros = litros, fotoTicketUri = fotoTicketUri,
                    latitude = latitude, longitude = longitude,
                    endereco = endereco
                )
            )
        }
    }

    suspend fun marcarInicioDesvio(item: DesvioItemEntity) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(item.diarioId) ?: return@withTransaction
            if (diarioAtual.diarioFechado || diarioAtual.statusFechamentoDo == StatusEtapa.CONCLUIDA) {
                return@withTransaction
            }
            dao.atualizarDesvio(item.copy(inicio = horaAtual()))
        }
    }

    suspend fun marcarFimDesvio(item: DesvioItemEntity) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(item.diarioId) ?: return@withTransaction
            if (diarioAtual.diarioFechado || diarioAtual.statusFechamentoDo == StatusEtapa.CONCLUIDA) {
                return@withTransaction
            }
            dao.atualizarDesvio(item.copy(fim = horaAtual()))
        }
    }

    suspend fun atualizarHorarioDesvio(item: DesvioItemEntity, novoInicio: String?, novoFim: String?) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(item.diarioId) ?: return@withTransaction
            if (diarioAtual.diarioFechado || diarioAtual.statusFechamentoDo == StatusEtapa.CONCLUIDA) {
                return@withTransaction
            }
            dao.atualizarDesvio(item.copy(inicio = novoInicio.orEmpty(), fim = novoFim.orEmpty()))
        }
    }

    suspend fun atualizarCarregamentoDiario(
        diarioId: Long, localCarregamento: String,
        pesoLiquidoTon: String, fotoTicketUri: String
    ) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction

            dao.atualizarDiario(
                diarioAtual.copy(
                    localCarregamento = localCarregamento,
                    pesoLiquidoTon = pesoLiquidoTon,
                    fotoTicketUri = fotoTicketUri
                )
            )
            dao.atualizarStatusEtapasDiario(
                diarioId = diarioId, etapaAtual = 4,
                statusEquipe = diarioAtual.statusEquipe,
                statusEquipamento = diarioAtual.statusEquipamento,
                statusCarregamento = StatusEtapa.CONCLUIDA,
                statusServicos = StatusEtapa.DISPONIVEL,
                statusFechamentoServicos = diarioAtual.statusFechamentoServicos,
                statusRetornoBase = diarioAtual.statusRetornoBase,
                statusFechamentoDo = diarioAtual.statusFechamentoDo,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    suspend fun atualizarIntervaloEFechamentoServicos(
        diarioId: Long, inicioIntervalo: String?, fimIntervalo: String?,
        observacaoIntervalo: String, intervaloRegistrado: Boolean,
        horarioFechamentoServicos: String?, observacaoFechamentoServicos: String,
        fechamentoServicosConcluido: Boolean
    ) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction
            dao.atualizarIntervaloEFechamentoServicos(
                diarioId = diarioId, inicioIntervalo = inicioIntervalo,
                fimIntervalo = fimIntervalo, observacaoIntervalo = observacaoIntervalo,
                intervaloRegistrado = intervaloRegistrado,
                horarioFechamentoServicos = horarioFechamentoServicos,
                observacaoFechamentoServicos = observacaoFechamentoServicos,
                proximoDestino = diarioAtual.proximoDestino,
                fechamentoServicosConcluido = fechamentoServicosConcluido
            )
        }
    }

    suspend fun atualizarRetornoEFechamentoDo(
        diarioId: Long, saidaRetornoBase: String?, chegadaBase: String?,
        observacaoRetornoBase: String, retornoBaseConcluido: Boolean,
        observacaoFinalDo: String, diarioFechado: Boolean
    ) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction
            dao.atualizarRetornoEFechamentoDo(
                diarioId = diarioId, saidaRetornoBase = saidaRetornoBase,
                chegadaBase = chegadaBase, observacaoRetornoBase = observacaoRetornoBase,
                retornoBaseConcluido = retornoBaseConcluido, observacaoFinalDo = observacaoFinalDo,
                horarioPontoCidade = diarioAtual.horarioPontoCidade,
                fotoHospedagemPath = diarioAtual.fotoHospedagemPath,
                enderecoHospedagem = diarioAtual.enderecoHospedagem,
                diarioFechado = diarioFechado
            )
        }
    }

    suspend fun salvarFotoHospedagem(diarioId: Long, caminhoFoto: String, endereco: String) {
        db.withTransaction {
            val diarioAtual = dao.buscarDiarioPorId(diarioId) ?: return@withTransaction
            dao.atualizarRetornoEFechamentoDo(
                diarioId = diarioId, saidaRetornoBase = diarioAtual.saidaRetornoBase,
                chegadaBase = diarioAtual.chegadaBase,
                observacaoRetornoBase = diarioAtual.observacaoRetornoBase,
                retornoBaseConcluido = diarioAtual.retornoBaseConcluido,
                observacaoFinalDo = diarioAtual.observacaoFinalDo,
                horarioPontoCidade = diarioAtual.horarioPontoCidade,
                fotoHospedagemPath = caminhoFoto, enderecoHospedagem = endereco,
                diarioFechado = diarioAtual.diarioFechado
            )
        }
    }

    suspend fun concluirEtapa6ECriarDiarioDestino(
        diarioOrigemId: Long,
        obraDestinoId: Long,
        contratoDestinoDescricao: String
    ): Long? {
        return db.withTransaction {
            val diarioOrigem = dao.buscarDiarioPorId(diarioOrigemId)
                ?: return@withTransaction null

            val novoDiarioId = dao.inserirDiario(
                DiarioEntity(
                    obraId = obraDestinoId, data = diarioOrigem.data,
                    encarregado = diarioOrigem.encarregado, equipe = diarioOrigem.equipe,
                    veiculo = diarioOrigem.veiculo,
                    equipamentosAuxiliares = diarioOrigem.equipamentosAuxiliares,
                    localCarregamento = diarioOrigem.localCarregamento,
                    pesoLiquidoTon = diarioOrigem.pesoLiquidoTon,
                    fotoTicketUri = diarioOrigem.fotoTicketUri,
                    statusEquipe = StatusEtapa.CONCLUIDA,
                    statusEquipamento = StatusEtapa.CONCLUIDA,
                    statusCarregamento = StatusEtapa.CONCLUIDA,
                    statusServicos = StatusEtapa.EM_ANDAMENTO,
                    etapaAtual = 4, sincronizado = false
                )
            )

            val deslocamentosPadrao = listOf(
                "Batendo ponto na entrada",
                "Organizando materiais e ferramentas (Manhã)",
                "A caminho da Usina",
                "Chegada na Usina",
                "Carregando asfalto",
                "Término do carregamento",
                "Pesagem do caminhão carregado",
                "Saída da Usina para o trecho",
                "Chegada no trecho"
            )
            deslocamentosPadrao.forEachIndexed { index, titulo ->
                dao.inserirDeslocamento(
                    DeslocamentoItemEntity(
                        diarioId = novoDiarioId, ordem = index + 1, titulo = titulo,
                        inicio = if (index == 8) diarioOrigem.chegadaBase else "", fim = ""
                    )
                )
            }

            dao.atualizarRetornoEFechamentoDo(
                diarioId = diarioOrigemId,
                saidaRetornoBase = diarioOrigem.saidaRetornoBase,
                chegadaBase = diarioOrigem.chegadaBase,
                observacaoRetornoBase = "Continua no diário do contrato: $contratoDestinoDescricao",
                retornoBaseConcluido = true,
                observacaoFinalDo = diarioOrigem.observacaoFinalDo,
                horarioPontoCidade = diarioOrigem.horarioPontoCidade,
                fotoHospedagemPath = diarioOrigem.fotoHospedagemPath,
                enderecoHospedagem = diarioOrigem.enderecoHospedagem,
                diarioFechado = diarioOrigem.diarioFechado
            )
            dao.atualizarStatusEtapasDiario(
                diarioId = diarioOrigemId, etapaAtual = 7,
                statusEquipe = diarioOrigem.statusEquipe,
                statusEquipamento = diarioOrigem.statusEquipamento,
                statusCarregamento = diarioOrigem.statusCarregamento,
                statusServicos = diarioOrigem.statusServicos,
                statusFechamentoServicos = diarioOrigem.statusFechamentoServicos,
                statusRetornoBase = StatusEtapa.CONCLUIDA,
                statusFechamentoDo = StatusEtapa.DISPONIVEL,
                diarioFechado = diarioOrigem.diarioFechado
            )

            novoDiarioId
        }
    }

    // ── Montagem para exportação / relatório ─────────────────────────────

    suspend fun montarServicosParaExportacao(diarioId: Long): List<ServicoExportacao> {
        val diarioCompleto = dao.buscarDiarioCompletoPorId(diarioId) ?: return emptyList()
        return diarioCompleto.servicos.map { servico ->
            ServicoExportacao(
                servico = servico,
                subservicos = dao.listarSubservicosDoServicoDireto(servico.id)
            )
        }
    }

    suspend fun montarDiarioParaExportacao(diarioId: Long): DiarioExportacao? {
        val diarioCompleto = dao.buscarDiarioCompletoPorId(diarioId) ?: return null
        return DiarioExportacao(
            diario = diarioCompleto.diario, obra = diarioCompleto.obra,
            deslocamentos = diarioCompleto.deslocamentos,
            carregamentos = dao.listarCarregamentosDireto(diarioId),
            desvios = diarioCompleto.desvios,
            servicos = montarServicosParaExportacao(diarioId)
        )
    }

    suspend fun montarDiarioParaRelatorio(diarioId: Long): DiarioRelatorio? {
        val diarioCompleto = dao.buscarDiarioCompletoPorId(diarioId) ?: return null
        return DiarioRelatorio(
            diario = diarioCompleto.diario, obra = diarioCompleto.obra,
            deslocamentos = diarioCompleto.deslocamentos,
            carregamentos = dao.listarCarregamentosDireto(diarioId),
            desvios = diarioCompleto.desvios,
            servicos = montarServicosParaExportacao(diarioId)
        )
    }

    suspend fun montarDiarioParaJson(diarioId: Long): DiarioJsonExport? {
        val diarioCompleto = dao.buscarDiarioCompletoPorId(diarioId) ?: return null
        val d = diarioCompleto.diario
        val o = diarioCompleto.obra
        val carregamentos = dao.listarCarregamentosDireto(diarioId)
        val abastecimentos = dao.listarAbastecimentosDireto(diarioId)

        val servicosJson = diarioCompleto.servicos.map { servico ->
            val areas = dao.listarServicoAreasDireto(servico.id)
            ServicoJson(
                ordemServico = servico.ordemServico,
                tipo = servico.tipo,
                numeroProtocolo = servico.numeroProtocolo,
                endereco = servico.endereco,
                comprimento = servico.comprimento,
                largura = servico.largura,
                altura = servico.altura,
                inicio = servico.inicio,
                fim = servico.fim,
                latitude = servico.latitude,
                longitude = servico.longitude,
                nomeRua = servico.nomeRua,
                fotoAntes = servico.fotoUri,
                horarioFotoAntes = servico.horarioFotoAntes,
                fotoCavaAberta = servico.fotoCavaAbertaUri,
                fotoEspessura = servico.fotoEspessuraUri,
                fotoConclusao = servico.fotoConclusaoUri,
                horarioFotoConclusao = servico.horarioFotoConclusao,
                aberturaCava = servico.aberturaCava,
                limpezaEntulho = servico.limpezaEntulho,
                pinturaLigacao = servico.pinturaLigacao,
                equipamentoCompactacaoUsado = servico.equipamentoCompactacaoUsado,
                areas = areas.map { area ->
                    ServicoAreaJson(
                        ordem = area.ordem,
                        comprimento = area.comprimento,
                        largura = area.largura,
                        espessuraCm = area.espessuraCm
                    )
                }
            )
        }

        return DiarioJsonExport(
            data = d.data,
            obra = ObraJson(
                nome = o.nome,
                local = o.local,
                contratante = o.contratante,
                contrato = o.contrato,
                dataInicioContrato = o.dataInicioContrato,
                prazoContratoDias = o.prazoContratoDias,
                espessuraContratoCm = o.espessuraContratoCm
            ),
            status = StatusJson(
                etapaAtual = d.etapaAtual,
                equipe = d.statusEquipe.name,
                equipamento = d.statusEquipamento.name,
                carregamento = d.statusCarregamento.name,
                servicos = d.statusServicos.name,
                fechamentoServicos = d.statusFechamentoServicos.name,
                retornoBase = d.statusRetornoBase.name,
                fechamentoDo = d.statusFechamentoDo.name,
                diarioFechado = d.diarioFechado
            ),
            equipe = EquipeJson(
                encarregado = d.encarregado,
                membros = d.equipe.paraLista()
            ),
            equipamento = EquipamentoJson(
                veiculo = d.veiculo,
                auxiliares = d.equipamentosAuxiliares.paraLista(),
                compactacao = d.equipamentosCompactacao.paraLista()
            ),
            deslocamentos = diarioCompleto.deslocamentos.map { deslocamento ->
                DeslocamentoJson(
                    ordem = deslocamento.ordem,
                    titulo = deslocamento.titulo,
                    inicio = deslocamento.inicio,
                    fim = deslocamento.fim
                )
            },
            carregamentos = carregamentos.map { carregamento ->
                CarregamentoJson(
                    ordem = carregamento.ordem,
                    veiculo = carregamento.veiculo,
                    chegadaUsina = carregamento.chegadaUsina,
                    inicioCarregamento = carregamento.inicioCarregamento,
                    fimCarregamento = carregamento.fimCarregamento,
                    horarioPesagem = carregamento.horarioPesagem,
                    saidaUsinaTrecho = carregamento.saidaUsinaTrecho,
                    localCarregamento = carregamento.localCarregamento,
                    pesoLiquidoTon = carregamento.pesoLiquidoTon,
                    fotoTicket = carregamento.fotoTicketUri,
                    latitude = carregamento.latitude,
                    longitude = carregamento.longitude
                )
            },
            abastecimentos = abastecimentos.map { abastecimento ->
                AbastecimentoJson(
                    veiculo = abastecimento.veiculo,
                    litros = abastecimento.litros,
                    fotoTicket = abastecimento.fotoTicketUri,
                    horario = abastecimento.horario
                )
            },
            intervalo = IntervaloJson(
                inicio = d.inicioIntervalo,
                fim = d.fimIntervalo,
                observacao = d.observacaoIntervalo,
                registrado = d.intervaloRegistrado
            ),
            fechamentoServicos = FechamentoServicosJson(
                horario = d.horarioFechamentoServicos,
                observacao = d.observacaoFechamentoServicos,
                proximoDestino = d.proximoDestino,
                concluido = d.fechamentoServicosConcluido
            ),
            retornoBase = RetornoBaseJson(
                saida = d.saidaRetornoBase,
                chegada = d.chegadaBase,
                observacao = d.observacaoRetornoBase,
                concluido = d.retornoBaseConcluido,
                fotoHospedagem = d.fotoHospedagemPath,
                enderecoHospedagem = d.enderecoHospedagem
            ),
            fechamentoDo = FechamentoDoJson(
                observacaoFinal = d.observacaoFinalDo,
                horarioPontoCidade = d.horarioPontoCidade
            ),
            desvios = diarioCompleto.desvios.map { desvio ->
                DesvioJson(
                    codigo = desvio.codigo,
                    descricao = desvio.descricao,
                    inicio = desvio.inicio,
                    fim = desvio.fim,
                    observacao = desvio.observacao,
                    litros = desvio.litros,
                    fotoTicket = desvio.fotoTicketUri,
                    latitude = desvio.latitude,
                    longitude = desvio.longitude,
                    endereco = desvio.endereco
                )
            },
            servicos = servicosJson
        )
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    private fun horaAtual(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    private fun dataAtual(): String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    suspend fun marcarDiarioComoSincronizado(diarioId: Long) {
        val diario = dao.buscarDiarioPorId(diarioId) ?: return
        dao.atualizarDiario(diario.copy(sincronizado = true))
    }
}
