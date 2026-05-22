package com.example.diarioobras.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.diarioobras.data.StatusEtapa

@Composable
internal fun EtapaCard(
    numero: Int,
    titulo: String,
    status: StatusEtapa,
    expandida: Boolean,
    onClick: () -> Unit,
    conteudo: (@Composable () -> Unit)? = null
) {
    val corFundo = when (status) {
        StatusEtapa.CONCLUIDA -> Color(0xFFDFF5E1)
        StatusEtapa.EM_ANDAMENTO -> Color(0xFFFFF4CC)
        StatusEtapa.DISPONIVEL -> Color(0xFFEAF2FF)
        StatusEtapa.BLOQUEADA -> Color(0xFFF2F2F2)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(enabled = status != StatusEtapa.BLOQUEADA) { onClick() },
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
