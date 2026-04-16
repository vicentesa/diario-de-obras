package com.example.diarioobras.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction

@Dao
interface ObrasDao {

    @Query("SELECT * FROM obras ORDER BY id DESC")
    fun listarObras(): Flow<List<ObraEntity>>

    @Insert
    suspend fun inserirObra(obra: ObraEntity): Long

    @Query("SELECT * FROM obras WHERE id = :obraId LIMIT 1")
    suspend fun buscarObraPorId(obraId: Long): ObraEntity?

    @Query("SELECT * FROM diarios WHERE obraId = :obraId ORDER BY id DESC")
    fun listarDiariosDaObra(obraId: Long): Flow<List<DiarioEntity>>

    @Insert
    suspend fun inserirDiario(diario: DiarioEntity): Long

    @Query("SELECT * FROM diarios WHERE id = :diarioId LIMIT 1")
    suspend fun buscarDiarioPorId(diarioId: Long): DiarioEntity?

    @Transaction
    @Query("SELECT * FROM diarios WHERE id = :diarioId LIMIT 1")
    suspend fun buscarDiarioCompletoPorId(diarioId: Long): DiarioCompleto?

    @Insert
    suspend fun inserirDeslocamentos(itens: List<DeslocamentoItemEntity>)

    @Query("SELECT * FROM deslocamentos WHERE diarioId = :diarioId ORDER BY ordem ASC")
    fun listarDeslocamentos(diarioId: Long): Flow<List<DeslocamentoItemEntity>>

    @Update
    suspend fun atualizarDeslocamento(item: DeslocamentoItemEntity)

    @Insert
    suspend fun inserirCarregamento(item: CarregamentoItemEntity): Long

    @Query("SELECT * FROM carregamentos WHERE diarioId = :diarioId ORDER BY ordem ASC, id ASC")
    fun listarCarregamentos(diarioId: Long): Flow<List<CarregamentoItemEntity>>

    @Query("SELECT * FROM carregamentos WHERE diarioId = :diarioId ORDER BY ordem ASC, id ASC")
    suspend fun listarCarregamentosDireto(diarioId: Long): List<CarregamentoItemEntity>

    @Update
    suspend fun atualizarCarregamento(item: CarregamentoItemEntity)

    @Delete
    suspend fun excluirCarregamento(item: CarregamentoItemEntity)

    @Insert
    suspend fun inserirServico(servico: ServicoEntity): Long

    @Query("SELECT * FROM servicos WHERE diarioId = :diarioId ORDER BY ordemServico ASC")
    fun listarServicos(diarioId: Long): Flow<List<ServicoEntity>>

    @Update
    suspend fun atualizarDiario(diario: DiarioEntity)

    @Insert
    suspend fun inserirDesvio(item: DesvioItemEntity)

    @Query("SELECT * FROM desvios WHERE diarioId = :diarioId ORDER BY id ASC")
    fun listarDesvios(diarioId: Long): Flow<List<DesvioItemEntity>>

    @Update
    suspend fun atualizarDesvio(item: DesvioItemEntity)

    @Query("SELECT * FROM diarios WHERE obraId = :obraId AND data = :data LIMIT 1")
    suspend fun buscarDiarioPorObraEData(obraId: Long, data: String): DiarioEntity?

    @Update
    suspend fun atualizarServico(servico: ServicoEntity)

    @Query("SELECT * FROM servicos WHERE id = :servicoId LIMIT 1")
    suspend fun buscarServicoPorId(servicoId: Long): ServicoEntity?

    @Insert
    suspend fun inserirSubservico(subservico: SubservicoEntity)

    @Query("SELECT * FROM subservicos WHERE servicoId = :servicoId ORDER BY id ASC")
    fun listarSubservicosDoServico(servicoId: Long): Flow<List<SubservicoEntity>>

    @Query("SELECT * FROM subservicos WHERE servicoId = :servicoId ORDER BY id ASC")
    suspend fun listarSubservicosDoServicoDireto(servicoId: Long): List<SubservicoEntity>

    @Update
    suspend fun atualizarSubservico(subservico: SubservicoEntity)

    @Query("""
        UPDATE diarios SET
            etapaAtual = :etapaAtual,
            statusEquipe = :statusEquipe,
            statusEquipamento = :statusEquipamento,
            statusCarregamento = :statusCarregamento,
            statusServicos = :statusServicos,
            statusFechamentoServicos = :statusFechamentoServicos,
            statusRetornoBase = :statusRetornoBase,
            statusFechamentoDo = :statusFechamentoDo,
            diarioFechado = :diarioFechado
        WHERE id = :diarioId
    """)
    suspend fun atualizarStatusEtapasDiario(
        diarioId: Long,
        etapaAtual: Int,
        statusEquipe: String,
        statusEquipamento: String,
        statusCarregamento: String,
        statusServicos: String,
        statusFechamentoServicos: String,
        statusRetornoBase: String,
        statusFechamentoDo: String,
        diarioFechado: Boolean
    )
    @Query("""
        UPDATE diarios SET
            inicioIntervalo = :inicioIntervalo,
            fimIntervalo = :fimIntervalo,
            observacaoIntervalo = :observacaoIntervalo,
            intervaloRegistrado = :intervaloRegistrado,
            horarioFechamentoServicos = :horarioFechamentoServicos,
            observacaoFechamentoServicos = :observacaoFechamentoServicos,
            fechamentoServicosConcluido = :fechamentoServicosConcluido
        WHERE id = :diarioId
    """)
    suspend fun atualizarIntervaloEFechamentoServicos(
        diarioId: Long,
        inicioIntervalo: String?,
        fimIntervalo: String?,
        observacaoIntervalo: String,
        intervaloRegistrado: Boolean,
        horarioFechamentoServicos: String?,
        observacaoFechamentoServicos: String,
        fechamentoServicosConcluido: Boolean
    )

    @Query("""
        UPDATE diarios SET
            saidaRetornoBase = :saidaRetornoBase,
            chegadaBase = :chegadaBase,
            observacaoRetornoBase = :observacaoRetornoBase,
            retornoBaseConcluido = :retornoBaseConcluido,
            observacaoFinalDo = :observacaoFinalDo,
            diarioFechado = :diarioFechado
        WHERE id = :diarioId
    """)
    suspend fun atualizarRetornoEFechamentoDo(
        diarioId: Long,
        saidaRetornoBase: String?,
        chegadaBase: String?,
        observacaoRetornoBase: String,
        retornoBaseConcluido: Boolean,
        observacaoFinalDo: String,
        diarioFechado: Boolean
    )
}