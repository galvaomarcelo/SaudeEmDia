package com.geosapiens.saudeemdia.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.geosapiens.saudeemdia.R
import com.geosapiens.saudeemdia.ui.components.FilePickerField
import com.geosapiens.saudeemdia.ui.viewmodel.PrescriptionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionForm(
    snackbarHostState: SnackbarHostState,
    viewModel: PrescriptionViewModel
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    if (viewModel.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.selectedPrescriptionDate
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.selectedPrescriptionDate = datePickerState.selectedDateMillis
                    viewModel.showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Registrar Receita Médica",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 1. Prescription Document
        FilePickerField(
            label = "Documento da Receita",
            fileName = viewModel.documentFileName,
            fileUri = viewModel.documentFileURI,
            acceptedMimeTypes = arrayOf("image/*", "application/pdf"),
            onFileSelected = { uri, name ->
                viewModel.documentFileURI = uri
                viewModel.documentFileName = name
            },
            onClear = {
                viewModel.documentFileURI = null
                viewModel.documentFileName = ""
            }
        )

        // ---------------------------------------------------
        // IA BUTTON (só aparece se tiver arquivo selecionado)
        // ---------------------------------------------------
        if (viewModel.documentFileURI != null && viewModel.documentFileName.isNotBlank()) {
            Button(
                onClick = {
                    viewModel.processFileAndAutoFillPrescription(
                        context = context,
                        onSuccess = { message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                        onError = { error ->
                            scope.launch {
                                snackbarHostState.showSnackbar(error)
                            }
                        }
                    )
                },
                enabled = !viewModel.isProcessingAI,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.isProcessingAI) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.wand_stars_24px),
                        contentDescription = "AI fill"
                    )
                    Text("Completar com IA", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 1.dp)
        Text("* Campos Obrigatórios", style = MaterialTheme.typography.labelSmall)

        OutlinedTextField(
            value = viewModel.title,
            onValueChange = { viewModel.title = it },
            label = { Text("Título *") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 2. Description
        OutlinedTextField(
            value = viewModel.description,
            onValueChange = { viewModel.description = it },
            label = { Text("Descrição / Indicação *") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 3. Date Field
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = viewModel.formattedDate.value,
                onValueChange = {},
                readOnly = true,
                label = { Text("Data *") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_drop_down_24px),
                        contentDescription = "Data da receita"
                    )
                }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { viewModel.showDatePicker = true }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // CAMPOS OPCIONAIS
        HorizontalDivider(thickness = 1.dp)
        Text("Campos Opcionais", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Doctor Name
        OutlinedTextField(
            value = viewModel.doctorName,
            onValueChange = { viewModel.doctorName = it },
            label = { Text("Nome do Médico") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Doctor CRM
        OutlinedTextField(
            value = viewModel.doctorCrm,
            onValueChange = { viewModel.doctorCrm = it },
            label = { Text("CRM do Médico") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Institution
        OutlinedTextField(
            value = viewModel.institution,
            onValueChange = { viewModel.institution = it },
            label = { Text("Instituição / Clínica / Hospital") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Save Button
        Button(
            onClick = {
                viewModel.savePrescription(
                    context = context,
                    onSuccess = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    },
                    onError = { error ->
                        scope.launch { snackbarHostState.showSnackbar(error) }
                    }
                )
            },
            enabled = viewModel.isFormValid && !viewModel.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (viewModel.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text("Salvar Receita")
            }
        }
    }
}
