package com.example.diarioobras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.diarioobras.data.SyncScheduler
import com.example.diarioobras.ui.DiarioScreen
import com.example.diarioobras.ui.MainViewModel
import com.example.diarioobras.ui.ObraDetalheScreen
import com.example.diarioobras.ui.ObraInfoScreen
import com.example.diarioobras.ui.ObrasScreen
import com.example.diarioobras.ui.theme.ServicoFormScreen
import com.example.diarioobras.ui.DiarioEtapasScreen

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
                onAbrirServico = { servicoId ->
                    navController.navigate("servico/$diarioId/$servicoId")
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
    }
}