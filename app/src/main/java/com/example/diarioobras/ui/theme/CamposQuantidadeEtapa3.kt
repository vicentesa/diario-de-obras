package com.example.diarioobras.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun CamposQuantidadeEtapa3(
    veiculos: List<String>,
    quantidades: SnapshotStateList<String>,
    bloqueado: Boolean = false
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
                enabled = !bloqueado,
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
