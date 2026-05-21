package com.example.diarioobras.ui

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.diarioobras.data.DiarioEntity
import com.example.diarioobras.data.ObraEntity
import com.google.android.gms.location.LocationServices
import java.util.Locale
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.ExperimentalFoundationApi


import com.example.diarioobras.data.ServicoEntity
import com.example.diarioobras.data.StatusEtapa
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

import androidx.compose.runtime.key

import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.withTimeoutOrNull
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.AnimatedVisibility

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiarioEtapasScreen(
    diarioId: Long,
    viewModel: MainViewModel,
    navController: NavHostController,
    onAbrirServico: (Long) -> Unit,
    onAbrirDiario: (Long) -> Unit
) {

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

    val diarioFechado = diario?.diarioFechado == true || diario?.statusFechamentoDo == StatusEtapa.CONCLUIDA

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
    var veiculoEtapa3Selecionado by remember(diarioId) { mutableStateOf("") }
    val quantidadesEtapa3 = remember(diarioId) { mutableStateListOf<String>() }
    var fotoTicketEtapa3Uri by remember(diarioId) { mutableStateOf<Uri?>(null) }
    var latitudeTicket by remember(diarioId) { mutableStateOf(0.0) }
    var longitudeTicket by remember(diarioId) { mutableStateOf(0.0) }
    var mostrarTicketAmpliado by remember { mutableStateOf(false) }
    var versaoPreviewTicket by remember(diarioId) { mutableStateOf(0) }
    var servicoSelecionadoParaExcluir by remember { mutableStateOf<Long?>(null) }
    var servicoPendenteExclusao by remember { mutableStateOf<ServicoEntity?>(null) }

    var mostrarDialogoEncerrarServicos by remember { mutableStateOf(false) }

    var menuProximoDestinoExpandido by remember { mutableStateOf(false) }
    var proximoDestinoSelecionado by remember(diarioId) { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val uploadEstado by viewModel.uploadEstado.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uploadEstado) {
        when (val estado = uploadEstado) {
            is UploadEstado.Sucesso -> {
                snackbarHostState.showSnackbar("Diário enviado ao Firebase com sucesso!")
                viewModel.resetarUploadEstado()
            }
            is UploadEstado.Erro -> {
                snackbarHostState.showSnackbar("Erro ao enviar: ${estado.mensagem}")
                viewModel.resetarUploadEstado()
            }
            else -> Unit
        }
    }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var fotoHospedagemUriTemporaria by remember(diarioId) { mutableStateOf<Uri?>(null) }
    var versaoPreviewHospedagem by remember(diarioId) { mutableStateOf(0) }

    var menuOutroContratoExpandido by remember { mutableStateOf(false) }
    var outroContratoSelecionado by remember(diarioId) { mutableStateOf("") }

    val hospedagemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            fotoHospedagemUriTemporaria?.let { uri -> coroutineScope.launch { comprimirFoto(context, uri); salvarFotoNaGaleria(context, uri) } }
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

    val ticketCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            fotoTicketEtapa3Uri?.let { uri -> coroutineScope.launch { comprimirFoto(context, uri); salvarFotoNaGaleria(context, uri) } }
            versaoPreviewTicket++
            coroutineScope.launch {
                val cts1 = CancellationTokenSource()
                val locHigh = withTimeoutOrNull(5_000L) {
                    runCatching {
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY, cts1.token
                        ).await()
                    }.getOrNull()
                }
                if (locHigh == null) cts1.cancel()

                val locFinal = locHigh ?: run {
                    val cts2 = CancellationTokenSource()
                    val locBalanced = withTimeoutOrNull(5_000L) {
                        runCatching {
                            fusedLocationClient.getCurrentLocation(
                                Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts2.token
                            ).await()
                        }.getOrNull()
                    }
                    if (locBalanced == null) cts2.cancel()
                    locBalanced
                }

                locFinal?.let {
                    latitudeTicket = it.latitude
                    longitudeTicket = it.longitude
                }
            }
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

    LaunchedEffect(carregamentos, veiculosEtapa3.size) {
        val carregamento = carregamentos.firstOrNull() ?: return@LaunchedEffect
        if (fotoTicketEtapa3Uri == null && carregamento.fotoTicketUri.isNotBlank()) {
            fotoTicketEtapa3Uri = Uri.parse(carregamento.fotoTicketUri)
        }
        if (quantidadesEtapa3.isNotEmpty()) {
            val pesos = carregamento.pesoLiquidoTon.split(" / ")
            if (pesos.size == quantidadesEtapa3.size) {
                pesos.forEachIndexed { index, peso ->
                    if (quantidadesEtapa3.getOrNull(index).isNullOrBlank()) {
                        quantidadesEtapa3[index] = peso
                    }
                }
            }
        }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val nomeArquivo = viewModel.salvarJsonDiarioNoApp(context, diarioId)
                            if (nomeArquivo != null) {
                                val uri = viewModel.obterUriJsonSalvo(context, nomeArquivo)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, "Exportar Diário JSON")
                                )
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Exportar JSON")
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
                    status = diario?.statusEquipe ?: StatusEtapa.BLOQUEADA,
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
                                enabled = !diarioFechado,
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
                                enabled = !diarioFechado,
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
                                        bloqueado = diarioFechado,
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
                                enabled = etapa1PodeConcluir && !diarioFechado
                            ) {
                                Text("Salvar e concluir etapa 1")
                            }
                        }
                    }
                )

                EtapaCard(
                    numero = 2,
                    titulo = "Equipamento",
                    status = diario?.statusEquipamento ?: StatusEtapa.BLOQUEADA,
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
                                enabled = !diarioFechado,
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
                                enabled = !diarioFechado,
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
                                enabled = !diarioFechado,
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
                                    bloqueado = diarioFechado,
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
                                enabled = etapa2PodeConcluir && !diarioFechado
                            ) {
                                Text("Salvar e concluir etapa 2")
                            }
                        }
                    }
                )

                EtapaCard(
                    numero = 3,
                    titulo = "Carregamento",
                    status = diario?.statusCarregamento ?: StatusEtapa.BLOQUEADA,
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
                                    bloqueado = diarioFechado,
                                    onMarcarInicio = { viewModel.marcarInicio(item) },
                                    onMarcarFim = { },
                                    onSalvarManual = { inicio, _ ->
                                        viewModel.atualizarHorarioManual(item, inicio, item.fim.orEmpty())
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Início do carregamento", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            deslocamentoInicioCarregamento.forEach { item ->
                                DeslocamentoCard(
                                    item = item,
                                    somenteInicio = true,
                                    bloqueado = diarioFechado,
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
                                    bloqueado = diarioFechado,
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
                                    bloqueado = diarioFechado,
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
                                enabled = !diarioFechado,
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
                                quantidades = quantidadesEtapa3,
                                bloqueado = diarioFechado
                            )

                            Text("Saída da Usina para o trecho", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            deslocamentoSaidaUsinaTrecho.forEach { item ->
                                DeslocamentoCard(
                                    item = item,
                                    somenteInicio = true,
                                    bloqueado = diarioFechado,
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
                                    viewModel.salvarCarregamentoEtapa3(
                                        diarioId = diarioId,
                                        veiculo = veiculosEtapa3.joinToString(" / "),
                                        pesoLiquidoTon = quantidadesEtapa3.joinToString(" / "),
                                        fotoTicketUri = fotoTicketEtapa3Uri?.toString().orEmpty(),
                                        latitude = latitudeTicket,
                                        longitude = longitudeTicket
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = etapa3PodeConcluir && !diarioFechado
                            ) {
                                Text("Salvar e concluir etapa 3")
                            }
                        }
                    }
                )

                EtapaCard(
                    numero = 4,
                    titulo = "Serviços",
                    status = diario?.statusServicos ?: StatusEtapa.BLOQUEADA,
                    expandida = etapaExpandida == 4,
                    onClick = {
                        etapaExpandida = 4
                        desviosExpandido = false
                    },
                    conteudo = {
                        val chegadaTrechoRegistrada = deslocamentoChegadaTrechoEtapa4.any {
                            !it.inicio.isNullOrBlank()
                        }

                        val servicosEncerrados = diario?.statusServicos == StatusEtapa.CONCLUIDA

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

                        }
                    }
                )

                EtapaCard(
                    numero = 5,
                    titulo = "Fechamento dos serviços",
                    status = diario?.statusFechamentoServicos ?: StatusEtapa.BLOQUEADA,
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
                                enabled = diario?.statusServicos != StatusEtapa.CONCLUIDA
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
                                expanded = menuProximoDestinoExpandido && diario?.statusServicos != StatusEtapa.CONCLUIDA,
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

                            if (diario?.statusServicos != StatusEtapa.CONCLUIDA) {

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

                            EncerrarServicosDialog(
                                show = mostrarDialogoEncerrarServicos,
                                onConfirm = {
                                    viewModel.encerrarServicos(
                                        diarioId = diarioId,
                                        proximoDestino = proximoDestinoSelecionado
                                    )
                                    mostrarDialogoEncerrarServicos = false
                                },
                                onDismiss = { mostrarDialogoEncerrarServicos = false }
                            )
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
                    status = diario?.statusRetornoBase ?: StatusEtapa.BLOQUEADA,
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
                        val etapaConcluida = diario?.statusRetornoBase == StatusEtapa.CONCLUIDA

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
                    tiposDesvio = TIPOS_DESVIO,
                    diarioId = diarioId,
                    viewModel = viewModel,
                    bloqueado = diario?.diarioFechado == true || diario?.statusFechamentoDo == StatusEtapa.CONCLUIDA,
                    expandido = desviosExpandido,
                    onClick = {
                        desviosExpandido = !desviosExpandido
                        if (desviosExpandido) {
                            etapaExpandida = 0
                        }
                    },
                    onEditarObservacao = { desvio ->
                        navController.navigate("editar_observacao_desvio/${desvio.id}")
                    }
                )


                EtapaCard(
                    numero = 7,
                    titulo = "Fechamento do D.O.",
                    status = diario?.statusFechamentoDo ?: StatusEtapa.BLOQUEADA,
                    expandida = etapaExpandida == 7,
                    onClick = {
                        etapaExpandida = 7
                        desviosExpandido = false
                    },
                    conteudo = {
                        val isRetornoBase = diario?.proximoDestino == "Retorno à base"

                        var observacaoFinalLocal by remember(diario?.id, diario?.observacaoFinalDo) {
                            mutableStateOf(diario?.observacaoFinalDo ?: "")
                        }
                        var horarioPontoLocal by remember(diario?.id, diario?.horarioPontoCidade) {
                            mutableStateOf(
                                diario?.horarioPontoCidade
                                    ?: SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            )
                        }
                        var mostrarDialogPonto by remember { mutableStateOf(false) }

                        Column {
                            if (diario?.statusFechamentoDo == StatusEtapa.CONCLUIDA) {
                                Text(
                                    text = "Observação final: ${
                                        diario?.observacaoFinalDo?.ifBlank { "Nenhuma observação" }
                                            ?: "Nenhuma observação"
                                    }"
                                )
                                if (isRetornoBase && !diario?.horarioPontoCidade.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Batendo ponto na chegada: ${diario?.horarioPontoCidade}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Diário encerrado com sucesso.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                OutlinedTextField(
                                    value = observacaoFinalLocal,
                                    onValueChange = { observacaoFinalLocal = it },
                                    label = { Text("Observação final") },
                                    placeholder = { Text("Digite a observação final do D.O.") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3
                                )

                                if (isRetornoBase) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Batendo ponto na chegada: ${horarioPontoLocal.ifBlank { "--:--" }}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { mostrarDialogPonto = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Editar horário")
                                    }

                                    if (mostrarDialogPonto) {
                                        HoraMinutoDialog(
                                            titulo = "Batendo ponto na chegada",
                                            valorAtual = horarioPontoLocal.ifBlank { null },
                                            onDismiss = { mostrarDialogPonto = false },
                                            onConfirmar = { novoHorario ->
                                                horarioPontoLocal = novoHorario
                                                mostrarDialogPonto = false
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        viewModel.concluirFechamentoDo(
                                            diarioId = diarioId,
                                            observacaoFinalDo = observacaoFinalLocal.ifBlank { "D.O. finalizado" },
                                            horarioPontoCidade = if (isRetornoBase) horarioPontoLocal.ifBlank { null } else null
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (isRetornoBase)
                                            "Encerramento / Batendo ponto na chegada"
                                        else
                                            "Concluir etapa 7"
                                    )
                                }
                                // Botão de reenvio manual caso o envio automático falhe
                                val diarioFechadoAtual = diario?.diarioFechado ?: false
                                if (diarioFechadoAtual) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    when (uploadEstado) {
                                        is UploadEstado.Enviando -> {
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                            Text(
                                                "Enviando dados...",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        else -> {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.reenviarDiario(diarioId)
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Reenviar para servidor")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }

            fotoTicketEtapa3Uri?.let { uri ->
                TicketAmpliadoDialog(
                    show = mostrarTicketAmpliado,
                    uri = uri,
                    versao = versaoPreviewTicket,
                    onDismiss = { mostrarTicketAmpliado = false }
                )
            }
        }
    }

    EnviandoFirebaseDialog(show = uploadEstado is UploadEstado.Enviando)

    ExcluirServicoDialog(
        servico = servicoPendenteExclusao,
        onConfirm = { servico ->
            viewModel.excluirServico(servico)
            servicoSelecionadoParaExcluir = null
            servicoPendenteExclusao = null
        },
        onDismiss = { servicoPendenteExclusao = null }
    )
}
