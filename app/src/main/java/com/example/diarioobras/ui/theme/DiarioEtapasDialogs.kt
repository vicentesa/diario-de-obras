package com.example.diarioobras.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.diarioobras.data.ServicoEntity

@Composable
internal fun EnviandoFirebaseDialog(show: Boolean) {
    if (!show) return
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text("Enviando ao Firebase...") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }
    )
}

@Composable
internal fun ExcluirServicoDialog(
    servico: ServicoEntity?,
    onConfirm: (ServicoEntity) -> Unit,
    onDismiss: () -> Unit
) {
    if (servico == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir serviço") },
        text = { Text("Tem certeza que deseja apagar este serviço?") },
        confirmButton = {
            TextButton(onClick = { onConfirm(servico) }) {
                Text("Apagar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
internal fun EncerrarServicosDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encerrar serviços") },
        text = {
            Text(
                "Após o fechamento dos serviços, não serão permitidos novos lançamentos " +
                "nem alterações nos serviços já registrados. Será permitida apenas a consulta. " +
                "Deseja continuar?"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Encerrar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
internal fun TicketAmpliadoDialog(
    show: Boolean,
    uri: Uri,
    versao: Int,
    onDismiss: () -> Unit
) {
    if (!show) return
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                AsyncImage(
                    model = "$uri#$versao",
                    contentDescription = "Ticket ampliado",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}
