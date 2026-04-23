package com.example.diarioobras.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.example.diarioobras.data.ServicoEntity

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

    @Query("SELECT * FROM diarios WHERE id = :diarioId LIMIT 1")
    fun buscarDiarioFlowPorId(diarioId: Long): Flow<DiarioEntity?>

    @Update
    suspend fun atualizarDiario(diario: DiarioEntity)

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
    suspend fun inserirDesvio(item: DesvioItemEntity)

    @Query("SELECT * FROM desvios WHERE diarioId = :diarioId ORDER BY id ASC")
    fun listarDesvios(diarioId: Long): Flow<List<DesvioItemEntity>>

    @Update
    suspend fun atualizarDesvio(item: DesvioItemEntity)

    @Insert
    suspend fun inserirServico(servico: ServicoEntity): Long

    @Update
    suspend fun atualizarServico(servico: ServicoEntity)

    @Query("SELECT * FROM servicos WHERE diarioId = :diarioId ORDER BY ordemServico ASC")
    fun listarServicos(diarioId: Long): Flow<List<ServicoEntity>>

    @Query("SELECT * FROM servicos WHERE id = :servicoId LIMIT 1")
    suspend fun buscarServicoPorId(servicoId: Long): ServicoEntity?

    @Transaction
    @Query("SELECT * FROM servicos WHERE id = :servicoId LIMIT 1")
    suspend fun buscarServicoComAreasPorId(servicoId: Long): ServicoComAreas?

    @Insert
    suspend fun inserirSubservico(subservico: SubservicoEntity)

    @Query("SELECT * FROM subservicos WHERE servicoId = :servicoId ORDER BY id ASC")
    fun listarSubservicosDoServico(servicoId: Long): Flow<List<SubservicoEntity>>

    @Query("SELECT * FROM subservicos WHERE servicoId = :servicoId ORDER BY id ASC")
    suspend fun listarSubservicosDoServicoDireto(servicoId: Long): List<SubservicoEntity>

    @Update
    suspend fun atualizarSubservico(subservico: SubservicoEntity)

    @Insert
    suspend fun inserirServicoAreas(itens: List<ServicoAreaEntity>)

    @Insert
    suspend fun inserirServicoArea(item: ServicoAreaEntity): Long

    @Query("SELECT * FROM servico_areas WHERE servicoId = :servicoId ORDER BY ordem ASC, id ASC")
    fun listarServicoAreas(servicoId: Long): Flow<List<ServicoAreaEntity>>

    @Query("SELECT * FROM servico_areas WHERE servicoId = :servicoId ORDER BY ordem ASC, id ASC")
    suspend fun listarServicoAreasDireto(servicoId: Long): List<ServicoAreaEntity>

    @Query("DELETE FROM servico_areas WHERE servicoId = :servicoId")
    suspend fun excluirServicoAreasPorServicoId(servicoId: Long)

    @Update
    suspend fun atualizarServicoArea(item: ServicoAreaEntity)

    @Delete
    suspend fun excluirServicoArea(item: ServicoAreaEntity)

    @Query("SELECT * FROM diarios WHERE obraId = :obraId AND data = :data LIMIT 1")
    suspend fun buscarDiarioPorObraEData(obraId: Long, data: String): DiarioEntity?

    @Query(
        """
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
        """
    )
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

    @Query(
        """
        UPDATE diarios SET
            inicioIntervalo = :inicioIntervalo,
            fimIntervalo = :fimIntervalo,
            observacaoIntervalo = :observacaoIntervalo,
            intervaloRegistrado = :intervaloRegistrado,
            horarioFechamentoServicos = :horarioFechamentoServicos,
            observacaoFechamentoServicos = :observacaoFechamentoServicos,
            proximoDestino = :proximoDestino,
            fechamentoServicosConcluido = :fechamentoServicosConcluido
        WHERE id = :diarioId
        """
    )
    suspend fun atualizarIntervaloEFechamentoServicos(
        diarioId: Long,
        inicioIntervalo: String?,
        fimIntervalo: String?,
        observacaoIntervalo: String,
        intervaloRegistrado: Boolean,
        horarioFechamentoServicos: String?,
        observacaoFechamentoServicos: String,
        proximoDestino: String,
        fechamentoServicosConcluido: Boolean

    )

    @Query(
        """
        UPDATE diarios SET
            saidaRetornoBase = :saidaRetornoBase,
            chegadaBase = :chegadaBase,
            observacaoRetornoBase = :observacaoRetornoBase,
            retornoBaseConcluido = :retornoBaseConcluido,
            observacaoFinalDo = :observacaoFinalDo,
            diarioFechado = :diarioFechado
        WHERE id = :diarioId
        """
    )
    suspend fun atualizarRetornoEFechamentoDo(
        diarioId: Long,
        saidaRetornoBase: String?,
        chegadaBase: String?,
        observacaoRetornoBase: String,
        retornoBaseConcluido: Boolean,
        observacaoFinalDo: String,
        diarioFechado: Boolean
    )

    @Query("DELETE FROM servicos WHERE id = :servicoId")
    suspend fun excluirServicoPorId(servicoId: Long)

    @Delete
    suspend fun excluirServico(servico: ServicoEntity)
}
