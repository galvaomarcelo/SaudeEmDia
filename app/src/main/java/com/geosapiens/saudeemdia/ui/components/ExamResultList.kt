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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.geosapiens.saudeemdia.model.ExamResult
import com.geosapiens.saudeemdia.ui.viewmodel.ExamResultViewModel
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun ExamResultList(
    viewModel: ExamResultViewModel = viewModel(),
    onEditClick: (ExamResult) -> Unit = {}
) {
    // Load exams when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadExams()
    }

    if (viewModel.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (viewModel.exams.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum exame encontrado.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(viewModel.exams) { exam ->
            ExamCard(exam, onEditClick)
        }
    }
}

@Composable
fun ExamCard(exam: ExamResult, onEditClick: (ExamResult) -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        // Define uma linha nítida e fina no contorno
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            //containerColor = MaterialTheme.colorScheme.surfaceContainerLow
             //       containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f)
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
                        text = formatIsoDate(exam.examDate),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = exam.examName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = getSpecialtyEmoji(exam.specialty),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            // Expanded info
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                InfoRow(label = "Tipo de Exame", value = exam.examType)
                InfoRow(label = "Especialidade", value = exam.specialty)
                InfoRow(label = "Médico Solicitante", value = exam.requestingDoctor)
                InfoRow(label = "Responsável", value = exam.examiner)
                InfoRow(label = "Instituição", value = exam.institution)
                InfoRow(label = "Resumo", value = exam.summary)

                if (exam.patientCode != null || exam.examCode != null || exam.repositoryHyperlink != null) {
                    Text(
                        text = "Outros detalhes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    SmallInfoRow(label = "Cód. Paciente:", value = exam.patientCode)
                    SmallInfoRow(label = "Cód. Exame:", value = exam.examCode)
                    
                    if (!exam.repositoryHyperlink.isNullOrBlank()) {
                        Text(
                            text = "Link do Repositório",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.clickable { uriHandler.openUri(exam.repositoryHyperlink) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!exam.examCoverUrl.isNullOrBlank()) {
                        Button(
                            onClick = { uriHandler.openUri(exam.examCoverUrl) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ver Capa")
                        }
                    }
                    if (!exam.documentUrl.isNullOrBlank()) {
                        Button(
                            onClick = { uriHandler.openUri(exam.documentUrl) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Documento")
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onEditClick(exam)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.edit_square_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar Exame", style = MaterialTheme.typography.labelSmall)
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

@Composable
private fun SmallInfoRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Text(
                text = "$label ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
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
