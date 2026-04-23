package com.example.diarioobras.ui.theme

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.diarioobras.data.ServicoAreaEntity
import com.example.diarioobras.data.ServicoEntity
import com.example.diarioobras.ui.MainViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val LISTA_ABERTURA_CAVA = listOf(
    "Já aberta",
    "Rompedor",
    "Fresadora",
    "Policorte"
)

private val LISTA_LIMPEZA_ENTULHO = listOf(
    "Já limpo",
    "Carregado",
    "Em espera"
)

private fun criarUriParaFoto(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val arquivo = File(imagesDir, "foto_${System.currentTimeMillis()}.jpg")
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        arquivo
    )
}

private fun normalizarDecimal(valor: String): String {
    return valor.replace(",", ".")
}

private fun formatarDecimalTruncado(valor: Double): String {
    val truncado = kotlin.math.floor(valor * 100.0) / 100.0
    return String.format(Locale.US, "%.2f", truncado).replace(".", ",")
}

private fun formatarCampoDecimal(valor: String): String {
    val numero = valor.replace(",", ".").toDoubleOrNull() ?: return valor
    return formatarDecimalTruncado(numero)
}

data class AreaCavaUi(
    val numero: Int,
    val comprimento: Double,
    val largura: Double,
    val espessuraCm: Double
) {
    val area: Double
        get() = comprimento * largura
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicoFormScreen(
    diarioId: Long,
    servicoId: Long,
    viewModel: MainViewModel,
    onVoltar: () -> Unit,
    onSalvarConcluir: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val servicosDoDiario by viewModel
        .servicosDoDiario(diarioId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val diario by viewModel
        .buscarDiarioFlow(diarioId)
        .collectAsStateWithLifecycle(initialValue = null)

    val modoSomenteLeitura = diario?.statusServicos == "CONCLUIDA"

    var servicoIdAtual by remember { mutableLongStateOf(servicoId) }

    var ordemServico by remember { mutableStateOf("") }
    var numeroProtocolo by remember { mutableStateOf("") }
    var enderecoServico by remember { mutableStateOf("") }

    var aberturaCava by remember { mutableStateOf("") }
    var menuAberturaExpandido by remember { mutableStateOf(false) }

    var limpezaEntulho by remember { mutableStateOf("") }
    var menuLimpezaExpandido by remember { mutableStateOf(false) }

    var comprimento by remember { mutableStateOf("") }
    var largura by remember { mutableStateOf("") }
    var espessura by remember { mutableStateOf("5,00") }

    var exibindoFormularioArea by remember { mutableStateOf(true) }

    var pinturaLigacao by remember { mutableStateOf(false) }

    val equipamentosCompactacao = remember(diario?.equipamentosCompactacao) {
        diario?.equipamentosCompactacao
            ?.split(" / ")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    var menuEquipamentoCompactacaoExpandido by remember { mutableStateOf(false) }
    var equipamentoCompactacaoSelecionado by remember { mutableStateOf("") }

    var fotoEspessuraUri by remember { mutableStateOf<Uri?>(null) }
    var mostrarFotoEspessuraAmpliada by remember { mutableStateOf(false) }

    val larguraFocusRequester = remember { FocusRequester() }
    val espessuraFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var fotoAntesUri by remember { mutableStateOf<Uri?>(null) }
    var fotoCavaAbertaUri by remember { mutableStateOf<Uri?>(null) }
    var fotoConclusaoUri by remember { mutableStateOf<Uri?>(null) }
    var versaoPreviewFotos by remember { mutableStateOf(0) }
    var horarioFotoAntes by remember { mutableStateOf<String?>(null) }
    var horarioFotoConclusao by remember { mutableStateOf<String?>(null) }

    var latitudeAtual by remember { mutableStateOf<Double?>(null) }
    var longitudeAtual by remember { mutableStateOf<Double?>(null) }

    var mostrarFotoAntesAmpliada by remember { mutableStateOf(false) }
    var mostrarFotoCavaAbertaAmpliada by remember { mutableStateOf(false) }
    var mostrarFotoConclusaoAmpliada by remember { mutableStateOf(false) }

    var fotoEmCaptura by remember { mutableStateOf("ANTES") }

    val regexDecimal = Regex("^\\d*([.,]\\d*)?$")
    val areasCava = remember { mutableStateListOf<AreaCavaUi>() }

    val preencherEnderecoPorCoordenada: (Double, Double) -> Unit = { latitude, longitude ->
        try {
            val geocoder = Geocoder(context, Locale("pt", "BR"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(
                    latitude,
                    longitude,
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

                        override fun onError(errorMessage: String?) {
                            android.util.Log.d("ServicoForm", "Erro no Geocoder: $errorMessage")
                        }
                    }
                )
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    val addresses = withContext(Dispatchers.IO) {
                        try {
                            geocoder.getFromLocation(latitude, longitude, 1)
                        } catch (e: Exception) {
                            android.util.Log.d("ServicoForm", "Exceção no Geocoder: ${e.message}")
                            null
                        }
                    }

                    val address = addresses?.firstOrNull()
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
            }
        } catch (e: Exception) {
            android.util.Log.d("ServicoForm", "Erro geral ao buscar endereço: ${e.message}")
        }
    }

    fun montarServicoEntity(id: Long): ServicoEntity {
        return ServicoEntity(
            id = id,
            diarioId = diarioId,
            tipo = "Tapa buraco",
            ordemServico = ordemServico.toIntOrNull() ?: 0,
            numeroProtocolo = numeroProtocolo,
            endereco = enderecoServico,
            comprimento = areasCava.lastOrNull()?.comprimento ?: 0.0,
            largura = areasCava.lastOrNull()?.largura ?: 0.0,
            altura = (areasCava.lastOrNull()?.espessuraCm ?: 0.0) / 100.0,
            inicio = null,
            fim = null,
            latitude = latitudeAtual,
            longitude = longitudeAtual,
            nomeRua = enderecoServico.ifBlank { null },
            fotoUri = fotoAntesUri?.toString(),
            horarioFotoAntes = horarioFotoAntes,
            fotoCavaAbertaUri = fotoCavaAbertaUri?.toString(),
            fotoEspessuraUri = fotoEspessuraUri?.toString(),
            fotoConclusaoUri = fotoConclusaoUri?.toString(),
            horarioFotoConclusao = horarioFotoConclusao,
            sincronizado = false,
            aberturaCava = aberturaCava,
            limpezaEntulho = limpezaEntulho,
            pinturaLigacao = pinturaLigacao,
            equipamentoCompactacaoUsado = equipamentoCompactacaoSelecionado
        )
    }

    fun montarAreasEntity(servicoId: Long): List<ServicoAreaEntity> {
        return areasCava.mapIndexed { index, area ->
            ServicoAreaEntity(
                id = 0,
                servicoId = servicoId,
                ordem = index + 1,
                comprimento = area.comprimento,
                largura = area.largura,
                espessuraCm = area.espessuraCm
            )
        }
    }

    suspend fun salvarServicoCompletoTela(): Long {
        val entidade = montarServicoEntity(servicoIdAtual)
        val areas = montarAreasEntity(if (servicoIdAtual == 0L) 0L else servicoIdAtual)
        val idSalvo = viewModel.salvarServicoCompleto(entidade, areas)
        servicoIdAtual = idSalvo
        return idSalvo
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            versaoPreviewFotos++

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            latitudeAtual = location.latitude
                            longitudeAtual = location.longitude
                            preencherEnderecoPorCoordenada(location.latitude, location.longitude)
                        } else {
                            fusedLocationClient.getCurrentLocation(
                                Priority.PRIORITY_HIGH_ACCURACY,
                                null
                            ).addOnSuccessListener { currentLocation ->
                                if (currentLocation != null) {
                                    latitudeAtual = currentLocation.latitude
                                    longitudeAtual = currentLocation.longitude
                                    preencherEnderecoPorCoordenada(
                                        currentLocation.latitude,
                                        currentLocation.longitude
                                    )
                                }
                            }
                        }
                    }
            }

            val horarioAtual =
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())

            if (fotoEmCaptura == "ANTES") {
                horarioFotoAntes = horarioAtual
            }

            if (fotoEmCaptura == "CONCLUSAO") {
                horarioFotoConclusao = horarioAtual
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            when (fotoEmCaptura) {
                "ANTES" -> fotoAntesUri?.let { cameraLauncher.launch(it) }
                "CAVA_ABERTA" -> fotoCavaAbertaUri?.let { cameraLauncher.launch(it) }
                "CAVA_ESPESSURA" -> fotoEspessuraUri?.let { cameraLauncher.launch(it) }
                "CONCLUSAO" -> fotoConclusaoUri?.let { cameraLauncher.launch(it) }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val novaUri = criarUriParaFoto(context)

            when (fotoEmCaptura) {
                "ANTES" -> fotoAntesUri = novaUri
                "CAVA_ABERTA" -> fotoCavaAbertaUri = novaUri
                "CAVA_ESPESSURA" -> fotoEspessuraUri = novaUri
                "CONCLUSAO" -> fotoConclusaoUri = novaUri
            }

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

    LaunchedEffect(servicoId) {
        if (servicoId != 0L) {
            val servicoComAreas = viewModel.buscarServicoComAreasPorId(servicoId)
            val servico = servicoComAreas?.servico

            if (servico != null) {
                servicoIdAtual = servico.id
                ordemServico = servico.ordemServico.toString().takeIf { it != "0" }.orEmpty()
                numeroProtocolo = servico.numeroProtocolo
                enderecoServico = servico.endereco

                fotoAntesUri = servico.fotoUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                horarioFotoAntes = servico.horarioFotoAntes
                fotoCavaAbertaUri =
                    servico.fotoCavaAbertaUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                fotoEspessuraUri =
                    servico.fotoEspessuraUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                fotoConclusaoUri =
                    servico.fotoConclusaoUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                horarioFotoConclusao = servico.horarioFotoConclusao

                latitudeAtual = servico.latitude
                longitudeAtual = servico.longitude

                aberturaCava = servico.aberturaCava
                limpezaEntulho = servico.limpezaEntulho
                pinturaLigacao = servico.pinturaLigacao
                equipamentoCompactacaoSelecionado = servico.equipamentoCompactacaoUsado

                areasCava.clear()
                servicoComAreas.areas
                    .sortedBy { it.ordem }
                    .forEach { area ->
                        areasCava.add(
                            AreaCavaUi(
                                numero = area.ordem,
                                comprimento = area.comprimento,
                                largura = area.largura,
                                espessuraCm = area.espessuraCm
                            )
                        )
                    }

                exibindoFormularioArea = areasCava.isEmpty()
            }
        } else {
            val servicosExistentes = viewModel.buscarDiarioCompleto(diarioId)?.servicos.orEmpty()
            val proximo = (servicosExistentes.maxOfOrNull { it.ordemServico } ?: 0) + 1

            numeroProtocolo = proximo.toString()
            ordemServico = proximo.toString()
        }
    }

    LaunchedEffect(equipamentosCompactacao) {
        if (equipamentosCompactacao.size == 1 && equipamentoCompactacaoSelecionado.isBlank()) {
            equipamentoCompactacaoSelecionado = equipamentosCompactacao.first()
        } else if (equipamentosCompactacao.isEmpty()) {
            equipamentoCompactacaoSelecionado = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            modoSomenteLeitura -> "Consulta do serviço"
                            servicoIdAtual == 0L -> "Cadastro de serviço"
                            else -> "Editar serviço"
                        }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            Text(
                text = "Foto antes",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    fotoEmCaptura = "ANTES"

                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val novaUri = criarUriParaFoto(context)
                        fotoAntesUri = novaUri

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
                enabled = !modoSomenteLeitura,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (fotoAntesUri == null) "Capturar foto antes" else "Trocar foto antes")
            }

            if (fotoAntesUri != null) {
                Spacer(modifier = Modifier.height(12.dp))

                AsyncImage(
                    model = fotoAntesUri?.let { "${it}#${versaoPreviewFotos}" },
                    contentDescription = "Foto antes do serviço",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable { mostrarFotoAntesAmpliada = true },
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = numeroProtocolo,
                onValueChange = { numeroProtocolo = it },
                label = { Text("Protocolo") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !modoSomenteLeitura
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ordemServico,
                onValueChange = { ordemServico = it.filter(Char::isDigit) },
                label = { Text("Ordem de serviço") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !modoSomenteLeitura
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = enderecoServico,
                onValueChange = { enderecoServico = it },
                label = { Text("Endereço") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !modoSomenteLeitura
            )

            if (latitudeAtual != null && longitudeAtual != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Coord.: $latitudeAtual, $longitudeAtual",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Abertura da cava",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { menuAberturaExpandido = true },
                enabled = !modoSomenteLeitura,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (aberturaCava.isBlank()) "Selecionar" else aberturaCava)
            }

            DropdownMenu(
                expanded = menuAberturaExpandido && !modoSomenteLeitura,
                onDismissRequest = { menuAberturaExpandido = false }
            ) {
                LISTA_ABERTURA_CAVA.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            aberturaCava = item
                            menuAberturaExpandido = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Limpeza do entulho",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { menuLimpezaExpandido = true },
                enabled = !modoSomenteLeitura,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (limpezaEntulho.isBlank()) "Selecionar" else limpezaEntulho)
            }

            DropdownMenu(
                expanded = menuLimpezaExpandido && !modoSomenteLeitura,
                onDismissRequest = { menuLimpezaExpandido = false }
            ) {
                LISTA_LIMPEZA_ENTULHO.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            limpezaEntulho = item
                            menuLimpezaExpandido = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Foto da cava aberta",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    fotoEmCaptura = "CAVA_ABERTA"

                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val novaUri = criarUriParaFoto(context)
                        fotoCavaAbertaUri = novaUri

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
                enabled = !modoSomenteLeitura,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (fotoCavaAbertaUri == null) {
                        "Capturar foto da cava aberta"
                    } else {
                        "Trocar foto da cava aberta"
                    }
                )
            }

            if (fotoCavaAbertaUri != null) {
                Spacer(modifier = Modifier.height(12.dp))

                AsyncImage(
                    model = fotoCavaAbertaUri?.let { "${it}#${versaoPreviewFotos}" },
                    contentDescription = "Foto da cava aberta",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable { mostrarFotoCavaAbertaAmpliada = true },
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Dimensões da cava",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (exibindoFormularioArea) {
                Text(
                    text = "A${areasCava.size + 1}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = comprimento,
                    onValueChange = {
                        if (it.matches(regexDecimal)) comprimento = normalizarDecimal(it)
                    },
                    label = { Text("Comprimento (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            comprimento = formatarCampoDecimal(comprimento)
                            larguraFocusRequester.requestFocus()
                        }
                    ),
                    singleLine = true,
                    enabled = !modoSomenteLeitura
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = largura,
                    onValueChange = {
                        if (it.matches(regexDecimal)) largura = normalizarDecimal(it)
                    },
                    label = { Text("Largura (m)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(larguraFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            largura = formatarCampoDecimal(largura)
                            espessuraFocusRequester.requestFocus()
                        }
                    ),
                    singleLine = true,
                    enabled = !modoSomenteLeitura
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = espessura,
                    onValueChange = {
                        if (it.matches(regexDecimal)) espessura = normalizarDecimal(it)
                    },
                    label = { Text("Espessura (cm)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(espessuraFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!modoSomenteLeitura) {
                                comprimento = formatarCampoDecimal(comprimento)
                                largura = formatarCampoDecimal(largura)
                                espessura = formatarCampoDecimal(espessura)

                                val comp = comprimento.replace(",", ".").toDoubleOrNull()
                                val larg = largura.replace(",", ".").toDoubleOrNull()
                                val esp = espessura.replace(",", ".").toDoubleOrNull()

                                if (comp != null && larg != null && esp != null) {
                                    areasCava.add(
                                        AreaCavaUi(
                                            numero = areasCava.size + 1,
                                            comprimento = comp,
                                            largura = larg,
                                            espessuraCm = esp
                                        )
                                    )

                                    comprimento = ""
                                    largura = ""
                                    espessura = "5,00"
                                    exibindoFormularioArea = false
                                }

                                keyboardController?.hide()
                            }
                        }
                    ),
                    singleLine = true,
                    enabled = !modoSomenteLeitura
                )
            }

            val areaTotalAcumulada = areasCava.sumOf { it.area }

            if (areasCava.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "ÁREA TOTAL",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = formatarDecimalTruncado(areaTotalAcumulada),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            areasCava.forEach { area ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Área ${area.numero}",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "C x L x E: ${formatarDecimalTruncado(area.comprimento)} x ${formatarDecimalTruncado(area.largura)} x ${formatarDecimalTruncado(area.espessuraCm)} cm",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Área total: ${formatarDecimalTruncado(area.area)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (!exibindoFormularioArea) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        exibindoFormularioArea = true
                        comprimento = ""
                        largura = ""
                        espessura = "5,00"
                    },
                    enabled = !modoSomenteLeitura,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Foto da espessura (opcional)",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    fotoEmCaptura = "CAVA_ESPESSURA"

                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val novaUri = criarUriParaFoto(context)
                        fotoEspessuraUri = novaUri

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
                enabled = !modoSomenteLeitura,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (fotoEspessuraUri == null) {
                        "Capturar foto da espessura"
                    } else {
                        "Trocar foto da espessura"
                    }
                )
            }

            if (fotoEspessuraUri != null) {
                Spacer(modifier = Modifier.height(12.dp))

                AsyncImage(
                    model = fotoEspessuraUri?.let { "${it}#${versaoPreviewFotos}" },
                    contentDescription = "Foto da espessura",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable { mostrarFotoEspessuraAmpliada = true },
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !modoSomenteLeitura) {
                        pinturaLigacao = !pinturaLigacao
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = pinturaLigacao,
                    onCheckedChange = { checked ->
                        if (!modoSomenteLeitura) {
                            pinturaLigacao = checked
                        }
                    },
                    enabled = !modoSomenteLeitura
                )

                Text(
                    text = "Pintura de Ligação",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (equipamentosCompactacao.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Equipamento de compactação usado",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        if (equipamentosCompactacao.size > 1) {
                            menuEquipamentoCompactacaoExpandido = true
                        }
                    },
                    enabled = !modoSomenteLeitura,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            equipamentoCompactacaoSelecionado.isNotBlank() -> equipamentoCompactacaoSelecionado
                            equipamentosCompactacao.size == 1 -> equipamentosCompactacao.first()
                            else -> "Selecionar equipamento"
                        }
                    )
                }

                if (equipamentosCompactacao.size > 1) {
                    DropdownMenu(
                        expanded = menuEquipamentoCompactacaoExpandido && !modoSomenteLeitura,
                        onDismissRequest = { menuEquipamentoCompactacaoExpandido = false }
                    ) {
                        equipamentosCompactacao.forEach { equipamento ->
                            DropdownMenuItem(
                                text = { Text(equipamento) },
                                onClick = {
                                    equipamentoCompactacaoSelecionado = equipamento
                                    menuEquipamentoCompactacaoExpandido = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Foto do serviço finalizado",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    fotoEmCaptura = "CONCLUSAO"

                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val novaUri = criarUriParaFoto(context)
                        fotoConclusaoUri = novaUri

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
                enabled = !modoSomenteLeitura,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (fotoConclusaoUri == null) {
                        "Capturar foto do serviço finalizado"
                    } else {
                        "Trocar foto do serviço finalizado"
                    }
                )
            }

            if (fotoConclusaoUri != null) {
                Spacer(modifier = Modifier.height(12.dp))

                AsyncImage(
                    model = fotoConclusaoUri?.let { "${it}#${versaoPreviewFotos}" },
                    contentDescription = "Foto do serviço finalizado",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable { mostrarFotoConclusaoAmpliada = true },
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (modoSomenteLeitura) {
                Text(
                    text = "Serviço em modo consulta. Alterações bloqueadas após o encerramento dos serviços.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    scope.launch {
                        salvarServicoCompletoTela()
                        onSalvarConcluir()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = numeroProtocolo.isNotBlank() &&
                        enderecoServico.isNotBlank() &&
                        !modoSomenteLeitura
            ) {
                Text("Salvar serviço")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onVoltar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Voltar")
            }
        }

        if (mostrarFotoAntesAmpliada && fotoAntesUri != null) {
            Dialog(
                onDismissRequest = { mostrarFotoAntesAmpliada = false }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        AsyncImage(
                            model = fotoAntesUri,
                            contentDescription = "Foto antes ampliada",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { mostrarFotoAntesAmpliada = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Fechar")
                        }
                    }
                }
            }
        }

        if (mostrarFotoCavaAbertaAmpliada && fotoCavaAbertaUri != null) {
            Dialog(
                onDismissRequest = { mostrarFotoCavaAbertaAmpliada = false }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        AsyncImage(
                            model = fotoCavaAbertaUri,
                            contentDescription = "Foto da cava aberta ampliada",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { mostrarFotoCavaAbertaAmpliada = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Fechar")
                        }
                    }
                }
            }
        }

        if (mostrarFotoEspessuraAmpliada && fotoEspessuraUri != null) {
            Dialog(
                onDismissRequest = { mostrarFotoEspessuraAmpliada = false }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        AsyncImage(
                            model = fotoEspessuraUri,
                            contentDescription = "Foto da espessura ampliada",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { mostrarFotoEspessuraAmpliada = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Fechar")
                        }
                    }
                }
            }
        }

        if (mostrarFotoConclusaoAmpliada && fotoConclusaoUri != null) {
            Dialog(
                onDismissRequest = { mostrarFotoConclusaoAmpliada = false }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        AsyncImage(
                            model = fotoConclusaoUri,
                            contentDescription = "Foto do serviço finalizado ampliada",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { mostrarFotoConclusaoAmpliada = false },
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