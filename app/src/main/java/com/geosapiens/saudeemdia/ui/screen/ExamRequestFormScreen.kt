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
import com.geosapiens.saudeemdia.ui.viewmodel.ExamRequestViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamRequestForm(
    snackbarHostState: SnackbarHostState,
    viewModel: ExamRequestViewModel
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    if (viewModel.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.selectedRequestDate
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.selectedRequestDate = datePickerState.selectedDateMillis
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
            text = "Registrar Solicitação de Exame",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 1. Exam Cover Image
        FilePickerField(
            label = "Documento de Solicitação",
            fileName = viewModel.documentFileName,
            fileUri = viewModel.documentFileURI,
            acceptedMimeTypes = arrayOf("image/*"),
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
            val context = LocalContext.current
            Button(
                onClick = {
                    viewModel.processFileAndAutoFillExamRequest(
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
                        })
                },
                enabled = !viewModel.isProcessingAI && viewModel.documentFileURI != null,
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

        //CAMPOS OPICIONAIS
        HorizontalDivider(thickness = 1.dp)
        Text("Campos Opicionais", style = MaterialTheme.typography.labelSmall)


        // 4. Specialty Dropdown
        DropdownField(
            label = "Especialidade",
            selectedOption = viewModel.speciality,
            options = viewModel.specialtyOptions,
            onOptionSelected = { viewModel.speciality = it }
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

        // 7. Summary
        OutlinedTextField(
            value = viewModel.summary,
            onValueChange = { viewModel.summary = it },
            label = { Text("Resumo") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )


        // Save Button
        Button(
            onClick = {
                viewModel.saveExamRequest(context = context, onSuccess = { message ->
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
                Text("Salvar Exame")
            }
        }



    }



}

