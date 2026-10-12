package com.geosapiens.saudeemdia.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geosapiens.saudeemdia.R
import com.geosapiens.saudeemdia.model.ExamRequest
import com.geosapiens.saudeemdia.ui.viewmodel.ExamRequestViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ExamRequestList(
    viewModel: ExamRequestViewModel = viewModel(),
) {
    // Load exam requests when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadRequests()
    }

    if (viewModel.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (viewModel.requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhuma solicitação de exame encontrada.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(viewModel.requests) { request ->
            ExamRequestCard(
                request = request,
                onDeleteClick = { id ->
                    viewModel.deleteExamRequest(id)
                }
            )
        }
    }
}

@Composable
fun ExamRequestCard(
    request: ExamRequest,
    onDeleteClick: (Long) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Excluir Solicitação") },
            text = { Text("Tem certeza que deseja excluir a solicitação para \"${request.examName}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        request.id?.let { onDeleteClick(it) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Main info (always visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatIsoDate(request.requestDate),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = request.examName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!request.status.isNullOrBlank()) {
                        Text(
                            text = "Status: ${request.status}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Text(
                    text = getSpecialtyEmoji(request.speciality),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            // Expanded info
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                InfoRow(label = "Tipo de Exame", value = request.examType)
                InfoRow(label = "Especialidade", value = request.speciality)
                InfoRow(label = "Médico Solicitante", value = request.requestingDoctor)
                InfoRow(label = "Instituição", value = request.institution)
                InfoRow(label = "Prioridade", value = request.priority)
                InfoRow(label = "Status", value = request.status)
                InfoRow(label = "Resumo", value = request.summary)

                if (!request.documentUrl.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Button(
                            onClick = { uriHandler.openUri(request.documentUrl) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ver Documento")
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.delete_24px),
                            contentDescription = "Excluir",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir Solicitação", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun getSpecialtyEmoji(specialty: String?): String {
    return when (specialty) {
        "Cardiologia" -> "❤️"
        "Clínica Médica" -> "🩺"
        "Dermatologia" -> "✨"
        "Endocrinologia" -> "⚖️"
        "Gastroenterologia" -> "🍱"
        "Ginecologia" -> "🚺"
        "Hematologia" -> "🩸"
        "Neurologia" -> "🧠"
        "Oftalmologia" -> "👁️"
        "Ortopedia" -> "🦴"
        "Otorrinolaringologia" -> "👂"
        "Pneumologia" -> "🫁"
        "Psiquiatria" -> "🧩"
        "Reumatologia" -> "🦵"
        "Urologia" -> "🚻"
        "Oncologia" -> "🎗️"
        "Nefrologia" -> "🧬"
        "Infectologia" -> "🦠"
        "Cirurgia Geral" -> "🔪"
        else -> "📋"
    }
}

private fun formatIsoDate(isoDate: String): String {
    return try {
        val datePart = isoDate.split("T")[0]
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = parser.parse(datePart)
        if (date != null) formatter.format(date) else isoDate
    } catch (_: Exception) {
        isoDate
    }
}
