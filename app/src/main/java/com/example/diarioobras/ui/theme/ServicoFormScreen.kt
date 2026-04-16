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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

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
    var espessura by remember { mutableStateOf("") }

    val larguraFocusRequester = remember { FocusRequester() }
    val espessuraFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var fotoAntesUri by remember { mutableStateOf<Uri?>(null) }
    var fotoCavaAbertaUri by remember { mutableStateOf<Uri?>(null) }
    var fotoConclusaoUri by remember { mutableStateOf<Uri?>(null) }
    var versaoPreviewFotos by remember { mutableStateOf(0) }

    var latitudeAtual by remember { mutableStateOf<Double?>(null) }
    var longitudeAtual by remember { mutableStateOf<Double?>(null) }

    var mostrarFotoAntesAmpliada by remember { mutableStateOf(false) }
    var mostrarFotoCavaAbertaAmpliada by remember { mutableStateOf(false) }

    var fotoEmCaptura by remember { mutableStateOf("ANTES") }

    val regexDecimal = Regex("^\\d*([.,]\\d*)?$")

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
            ordemServico = ordemServico.toIntOrNull() ?: 0,
            numeroProtocolo = numeroProtocolo,
            endereco = enderecoServico,
            comprimento = comprimento.toDoubleOrNull() ?: 0.0,
            largura = largura.toDoubleOrNull() ?: 0.0,
            altura = (espessura.toDoubleOrNull() ?: 0.0) / 100.0,
            inicio = null,
            fim = null,
            latitude = latitudeAtual,
            longitude = longitudeAtual,
            nomeRua = enderecoServico.ifBlank { null },
            fotoUri = fotoAntesUri?.toString(),

            // Se o seu ServicoEntity já tiver esse campo, ele será salvo.
            // Se ainda não tiver, apague esta linha por enquanto para compilar.
            fotoCavaAbertaUri = fotoCavaAbertaUri?.toString(),

            fotoConclusaoUri = fotoConclusaoUri?.toString(),
            aberturaCava = aberturaCava,
            limpezaEntulho = limpezaEntulho
        )
    }

    suspend fun garantirServicoSalvo(): Long {
        val entidade = montarServicoEntity(servicoIdAtual)

        return if (servicoIdAtual == 0L) {
            val novoId = viewModel.salvarServicoRetornandoId(entidade.copy(id = 0L))
            servicoIdAtual = novoId
            novoId
        } else {
            viewModel.atualizarServicoSuspend(entidade.copy(id = servicoIdAtual))
            servicoIdAtual
        }
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
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            when (fotoEmCaptura) {
                "ANTES" -> fotoAntesUri?.let { cameraLauncher.launch(it) }
                "CAVA_ABERTA" -> fotoCavaAbertaUri?.let { cameraLauncher.launch(it) }
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
            val servico = viewModel.buscarServicoPorId(servicoId)
            if (servico != null) {
                servicoIdAtual = servico.id
                ordemServico = servico.ordemServico.toString().takeIf { it != "0" }.orEmpty()
                numeroProtocolo = servico.numeroProtocolo
                enderecoServico = servico.endereco

                comprimento = if (servico.comprimento != 0.0) servico.comprimento.toString() else ""
                largura = if (servico.largura != 0.0) servico.largura.toString() else ""
                espessura = if (servico.altura != 0.0) {
                    val valorCm = servico.altura * 100.0
                    if (valorCm % 1.0 == 0.0) valorCm.toInt().toString() else valorCm.toString()
                } else ""

                fotoAntesUri = servico.fotoUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }

                // Se o seu ServicoEntity ainda não tiver esse campo, comente este bloco.
                try {
                    val campo = servico::class.java.getDeclaredField("fotoCavaAbertaUri")
                    campo.isAccessible = true
                    val valor = campo.get(servico) as? String
                    fotoCavaAbertaUri = valor?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                } catch (_: Exception) {
                    // campo ainda não existe na entidade, segue sem quebrar a tela
                }

                fotoConclusaoUri = servico.fotoConclusaoUri
                    ?.takeIf { it.isNotBlank() }
                    ?.let { Uri.parse(it) }

                latitudeAtual = servico.latitude
                longitudeAtual = servico.longitude

                // Se esses campos já existirem na entidade, serão carregados.
                try {
                    val campoAbertura = servico::class.java.getDeclaredField("aberturaCava")
                    campoAbertura.isAccessible = true
                    aberturaCava = campoAbertura.get(servico) as? String ?: ""
                } catch (_: Exception) {
                    aberturaCava = ""
                }

                try {
                    val campoLimpeza = servico::class.java.getDeclaredField("limpezaEntulho")
                    campoLimpeza.isAccessible = true
                    limpezaEntulho = campoLimpeza.get(servico) as? String ?: ""
                } catch (_: Exception) {
                    limpezaEntulho = ""
                }
            }
        }
    }

    LaunchedEffect(servicoIdAtual, servicosDoDiario.size) {
        if (servicoIdAtual == 0L && numeroProtocolo.isBlank()) {
            val proximo = (servicosDoDiario.maxOfOrNull { it.ordemServico } ?: 0) + 1
            numeroProtocolo = proximo.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (servicoIdAtual == 0L) "Cadastro de serviço" else "Editar serviço")
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ordemServico,
                onValueChange = { ordemServico = it.filter(Char::isDigit) },
                label = { Text("Ordem de serviço") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = enderecoServico,
                onValueChange = { enderecoServico = it },
                label = { Text("Endereço") },
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (aberturaCava.isBlank()) "Selecionar" else aberturaCava)
            }

            DropdownMenu(
                expanded = menuAberturaExpandido,
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (limpezaEntulho.isBlank()) "Selecionar" else limpezaEntulho)
            }

            DropdownMenu(
                expanded = menuLimpezaExpandido,
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

            Text(
                text = "Dimensões da cava",
                style = MaterialTheme.typography.titleMedium
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
                        larguraFocusRequester.requestFocus()
                    }
                ),
                singleLine = true
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
                        espessuraFocusRequester.requestFocus()
                    }
                ),
                singleLine = true
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
                        keyboardController?.hide()
                    }
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        garantirServicoSalvo()
                        onSalvarConcluir()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = numeroProtocolo.isNotBlank() && enderecoServico.isNotBlank()
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
    }
}