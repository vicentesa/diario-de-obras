package com.example.diarioobras.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.diarioobras.data.DeslocamentoItemEntity
import com.example.diarioobras.data.DesvioItemEntity
import com.example.diarioobras.data.DiarioEntity
import com.example.diarioobras.data.ObraEntity
import com.example.diarioobras.data.ServicoEntity
import com.google.android.gms.location.LocationServices
import java.io.File
import java.util.Calendar
import java.util.Locale
import android.util.Log
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField


import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObrasScreen(
    viewModel: MainViewModel,
    onAbrirObra: (Long) -> Unit
) {
    val obras by viewModel.obras.collectAsStateWithLifecycle()

    var nome by remember { mutableStateOf("") }
    var local by remember { mutableStateOf("") }
    var contratante by remember { mutableStateOf("") }
    var contrato by remember { mutableStateOf("") }
    var dataInicioContrato by remember { mutableStateOf("") }
    var prazoContratoDias by remember { mutableStateOf("") }
    var mostrarCadastro by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val calendario = remember { Calendar.getInstance() }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dataInicioContrato = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sistema de Controle de Obras") })
        },
        floatingActionButton = {
            if (!mostrarCadastro) {
                ExtendedFloatingActionButton(
                    onClick = { mostrarCadastro = true }
                ) {
                    Text("Cadastrar novo contrato")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Contratos Cadastrados", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(obras) { obra ->
                    Card(
                        onClick = { onAbrirObra(obra.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(obra.nome, style = MaterialTheme.typography.titleMedium)
                            if (obra.local.isNotBlank()) {
                                Text(buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("Local: ")
                                    }
                                    append(obra.local)
                                })
                            }

                            if (obra.contratante.isNotBlank()) {
                                Text(buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("Contratante: ")
                                    }
                                    append(obra.contratante)
                                })
                            }

                            if (obra.contrato.isNotBlank()) {
                                Text(buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("Contrato: ")
                                    }
                                    append(obra.contrato)
                                })
                            }
                        }
                    }
                }
            }

            if (mostrarCadastro) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Cadastrar novo contrato", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome da obra") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = local,
                    onValueChange = { local = it },
                    label = { Text("Local") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = contratante,
                    onValueChange = { contratante = it },
                    label = { Text("Contratante") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = contrato,
                    onValueChange = { contrato = it },
                    label = { Text("Número do contrato") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (dataInicioContrato.isBlank()) {
                            "Selecionar data de início do contrato"
                        } else {
                            dataInicioContrato
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = prazoContratoDias,
                    onValueChange = { prazoContratoDias = it.filter(Char::isDigit) },
                    label = { Text("Prazo do contrato (dias)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.adicionarObra(
                            nome = nome,
                            local = local,
                            contratante = contratante,
                            contrato = contrato,
                            dataInicioContrato = dataInicioContrato,
                            prazoContratoDias = prazoContratoDias.toIntOrNull() ?: 0
                        )
                        nome = ""
                        local = ""
                        contratante = ""
                        contrato = ""
                        dataInicioContrato = ""
                        prazoContratoDias = ""
                        mostrarCadastro = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar obra")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObraDetalheScreen(
    obraId: Long,
    viewModel: MainViewModel,
    onAbrirDiario: (Long) -> Unit
) {
    val diarios by viewModel.diariosDaObra(obraId).collectAsStateWithLifecycle()
    val context = LocalContext.current
    var obra by remember { mutableStateOf<ObraEntity?>(null) }

    LaunchedEffect(obraId) {
        obra = viewModel.buscarObraPorId(obraId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Diários da obra")
                        obra?.nome?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.criarNovoDiario(
                    obraId = obraId,
                    onCriado = { novoDiarioId ->
                        onAbrirDiario(novoDiarioId)
                    },
                    onJaExiste = { mensagem ->
                        android.widget.Toast
                            .makeText(
                                context,
                                mensagem,
                                android.widget.Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                )
            }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Clique no + para abrir novo diário", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(diarios) { diario ->
                    Card(
                        onClick = { onAbrirDiario(diario.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Diário ${diario.data}", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiarioScreen(
    diarioId: Long,
    viewModel: MainViewModel,
    onAbrirServico: (Long) -> Unit,
    abaInicial: Int = 0
) {
    val scope = rememberCoroutineScope()
    val deslocamentos by viewModel.deslocamentosDoDiario(diarioId).collectAsStateWithLifecycle()
    val desvios by viewModel.desviosDoDiario(diarioId).collectAsStateWithLifecycle()
    val scrollStateDeslocamentos = rememberScrollState()
    val servicos by viewModel.servicosDoDiario(diarioId).collectAsStateWithLifecycle()
    val carregamentos by viewModel.carregamentosDoDiario(diarioId).collectAsState()


    var diario by remember { mutableStateOf<DiarioEntity?>(null) }
    var obra by remember { mutableStateOf<ObraEntity?>(null) }
    var abaSelecionada by remember(diarioId, abaInicial) { mutableStateOf(abaInicial) }

    var menuEncarregadoExpandido by remember { mutableStateOf(false) }
    var encarregadoSelecionado by remember { mutableStateOf("") }

    var menuEquipeExpandido by remember { mutableStateOf(false) }
    var equipeSelecionada by remember { mutableStateOf(setOf<String>()) }

    var menuVeiculoExpandido by remember { mutableStateOf(false) }
    var veiculoSelecionado by remember { mutableStateOf("") }

    var menuEquipamentosExpandido by remember { mutableStateOf(false) }
    var equipamentosSelecionados by remember { mutableStateOf(setOf<String>()) }
    var menuDesvioExpandido by remember { mutableStateOf(false) }

    var menuLocalCarregamentoExpandido by remember { mutableStateOf(false) }
    var localCarregamentoSelecionado by remember { mutableStateOf("") }
    var pesoLiquidoTon by remember { mutableStateOf("") }
    var fotoTicketUri by remember { mutableStateOf<Uri?>(null) }
    var mostrarTicketAmpliado by remember { mutableStateOf(false) }

    var ordemServico by remember { mutableStateOf("") }
    var comprimento by remember { mutableStateOf("") }
    var largura by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var latitudeAtual by remember { mutableStateOf<Double?>(null) }
    var longitudeAtual by remember { mutableStateOf<Double?>(null) }

    var numeroProtocolo by remember { mutableStateOf("") }
    var enderecoServico by remember { mutableStateOf("") }
    var inicioServico by remember { mutableStateOf("") }
    var fimServico by remember { mutableStateOf("") }
    var editandoInicioServico by remember { mutableStateOf(false) }
    var editandoFimServico by remember { mutableStateOf(false) }
    var servicoEmEdicaoId by remember { mutableStateOf<Long?>(null) }

    var capturaParaTicket by remember { mutableStateOf(false) }

    var csvTeste by remember { mutableStateOf<String?>(null) }

    var csvSalvo by remember { mutableStateOf<String?>(null) }

    var textoCsvTeste by remember { mutableStateOf<String?>(null) }

    var textoRelatorio by remember { mutableStateOf<String?>(null) }

    var mensagemCsv by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    latitudeAtual = location?.latitude
                    longitudeAtual = location?.longitude

                    if (location != null) {
                        try {
                            val geocoder = Geocoder(context, Locale("pt", "BR"))
                            geocoder.getFromLocation(
                                location.latitude,
                                location.longitude,
                                1,
                                object : Geocoder.GeocodeListener {
                                    override fun onGeocode(addresses: List<Address>) {
                                        val address = addresses.firstOrNull()
                                        val rua = address?.thoroughfare.orEmpty()
                                        val numero = address?.subThoroughfare.orEmpty()

                                        val resultado = if (rua.isNotBlank() || numero.isNotBlank()) {
                                            listOf(rua, numero)
                                                .filter { it.isNotBlank() }
                                                .joinToString(", ")
                                        } else {
                                            address?.getAddressLine(0).orEmpty()
                                        }

                                        if (resultado.isNotBlank()) {
                                            enderecoServico = resultado
                                        }
                                    }

                                    override fun onError(errorMessage: String?) {}
                                }
                            )
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    val ticketCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.atualizarCarregamentoDiario(
                diarioId = diarioId,
                localCarregamento = localCarregamentoSelecionado,
                pesoLiquidoTon = pesoLiquidoTon,
                fotoTicketUri = fotoTicketUri?.toString() ?: ""
            )
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (capturaParaTicket) {
                fotoTicketUri?.let { ticketCameraLauncher.launch(it) }
            } else {
                fotoUri?.let { cameraLauncher.launch(it) }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (capturaParaTicket) {
                val novaUri = criarUriParaFoto(context)
                fotoTicketUri = novaUri
                ticketCameraLauncher.launch(novaUri)
            } else {
                val novaUri = criarUriParaFoto(context)
                fotoUri = novaUri

                if (
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    cameraLauncher.launch(novaUri)
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
    }

    LaunchedEffect(diarioId) {
        diario = viewModel.buscarDiarioPorId(diarioId)
        encarregadoSelecionado = diario?.encarregado ?: ""
        equipeSelecionada = diario?.equipe
            ?.split(" / ")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
        veiculoSelecionado = diario?.veiculo ?: ""
        equipamentosSelecionados = diario?.equipamentosAuxiliares
            ?.split(" / ")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
        localCarregamentoSelecionado = diario?.localCarregamento ?: ""
        pesoLiquidoTon = diario?.pesoLiquidoTon ?: ""
        fotoTicketUri = diario?.fotoTicketUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        diario?.let {
            obra = viewModel.buscarObraPorId(it.obraId)
        }
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            Button(
                onClick = {
                    scope.launch {
                        mensagemCsv = "Tentando salvar..."
                        try {
                            val nomeArquivo = viewModel.salvarCsvDiarioNoApp(context, diarioId)
                            if (nomeArquivo != null) {
                                csvSalvo = nomeArquivo
                                mensagemCsv = "CSV salvo com sucesso: $nomeArquivo"
                            } else {
                                mensagemCsv = "Falha: retorno nulo ao salvar CSV"
                            }
                        } catch (e: Exception) {
                            mensagemCsv = "Erro ao salvar CSV: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar CSV")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        mensagemCsv = "Preparando compartilhamento..."
                        try {
                            val nomeArquivo = viewModel.salvarCsvDiarioNoApp(context, diarioId)
                            if (nomeArquivo != null) {
                                val uri = viewModel.obterUriCsvSalvo(context, nomeArquivo)

                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, nomeArquivo)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                context.startActivity(
                                    Intent.createChooser(intent, "Compartilhar CSV")
                                )

                                mensagemCsv = "Compartilhamento aberto para: $nomeArquivo"
                            } else {
                                mensagemCsv = "Falha: não foi possível gerar o CSV para compartilhamento"
                            }
                        } catch (e: Exception) {
                            mensagemCsv = "Erro ao compartilhar CSV: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Compartilhar CSV")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        textoCsvTeste = viewModel.gerarCsvDiarioPorId(diarioId)
                            ?: "Não foi possível gerar o CSV."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Testar CSV atual")
            }

            Spacer(modifier = Modifier.height(8.dp))

            ScrollableTabRow(selectedTabIndex = abaSelecionada) {
                Tab(
                    selected = abaSelecionada == 0,
                    onClick = { abaSelecionada = 0 },
                    text = { Text("Equipe/Equip.") }
                )
                Tab(
                    selected = abaSelecionada == 1,
                    onClick = { abaSelecionada = 1 },
                    text = { Text("Carregamento") }
                )
                Tab(
                    selected = abaSelecionada == 2,
                    onClick = { abaSelecionada = 2 },
                    text = { Text("Deslocamentos") }
                )
                Tab(
                    selected = abaSelecionada == 3,
                    onClick = { abaSelecionada = 3 },
                    text = { Text("Serviços") }
                )
                Tab(
                    selected = abaSelecionada == 4,
                    onClick = { abaSelecionada = 4 },
                    text = { Text("Desvios") }
                )
            }

            when (abaSelecionada) {
                2 -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(scrollStateDeslocamentos)
                                .fillMaxSize()
                                .padding(bottom = 88.dp)
                        ) {
                            Text("Deslocamentos", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))

                            deslocamentos.forEach { item ->
                                DeslocamentoCard(
                                    item = item,
                                    onMarcarInicio = { viewModel.marcarInicio(item) },
                                    onMarcarFim = { viewModel.marcarFim(item) },
                                    onSalvarManual = { inicio, fim ->
                                        viewModel.atualizarHorarioManual(item, inicio, fim)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                4 -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                                .fillMaxSize()
                                .padding(bottom = 88.dp)
                        ) {
                            Text("Desvios", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))

                            desvios.forEach { item ->
                                DesvioCard(
                                    item = item,
                                    onMarcarInicio = { viewModel.marcarInicioDesvio(item) },
                                    onMarcarFim = { viewModel.marcarFimDesvio(item) },
                                    onSalvarManual = { inicio, fim ->
                                        viewModel.atualizarHorarioDesvio(item, inicio, fim)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = { menuDesvioExpandido = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Informar desvio")
                            }

                            DropdownMenu(
                                expanded = menuDesvioExpandido,
                                onDismissRequest = { menuDesvioExpandido = false }
                            ) {
                                LISTA_DESVIOS.forEach { (codigo, descricao) ->
                                    DropdownMenuItem(
                                        text = { Text("$codigo - $descricao") },
                                        onClick = {
                                            viewModel.adicionarDesvio(
                                                diarioId = diarioId,
                                                codigo = codigo,
                                                descricao = descricao
                                            )
                                            menuDesvioExpandido = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                0 -> {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize()
                    ) {
                        Text("Equipe/Equipamento", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Encarregado", style = MaterialTheme.typography.titleMedium)
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
                                        viewModel.atualizarEquipeDiario(
                                            diarioId = diarioId,
                                            encarregado = encarregadoSelecionado,
                                            equipe = equipeSelecionada.toList(),
                                            veiculo = veiculoSelecionado,
                                            equipamentosAuxiliares = equipamentosSelecionados.toList()
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Equipe", style = MaterialTheme.typography.titleMedium)
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
                            onDismissRequest = {
                                menuEquipeExpandido = false
                                viewModel.atualizarEquipeDiario(
                                    diarioId = diarioId,
                                    encarregado = encarregadoSelecionado,
                                    equipe = equipeSelecionada.toList(),
                                    veiculo = veiculoSelecionado,
                                    equipamentosAuxiliares = equipamentosSelecionados.toList()
                                )
                            }
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
                            FlowRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                equipeSelecionada.forEach { nome ->
                                    FilterChip(
                                        selected = true,
                                        onClick = { },
                                        label = { Text(nome) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Veículo", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { menuVeiculoExpandido = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (veiculoSelecionado.isBlank()) {
                                    "Selecionar veículo"
                                } else {
                                    veiculoSelecionado
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
                                        veiculoSelecionado = veiculo
                                        menuVeiculoExpandido = false
                                        viewModel.atualizarEquipeDiario(
                                            diarioId = diarioId,
                                            encarregado = encarregadoSelecionado,
                                            equipe = equipeSelecionada.toList(),
                                            veiculo = veiculoSelecionado,
                                            equipamentosAuxiliares = equipamentosSelecionados.toList()
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Equipamentos Auxiliares", style = MaterialTheme.typography.titleMedium)
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
                            onDismissRequest = {
                                menuEquipamentosExpandido = false
                                viewModel.atualizarEquipeDiario(
                                    diarioId = diarioId,
                                    encarregado = encarregadoSelecionado,
                                    equipe = equipeSelecionada.toList(),
                                    veiculo = veiculoSelecionado,
                                    equipamentosAuxiliares = equipamentosSelecionados.toList()
                                )
                            }
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
                            FlowRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                equipamentosSelecionados.forEach { nome ->
                                    FilterChip(
                                        selected = true,
                                        onClick = { },
                                        label = { Text(nome) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                1 -> {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize()
                    ) {
                        Text("Carregamento", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Local de Carregamento", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (carregamentos.isNotEmpty()) {
                            carregamentos.forEach { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            "Carregamento ${item.ordem}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text("Local: ${item.localCarregamento}")
                                        Text("Peso líquido: ${item.pesoLiquidoTon}")

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (item.fotoTicketUri.isBlank()) {
                                            Text("Ticket: sem foto")
                                        } else {
                                            Text("Ticket:")
                                            Spacer(modifier = Modifier.height(8.dp))

                                            AsyncImage(
                                                model = item.fotoTicketUri,
                                                contentDescription = "Foto do ticket do carregamento ${item.ordem}",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.excluirCarregamento(item)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Excluir carregamento")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        OutlinedButton(
                            onClick = { menuLocalCarregamentoExpandido = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (localCarregamentoSelecionado.isBlank()) {
                                    "Selecionar local de carregamento"
                                } else {
                                    localCarregamentoSelecionado
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = { menuLocalCarregamentoExpandido = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (localCarregamentoSelecionado.isBlank()) {
                                    "Selecionar local de carregamento"
                                } else {
                                    localCarregamentoSelecionado
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = menuLocalCarregamentoExpandido,
                            onDismissRequest = { menuLocalCarregamentoExpandido = false }
                        ) {
                            LISTA_LOCAIS_CARREGAMENTO.forEach { local ->
                                DropdownMenuItem(
                                    text = { Text(local) },
                                    onClick = {
                                        localCarregamentoSelecionado = local
                                        menuLocalCarregamentoExpandido = false
                                        viewModel.atualizarCarregamentoDiario(
                                            diarioId = diarioId,
                                            localCarregamento = localCarregamentoSelecionado,
                                            pesoLiquidoTon = pesoLiquidoTon,
                                            fotoTicketUri = fotoTicketUri?.toString() ?: ""
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = pesoLiquidoTon,
                            onValueChange = {
                                pesoLiquidoTon = it
                                viewModel.atualizarCarregamentoDiario(
                                    diarioId = diarioId,
                                    localCarregamento = localCarregamentoSelecionado,
                                    pesoLiquidoTon = pesoLiquidoTon,
                                    fotoTicketUri = fotoTicketUri?.toString() ?: ""
                                )
                            },
                            label = { Text("Peso Líq. Ton") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                capturaParaTicket = true
                                if (
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    val novaUri = criarUriParaFoto(context)
                                    fotoTicketUri = novaUri
                                    ticketCameraLauncher.launch(novaUri)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (fotoTicketUri == null) "Foto do Ticket" else "Ticket capturado")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (fotoTicketUri != null) {
                            AsyncImage(
                                model = fotoTicketUri,
                                contentDescription = "Foto do ticket",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clickable { mostrarTicketAmpliado = true },
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Button(
                            onClick = {
                                viewModel.adicionarCarregamento(
                                    diarioId = diarioId,
                                    veiculo = "",
                                    chegadaUsina = null,
                                    inicioCarregamento = null,
                                    fimCarregamento = null,
                                    horarioPesagem = null,
                                    saidaUsinaTrecho = null,
                                    localCarregamento = "",
                                    pesoLiquidoTon = pesoLiquidoTon,
                                    fotoTicketUri = fotoTicketUri?.toString().orEmpty()
                                )

                                localCarregamentoSelecionado = ""
                                pesoLiquidoTon = ""
                                fotoTicketUri = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Salvar carregamento")
                        }

                    }
                }

                3 -> {
                    var mostrarFormulario by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize()
                    ) {
                        Text("Serviços executados", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            servicos.forEach { servico ->
                                ServicoCard(
                                    servico = servico,
                                    onClick = {
                                        onAbrirServico(servico.id)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (mostrarFormulario) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Cadastrar novo serviço - Tapa buraco", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        capturaParaTicket = false
                                        if (
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.CAMERA
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            val novaUri = criarUriParaFoto(context)
                                            fotoUri = novaUri

                                            if (
                                                ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.ACCESS_FINE_LOCATION
                                                ) == PackageManager.PERMISSION_GRANTED
                                            ) {
                                                cameraLauncher.launch(novaUri)
                                            } else {
                                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                            }
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (fotoUri == null) "Foto antes" else "Foto capturada")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = numeroProtocolo,
                                    onValueChange = { numeroProtocolo = it },
                                    label = { Text("Número protocolo") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = enderecoServico,
                                    onValueChange = { enderecoServico = it },
                                    label = { Text("Endereço") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = comprimento,
                                    onValueChange = { comprimento = it },
                                    label = { Text("Comp. (m)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = largura,
                                    onValueChange = { largura = it },
                                    label = { Text("Largura (m)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = altura,
                                    onValueChange = { altura = it },
                                    label = { Text("Espessura (m)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Horário início", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            inicioServico = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                                .format(java.util.Date())
                                            editandoInicioServico = false
                                        }
                                    ) {
                                        Text("Marcar início")
                                    }

                                    OutlinedButton(
                                        onClick = { editandoInicioServico = !editandoInicioServico }
                                    ) {
                                        Text("Editar")
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = inicioServico,
                                    onValueChange = { inicioServico = it },
                                    label = { Text("Início") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = editandoInicioServico
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Horário fim", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            fimServico = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                                .format(java.util.Date())
                                            editandoFimServico = false
                                        }
                                    ) {
                                        Text("Marcar fim")
                                    }

                                    OutlinedButton(
                                        onClick = { editandoFimServico = !editandoFimServico }
                                    ) {
                                        Text("Editar")
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = fimServico,
                                    onValueChange = { fimServico = it },
                                    label = { Text("Fim") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = editandoFimServico
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        val ordem = ordemServico.toIntOrNull() ?: 0
                                        val comp = comprimento.toDoubleOrNull() ?: 0.0
                                        val larg = largura.toDoubleOrNull() ?: 0.0
                                        val alt = altura.toDoubleOrNull() ?: 0.0

                                        if (servicoEmEdicaoId != null) {
                                            viewModel.atualizarServico(
                                                ServicoEntity(
                                                    id = servicoEmEdicaoId!!,
                                                    diarioId = diarioId,
                                                    ordemServico = ordem,
                                                    numeroProtocolo = numeroProtocolo,
                                                    endereco = enderecoServico,
                                                    comprimento = comp,
                                                    largura = larg,
                                                    altura = alt,
                                                    inicio = inicioServico.ifBlank { null },
                                                    fim = fimServico.ifBlank { null },
                                                    latitude = latitudeAtual,
                                                    longitude = longitudeAtual,
                                                    fotoUri = fotoUri?.toString()
                                                )
                                            )
                                        } else {
                                            viewModel.adicionarServico(
                                                diarioId = diarioId,
                                                ordemServico = ordem,
                                                numeroProtocolo = numeroProtocolo,
                                                endereco = enderecoServico,
                                                comprimento = comp,
                                                largura = larg,
                                                altura = alt,
                                                inicio = inicioServico.ifBlank { null },
                                                fim = fimServico.ifBlank { null },
                                                latitude = latitudeAtual,
                                                longitude = longitudeAtual,
                                                fotoUri = fotoUri?.toString()
                                            )
                                        }

                                        servicoEmEdicaoId = null
                                        ordemServico = ""
                                        numeroProtocolo = ""
                                        enderecoServico = ""
                                        comprimento = ""
                                        largura = ""
                                        altura = ""
                                        inicioServico = ""
                                        fimServico = ""
                                        fotoUri = null
                                        latitudeAtual = null
                                        longitudeAtual = null
                                        editandoInicioServico = false
                                        editandoFimServico = false
                                        mostrarFormulario = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Salvar serviço")
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Button(
                            onClick = {
                                onAbrirServico(0L)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cadastrar novo serviço")
                        }
                    }
                }
            }
        }
    }

    if (mostrarTicketAmpliado && fotoTicketUri != null) {
        Dialog(onDismissRequest = { mostrarTicketAmpliado = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { mostrarTicketAmpliado = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = fotoTicketUri,
                    contentDescription = "Ticket ampliado",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    mensagemCsv?.let { mensagem ->
        AlertDialog(
            onDismissRequest = { mensagemCsv = null },
            confirmButton = {
                TextButton(onClick = { mensagemCsv = null }) {
                    Text("Fechar")
                }
            },
            title = {
                Text("Salvar CSV")
            },
            text = {
                Text(mensagem)
            }
        )
    }

    textoRelatorio?.let { texto ->
        AlertDialog(
            onDismissRequest = { textoRelatorio = null },
            confirmButton = {
                TextButton(onClick = { textoRelatorio = null }) {
                    Text("Fechar")
                }
            },
            title = {
                Text("Relatório Diário")
            },
            text = {
                Text(
                    text = texto.take(4000),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        )
    }
    textoCsvTeste?.let { texto ->
        AlertDialog(
            onDismissRequest = { textoCsvTeste = null },
            confirmButton = {
                TextButton(onClick = { textoCsvTeste = null }) {
                    Text("Fechar")
                }
            },
            title = {
                Text("CSV Atual")
            },
            text = {
                Text(
                    text = texto.take(5000),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        )
    }
}

@Composable
fun DeslocamentoCard(
    item: DeslocamentoItemEntity,
    somenteInicio: Boolean = false,
    onMarcarInicio: () -> Unit,
    onMarcarFim: () -> Unit,
    onSalvarManual: (String, String) -> Unit
) {
    val isSomenteInicio =
        somenteInicio ||
                item.titulo.equals("Batendo ponto de entrada", ignoreCase = true) ||
                item.titulo.equals("Batendo ponto na entrada", ignoreCase = true)

    var mostrarDialogInicio by remember { mutableStateOf(false) }
    var mostrarDialogFim by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            if (item.titulo.isNotBlank()) {
                Text(
                    text = item.titulo,
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Text("Início: ${item.inicio ?: "--:--"}")

            if (!isSomenteInicio) {
                Text("Fim: ${item.fim ?: "--:--"}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMarcarInicio,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Marcar entrada")
                }

                OutlinedButton(
                    onClick = { mostrarDialogInicio = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Editar entrada")
                }
            }

            if (!isSomenteInicio) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onMarcarFim,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Marcar fim")
                    }

                    OutlinedButton(
                        onClick = { mostrarDialogFim = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Editar fim")
                    }
                }
            }
        }
    }

    if (mostrarDialogInicio) {
        HoraMinutoDialog(
            titulo = "Editar entrada",
            valorAtual = item.inicio,
            onDismiss = { mostrarDialogInicio = false },
            onConfirmar = { novoHorario ->
                onSalvarManual(
                    novoHorario,
                    item.fim.orEmpty()
                )
                mostrarDialogInicio = false
            }
        )
    }

    if (!isSomenteInicio && mostrarDialogFim) {
        HoraMinutoDialog(
            titulo = "Editar fim",
            valorAtual = item.fim,
            onDismiss = { mostrarDialogFim = false },
            onConfirmar = { novoHorario ->
                onSalvarManual(
                    item.inicio.orEmpty(),
                    novoHorario
                )
                mostrarDialogFim = false
            }
        )
    }
}

@Composable
private fun HoraMinutoDialog(
    titulo: String,
    valorAtual: String?,
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    val partes = valorAtual?.split(":").orEmpty()
    var hora by remember { mutableStateOf(partes.getOrNull(0) ?: "") }
    var minuto by remember { mutableStateOf(partes.getOrNull(1) ?: "") }

    val horaValida = hora.toIntOrNull()?.let { it in 0..23 } == true
    val minutoValido = minuto.toIntOrNull()?.let { it in 0..59 } == true
    val podeSalvar = horaValida && minutoValido

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val horarioFormatado =
                        "${hora.padStart(2, '0')}:${minuto.padStart(2, '0')}"
                    onConfirmar(horarioFormatado)
                },
                enabled = podeSalvar
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = {
            Text(titulo)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = hora,
                    onValueChange = {
                        if (it.length <= 2 && it.all(Char::isDigit)) {
                            hora = it
                        }
                    },
                    label = { Text("Hora") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = minuto,
                    onValueChange = {
                        if (it.length <= 2 && it.all(Char::isDigit)) {
                            minuto = it
                        }
                    },
                    label = { Text("Minuto") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
fun ServicoCard(
    servico: ServicoEntity,
    onClick: () -> Unit
) {
    val volumeCalculado = servico.comprimento * servico.largura * servico.altura

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "#${servico.ordemServico} - ${servico.endereco.ifBlank { "Endereço não informado" }}",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Comprimento: ${servico.comprimento}")
            Text("Largura: ${servico.largura}")
            Text("Espessura: ${servico.altura}")
            Text("Volume Calculado: $volumeCalculado")

            servico.latitude?.let { Text("Latitude: $it") }
            servico.longitude?.let { Text("Longitude: $it") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObraInfoScreen(
    obraId: Long,
    viewModel: MainViewModel,
    onAbrirDiarios: (Long) -> Unit
) {
    var obra by remember { mutableStateOf<ObraEntity?>(null) }

    LaunchedEffect(obraId) {
        obra = viewModel.buscarObraPorId(obraId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(obra?.nome ?: "Obra") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                obra?.let {
                    Text("Título: ${it.nome}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Local: ${it.local}")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Contratante: ${it.contratante}")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Contrato: ${it.contrato}")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Início do contrato: ${it.dataInicioContrato}")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Prazo do contrato (dias): ${it.prazoContratoDias}")
                }
            }

            Button(
                onClick = { onAbrirDiarios(obraId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Diários de Obra")
            }
        }
    }
}

@Composable
fun DesvioCard(
    item: DesvioItemEntity,
    onMarcarInicio: () -> Unit,
    onMarcarFim: () -> Unit,
    onSalvarManual: (String?, String?) -> Unit
) {
    var inicioEdit by remember(item.id, item.inicio) { mutableStateOf(item.inicio ?: "") }
    var fimEdit by remember(item.id, item.fim) { mutableStateOf(item.fim ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${item.codigo} - ${item.descricao}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onMarcarInicio) {
                    Text("Marcar início")
                }
                Button(onClick = onMarcarFim) {
                    Text("Marcar fim")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = inicioEdit,
                onValueChange = { inicioEdit = it },
                label = { Text("Início") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = fimEdit,
                onValueChange = { fimEdit = it },
                label = { Text("Fim") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    onSalvarManual(
                        inicioEdit.ifBlank { null },
                        fimEdit.ifBlank { null }
                    )
                }
            ) {
                Text("Salvar edição")
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

private fun criarUriParaFoto(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imagesDir, "servico_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

private val LISTA_DESVIOS = listOf(
    "P11" to "AGUARDANDO FILA (USINA)",
    "P19" to "AGUARDANDO FISCALIZAÇÃO PARA DEFINIÇÃO",
    "P37" to "AGUARDANDO FISCALIZAÇÃO COM FINAL EM POSTERGAMENTO",
    "P31" to "AGUARDANDO LIBERAÇÃO DO SERVIÇO PELA CONTRATANTE",
    "P12" to "CARREGAMENTO ASFALTO",
    "P36" to "CARREGANDO MATERIAL PARA O SERVIÇO",
    "P2" to "CHUVA",
    "P38" to "DDS",
    "P13" to "DESLOCAMENTO",
    "P21" to "ENCONTRADO ROCHA",
    "P16" to "INTERFERÊNCIA REDE DE ÁGUA",
    "P18" to "INTERFERÊNCIA REDE ELÉTRICA / DADOS",
    "P17" to "INTERFERÊNCIA REDE PLUVIAL",
    "P45" to "LIMPEZA DO CANTEIRO DE OBRAS",
    "P41" to "MONTAGEM DO ROLO PÉ DE CARNEIRO",
    "P27" to "MORADOR NÃO PERMITE REALIZAR O SERVIÇO",
    "P42" to "ORGANIZAÇÃO DE MATERIAL DE TRABALHO",
    "T3" to "OUTRO SERVIÇO JÁ APONTADO",
    "P44" to "QUEBRA DE EQUIPAMENTO",
    "P14" to "QUEBRA EQUIPAMENTO DE ESCAVAÇÃO",
    "P4" to "QUEBRA GERADOR",
    "P5" to "QUEBRA MARTELETE",
    "P6" to "QUEBRA COMPACTADOR DE PERCUSSÃO",
    "P7" to "QUEBRA PLACA VIBRATÓRIA",
    "P32" to "QUEBRA DE REDE EXISTENTE",
    "P15" to "QUEBRA ROMPEDOR HIDRÁULICO",
    "P43" to "QUEBRA ROLO COMPACTADOR DE SOLO",
    "P3" to "QUEBRA VEÍCULO",
    "P1" to "REFEIÇÃO / INTERVALO",
    "P35" to "SEM ATIVIDADE",
    "P25" to "SINALIZAÇÃO DE VIA",
    "P46" to "SONDAGEM",
    "P26" to "SOLICITAÇÃO DE ABERTURA DE TRÁF. POR ÓRGÃO COMPETENTE",
    "P20" to "SOLICITAÇÃO DE ÓRGÃO PÚBLICO",
    "P29" to "SOLICITAÇÃO IMPROCEDENTE (OS COM CÓD. DE SERV. ERRADO)",
    "P22" to "SOLICITAÇÃO SETOR SEGURANÇA DO TRABALHO",
    "T2" to "SERVIÇOS DE RETRABALHO / GARANTIA",
    "P30" to "FALTA DE COLABORADOR",
    "P24" to "FALTA DE EPC",
    "P23" to "FALTA DE EPI",
    "P10" to "FALTA DE FERRAMENTAS",
    "P8" to "FALTA DE MATERIAL - INSUMOS (AREIA, BASE, CBUQ)",
    "P40" to "FALTA DE MATERIAL FORNECIDO PELO CLIENTE",
    "P9" to "FALTA DE MATERIAL HIDRÁULICO",
    "P39" to "TREINAMENTO",
    "T1" to "TRABALHANDO",
    "P33" to "VAZAMENTO EM REDE EXISTENTE",
    "P28" to "VEÍCULO ESTACIONADO NO LOCAL DA EXECUÇÃO DO SERVIÇO"
)

private val LISTA_LOCAIS_CARREGAMENTO = listOf(
    "Conpla",
    "Adrimar",
    "Infrasul",
    "Vogelsanger",
    "Tec Via",
    "BRF",
    "PS Asfalto",
    "Genuíno - Porto Alegre"
)
