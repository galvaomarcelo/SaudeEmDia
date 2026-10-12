package com.geosapiens.saudeemdia.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.geosapiens.saudeemdia.R
import com.geosapiens.saudeemdia.ui.components.FilePickerField
import com.geosapiens.saudeemdia.ui.viewmodel.ExamResultViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultForm(
    snackbarHostState: SnackbarHostState,
    viewModel: ExamResultViewModel
) {
    val scope = rememberCoroutineScope()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    if (viewModel.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.selectedExamDate
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.selectedExamDate = datePickerState.selectedDateMillis
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (viewModel.editingExamId == null) "Registrar Resultado de Exame" else "Editar Resultado de Exame",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            if (viewModel.editingExamId != null) {
                OutlinedButton(
                    onClick = { viewModel.resetForm() }
                ) {
                    Text("Cancelar")
                }
            }
        }

        // 1. Exam Cover Image
        FilePickerField(
            label = "Imagem da Capa",
            fileName = viewModel.examCoverFileName,
            fileUri = viewModel.examCoverFileURI,
            acceptedMimeTypes = arrayOf("image/*"),
            onFileSelected = { uri, name ->
                viewModel.examCoverFileURI = uri
                viewModel.examCoverFileName = name
            },
            onClear = {
                viewModel.examCoverFileURI = null
                viewModel.examCoverFileName = ""
            }
        )
// ---------------------------------------------------
        // IA BUTTON (só aparece se tiver arquivo selecionado)
        // ---------------------------------------------------
        if (viewModel.examCoverFileURI != null && viewModel.examCoverFileName.isNotBlank()) {
            val context = LocalContext.current
            Button(
                onClick = {
                    viewModel.processFileAndAutoFillComprovante(context = context, onSuccess = { message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }, onError = { error ->
                        scope.launch {
                            snackbarHostState.showSnackbar(error)
                        }
                    })
                },
                enabled = !viewModel.isProcessingAI && viewModel.examCoverFileURI != null,
                modifier = Modifier.fillMaxWidth()) {
                if (viewModel.isProcessingAI) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSecondary)

                } else {
                    Icon(painter = painterResource(id = R.drawable.wand_stars_24px),
                        contentDescription = "AI fill")
                    Text("Completar com IA", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))


        HorizontalDivider(thickness = 1.dp)
        Text("* Campos Obrigatórios", style = MaterialTheme.typography.labelSmall)

        // 2. Exam Name
        OutlinedTextField(
            value = viewModel.examName,
            onValueChange = { viewModel.examName = it },
            label = { Text("Nome do Exame *") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 6. Date Field
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
                        contentDescription = "Data do exame"
                    )
                }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { viewModel.showDatePicker = true }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Exam Type Dropdown
        DropdownField(
            label = "Tipo de Exame *",
            selectedOption = viewModel.examType,
            options = viewModel.examTypeOptions,
            onOptionSelected = { viewModel.examType = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        //CAMPOS OPICIONAIS
        HorizontalDivider(thickness = 1.dp)
        Text("Campos Opicionais", style = MaterialTheme.typography.labelSmall)


        // 4. Specialty Dropdown
        DropdownField(
            label = "Especialidade",
            selectedOption = viewModel.specialty,
            options = viewModel.specialtyOptions,
            onOptionSelected = { viewModel.specialty = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Institution
        OutlinedTextField(
            value = viewModel.institution,
            onValueChange = { viewModel.institution = it },
            label = { Text("Instituição") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 5.1 Requesting Doctor
        OutlinedTextField(
            value = viewModel.requestingDoctor,
            onValueChange = { viewModel.requestingDoctor = it },
            label = { Text("Médico Solicitante") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.height(8.dp))

        // 5.2 Examiner
        OutlinedTextField(
            value = viewModel.examiner,
            onValueChange = { viewModel.examiner = it },
            label = { Text("Examinador") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 2.1 Exam Code
        OutlinedTextField(
            value = viewModel.examCode,
            onValueChange = { viewModel.examCode = it },
            label = { Text("Código do Exame") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))


        Spacer(modifier = Modifier.height(8.dp))

        // 5.3 Ammount
        OutlinedTextField(
            value = viewModel.amount,
            onValueChange = { 
                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                    viewModel.amount = it
                }
            },
            label = { Text("Quantidade de Exames") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 2.1 Patient Code
        OutlinedTextField(
            value = viewModel.patientCode,
            onValueChange = { viewModel.patientCode = it },
            label = { Text("Código do Paciente") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 11. Repository Hyperlink
        OutlinedTextField(
            value = viewModel.repositoryHyperlink,
            onValueChange = { viewModel.repositoryHyperlink = it },
            label = { Text("Link do Repositório (URL)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 7. Summary
        OutlinedTextField(
            value = viewModel.summary,
            onValueChange = { viewModel.summary = it },
            label = { Text("Resumo") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 8. Result
        OutlinedTextField(
            value = viewModel.result,
            onValueChange = { viewModel.result = it },
            label = { Text("Resultado") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 9. Conclusion
        OutlinedTextField(
            value = viewModel.conclusion,
            onValueChange = { viewModel.conclusion = it },
            label = { Text("Conclusão") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 10. Document URL (FilePicker max 4MB)
        FilePickerField(
            label = "Documento (PDF/Imagens - Máx 4MB)",
            fileName = viewModel.documentFileName,
            fileUri = viewModel.documentFileUri,
            acceptedMimeTypes = arrayOf("application/pdf", "image/*"),
            onFileSelected = { uri, name ->
                if (viewModel.validateFileSize(context, uri, 4)) {
                    viewModel.documentFileUri = uri
                    viewModel.documentFileName = name
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Arquivo muito grande! Máximo 4MB.")
                    }
                }
            },
            onClear = {
                viewModel.documentFileUri = null
                viewModel.documentFileName = ""
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Save Button
        Button(
            onClick = {
                viewModel.saveExpense(context = context, onSuccess = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message.toString()) }
                }, onError = { error ->
                    scope.launch { snackbarHostState.showSnackbar(error.toString()) }
                })
            },
            enabled = viewModel.isFormValid && !viewModel.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (viewModel.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text(if (viewModel.editingExamId == null) "Salvar Exame" else "Atualizar Exame")
            }
        }

        // ---------------------------------------------------
        // DELETE BUTTON (Aparece apenas ao editar)
        // ---------------------------------------------------
        if (viewModel.editingExamId != null) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                enabled = !viewModel.isSaving
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.delete_24px),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Excluir Exame")
            }
        }

    }

    // ---------------------------------------------------
    // DELETE CONFIRMATION DIALOG
    // ---------------------------------------------------
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Excluir Despesa") },
            text = { Text("Tem certeza que deseja excluir esta exame? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteExam(
                            onSuccess = { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            },
                            onError = { error ->
                                scope.launch { snackbarHostState.showSnackbar(error) }
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
