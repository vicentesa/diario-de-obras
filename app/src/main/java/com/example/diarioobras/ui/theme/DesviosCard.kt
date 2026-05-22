package com.example.diarioobras.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.diarioobras.data.DesvioItemEntity
import com.example.diarioobras.ui.MainViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun DesviosCard(
    quantidadeDesvios: Int,
    desvios: List<DesvioItemEntity>,
    tiposDesvio: List<Pair<String, String>>,
    diarioId: Long,
    veiculosDiario: List<String> = emptyList(),
    viewModel: MainViewModel,
    bloqueado: Boolean,
    expandido: Boolean,
    onClick: () -> Unit,
    onEditarObservacao: (DesvioItemEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var menuTipoDesvioExpandido by remember { mutableStateOf(false) }
    var codigoEmCadastro by remember { mutableStateOf("") }
    var descricaoEmCadastro by remember { mutableStateOf("") }
    var inicioEmCadastro by remember { mutableStateOf("") }
    var fimEmCadastro by remember { mutableStateOf("") }
    var observacaoEmCadastro by remember { mutableStateOf("") }
    var desvioExpandidoId by remember { mutableStateOf<Long?>(null) }
    var litrosDesvioAb by remember { mutableStateOf("") }
    var fotoTicketDesvioAbUri by remember { mutableStateOf<Uri?>(null) }
    var versaoPreviewDesvioAb by remember { mutableStateOf(0) }
    var latitudeDesvioAb by remember { mutableStateOf(0.0) }
    var longitudeDesvioAb by remember { mutableStateOf(0.0) }

    var fotoDesvioEditUri by remember { mutableStateOf<Uri?>(null) }
    var versaoPreviewFotoDesvioEdit by remember { mutableStateOf(0) }
    var latitudeDesvioEdit by remember { mutableStateOf(0.0) }
    var longitudeDesvioEdit by remember { mutableStateOf(0.0) }

    var veiculoAbSelecionado by remember { mutableStateOf("") }
    var menuVeiculoAbExpandido by remember { mutableStateOf(false) }

    var seletorHoraAberto by remember { mutableStateOf(false) }
    var seletorHoraInicialHora by remember { mutableStateOf(8) }
    var seletorHoraInicialMinuto by remember { mutableStateOf(0) }
    var seletorHoraCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val desvioAbCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { sucesso ->
        if (sucesso) {
            coroutineScope.launch {
                fotoTicketDesvioAbUri?.let { uri ->
                    comprimirFoto(context, uri)
                    salvarFotoNaGaleria(context, uri)
                }
                versaoPreviewDesvioAb++
            }
            coroutineScope.launch {
                try {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    val cts = CancellationTokenSource()
                    val loc = withTimeoutOrNull(5_000L) {
                        runCatching {
                            fusedLocationClient.getCurrentLocation(
                                Priority.PRIORITY_HIGH_ACCURACY, cts.token
                            ).await()
                        }.getOrNull()
                    }
                    if (loc != null) {
                        latitudeDesvioAb = loc.latitude
                        longitudeDesvioAb = loc.longitude
                    }
                } catch (e: Exception) {
                    // GPS não disponível
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val novaUri = criarUriParaFotoEtapa(context)
            fotoTicketDesvioAbUri = novaUri
            desvioAbCameraLauncher.launch(novaUri)
        }
    }

    val desvioEditCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { sucesso ->
        if (sucesso) {
            coroutineScope.launch {
                fotoDesvioEditUri?.let { uri ->
                    comprimirFoto(context, uri)
                    salvarFotoNaGaleria(context, uri)
                }
                versaoPreviewFotoDesvioEdit++
            }
            coroutineScope.launch {
                try {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    val cts = CancellationTokenSource()
                    val loc = withTimeoutOrNull(5_000L) {
                        runCatching {
                            fusedLocationClient.getCurrentLocation(
                                Priority.PRIORITY_HIGH_ACCURACY, cts.token
                            ).await()
                        }.getOrNull()
                    }
                    if (loc != null) {
                        latitudeDesvioEdit = loc.latitude
                        longitudeDesvioEdit = loc.longitude
                    }
                } catch (e: Exception) {
                    // GPS não disponível
                }
            }
        }
    }

    val cameraPermDesvioEditLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val novaUri = criarUriParaFotoEtapa(context)
            fotoDesvioEditUri = novaUri
            desvioEditCameraLauncher.launch(novaUri)
        }
    }

    LaunchedEffect(desvioExpandidoId) {
        val expandido = desvios.firstOrNull { it.id == desvioExpandidoId }
        fotoDesvioEditUri = expandido?.fotoTicketUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        versaoPreviewFotoDesvioEdit = 0
        latitudeDesvioEdit = expandido?.latitude ?: 0.0
        longitudeDesvioEdit = expandido?.longitude ?: 0.0
    }

    fun abrirSeletorHora(valorAtual: String, onHoraSelecionada: (String) -> Unit) {
        val partes = valorAtual.split(":")
        seletorHoraInicialHora = partes.getOrNull(0)?.toIntOrNull() ?: 8
        seletorHoraInicialMinuto = partes.getOrNull(1)?.toIntOrNull() ?: 0
        seletorHoraCallback = onHoraSelecionada
        seletorHoraAberto = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
            ) {
                Text(
                    text = "Desvios / Abastecimentos",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (quantidadeDesvios == 0) "Nenhum desvio registrado."
                    else "Desvios registrados: $quantidadeDesvios"
                )
            }

            if (expandido) {
                Spacer(modifier = Modifier.height(12.dp))

                desvios.forEach { desvio ->
                    key(desvio.id) {
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

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = corCard)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            desvioExpandidoId = if (estaExpandido) null else desvio.id
                                        }
                                ) {
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
                                                else -> "Não iniciado"
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
                                            onClick = { inicioEditado = horaAtualFormatada() },
                                            enabled = !bloqueado,
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Marcar início") }
                                        OutlinedButton(
                                            onClick = {
                                                abrirSeletorHora(inicioEditado) { hora ->
                                                    inicioEditado = hora
                                                }
                                            },
                                            enabled = !bloqueado,
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Editar início") }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Fim: ${fimEditado.ifBlank { "--:--" }}")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { fimEditado = horaAtualFormatada() },
                                            enabled = !bloqueado,
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Marcar fim") }
                                        OutlinedButton(
                                            onClick = {
                                                abrirSeletorHora(fimEditado) { hora ->
                                                    fimEditado = hora
                                                }
                                            },
                                            enabled = !bloqueado,
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Editar fim") }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Observação: ${desvio.observacao.ifBlank { "Nenhuma observação" }}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { onEditarObservacao(desvio) },
                                        enabled = !bloqueado,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Editar observação") }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            if (ContextCompat.checkSelfPermission(
                                                    context, Manifest.permission.CAMERA
                                                ) == PackageManager.PERMISSION_GRANTED
                                            ) {
                                                val novaUri = criarUriParaFotoEtapa(context)
                                                fotoDesvioEditUri = novaUri
                                                desvioEditCameraLauncher.launch(novaUri)
                                            } else {
                                                cameraPermDesvioEditLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        enabled = !bloqueado,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (fotoDesvioEditUri == null) "Foto do desvio" else "Foto do desvio ✓")
                                    }
                                    if (fotoDesvioEditUri != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AsyncImage(
                                            model = fotoDesvioEditUri?.let { "${it}#${versaoPreviewFotoDesvioEdit}" },
                                            contentDescription = "Foto do desvio",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.atualizarHorarioDesvio(
                                                item = desvio,
                                                novoInicio = inicioEditado,
                                                novoFim = fimEditado
                                            )
                                            viewModel.atualizarFotoDesvio(
                                                id = desvio.id,
                                                fotoUri = fotoDesvioEditUri?.toString() ?: "",
                                                latitude = latitudeDesvioEdit,
                                                longitude = longitudeDesvioEdit
                                            )
                                            desvioExpandidoId = null
                                        },
                                        enabled = !bloqueado,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Salvar horários") }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { menuTipoDesvioExpandido = !menuTipoDesvioExpandido },
                    enabled = !bloqueado && codigoEmCadastro.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (menuTipoDesvioExpandido) "Cancelar" else "Adicionar desvio")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        codigoEmCadastro = "AB"
                        descricaoEmCadastro = "Abastecimento"
                        inicioEmCadastro = ""
                        fimEmCadastro = ""
                        observacaoEmCadastro = ""
                        litrosDesvioAb = ""
                        fotoTicketDesvioAbUri = null
                        latitudeDesvioAb = 0.0
                        longitudeDesvioAb = 0.0
                        menuTipoDesvioExpandido = false
                        veiculoAbSelecionado = ""
                    },
                    enabled = !bloqueado && codigoEmCadastro.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Registrar Abastecimento")
                }

                DropdownMenu(
                    expanded = menuTipoDesvioExpandido,
                    onDismissRequest = { menuTipoDesvioExpandido = false },
                    properties = PopupProperties(focusable = false)
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
                                litrosDesvioAb = ""
                                fotoTicketDesvioAbUri = null
                                latitudeDesvioAb = 0.0
                                longitudeDesvioAb = 0.0
                                menuTipoDesvioExpandido = false
                                veiculoAbSelecionado = ""
                            }
                        )
                    }
                }

                if (codigoEmCadastro.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
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
                                    onClick = { inicioEmCadastro = horaAtualFormatada() },
                                    enabled = !bloqueado,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Marcar início") }
                                OutlinedButton(
                                    onClick = {
                                        abrirSeletorHora(inicioEmCadastro) { hora ->
                                            inicioEmCadastro = hora
                                        }
                                    },
                                    enabled = !bloqueado,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Editar início") }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Fim: ${fimEmCadastro.ifBlank { "--:--" }}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { fimEmCadastro = horaAtualFormatada() },
                                    enabled = !bloqueado,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Marcar fim") }
                                OutlinedButton(
                                    onClick = {
                                        abrirSeletorHora(fimEmCadastro) { hora ->
                                            fimEmCadastro = hora
                                        }
                                    },
                                    enabled = !bloqueado,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Editar fim") }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (codigoEmCadastro == "AB") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { menuVeiculoAbExpandido = true },
                                    enabled = !bloqueado,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (veiculoAbSelecionado.isBlank()) "Selecionar veículo abastecido" else veiculoAbSelecionado)
                                }
                                DropdownMenu(
                                    expanded = menuVeiculoAbExpandido,
                                    onDismissRequest = { menuVeiculoAbExpandido = false }
                                ) {
                                    if (veiculosDiario.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Nenhum veículo cadastrado") },
                                            onClick = { menuVeiculoAbExpandido = false }
                                        )
                                    } else {
                                        veiculosDiario.forEach { veiculo ->
                                            DropdownMenuItem(
                                                text = { Text(veiculo) },
                                                onClick = {
                                                    veiculoAbSelecionado = veiculo
                                                    menuVeiculoAbExpandido = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = litrosDesvioAb,
                                    onValueChange = { litrosDesvioAb = it },
                                    label = { Text("Litros abastecidos") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    enabled = !bloqueado,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OutlinedTextField(
                                    value = observacaoEmCadastro,
                                    onValueChange = { observacaoEmCadastro = it },
                                    label = { Text("Observação (opcional)") },
                                    enabled = !bloqueado,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        val novaUri = criarUriParaFotoEtapa(context)
                                        fotoTicketDesvioAbUri = novaUri
                                        desvioAbCameraLauncher.launch(novaUri)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                enabled = !bloqueado,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (codigoEmCadastro == "AB") {
                                        if (fotoTicketDesvioAbUri == null) "Foto do ticket" else "Foto do ticket ✓"
                                    } else {
                                        if (fotoTicketDesvioAbUri == null) "Foto do desvio" else "Foto do desvio ✓"
                                    }
                                )
                            }
                            if (fotoTicketDesvioAbUri != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                AsyncImage(
                                    model = fotoTicketDesvioAbUri?.let { "${it}#${versaoPreviewDesvioAb}" },
                                    contentDescription = "Foto do desvio",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val litros = if (codigoEmCadastro == "AB")
                                        litrosDesvioAb.toDoubleOrNull() ?: 0.0
                                    else 0.0
                                    val fotoUri = fotoTicketDesvioAbUri?.toString() ?: ""
                                    val observacao = if (codigoEmCadastro == "AB") veiculoAbSelecionado else observacaoEmCadastro

                                    viewModel.adicionarDesvioCompleto(
                                        diarioId = diarioId,
                                        codigo = codigoEmCadastro,
                                        descricao = descricaoEmCadastro,
                                        inicio = inicioEmCadastro,
                                        fim = fimEmCadastro,
                                        observacao = observacao,
                                        litros = litros,
                                        fotoTicketUri = fotoUri,
                                        latitude = latitudeDesvioAb,
                                        longitude = longitudeDesvioAb
                                    )

                                    codigoEmCadastro = ""
                                    descricaoEmCadastro = ""
                                    inicioEmCadastro = ""
                                    fimEmCadastro = ""
                                    observacaoEmCadastro = ""
                                    litrosDesvioAb = ""
                                    fotoTicketDesvioAbUri = null
                                    latitudeDesvioAb = 0.0
                                    longitudeDesvioAb = 0.0
                                    veiculoAbSelecionado = ""
                                },
                                enabled = !bloqueado &&
                                        inicioEmCadastro.isNotBlank() &&
                                        fimEmCadastro.isNotBlank() &&
                                        (codigoEmCadastro != "AB" || litrosDesvioAb.isNotBlank()),
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
                                    litrosDesvioAb = ""
                                    fotoTicketDesvioAbUri = null
                                    latitudeDesvioAb = 0.0
                                    longitudeDesvioAb = 0.0
                                    veiculoAbSelecionado = ""
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

    if (seletorHoraAberto) {
        TimePickerDialogCompose(
            initialHour = seletorHoraInicialHora,
            initialMinute = seletorHoraInicialMinuto,
            onConfirm = { hora ->
                seletorHoraCallback?.invoke(hora)
                seletorHoraAberto = false
                seletorHoraCallback = null
            },
            onDismiss = {
                seletorHoraAberto = false
                seletorHoraCallback = null
            }
        )
    }
}

private fun horaAtualFormatada(): String {
    return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogCompose(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar hora") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm("%02d:%02d".format(state.hour, state.minute)) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
