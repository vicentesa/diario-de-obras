package com.example.diarioobras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.diarioobras.data.DesvioItemEntity
import com.example.diarioobras.data.SyncScheduler
import com.example.diarioobras.ui.DiarioEtapasScreen
import com.example.diarioobras.ui.MainViewModel
import com.example.diarioobras.ui.ObraDetalheScreen
import com.example.diarioobras.ui.ObraInfoScreen
import com.example.diarioobras.ui.ObrasScreen
import com.example.diarioobras.ui.theme.ServicoFormScreen
import androidx.compose.material3.ExperimentalMaterial3Api


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SyncScheduler.agendar(this)
        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                DiarioObrasApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarObservacaoDesvioScreen(
    desvioId: Long,
    viewModel: MainViewModel,
    onVoltar: () -> Unit
) {
    var carregando by remember { mutableStateOf(true) }
    var desvio by remember { mutableStateOf<DesvioItemEntity?>(null) }
    var textoObservacao by remember { mutableStateOf("") }

    LaunchedEffect(desvioId) {
        val item = viewModel.buscarDesvioPorId(desvioId)
        desvio = item
        textoObservacao = item?.observacao.orEmpty()
        carregando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar observação") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (carregando) {
                Text("Carregando...")
            } else if (desvio == null) {
                Text("Desvio não encontrado.")
            } else {
                Text(
                    text = "${desvio!!.codigo} - ${desvio!!.descricao}",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = textoObservacao,
                    onValueChange = { textoObservacao = it },
                    label = { Text("Observação") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.atualizarObservacaoDesvio(
                            id = desvio!!.id,
                            texto = textoObservacao
                        )
                        onVoltar()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onVoltar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
fun DiarioObrasApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "obras"
    ) {

        composable("obras") {
            ObrasScreen(
                viewModel = viewModel,
                onAbrirObra = { obraId ->
                    navController.navigate("obra_info/$obraId")
                }
            )
        }

        composable(
            route = "obra_info/{obraId}",
            arguments = listOf(navArgument("obraId") { type = NavType.LongType })
        ) { backStackEntry ->
            val obraId = backStackEntry.arguments?.getLong("obraId") ?: 0L

            ObraInfoScreen(
                obraId = obraId,
                viewModel = viewModel,
                onAbrirDiarios = {
                    navController.navigate("obra/$obraId")
                }
            )
        }

        composable(
            route = "obra/{obraId}",
            arguments = listOf(navArgument("obraId") { type = NavType.LongType })
        ) { backStackEntry ->
            val obraId = backStackEntry.arguments?.getLong("obraId") ?: 0L

            ObraDetalheScreen(
                obraId = obraId,
                viewModel = viewModel,
                onAbrirDiario = { diarioId ->
                    navController.navigate("diario/$diarioId/0")
                }
            )
        }

        composable(
            route = "diario/{diarioId}/{abaInicial}",
            arguments = listOf(
                navArgument("diarioId") { type = NavType.LongType },
                navArgument("abaInicial") { type = NavType.IntType }
            )
        ) { backStackEntry ->

            val diarioId = backStackEntry.arguments?.getLong("diarioId") ?: 0L
            val abaInicial = backStackEntry.arguments?.getInt("abaInicial") ?: 0

            DiarioEtapasScreen(
                diarioId = diarioId,
                viewModel = viewModel,
                navController = navController,
                abaInicial = abaInicial,
                onAbrirServico = { servicoId ->
                    navController.navigate("servico/$diarioId/$servicoId")
                },
                onAbrirDiario = { novoDiarioId ->
                    navController.navigate("diario/$novoDiarioId/0") {
                        popUpTo("diario/$diarioId/0") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "servico/{diarioId}/{servicoId}",
            arguments = listOf(
                navArgument("diarioId") { type = NavType.LongType },
                navArgument("servicoId") { type = NavType.LongType }
            )
        ) { backStackEntry ->

            val diarioId = backStackEntry.arguments?.getLong("diarioId") ?: 0L
            val servicoId = backStackEntry.arguments?.getLong("servicoId") ?: 0L

            ServicoFormScreen(
                diarioId = diarioId,
                servicoId = servicoId,
                viewModel = viewModel,
                onVoltar = {
                    navController.popBackStack()
                },
                onSalvarConcluir = {
                    navController.navigate("diario/$diarioId/3") {
                        popUpTo("diario/$diarioId/0") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "editar_observacao_desvio/{desvioId}",
            arguments = listOf(
                navArgument("desvioId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val desvioId = backStackEntry.arguments?.getLong("desvioId") ?: return@composable

            EditarObservacaoDesvioScreen(
                desvioId = desvioId,
                viewModel = viewModel,
                onVoltar = { navController.popBackStack() }
            )
        }
    }
}