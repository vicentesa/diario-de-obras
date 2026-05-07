package com.example.diarioobras.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.diarioobras.data.DiarioEntity
import com.example.diarioobras.data.ObraEntity
import com.google.android.gms.location.LocationServices
import java.io.File
import java.util.Locale
import java.util.UUID
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.ExperimentalFoundationApi


import com.example.diarioobras.data.ServicoEntity
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.Image

import android.location.Address
import android.os.Build
import com.google.android.gms.location.Priority

import com.example.diarioobras.data.DesvioItemEntity
import com.example.diarioobras.ui.MainViewModel
import androidx.compose.foundation.layout.Row


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiarioEtapasScreen(
    diarioId: Long,
    viewModel: MainViewModel,
    onAbrirServico: (Long) -> Unit,
    onAbrirDiario: (Long) -> Unit
) {

    val tiposDesvio: List<Pair<String, String>> = listOf(
        "P1" to "Refeição / Intervalo",
        "P2" to "Chuva",
        "P3" to "Quebra veículo",
        "P4" to "Quebra gerador",
        "P5" to "Quebra martelete",
        "P6" to "Quebra compactador de percussão",
        "P7" to "Quebra placa vibratória",
        "P8" to "Falta de material - insumos",
        "P9" to "Falta de material hidráulico",
        "P10" to "Falta de ferramentas",
        "P11" to "Aguardando fila (usina)",
        "P12" to "Carregamento asfalto",
        "P13" to "Deslocamento",
        "P14" to "Quebra equipamento de escavação",
        "P15" to "Quebra rompedor hidráulico",
        "P16" to "Interferência rede de água",
        "P17" to "Interferência rede pluvial",
        "P18" to "Interferência rede elétrica/dados",
        "P19" to "Aguardando fiscalização",
        "P20" to "Solicitação órgão público",
        "P21" to "Encontrado rocha",
        "P22" to "Solicitação setor segurança",
        "P23" to "Falta de EPI",
        "P24" to "Falta de EPC",
        "P25" to "Sinalização de via",
        "P30" to "Falta de colaborador",
        "P31" to "Aguardando liberação",
        "P34" to "Programação insuficiente",
        "P35" to "Sem atividade",
        "P38" to "DDS",
        "P39" to "Treinamento",
        "P45" to "Limpeza do canteiro",
        "T1" to "Trabalhando",
        "T2" to "Retrabalho / garantia",
        "T3" to "Outro serviço já apontado"
    )

    var obra by remember { mutableStateOf<ObraEntity?>(null) }
    var etapaExpandida by remember(diarioId) { mutableStateOf(1) }
    var dadosLocaisInicializados by remember(diarioId) { mutableStateOf(false) }

    val diarioFlow = remember(diarioId) { viewModel.buscarDiarioFlow(diarioId) }
    val servicosFlow = remember(diarioId) { viewModel.servicosDoDiario(diarioId) }
    val desviosFlow = remember(diarioId) { viewModel.desviosDoDiario(diarioId) }
    val deslocamentosFlow = remember(diarioId) { viewModel.deslocamentosDoDiario(diarioId) }
    val carregamentosFlow = remember(diarioId) { viewModel.carregamentosDoDiario(diarioId) }

    val diario by diarioFlow.collectAsStateWithLifecycle(initialValue = null)
    val servicos by servicosFlow.collectAsStateWithLifecycle()
    val desvios by desviosFlow.collectAsStateWithLifecycle()
    val deslocamentos by deslocamentosFlow.collectAsStateWithLifecycle()
    val carregamentos by carregamentosFlow.collectAsStateWithLifecycle()
    val obras by viewModel.obras.collectAsStateWithLifecycle()

    var desviosExpandido by remember { mutableStateOf(false) }

    // Etapa 1
    var menuEncarregadoExpandido by remember { mutableStateOf(false) }
    var encarregadoSelecionado by remember(diarioId) { mutableStateOf("") }

    var menuEquipeExpandido by remember { mutableStateOf(false) }
    var equipeSelecionada by remember(diarioId) { mutableStateOf(setOf<String>()) }


    var obraDestinoIdSelecionada by remember(diarioId) { mutableStateOf<Long?>(null) }

    // Etapa 2
    var menuVeiculoExpandido by remember { mutableStateOf(false) }
    var veiculosSelecionados by remember(diarioId) { mutableStateOf(setOf<String>()) }

    var menuCompactacaoExpandido by remember { mutableStateOf(false) }
    var equipamentosCompactacaoSelecionados by remember(diarioId) { mutableStateOf(setOf<String>()) }

    var menuEquipamentosExpandido by remember { mutableStateOf(false) }
    var equipamentosSelecionados by remember(diarioId) { mutableStateOf(setOf<String>()) }

    // Etapa 3
    var menuVeiculoEtapa3Expandido by remember { mutableStateOf(false) }
    var veiculoEtapa3Selecionado by remember(diarioId) { mutableStateOf("") }
    val quantidadesEtapa3 = remember(diarioId) { mutableStateListOf<String>() }
    var fotoTicketEtapa3Uri by remember(diarioId) { mutableStateOf<Uri?>(null) }
    var mostrarTicketAmpliado by remember { mutableStateOf(false) }
    var versaoPreviewTicket by remember(diarioId) { mutableStateOf(0) }

    // Etapa 4
    var exibindoCadastroServico by remember(diarioId) { mutableStateOf(false) }
    var descricaoServicoEtapa4 by remember(diarioId) { mutableStateOf("") }

    var protocolo by remember { mutableStateOf("") }
    var ordemServico by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf("") }

    var aberturaCava by remember { mutableStateOf("") }
    var menuAberturaExpandido by remember { mutableStateOf(false) }

    var limpezaEntulho by remember { mutableStateOf("") }
    var menuLimpezaExpandido by remember { mutableStateOf(false) }

    var fotoAntesUri by remember { mutableStateOf<Uri?>(null) }

    var latitudeServico by remember { mutableStateOf<Double?>(null) }
    var longitudeServico by remember { mutableStateOf<Double?>(null) }
    var enderecoServico by remember { mutableStateOf("") }

    var servicoSelecionadoParaExcluir by remember { mutableStateOf<Long?>(null) }
    var servicoPendenteExclusao by remember { mutableStateOf<ServicoEntity?>(null) }

    var mostrarDialogoEncerrarServicos by remember { mutableStateOf(false) }

    var menuProximoDestinoExpandido by remember { mutableStateOf(false) }
    var proximoDestinoSelecionado by remember(diarioId) { mutableStateOf("") }

    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var fotoHospedagemUriTemporaria by remember(diarioId) { mutableStateOf<Uri?>(null) }
    var versaoPreviewHospedagem by remember(diarioId) { mutableStateOf(0) }

    var menuOutroContratoExpandido by remember { mutableStateOf(false) }
    var outroContratoSelecionado by remember(diarioId) { mutableStateOf("") }

    var menuTipoDesvioExpandido by remember { mutableStateOf(false) }

    val hospedagemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            versaoPreviewHospedagem++

            val uriFoto = fotoHospedagemUriTemporaria?.toString().orEmpty()

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            val geocoder = Geocoder(context, Locale("pt", "BR"))

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                geocoder.getFromLocation(
                                    location.latitude,
                                    location.longitude,
                                    1,
                                    object : Geocoder.GeocodeListener {
                                        override fun onGeocode(addresses: List<Address>) {
                                            val address = addresses.firstOrNull()
                                            val rua = address?.thoroughfare.orEmpty()
                                            val numero = address?.subThoroughfare.orEmpty()

                                            val endereco = if (rua.isNotBlank() || numero.isNotBlank()) {
                                                listOf(rua, numero)
                                                    .filter { it.isNotBlank() }
                                                    .joinToString(", ")
                                            } else {
                                                address?.getAddressLine(0).orEmpty()
                                            }

                                            viewModel.salvarFotoHospedagem(
                                                diarioId = diarioId,
                                                caminhoFoto = uriFoto,
                                                endereco = endereco
                                            )
                                        }

                                        override fun onError(errorMessage: String?) {
                                            viewModel.salvarFotoHospedagem(
                                                diarioId = diarioId,
                                                caminhoFoto = uriFoto,
                                                endereco = ""
                                            )
                                        }
                                    }
                                )
                            } else {
                                val addresses = try {
                                    geocoder.getFromLocation(
                                        location.latitude,
                                        location.longitude,
                                        1
                                    )
                                } catch (e: Exception) {
                                    null
                                }

                                val address = addresses?.firstOrNull()
                                val rua = address?.thoroughfare.orEmpty()
                                val numero = address?.subThoroughfare.orEmpty()

                                val endereco = if (rua.isNotBlank() || numero.isNotBlank()) {
                                    listOf(rua, numero)
                                        .filter { it.isNotBlank() }
                                        .joinToString(", ")
                                } else {
                                    address?.getAddressLine(0).orEmpty()
                                }

                                viewModel.salvarFotoHospedagem(
                                    diarioId = diarioId,
                                    caminhoFoto = uriFoto,
                                    endereco = endereco
                                )
                            }
                        } else {
                            fusedLocationClient.getCurrentLocation(
                                Priority.PRIORITY_HIGH_ACCURACY,
                                null
                            ).addOnSuccessListener { currentLocation ->
                                val endereco = ""

                                viewModel.salvarFotoHospedagem(
                                    diarioId = diarioId,
                                    caminhoFoto = uriFoto,
                                    endereco = endereco
                                )
                            }
                        }
                    }
            } else {
                viewModel.salvarFotoHospedagem(
                    diarioId = diarioId,
                    caminhoFoto = uriFoto,
                    endereco = ""
                )
            }
        }
    }

    val areasPorServico = remember { mutableStateMapOf<Long, Double>() }

    LaunchedEffect(servicos) {
        val novoMapa = mutableMapOf<Long, Double>()

        servicos.forEach { servico ->
            val areas = viewModel.listarServicoAreasDireto(servico.id)
            val areaTotal = areas.sumOf { it.comprimento * it.largura }
            novoMapa[servico.id] = areaTotal
        }

        areasPorServico.clear()
        areasPorServico.putAll(novoMapa)
    }

    val areaTotalDoDia = areasPorServico.values.sum()

    LaunchedEffect(diario?.obraId) {
        val obraId = diario?.obraId ?: return@LaunchedEffect
        obra = viewModel.buscarObraPorId(obraId)
    }

    LaunchedEffect(diario?.id) {
        val diarioAtual = diario ?: return@LaunchedEffect
        if (dadosLocaisInicializados) return@LaunchedEffect

        etapaExpandida = diarioAtual.etapaAtual
        proximoDestinoSelecionado = diarioAtual.proximoDestino

        // Etapa 1
        encarregadoSelecionado = diarioAtual.encarregado
        equipeSelecionada = diarioAtual.equipe
            .split(" / ")
            .filter { nome -> nome.isNotBlank() }
            .toSet()

        // Etapa 2
        veiculosSelecionados = diarioAtual.veiculo
            .split(" / ")
            .filter { nome -> nome.isNotBlank() }
            .toSet()

        equipamentosCompactacaoSelecionados = diarioAtual.equipamentosCompactacao
            .split(" / ")
            .filter { nome -> nome.isNotBlank() }
            .toSet()

        equipamentosSelecionados = diarioAtual.equipamentosAuxiliares
            .split(" / ")
            .filter { nome -> nome.isNotBlank() }
            .toSet()

        // Etapa 3
        if (veiculoEtapa3Selecionado.isBlank()) {
            val veiculos = diarioAtual.veiculo
                .split(" / ")
                .filter { nome -> nome.isNotBlank() }

            veiculoEtapa3Selecionado =
                if (veiculos.size == 1) veiculos.first() else ""
        }

        fotoTicketEtapa3Uri =
            if (diarioAtual.fotoTicketUri.isNotBlank()) Uri.parse(diarioAtual.fotoTicketUri) else null

        dadosLocaisInicializados = true
    }

    LaunchedEffect(diario?.etapaAtual) {
        val etapaAtualBanco = diario?.etapaAtual ?: return@LaunchedEffect
        if (etapaAtualBanco > etapaExpandida) {
            etapaExpandida = etapaAtualBanco
        }
    }

    LaunchedEffect(servicos) {
        servicos.forEach { servico ->
            val areas = viewModel.listarServicoAreasDireto(servico.id)
            val areaTotal = areas.sumOf { it.comprimento * it.largura }
            areasPorServico[servico.id] = areaTotal
        }
    }

    val cameraServicoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        latitudeServico = location.latitude
                        longitudeServico = location.longitude

                        val geocoder = Geocoder(context, Locale.getDefault())

                        try {
                            val lista = geocoder.getFromLocation(
                                latitudeServico!!,
                                longitudeServico!!,
                                1
                            )

                            if (!lista.isNullOrEmpty()) {
                                val enderecoCompleto = lista[0].getAddressLine(0)
                                endereco = enderecoCompleto ?: ""
                            }
                        } catch (_: Exception) {
                            endereco = "Endereço não encontrado"
                        }
                    } else {
                        endereco = "Localização indisponível"
                    }
                }
        }
    }

    val cameraPermissionServicoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val novaUri = criarUriParaFotoEtapa(context)
            fotoAntesUri = novaUri
            cameraServicoLauncher.launch(novaUri)
        }
    }

    val ticketCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            versaoPreviewTicket++
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val novaUri = criarUriParaFotoEtapa(context)
            fotoTicketEtapa3Uri = novaUri
            ticketCameraLauncher.launch(novaUri)
        }
    }

    val hospedagemCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val novaUri = criarUriParaFotoDiario(context)
            fotoHospedagemUriTemporaria = novaUri
            hospedagemCameraLauncher.launch(novaUri)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val deslocamentosEtapa1 = deslocamentos.filter { item ->
        val titulo = item.titulo.trim().lowercase()
        titulo.contains("batendo ponto") && titulo.contains("entrada")
    }

    val deslocamentosEtapa2 = deslocamentos.filter {
        it.titulo.equals(
            "Organizando materiais e ferramentas para o trabalho (Manhã)",
            ignoreCase = true
        ) || it.titulo.equals("A caminho da Usina", ignoreCase = true)
    }

    val deslocamentoChegadaUsina = deslocamentos.filter {
        it.titulo.equals("Chegada na Usina", ignoreCase = true)
    }

    val deslocamentoInicioCarregamento = deslocamentos.filter {
        it.titulo.equals("Carregando asfalto", ignoreCase = true)
    }

    val deslocamentoFimCarregamento = deslocamentos.filter {
        it.titulo.equals("Término do carregamento", ignoreCase = true)
    }

    val veiculosDisponiveisEtapa3 = diario?.veiculo
        ?.split(" / ")
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    val veiculosEtapa3 = diario?.veiculo
        ?.split(" / ")
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    LaunchedEffect(veiculosEtapa3.size) {
        if (veiculosEtapa3.isNotEmpty() && quantidadesEtapa3.size != veiculosEtapa3.size) {
            quantidadesEtapa3.clear()
            repeat(veiculosEtapa3.size) {
                quantidadesEtapa3.add("")
            }
        }
    }

    val veiculosJaCarregados = remember(carregamentos) {
        carregamentos
            .map { it.veiculo }
            .filter { it.isNotBlank() }
            .toSet()
    }

    val veiculosPendentesEtapa3 = remember(veiculosDisponiveisEtapa3, veiculosJaCarregados) {
        veiculosDisponiveisEtapa3.filter { !veiculosJaCarregados.contains(it) }
    }

    val deslocamentoPesagemCarregado = deslocamentos.filter {
        it.titulo.equals("Pesagem do caminhão carregado", ignoreCase = true)
    }

    val deslocamentoSaidaUsinaTrecho = deslocamentos.filter {
        it.titulo.equals("Saída da Usina para o trecho", ignoreCase = true)
    }

    val deslocamentoChegadaTrechoEtapa4 = deslocamentos.filter {
        it.titulo.equals("Chegada no trecho", ignoreCase = true)
    }

    val quantidadesPorVeiculoEtapa3 = remember(diarioId) {
        mutableStateMapOf<String, String>()
    }

    val todasQuantidadesPreenchidas = veiculosEtapa3.all { veiculo ->
        quantidadesPorVeiculoEtapa3[veiculo].orEmpty().isNotBlank()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Diário ${diario?.data ?: ""}")
                        obra?.nome?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                EtapaCard(
                    numero = 1,
                    titulo = "Equipe",
                    status = diario?.statusEquipe ?: "BLOQUEADA",
                    expandida = etapaExpandida == 1,
                    onClick = {
                        etapaExpandida = 1
                        desviosExpandido = false
                    },
                    conteudo = {
                        val etapa1PodeConcluir =
                            encarregadoSelecionado.isNotBlank() &&
                                    equipeSelecionada.isNotEmpty() &&
                                    deslocamentosEtapa1.any { !it.inicio.isNullOrBlank() }

                        Column {
                            Text("Encarregado", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { menuEncarregadoExpandido = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (encarregadoSelecionado.isBlank()) {
                                        "Selecionar encarregado"
                                    } else {
                                        encarregadoSelecionado
                                    }
                                )
                            }

                            DropdownMenu(
                                expanded = menuEncarregadoExpandido,
                                onDismissRequest = { menuEncarregadoExpandido = false }
                            ) {
                                LISTA_FUNCIONARIOS.forEach { nome ->
                                    DropdownMenuItem(
                                        text = { Text(nome) },
                                        onClick = {
                                            encarregadoSelecionado = nome
                                            menuEncarregadoExpandido = false
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Equipe", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { menuEquipeExpandido = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (equipeSelecionada.isEmpty()) {
                                        "Selecionar equipe"
                                    } else {
                                        "Equipe selecionada (${equipeSelecionada.size})"
                                    }
                                )
                            }

                            DropdownMenu(
                                expanded = menuEquipeExpandido,
                                onDismissRequest = { menuEquipeExpandido = false }
                            ) {
                                LISTA_FUNCIONARIOS.forEach { nome ->
                                    DropdownMenuItem(
                                        text = { Text(nome) },
                                        onClick = {
                                            equipeSelecionada =
                                                if (equipeSelecionada.contains(nome)) {
                                                    equipeSelecionada - nome
                                                } else {
                                                    equipeSelecionada + nome
                                                }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (equipeSelecionada.isNotEmpty()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    equipeSelecionada.forEach { nome ->
                                        Text("• $nome")
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))


                            Text("Deslocamento inicial", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (deslocamentosEtapa1.isEmpty()) {
                                Text(
                                    text = "Deslocamento inicial não encontrado.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                                deslocamentosEtapa1.forEach { item ->
                                    DeslocamentoCard(
                                        item = item,
                                        somenteInicio = true,
                                        onMarcarInicio = { viewModel.marcarInicio(item) },
                                        onMarcarFim = { },
                                        onSalvarManual = { inicio, _ ->
                                            viewModel.atualizarHorarioManual(item, inicio, item.fim.orEmpty())
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.atualizarEquipeDiario(
                                        diarioId = diarioId,
                                        encarregado = encarregadoSelecionado,
                                        equipe = equipeSelecionada.toList(),
                                        veiculo = diario?.veiculo.orEmpty(),
                                        equipamentosAuxiliares = diario?.equipamentosAuxiliares
                                            ?.split(" / ")
                                            ?.filter { it.isNotBlank() }
                                            ?: emptyList()
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = etapa1PodeConcluir
                            ) {
                                Text("Salvar e concluir etapa 1")
                            }
                        }
                    }
                )

                EtapaCard(
                    numero = 2,
                    titulo = "Equipamento",
                    status = diario?.statusEquipamento ?: "BLOQUEADA",
                    expandida = etapaExpandida == 2,
                    onClick = {
                        etapaExpandida = 2
                        desviosExpandido = false
                    },
                    conteudo = {
                        val etapa2PodeConcluir =
                            veiculosSelecionados.isNotEmpty() &&
                                    equipamentosCompactacaoSelecionados.isNotEmpty() &&
                                    deslocamentosEtapa2.all { !it.inicio.isNullOrBlank() }

                        Column {
                            Text("Veículos", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { menuVeiculoExpandido = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (veiculosSelecionados.isEmpty()) {
                                        "Selecionar veículos"
                                    } else {
                                        "Veículos selecionados (${veiculosSelecionados.size})"
                                    }
                                )
                            }

                            DropdownMenu(
                                expanded = menuVeiculoExpandido,
                                onDismissRequest = { menuVeiculoExpandido = false }
                            ) {
                                LISTA_VEICULOS.forEach { veiculo ->
                                    DropdownMenuItem(
                                        text = { Text(veiculo) },
                                        onClick = {
                                            veiculosSelecionados =
                                                if (veiculosSelecionados.contains(veiculo)) {
                                                    veiculosSelecionados - veiculo
                                                } else {
                                                    veiculosSelecionados + veiculo
                                                }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (veiculosSelecionados.isNotEmpty()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    veiculosSelecionados.forEach { nome ->
                                        Text("• $nome")
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Text("Equipamento de Compactação", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { menuCompactacaoExpandido = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (equipamentosCompactacaoSelecionados.isEmpty()) {
                                        "Selecionar equipamentos de compactação"
                                    } else {
                                        "Compactação selecionada (${equipamentosCompactacaoSelecionados.size})"
                                    }
                                )
                            }

                            DropdownMenu(
                                expanded = menuCompactacaoExpandido,
                                onDismissRequest = { menuCompactacaoExpandido = false }
                            ) {
                                LISTA_EQUIPAMENTOS_COMPACTACAO.forEach { equipamento ->
                                    DropdownMenuItem(
                                        text = { Text(equipamento) },
                                        onClick = {
                                            equipamentosCompactacaoSelecionados =
                                                if (equipamentosCompactacaoSelecionados.contains(equipamento)) {
                                                    equipamentosCompactacaoSelecionados - equipamento
                                                } else {
                                                    equipamentosCompactacaoSelecionados + equipamento
                                                }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (equipamentosCompactacaoSelecionados.isNotEmpty()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    equipamentosCompactacaoSelecionados.forEach { nome ->
                                        Text("• $nome")
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Text("Equipamentos auxiliares", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { menuEquipamentosExpandido = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (equipamentosSelecionados.isEmpty()) {
                                        "Selecionar equipamentos auxiliares"
                                    } else {
                                        "Equipamentos selecionados (${equipamentosSelecionados.size})"
                                    }
                                )
                            }

                            DropdownMenu(
                                expanded = menuEquipamentosExpandido,
                                onDismissRequest = { menuEquipamentosExpandido = false }
                            ) {
                                LISTA_EQUIPAMENTOS_AUXILIARES.forEach { equipamento ->
                                    DropdownMenuItem(
                                        text = { Text(equipamento) },
                                        onClick = {
                                            equipamentosSelecionados =
                                                if (equipamentosSelecionados.contains(equipamento)) {
                                                    equipamentosSelecionados - equipamento
                                                } else {
                                                    equipamentosSelecionados + equipamento
                                                }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (equipamentosSelecionados.isNotEmpty()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    equipamentosSelecionados.forEach { nome ->
                                        Text("• $nome")
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Text("Deslocamentos iniciais", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            deslocamentosEtapa2.forEach { item ->
                                DeslocamentoCard(
                                    item = item,
                                    somenteInicio = true,
                                    onMarcarInicio = { viewModel.marcarInicio(item) },
                                    onMarcarFim = { },
                                    onSalvarManual = { inicio, _ ->
                                        viewModel.atualizarHorarioManual(item, inicio, item.fim.orEmpty())
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.atualizarEquipamentoDiario(
                                        diarioId = diarioId,
                                        veiculo = veiculosSelecionados.joinToString(" / "),
                                        equipamentosAuxiliares = equipamentosSelecionados.toList(),
                                        equipamentosCompactacao = equipamentosCompactacaoSelecionados.toList()
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = etapa2PodeConcluir
                            ) {
                                Text("Salvar e concluir etapa 2")
                            }
                        }
                    }
                )

                EtapaCard(
                    numero = 3,
                    titulo = "Carregamento / Abastecimento",
                    status = diario?.statusCarregamento ?: "BLOQUEADA",
                    expandida = etapaExpandida == 3,
                    onClick = {
                        etapaExpandida = 3
                        desviosExpandido = false
                    },
                    conteudo = {
                        val todasQuantidadesPreenchidasLocais =
                            quantidadesEtapa3.size == veiculosEtapa3.size &&
                                    quantidadesEtapa3.all { it.isNotBlank() }

                        val etapa3PodeConcluir =
                            deslocamentoChegadaUsina.all { !it.inicio.isNullOrBlank() } &&
                                    deslocamentoInicioCarregamento.all { !it.inicio.isNullOrBlank() } &&
                                    deslocamentoFimCarregamento.all { !it.inicio.isNullOrBlank() } &&
                                    deslocamentoPesagemCarregado.all { !it.inicio.isNullOrBlank() } &&
                                    deslocamentoSaidaUsinaTrecho.all { !it.inicio.isNullOrBlank() } &&
                                    todasQuantidadesPreenchidasLocais &&
                                    fotoTicketEtapa3Uri != null

                        Column {
                            Text("Chegada na usina", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            deslocamentoChegadaUsina.forEach { item ->
                                DeslocamentoCard(
                                    item = item,
                                    somenteInicio = true,
                                    onMarcarInicio = { viewModel.marcarInicio(item) },
                                    onMarcarFim = { },
                                    onSalvarManual = { inicio, _ ->
                                        viewModel.atualizarHorarioManual(item, inicio, item.fim.orEmpty())
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Início do carregamento", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            deslocamentoInicioCarregamento.forEach { item ->
                                DeslocamentoCard(
                                    item = item,
                                    somenteInicio = true,
                                    onMarcarInicio = { viewModel.marcarInicio(item) },
                                    onMarcarFim = { },
                                    onSalvarManual = { inicio, _ ->
                                        viewModel.atualizarHorarioManual(item, inicio, item.fim.orEmpty())
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Término do carregamento", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            deslocamentoFimCarregamento.forEach { item ->
                                DeslocamentoCard(
                                    item = item,
                                    somenteInicio = true,
                                    onMarcarInicio = { viewModel.marcarInicio(item) },
                                    onMarcarFim = { },
                                    onSalvarManual = { inicio, _ ->
                                        viewModel.atualizarHorarioManual(item, inicio, item.fim.orEmpty())
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Pesagem do caminhão carregado", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            deslocamentoPesagemCarregado.forEach { item ->
                                DeslocamentoCard(
                                    item = item,
                                    somenteInicio = true,
                                    onMarcarInicio = { viewModel.marcarInicio(item) },
                                    onMarcarFim = { },
                                    onSalvarManual = { inicio, _ ->
                                        viewModel.atualizarHorarioManual(item, inicio, item.fim.orEmpty())
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Foto dos tickets", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    if (
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        val novaUri = criarUriParaFotoEtapa(context)
                                        fotoTicketEtapa3Uri = novaUri
                                        ticketCameraLauncher.launch(novaUri)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (fotoTicketEtapa3Uri == null) {
                                        "Foto do Ticket"
                                    } else {
                                        "Ticket capturado"
                                    }
                                )
                            }

                            if (fotoTicketEtapa3Uri != null) {
                                Spacer(modifier = Modifier.height(12.dp))

                                AsyncImage(
                                    model = fotoTicketEtapa3Uri?.let { "${it}#${versaoPreviewTicket}" },
                                    contentDescription = "Miniatura do ticket",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clickable { mostrarTicketAmpliado = true }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            CamposQuantidadeEtapa3(
                                veiculos = veiculosEtapa3,
                                quantidades = quantidadesEtapa3
                            )

                            Text("Saída da Usina para o trecho", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            deslocamentoSaidaUsinaTrecho.forEach { item ->
                                DeslocamentoCard(
                                    item = item,
                                    somenteInicio = true,
                                    onMarcarInicio = { viewModel.marcarInicio(item) },
                                    onMarcarFim = { },
                                    onSalvarManual = { inicio, _ ->
                                        viewModel.atualizarHorarioManual(item, inicio, item.fim.orEmpty())
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val cargaConsolidada = veiculosEtapa3.mapIndexed { index, veiculo ->
                                        "$veiculo: ${quantidadesEtapa3.getOrNull(index).orEmpty()} ton"
                                    }.joinToString(" / ")

                                    viewModel.atualizarCarregamentoDiario(
                                        diarioId = diarioId,
                                        localCarregamento = "",
                                        pesoLiquidoTon = cargaConsolidada,
                                        fotoTicketUri = fotoTicketEtapa3Uri?.toString().orEmpty()
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = etapa3PodeConcluir
                            ) {
                                Text("Salvar e concluir etapa 3")
                            }
                        }
                    }
                )

                EtapaCard(
                    numero = 4,
                    titulo = "Serviços",
                    status = diario?.statusServicos ?: "BLOQUEADA",
                    expandida = etapaExpandida == 4,
                    onClick = {
                        etapaExpandida = 4
                        desviosExpandido = false
                    },
                    conteudo = {
                        val chegadaTrechoRegistrada = deslocamentoChegadaTrechoEtapa4.any {
                            !it.inicio.isNullOrBlank()
                        }

                        val servicosEncerrados = diario?.statusServicos == "CONCLUIDA"

                        Column {
                            Text(
                                "Chegada ao trecho",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (deslocamentoChegadaTrechoEtapa4.isEmpty()) {
                                Text("Deslocamento não encontrado.")
                            } else {
                                deslocamentoChegadaTrechoEtapa4.forEach { item ->
                                    val itemSemTitulo = item.copy(titulo = "")

                                    DeslocamentoCard(
                                        item = itemSemTitulo,
                                        somenteInicio = true,
                                        onMarcarInicio = { viewModel.marcarInicio(item) },
                                        onMarcarFim = { },
                                        onSalvarManual = { inicio, _ ->
                                            viewModel.atualizarHorarioManual(
                                                item,
                                                inicio,
                                                item.fim.orEmpty()
                                            )
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Serviços executados: ${servicos.size} | Área do dia: ${
                                    formatarDecimalTruncado(areaTotalDoDia)
                                } m²",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (servicos.isEmpty()) {
                                Text(
                                    text = "Nenhum serviço lançado ainda.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                servicos.forEach { servico ->

                                    val textoHorario = when {
                                        !servico.horarioFotoAntes.isNullOrBlank() && !servico.horarioFotoConclusao.isNullOrBlank() ->
                                            "Horário: ${servico.horarioFotoAntes} às ${servico.horarioFotoConclusao}"

                                        !servico.horarioFotoAntes.isNullOrBlank() ->
                                            "Horário: ${servico.horarioFotoAntes}"

                                        else ->
                                            "Horário não registrado"
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    if (servicoSelecionadoParaExcluir == servico.id) {
                                                        servicoSelecionadoParaExcluir = null
                                                    } else {
                                                        onAbrirServico(servico.id)
                                                    }
                                                },
                                                onLongClick = {
                                                    servicoSelecionadoParaExcluir = servico.id
                                                }
                                            ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF5F5F5)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Protocolo: ${servico.numeroProtocolo}",
                                                        style = MaterialTheme.typography.titleSmall
                                                    )

                                                    if (!servicosEncerrados && servicoSelecionadoParaExcluir == servico.id) {
                                                        IconButton(
                                                            onClick = {
                                                                servicoPendenteExclusao = servico
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Excluir serviço"
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = servico.endereco.ifBlank {
                                                        "Endereço não informado"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = textoHorario,
                                                    style = MaterialTheme.typography.bodySmall
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = "Área total: ${formatarDecimalTruncado(areasPorServico[servico.id] ?: 0.0)} m²",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }

                                            servico.fotoConclusaoUri?.takeIf { it.isNotBlank() }?.let { fotoUri ->
                                                AsyncImage(
                                                    model = fotoUri,
                                                    contentDescription = "Foto final do serviço",
                                                    modifier = Modifier
                                                        .height(88.dp)
                                                        .fillMaxWidth(0.28f),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (servicosEncerrados) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Serviços encerrados. Novos lançamentos e alterações estão bloqueados.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Button(
                                onClick = {
                                    if (!servicosEncerrados) {
                                        onAbrirServico(0L)
                                    }
                                },
                                enabled = chegadaTrechoRegistrada && !servicosEncerrados,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Adicionar serviço")
                            }

                            if (servicoPendenteExclusao != null) {
                                AlertDialog(
                                    onDismissRequest = {
                                        servicoPendenteExclusao = null
                                    },
                                    title = {
                                        Text("Excluir serviço")
                                    },
                                    text = {
                                        Text("Tem certeza que deseja apagar este serviço?")
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                servicoPendenteExclusao?.let { servico ->
                                                    viewModel.excluirServico(servico)
                                                }
                                                servicoSelecionadoParaExcluir = null
                                                servicoPendenteExclusao = null
                                            }
                                        ) {
                                            Text("Apagar")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                servicoPendenteExclusao = null
                                            }
                                        ) {
                                            Text("Cancelar")
                                        }
                                    }
                                )
                            }
                        }
                    }
                )

                EtapaCard(
                    numero = 5,
                    titulo = "Fechamento dos serviços",
                    status = diario?.statusFechamentoServicos ?: "BLOQUEADA",
                    expandida = etapaExpandida == 5,
                    onClick = {
                        etapaExpandida = 5
                        desviosExpandido = false
                    },
                    conteudo = {
                        Column {
                            Text(
                                "Fechamento: ${
                                    diario?.horarioFechamentoServicos?.ifBlank { "Não informado" } ?: "Não informado"
                                }"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Próximo destino",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    menuProximoDestinoExpandido = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = diario?.statusServicos != "CONCLUIDA"
                            ) {
                                Text(
                                    if (proximoDestinoSelecionado.isBlank()) {
                                        "Selecionar próximo destino"
                                    } else {
                                        proximoDestinoSelecionado
                                    }
                                )
                            }

                            DropdownMenu(
                                expanded = menuProximoDestinoExpandido && diario?.statusServicos != "CONCLUIDA",
                                onDismissRequest = { menuProximoDestinoExpandido = false }
                            ) {
                                listOf(
                                    "Retorno à base",
                                    "Atendimento a outro Contrato",
                                    "Para hospedagem"
                                ).forEach { opcao ->
                                    DropdownMenuItem(
                                        text = { Text(opcao) },
                                        onClick = {
                                            proximoDestinoSelecionado = opcao
                                            menuProximoDestinoExpandido = false
                                        }
                                    )
                                }
                            }

                            if (diario?.proximoDestino?.isNotBlank() == true) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Destino selecionado: ${diario?.proximoDestino}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (diario?.statusServicos != "CONCLUIDA") {

                                Button(
                                    onClick = {
                                        mostrarDialogoEncerrarServicos = true
                                    },
                                    enabled = proximoDestinoSelecionado.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Encerrar serviços")
                                }

                            } else {

                                Text(
                                    text = "Serviços encerrados. Apenas consulta disponível.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (mostrarDialogoEncerrarServicos) {
                                AlertDialog(
                                    onDismissRequest = {
                                        mostrarDialogoEncerrarServicos = false
                                    },
                                    title = {
                                        Text("Encerrar serviços")
                                    },
                                    text = {
                                        Text("Após o fechamento dos serviços, não serão permitidos novos lançamentos nem alterações nos serviços já registrados. Será permitida apenas a consulta. Deseja continuar?")
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                viewModel.encerrarServicos(
                                                    diarioId = diarioId,
                                                    proximoDestino = proximoDestinoSelecionado
                                                )
                                                mostrarDialogoEncerrarServicos = false
                                            }
                                        ) {
                                            Text("Encerrar")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                mostrarDialogoEncerrarServicos = false
                                            }
                                        ) {
                                            Text("Cancelar")
                                        }
                                    }
                                )
                            }
                        }
                    }
                )

                EtapaCard(
                    numero = 6,
                    titulo = when (diario?.proximoDestino) {
                        "Retorno à base" -> "Retorno à base"
                        "Atendimento a outro Contrato" -> "Deslocamento para outro contrato"
                        "Para hospedagem" -> "Deslocamento para hospedagem"
                        else -> "Deslocamento ao próximo destino"
                    },
                    status = diario?.statusRetornoBase ?: "BLOQUEADA",
                    expandida = etapaExpandida == 6,
                    onClick = {
                        etapaExpandida = 6
                        desviosExpandido = false
                    },
                    conteudo = {
                        val destinoSelecionado =
                            if (diario?.proximoDestino.isNullOrBlank()) {
                                proximoDestinoSelecionado
                            } else {
                                diario?.proximoDestino.orEmpty()
                            }

                        val atendimentoOutroContrato =
                            destinoSelecionado.equals("Atendimento a outro Contrato", ignoreCase = true)

                        val paraHospedagem =
                            destinoSelecionado.equals("Para hospedagem", ignoreCase = true)

                        val labelSaida = when {
                            destinoSelecionado.equals("Retorno à base", ignoreCase = true) -> "Saída para retorno"
                            atendimentoOutroContrato -> "Saída para outro contrato"
                            paraHospedagem -> "Saída para hospedagem"
                            else -> "Saída"
                        }

                        val labelChegada = when {
                            destinoSelecionado.equals("Retorno à base", ignoreCase = true) -> "Chegada à base"
                            atendimentoOutroContrato -> "Chegada ao outro contrato"
                            paraHospedagem -> "Chegada à hospedagem"
                            else -> "Chegada ao destino"
                        }

                        val mensagemConclusao = when {
                            destinoSelecionado.equals("Retorno à base", ignoreCase = true) -> "Retorno à base concluído."
                            atendimentoOutroContrato -> "Deslocamento para outro contrato concluído."
                            paraHospedagem -> "Deslocamento para hospedagem concluído."
                            else -> "Deslocamento concluído."
                        }

                        val saidaRegistrada = !diario?.saidaRetornoBase.isNullOrBlank()
                        val chegadaRegistrada = !diario?.chegadaBase.isNullOrBlank()
                        val etapaConcluida = diario?.statusRetornoBase == "CONCLUIDA"

                        Column {
                            if (destinoSelecionado.isNotBlank()) {
                                Text(
                                    text = "Próximo destino: $destinoSelecionado",
                                    style = MaterialTheme.typography.titleSmall
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (atendimentoOutroContrato) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Contrato de destino",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val outrasObras = obras.filter { it.id != diario?.obraId }

                                if (outrasObras.isEmpty()) {
                                    Text(
                                        text = "Nenhum outro contrato cadastrado.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    OutlinedButton(
                                        onClick = { menuOutroContratoExpandido = true },
                                        enabled = !etapaConcluida,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            if (outroContratoSelecionado.isBlank()) {
                                                "Selecionar contrato de destino"
                                            } else {
                                                outroContratoSelecionado
                                            }
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = menuOutroContratoExpandido && !etapaConcluida,
                                        onDismissRequest = { menuOutroContratoExpandido = false }
                                    ) {
                                        outrasObras.forEach { obra ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text("${obra.nome} - ${obra.contrato}")
                                                },
                                                onClick = {
                                                    outroContratoSelecionado = "${obra.nome} - ${obra.contrato}"
                                                    obraDestinoIdSelecionada = obra.id
                                                    menuOutroContratoExpandido = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Text(
                                text = "$labelSaida: ${
                                    diario?.saidaRetornoBase?.ifBlank { "Não informada" } ?: "Não informada"
                                }"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    viewModel.marcarSaidaEtapa6(diarioId)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = destinoSelecionado.isNotBlank() && !etapaConcluida
                            ) {
                                Text(if (saidaRegistrada) "Atualizar saída" else "Marcar saída")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "$labelChegada: ${
                                    diario?.chegadaBase?.ifBlank { "Não informada" } ?: "Não informada"
                                }"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    viewModel.marcarChegadaEtapa6(diarioId)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = destinoSelecionado.isNotBlank() && saidaRegistrada && !etapaConcluida
                            ) {
                                Text(if (chegadaRegistrada) "Atualizar chegada" else "Marcar chegada")
                            }

                            if (paraHospedagem) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Foto da hospedagem",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.CAMERA
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            val novaUri = criarUriParaFotoDiario(context)
                                            fotoHospedagemUriTemporaria = novaUri
                                            hospedagemCameraLauncher.launch(novaUri)
                                        } else {
                                            hospedagemCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    enabled = !etapaConcluida,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (diario?.fotoHospedagemPath.orEmpty().isBlank()) {
                                            "Tirar foto da hospedagem"
                                        } else {
                                            "Trocar foto da hospedagem"
                                        }
                                    )
                                }

                                if (diario?.fotoHospedagemPath.orEmpty().isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    AsyncImage(
                                        model = "${diario?.fotoHospedagemPath}#${versaoPreviewHospedagem}",
                                        contentDescription = "Foto da hospedagem",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                if (diario?.enderecoHospedagem?.isNotBlank() == true) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("Endereço: ${diario?.enderecoHospedagem}")
                                }
                            }

                            if (diario?.observacaoRetornoBase.orEmpty().isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = diario?.observacaoRetornoBase.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (etapaConcluida) {
                                Text(
                                    text = mensagemConclusao,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Button(
                                    onClick = {
                                        if (
                                            atendimentoOutroContrato &&
                                            obraDestinoIdSelecionada != null
                                        ) {
                                            viewModel.concluirEtapa6ECriarDiarioDestino(
                                                diarioOrigemId = diarioId,
                                                obraDestinoId = obraDestinoIdSelecionada!!,
                                                contratoDestinoDescricao = outroContratoSelecionado
                                            ) { novoDiarioId ->
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Novo Diário de Obras criado para o contrato selecionado.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()

                                                onAbrirDiario(novoDiarioId)
                                            }
                                        } else {
                                            viewModel.concluirRetornoBase(
                                                diarioId = diarioId,
                                                saidaRetornoBase = diario?.saidaRetornoBase,
                                                chegadaBase = diario?.chegadaBase,
                                                observacaoRetornoBase = diario?.observacaoRetornoBase.orEmpty()
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = destinoSelecionado.isNotBlank() &&
                                            saidaRegistrada &&
                                            chegadaRegistrada &&
                                            (
                                                    !paraHospedagem ||
                                                            diario?.fotoHospedagemPath.orEmpty().isNotBlank()
                                                    ) &&
                                            (
                                                    !atendimentoOutroContrato ||
                                                            obraDestinoIdSelecionada != null
                                                    )
                                ) {
                                    Text("Concluir etapa 6")
                                }
                            }
                        }
                    }
                )

                DesviosCard(
                    quantidadeDesvios = desvios.size,
                    desvios = desvios,
                    tiposDesvio = tiposDesvio,
                    diarioId = diarioId,
                    viewModel = viewModel,
                    bloqueado = false,
                    expandido = desviosExpandido,
                    onClick = {
                        desviosExpandido = !desviosExpandido
                        if (!desviosExpandido) {
                            etapaExpandida = 0
                        }
                    }
                )


                EtapaCard(
                    numero = 7,
                    titulo = "Fechamento do D.O.",
                    status = diario?.statusFechamentoDo ?: "BLOQUEADA",
                    expandida = etapaExpandida == 7,
                    onClick = {
                        etapaExpandida = 7
                        desviosExpandido = false
                    },
                    conteudo = {
                        Column {
                            Text(
                                "Observação final: ${
                                    diario?.observacaoFinalDo?.ifBlank { "Nenhuma observação" } ?: "Nenhuma observação"
                                }"
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (diario?.statusFechamentoDo == "CONCLUIDA") {
                                Text(
                                    text = "Diário encerrado com sucesso.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.concluirFechamentoDo(
                                            diarioId = diarioId,
                                            observacaoFinalDo = diario?.observacaoFinalDo?.ifBlank { "D.O. finalizado" }
                                                ?: "D.O. finalizado"
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Concluir etapa 7")
                                }
                            }
                        }
                    }
                )
            }

            if (mostrarTicketAmpliado && fotoTicketEtapa3Uri != null) {
                Dialog(
                    onDismissRequest = { mostrarTicketAmpliado = false }
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            AsyncImage(
                                model = fotoTicketEtapa3Uri?.let { "${it}#${versaoPreviewTicket}" },
                                contentDescription = "Ticket ampliado",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(500.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { mostrarTicketAmpliado = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Fechar")
                            }
                        }
                    }
                }
            }
        }
    }
    if (servicoPendenteExclusao != null) {
        AlertDialog(
            onDismissRequest = {
                servicoPendenteExclusao = null
            },
            title = {
                Text("Excluir serviço")
            },
            text = {
                Text("Tem certeza que deseja apagar este serviço?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        servicoPendenteExclusao?.let { servico ->
                            viewModel.excluirServico(servico)
                        }
                        servicoSelecionadoParaExcluir = null
                        servicoPendenteExclusao = null
                    }
                ) {
                    Text("Apagar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        servicoPendenteExclusao = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun EtapaCard(
    numero: Int,
    titulo: String,
    status: String,
    expandida: Boolean,
    onClick: () -> Unit,
    conteudo: (@Composable () -> Unit)? = null
) {
    val corFundo = when (status) {
        "CONCLUIDA" -> Color(0xFFDFF5E1)
        "EM_ANDAMENTO" -> Color(0xFFFFF4CC)
        "DISPONIVEL" -> Color(0xFFEAF2FF)
        else -> Color(0xFFF2F2F2)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(enabled = status != "BLOQUEADA") { onClick() },
        colors = CardDefaults.cardColors(containerColor = corFundo)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "$numero. $titulo",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Status: $status",
                style = MaterialTheme.typography.bodyMedium
            )

            if (expandida) {
                Spacer(modifier = Modifier.height(12.dp))
                if (conteudo != null) {
                    conteudo()
                } else {
                    Text(
                        text = "Conteúdo da etapa virá aqui.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


@Composable
fun DesviosCard(
    quantidadeDesvios: Int,
    desvios: List<DesvioItemEntity>,
    tiposDesvio: List<Pair<String, String>>,
    diarioId: Long,
    viewModel: MainViewModel,
    bloqueado: Boolean,
    expandido: Boolean,
    onClick: () -> Unit
) {
    var menuTipoDesvioExpandido by remember { mutableStateOf(false) }
    var codigoEmCadastro by remember { mutableStateOf("") }
    var descricaoEmCadastro by remember { mutableStateOf("") }
    var inicioEmCadastro by remember { mutableStateOf("") }
    var fimEmCadastro by remember { mutableStateOf("") }
    var observacaoEmCadastro by remember { mutableStateOf("") }
    var desvioExpandidoId by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

    fun abrirSeletorHora(
        valorAtual: String,
        onHoraSelecionada: (String) -> Unit
    ) {
        val partes = valorAtual.split(":")
        val horaInicial = partes.getOrNull(0)?.toIntOrNull() ?: 8
        val minutoInicial = partes.getOrNull(1)?.toIntOrNull() ?: 0

        android.app.TimePickerDialog(
            context,
            { _, hora, minuto ->
                onHoraSelecionada("%02d:%02d".format(hora, minuto))
            },
            horaInicial,
            minutoInicial,
            true
        ).show()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },

        colors = CardDefaults.cardColors(
            containerColor =
                if (quantidadeDesvios > 0) {
                    Color(0xFFFFE8C2)
                } else {
                    MaterialTheme.colorScheme.surface
                }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = "Desvios",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                if (quantidadeDesvios == 0) {
                    "Nenhum desvio registrado."
                } else {
                    "Desvios registrados: $quantidadeDesvios"
                }
            )

            if (expandido) {
                Spacer(modifier = Modifier.height(12.dp))

                desvios.forEach { desvio ->
                    val estaExpandido = desvioExpandidoId == desvio.id

                    val corCard = when {
                        desvio.inicio.isBlank() -> Color(0xFFF1F1F1)
                        desvio.fim.isBlank() -> Color(0xFFFFE8C2)
                        else -> Color(0xFFDDF5DD)
                    }

                    var inicioEditado by remember(desvio.id, desvio.inicio) {
                        mutableStateOf(desvio.inicio)
                    }

                    var fimEditado by remember(desvio.id, desvio.fim) {
                        mutableStateOf(desvio.fim)
                    }

                    var observacaoEditada by remember(desvio.id, desvio.observacao) {
                        mutableStateOf(desvio.observacao)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                desvioExpandidoId = if (estaExpandido) null else desvio.id
                            },
                        colors = CardDefaults.cardColors(containerColor = corCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {

                            Text(
                                text = "${desvio.codigo} - ${desvio.descricao}",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Horário: ${
                                    when {
                                        desvio.inicio.isNotBlank() && desvio.fim.isNotBlank() ->
                                            "${desvio.inicio} às ${desvio.fim}"

                                        desvio.inicio.isNotBlank() ->
                                            "Início ${desvio.inicio}"

                                        else ->
                                            "Não iniciado"
                                    }
                                }",
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (desvio.observacao.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Obs.: ${desvio.observacao}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (estaExpandido) {
                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Início: ${inicioEditado.ifBlank { "--:--" }}")

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            inicioEditado = horaAtualFormatada()
                                        },
                                        enabled = !bloqueado,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Marcar início")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            abrirSeletorHora(inicioEditado) { hora ->
                                                inicioEditado = hora
                                            }
                                        },
                                        enabled = !bloqueado,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Editar início")
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Fim: ${fimEditado.ifBlank { "--:--" }}")

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            fimEditado = horaAtualFormatada()
                                        },
                                        enabled = !bloqueado,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Marcar fim")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            abrirSeletorHora(fimEditado) { hora ->
                                                fimEditado = hora
                                            }
                                        },
                                        enabled = !bloqueado,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Editar fim")
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = observacaoEditada,
                                    onValueChange = { observacaoEditada = it },
                                    label = { Text("Observação (opcional)") },
                                    enabled = !bloqueado,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        viewModel.atualizarHorarioDesvio(
                                            item = desvio,
                                            novoInicio = inicioEditado,
                                            novoFim = fimEditado
                                        )
                                        viewModel.atualizarObservacaoDesvio(
                                            id = desvio.id,
                                            texto = observacaoEditada
                                        )
                                        desvioExpandidoId = null
                                    },
                                    enabled = !bloqueado,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Salvar alterações")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        menuTipoDesvioExpandido = true
                    },
                    enabled = !bloqueado && codigoEmCadastro.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Adicionar desvio")
                }

                DropdownMenu(
                    expanded = menuTipoDesvioExpandido,
                    onDismissRequest = { menuTipoDesvioExpandido = false }
                ) {
                    tiposDesvio.forEach { item ->
                        val codigo = item.first
                        val descricao = item.second

                        DropdownMenuItem(
                            text = { Text("$codigo - $descricao") },
                            onClick = {
                                codigoEmCadastro = codigo
                                descricaoEmCadastro = descricao
                                inicioEmCadastro = ""
                                fimEmCadastro = ""
                                observacaoEmCadastro = ""
                                menuTipoDesvioExpandido = false
                            }
                        )
                    }
                }

                if (codigoEmCadastro.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {

                            Text(
                                text = "$codigoEmCadastro - $descricaoEmCadastro",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Início: ${inicioEmCadastro.ifBlank { "--:--" }}")

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        inicioEmCadastro = horaAtualFormatada()
                                    },
                                    enabled = !bloqueado,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Marcar início")
                                }

                                OutlinedButton(
                                    onClick = {
                                        abrirSeletorHora(inicioEmCadastro) { hora ->
                                            inicioEmCadastro = hora
                                        }
                                    },
                                    enabled = !bloqueado,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Editar início")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Fim: ${fimEmCadastro.ifBlank { "--:--" }}")

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        fimEmCadastro = horaAtualFormatada()
                                    },
                                    enabled = !bloqueado,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Marcar fim")
                                }

                                OutlinedButton(
                                    onClick = {
                                        abrirSeletorHora(fimEmCadastro) { hora ->
                                            fimEmCadastro = hora
                                        }
                                    },
                                    enabled = !bloqueado,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Editar fim")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = observacaoEmCadastro,
                                onValueChange = { observacaoEmCadastro = it },
                                label = { Text("Observação (opcional)") },
                                enabled = !bloqueado,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.adicionarDesvioCompleto(
                                        diarioId = diarioId,
                                        codigo = codigoEmCadastro,
                                        descricao = descricaoEmCadastro,
                                        inicio = inicioEmCadastro,
                                        fim = fimEmCadastro,
                                        observacao = observacaoEmCadastro
                                    )

                                    codigoEmCadastro = ""
                                    descricaoEmCadastro = ""
                                    inicioEmCadastro = ""
                                    fimEmCadastro = ""
                                    observacaoEmCadastro = ""
                                },
                                enabled = !bloqueado &&
                                        inicioEmCadastro.isNotBlank() &&
                                        fimEmCadastro.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Confirmar desvio")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    codigoEmCadastro = ""
                                    descricaoEmCadastro = ""
                                    inicioEmCadastro = ""
                                    fimEmCadastro = ""
                                    observacaoEmCadastro = ""
                                },
                                enabled = !bloqueado,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancelar")
                            }
                        }
                    }
                }
            }
        }
    }
}

private val LISTA_FUNCIONARIOS = listOf(
    "Natanael",
    "Ronaldo",
    "Elio",
    "Roger",
    "Sergio",
    "Edson S.",
    "Paulo S.",
    "Abraham",
    "Diego Angel",
    "Armando",
    "Edilasaro",
    "Paulo H.",
    "Valdir R.",
    "Juliano",
    "Francisco",
    "Salvio",
    "Adinael",
    "Anthony",
    "Willian",
    "Joneci",
    "Marciano",
    "Angelo",
    "Jacó",
    "David",
    "Valdir",
    "Sadi",
    "Luiz C.",
    "Rhuan",
    "Aldair",
    "José",
    "João Pedro",
    "Valdeci",
    "Valdemar",
    "Gardin",
    "Nicolau",
    "Edilson",
    "Diego Alexandre"
)

private val LISTA_VEICULOS = listOf(
    "Térmico - ATA-8E06",
    "Térmico - QHQ-3G94",
    "Térmico - QHL-4B64",
    "Térmico - QIZ-5I82",
    "Térmico - RXZ-5I74",
    "Caçamba - AIJ-0K66",
    "Caçamba - MJI-8G32",
    "Caçamba - LYF-4330",
    "Térmico - SXL-3G10",
    "Prancha - IOX-7B63",
    "Caçamba - MIX-8A53",
    "Prancha - MBK-9080"
)

private val LISTA_EQUIPAMENTOS_AUXILIARES = listOf(
    "Sapo Mecânico",
    "Compressor",
    "Policorte",
    "Gerador",
    "Placa Vibratória",
    "Rompedor",
    "Vassoura Mecânica"
)

private val LISTA_EQUIPAMENTOS_COMPACTACAO = listOf(
    "Placa Vibratória",
    "Rolo Compactador"
)

private val LISTA_LIMPEZA_ENTULHO = listOf(
    "Já limpo",
    "Carregado",
    "Em espera"
)

private fun criarUriParaFotoEtapa(context: Context): Uri {
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

@Composable
private fun CamposQuantidadeEtapa3(
    veiculos: List<String>,
    quantidades: SnapshotStateList<String>
) {
    Column {
        veiculos.forEachIndexed { index, veiculo ->
            Text(
                text = veiculo,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = quantidades.getOrNull(index).orEmpty(),
                onValueChange = { novoValor ->
                    val filtrado = filtrarEntradaDecimal(novoValor)
                    if (filtrado != null && index in quantidades.indices) {
                        quantidades[index] = filtrado
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Quantidade em ton") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun filtrarEntradaDecimal(valor: String): String? {
    if (valor.isEmpty()) return ""

    if (!valor.all { it.isDigit() || it == ',' || it == '.' }) return null

    val separadores = valor.count { it == ',' || it == '.' }
    if (separadores > 1) return null

    return valor
}

private fun formatarDecimalTruncado(valor: Double): String {
    val truncado = kotlin.math.floor(valor * 100.0) / 100.0
    return String.format(Locale.US, "%.2f", truncado).replace(".", ",")
}

private fun horaAtualFormatada(): String {
    return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date())
}

private fun criarUriParaFotoDiario(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val arquivo = File(imagesDir, "foto_diario_${System.currentTimeMillis()}.jpg")

    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        arquivo
    )
}