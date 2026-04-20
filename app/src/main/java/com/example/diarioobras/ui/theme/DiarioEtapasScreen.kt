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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiarioEtapasScreen(
    diarioId: Long,
    viewModel: MainViewModel,
    onAbrirServico: (Long) -> Unit
) {
    val context = LocalContext.current

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

    var desviosExpandido by remember { mutableStateOf(false) }

    // Etapa 1
    var menuEncarregadoExpandido by remember { mutableStateOf(false) }
    var encarregadoSelecionado by remember(diarioId) { mutableStateOf("") }

    var menuEquipeExpandido by remember { mutableStateOf(false) }
    var equipeSelecionada by remember(diarioId) { mutableStateOf(setOf<String>()) }

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

    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    LaunchedEffect(diario?.obraId) {
        val obraId = diario?.obraId ?: return@LaunchedEffect
        obra = viewModel.buscarObraPorId(obraId)
    }

    LaunchedEffect(diario?.id) {
        val diarioAtual = diario ?: return@LaunchedEffect
        if (dadosLocaisInicializados) return@LaunchedEffect

        etapaExpandida = diarioAtual.etapaAtual

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
                                text = "Serviços executados: ${servicos.size}",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (servicos.isEmpty()) {
                                Text(
                                    text = "Nenhum serviço lançado ainda.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                servicos.forEach { servico ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .clickable { onAbrirServico(servico.id) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF5F5F5)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "Protocolo: ${servico.numeroProtocolo}",
                                                style = MaterialTheme.typography.titleSmall
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = servico.endereco.ifBlank {
                                                    "Endereço não informado"
                                                },
                                                style = MaterialTheme.typography.bodySmall
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = servico.inicio?.let {
                                                    "Início: $it"
                                                } ?: "Horário não registrado",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { onAbrirServico(0L) },
                                enabled = chegadaTrechoRegistrada,
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
                    status = diario?.statusFechamentoServicos ?: "BLOQUEADA",
                    expandida = etapaExpandida == 5,
                    onClick = {
                        etapaExpandida = 5
                        desviosExpandido = false
                    },
                    conteudo = {
                        Column {
                            Text(
                                "Intervalo: ${
                                    if (diario?.intervaloRegistrado == true) {
                                        "${diario?.inicioIntervalo ?: "--:--"} às ${diario?.fimIntervalo ?: "--:--"}"
                                    } else {
                                        "Não registrado"
                                    }
                                }"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Fechamento: ${
                                    diario?.horarioFechamentoServicos?.ifBlank { "Não informado" } ?: "Não informado"
                                }"
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.concluirFechamentoServicos(
                                        diarioId = diarioId,
                                        inicioIntervalo = diario?.inicioIntervalo ?: "12:00",
                                        fimIntervalo = diario?.fimIntervalo ?: "13:00",
                                        observacaoIntervalo = diario?.observacaoIntervalo.orEmpty(),
                                        intervaloRegistrado = true,
                                        horarioFechamentoServicos = diario?.horarioFechamentoServicos ?: "17:00",
                                        observacaoFechamentoServicos = diario?.observacaoFechamentoServicos.orEmpty()
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Concluir etapa 5")
                            }
                        }
                    }
                )

                EtapaCard(
                    numero = 6,
                    titulo = "Retorno à base",
                    status = diario?.statusRetornoBase ?: "BLOQUEADA",
                    expandida = etapaExpandida == 6,
                    onClick = {
                        etapaExpandida = 6
                        desviosExpandido = false
                    },
                    conteudo = {
                        Column {
                            Text(
                                "Saída: ${
                                    diario?.saidaRetornoBase?.ifBlank { "Não informada" } ?: "Não informada"
                                }"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Chegada à base: ${
                                    diario?.chegadaBase?.ifBlank { "Não informada" } ?: "Não informada"
                                }"
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.concluirRetornoBase(
                                        diarioId = diarioId,
                                        saidaRetornoBase = diario?.saidaRetornoBase ?: "17:30",
                                        chegadaBase = diario?.chegadaBase ?: "18:00",
                                        observacaoRetornoBase = diario?.observacaoRetornoBase.orEmpty()
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Concluir etapa 6")
                            }
                        }
                    }
                )

                DesviosCard(
                    quantidadeDesvios = desvios.size,
                    descricoes = desvios.map { "${it.codigo} - ${it.descricao}" },
                    bloqueado = diario?.diarioFechado == true,
                    expandido = desviosExpandido,
                    onClick = {
                        desviosExpandido = !desviosExpandido
                        if (desviosExpandido) {
                            etapaExpandida = 0
                        }
                    },
                    onAdicionarDesvio = {
                        viewModel.adicionarDesvio(
                            diarioId = diarioId,
                            codigo = "D01",
                            descricao = "Desvio teste"
                        )
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
private fun DesviosCard(
    quantidadeDesvios: Int,
    descricoes: List<String>,
    bloqueado: Boolean,
    expandido: Boolean,
    onClick: () -> Unit,
    onAdicionarDesvio: () -> Unit
) {
    val corFundo = if (quantidadeDesvios > 0) {
        Color(0xFFFFE0B2)
    } else {
        Color(0xFFF2F2F2)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = corFundo)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Desvios",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (quantidadeDesvios == 0) {
                    "Nenhum desvio registrado."
                } else {
                    "Quantidade de desvios: $quantidadeDesvios"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            if (expandido) {
                Spacer(modifier = Modifier.height(12.dp))

                if (bloqueado) {
                    Text(
                        text = "Diário encerrado. Desvios bloqueados para edição.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Button(
                        onClick = onAdicionarDesvio,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Adicionar desvio")
                    }
                }

                if (quantidadeDesvios > 0) {
                    Spacer(modifier = Modifier.height(12.dp))

                    descricoes.forEach { descricao ->
                        Text(
                            text = "• $descricao",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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