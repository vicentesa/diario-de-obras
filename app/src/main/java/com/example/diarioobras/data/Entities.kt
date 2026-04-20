package com.example.diarioobras.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "obras")
data class ObraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val local: String = "",
    val contratante: String = "",
    val contrato: String = "",
    val dataInicioContrato: String = "",
    val prazoContratoDias: Int = 0
)

@Entity(
    tableName = "diarios",
    foreignKeys = [
        ForeignKey(
            entity = ObraEntity::class,
            parentColumns = ["id"],
            childColumns = ["obraId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DiarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obraId: Long,
    val data: String,

    // Etapa 1 - Equipe
    val encarregado: String = "",
    val equipe: String = "",

    // Etapa 2 - Equipamento
    val veiculo: String = "",
    val equipamentosAuxiliares: String = "",
    val equipamentosCompactacao: String = "",

    // Etapa 3 - Carregamento / Abastecimento
    val localCarregamento: String = "",
    val pesoLiquidoTon: String = "",
    val fotoTicketUri: String = "",

    // Controle da V2
    val etapaAtual: Int = 1,

    val statusEquipe: String = "DISPONIVEL",
    val statusEquipamento: String = "BLOQUEADA",
    val statusCarregamento: String = "BLOQUEADA",
    val statusServicos: String = "BLOQUEADA",
    val statusFechamentoServicos: String = "BLOQUEADA",
    val statusRetornoBase: String = "BLOQUEADA",
    val statusFechamentoDo: String = "BLOQUEADA",

    // Etapa 4 - Serviços / intervalo legal
    val inicioIntervalo: String? = null,
    val fimIntervalo: String? = null,
    val observacaoIntervalo: String = "",
    val intervaloRegistrado: Boolean = false,

    // Etapa 5 - Fechamento dos serviços
    val horarioFechamentoServicos: String? = null,
    val observacaoFechamentoServicos: String = "",
    val fechamentoServicosConcluido: Boolean = false,

    // Etapa 6 - Retorno à base
    val saidaRetornoBase: String? = null,
    val chegadaBase: String? = null,
    val observacaoRetornoBase: String = "",
    val retornoBaseConcluido: Boolean = false,

    // Etapa 7 - Fechamento do D.O.
    val observacaoFinalDo: String = "",
    val diarioFechado: Boolean = false,

    val sincronizado: Boolean = false
)

@Entity(
    tableName = "deslocamentos",
    foreignKeys = [
        ForeignKey(
            entity = DiarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["diarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DeslocamentoItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val diarioId: Long,
    val ordem: Int,
    val titulo: String,
    val inicio: String? = null,
    val fim: String? = null
)

@Entity(
    tableName = "carregamentos",
    foreignKeys = [
        ForeignKey(
            entity = DiarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["diarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CarregamentoItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val diarioId: Long,
    val ordem: Int,

    val veiculo: String = "",

    val chegadaUsina: String? = null,
    val inicioCarregamento: String? = null,
    val fimCarregamento: String? = null,
    val horarioPesagem: String? = null,
    val saidaUsinaTrecho: String? = null,

    val localCarregamento: String = "",
    val pesoLiquidoTon: String = "",
    val fotoTicketUri: String = ""
)

@Entity(
    tableName = "desvios",
    foreignKeys = [
        ForeignKey(
            entity = DiarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["diarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DesvioItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val diarioId: Long,
    val codigo: String,
    val descricao: String,
    val inicio: String? = null,
    val fim: String? = null
)

@Entity(
    tableName = "servicos",
    foreignKeys = [
        ForeignKey(
            entity = DiarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["diarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("diarioId")]
)
data class ServicoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val diarioId: Long,
    val tipo: String = "Tapa buraco",
    val ordemServico: Int,
    val numeroProtocolo: String = "",
    val endereco: String = "",

    val comprimento: Double = 0.0,
    val largura: Double = 0.0,
    val altura: Double = 0.0,

    val inicio: String? = null,
    val fim: String? = null,

    val latitude: Double? = null,
    val longitude: Double? = null,
    val nomeRua: String? = null,

    val fotoUri: String? = null,
    val fotoCavaAbertaUri: String? = null,
    val fotoEspessuraUri: String? = null,
    val fotoConclusaoUri: String? = null,

    val sincronizado: Boolean = false,
    val aberturaCava: String = "",
    val limpezaEntulho: String = "",
    val pinturaLigacao: Boolean = false,
    val equipamentoCompactacaoUsado: String = ""
)

@Entity(
    tableName = "servico_areas",
    foreignKeys = [
        ForeignKey(
            entity = ServicoEntity::class,
            parentColumns = ["id"],
            childColumns = ["servicoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("servicoId")]
)
data class ServicoAreaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val servicoId: Long,
    val ordem: Int,
    val comprimento: Double,
    val largura: Double,
    val espessuraCm: Double
)

@Entity(
    tableName = "subservicos",
    foreignKeys = [
        ForeignKey(
            entity = ServicoEntity::class,
            parentColumns = ["id"],
            childColumns = ["servicoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SubservicoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val servicoId: Long,
    val tipo: String,
    val comprimento: Double? = null,
    val largura: Double? = null,
    val altura: Double? = null
)

data class DiarioCompleto(
    @Embedded val diario: DiarioEntity,

    @Relation(
        parentColumn = "obraId",
        entityColumn = "id"
    )
    val obra: ObraEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "diarioId"
    )
    val deslocamentos: List<DeslocamentoItemEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "diarioId"
    )
    val desvios: List<DesvioItemEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "diarioId"
    )
    val servicos: List<ServicoEntity>
)

data class ServicoComAreas(
    @Embedded val servico: ServicoEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "servicoId"
    )
    val areas: List<ServicoAreaEntity>
)

data class ServicoExportacao(
    val servico: ServicoEntity,
    val subservicos: List<SubservicoEntity>
)

data class DiarioExportacao(
    val diario: DiarioEntity,
    val obra: ObraEntity,
    val deslocamentos: List<DeslocamentoItemEntity>,
    val carregamentos: List<CarregamentoItemEntity>,
    val desvios: List<DesvioItemEntity>,
    val servicos: List<ServicoExportacao>
)

data class DiarioRelatorio(
    val diario: DiarioEntity,
    val obra: ObraEntity,
    val deslocamentos: List<DeslocamentoItemEntity>,
    val carregamentos: List<CarregamentoItemEntity>,
    val desvios: List<DesvioItemEntity>,
    val servicos: List<ServicoExportacao>
)
