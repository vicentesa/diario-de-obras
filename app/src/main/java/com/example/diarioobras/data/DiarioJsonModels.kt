package com.example.diarioobras.data

data class DiarioJsonExport(
    val data: String,
    val obra: ObraJson,
    val status: StatusJson,
    val equipe: EquipeJson,
    val equipamento: EquipamentoJson,
    val deslocamentos: List<DeslocamentoJson>,
    val carregamentos: List<CarregamentoJson>,
    val abastecimentos: List<AbastecimentoJson>,
    val intervalo: IntervaloJson,
    val fechamentoServicos: FechamentoServicosJson,
    val retornoBase: RetornoBaseJson,
    val fechamentoDo: FechamentoDoJson,
    val desvios: List<DesvioJson>,
    val servicos: List<ServicoJson>
)

data class ObraJson(
    val nome: String,
    val local: String,
    val contratante: String,
    val contrato: String,
    val dataInicioContrato: String,
    val prazoContratoDias: Int,
    val espessuraContratoCm: Double,
    val exigeFotosDimensoes: Boolean = false
)

data class StatusJson(
    val etapaAtual: Int,
    val equipe: String,
    val equipamento: String,
    val carregamento: String,
    val servicos: String,
    val fechamentoServicos: String,
    val retornoBase: String,
    val fechamentoDo: String,
    val diarioFechado: Boolean
)

data class EquipeJson(
    val encarregado: String,
    val membros: List<String>
)

data class EquipamentoJson(
    val veiculo: String,
    val auxiliares: List<String>,
    val compactacao: List<String>
)

data class DeslocamentoJson(
    val ordem: Int,
    val titulo: String,
    val inicio: String?,
    val fim: String?
)

data class CarregamentoJson(
    val ordem: Int,
    val veiculo: String,
    val chegadaUsina: String?,
    val inicioCarregamento: String?,
    val fimCarregamento: String?,
    val horarioPesagem: String?,
    val saidaUsinaTrecho: String?,
    val localCarregamento: String,
    val pesoLiquidoTon: String,
    val fotoTicket: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class AbastecimentoJson(
    val veiculo: String,
    val litros: Double,
    val fotoTicket: String,
    val horario: String?
)

data class IntervaloJson(
    val inicio: String?,
    val fim: String?,
    val observacao: String,
    val registrado: Boolean
)

data class FechamentoServicosJson(
    val horario: String?,
    val observacao: String,
    val proximoDestino: String,
    val concluido: Boolean
)

data class RetornoBaseJson(
    val saida: String?,
    val chegada: String?,
    val observacao: String,
    val concluido: Boolean,
    val fotoHospedagem: String,
    val enderecoHospedagem: String
)

data class FechamentoDoJson(
    val observacaoFinal: String,
    val horarioPontoCidade: String? = null
)

data class DesvioJson(
    val codigo: String,
    val descricao: String,
    val inicio: String,
    val fim: String,
    val observacao: String,
    val litros: Double = 0.0,
    val fotoTicket: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val endereco: String = ""
)

data class ServicoJson(
    val ordemServico: Int,
    val tipo: String,
    val numeroProtocolo: String,
    val endereco: String,
    val comprimento: Double,
    val largura: Double,
    val altura: Double,
    val inicio: String?,
    val fim: String?,
    val latitude: Double?,
    val longitude: Double?,
    val nomeRua: String?,
    val fotoAntes: String?,
    val horarioFotoAntes: String?,
    val fotoCavaAberta: String?,
    val fotoEspessura: String?,
    val fotoCompactacao: String? = null,
    val fotoConclusao: String?,
    val horarioFotoConclusao: String?,
    val aberturaCava: String,
    val limpezaEntulho: String,
    val pinturaLigacao: Boolean,
    val equipamentoCompactacaoUsado: String,
    val areas: List<ServicoAreaJson>
)

data class ServicoAreaJson(
    val ordem: Int,
    val comprimento: Double,
    val largura: Double,
    val espessuraCm: Double,
    val fotoDim1: String? = null,
    val fotoDim2: String? = null
)

fun String.paraLista(): List<String> =
    if (isBlank()) emptyList() else split(" / ").filter { it.isNotBlank() }