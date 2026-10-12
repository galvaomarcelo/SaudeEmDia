package com.geosapiens.saudeemdia.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geosapiens.saudeemdia.model.Prescription
import com.geosapiens.saudeemdia.service.AICloudService
import com.geosapiens.saudeemdia.service.SupabaseService
import com.geosapiens.saudeemdia.service.TextExtractionService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrescriptionViewModel : ViewModel() {

    var documentFileName by mutableStateOf("")
    var documentFileURI by mutableStateOf<Uri?>(null)
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var doctorName by mutableStateOf("")
    var doctorCrm by mutableStateOf("")
    var institution by mutableStateOf("")
    var medicationsRaw by mutableStateOf("")

    var selectedPrescriptionDate by mutableStateOf<Long?>(System.currentTimeMillis())

    var showDatePicker by mutableStateOf(false)
    var isProcessingAI by mutableStateOf(false)
    var isSaving by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    var lastSavedPrescription by mutableStateOf<Prescription?>(null)
    var showSuccessCard by mutableStateOf(false)

    var prescriptions by mutableStateOf<List<Prescription>>(emptyList())

    val isFormValid: Boolean
        get() = description.isNotBlank() && selectedPrescriptionDate != null

    private val displayDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val formattedDate = derivedStateOf {
        selectedPrescriptionDate?.let { displayDateFormatter.format(Date(it)) } ?: ""
    }

    fun loadPrescriptions() {
        viewModelScope.launch {
            try {
                isLoading = true
                prescriptions = SupabaseService.client.from("view_prescriptions")
                    .select()
                    .decodeList<Prescription>()
            } catch (e: Exception) {
                Log.e("PrescriptionViewModel", "Error loading prescriptions", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun deletePrescription(
        id: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                SupabaseService.client.postgrest.rpc(
                    function = "app_delete_prescription",
                    parameters = buildJsonObject {
                        put("p_prescription_id", id)
                    }
                )
                loadPrescriptions()
                onSuccess()
            } catch (e: Exception) {
                Log.e("PrescriptionViewModel", "Error deleting prescription", e)
                onError(e.localizedMessage ?: "Erro ao excluir receita")
            }
        }
    }

    fun savePrescription(
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val isoDate = selectedPrescriptionDate?.let { isoDateFormatter.format(Date(it)) } ?: ""

        viewModelScope.launch {
            isSaving = true

            try {
                var documentFileUrl: String? = null

                documentFileURI?.let { uri ->
                    if (documentFileName.isNotBlank() && !uri.toString().startsWith("http")) {
                        documentFileUrl = SupabaseService.uploadFile(
                            uri,
                            documentFileName,
                            "prescriptions",
                            context,
                            compress = true
                        )
                    } else if (uri.toString().startsWith("http")) {
                        documentFileUrl = uri.toString()
                    }
                }

                SupabaseService.client.postgrest.rpc(
                    function = "app_create_prescription",
                    parameters = buildJsonObject {
                        put("p_patient_id", 0)
                        put("p_description", description)
                        put("p_title", title)
                        put("p_prescription_date", isoDate)
                        put("p_doctor_name", doctorName)
                        put("p_doctor_crm", doctorCrm)
                        put("p_institution", institution)
                        put("p_document_url", documentFileUrl)
                    }
                )

                val newPrescription = Prescription(
                    patientId = 0,
                    description = description,
                    title = title,
                    prescriptionDate = isoDate,
                    doctorName = doctorName,
                    doctorCrm = doctorCrm,
                    institution = institution,
                    documentUrl = documentFileUrl
                )

                lastSavedPrescription = newPrescription
                showSuccessCard = true
                val successMessage = "Receita salva com sucesso!"
                resetForm()
                loadPrescriptions()
                onSuccess(successMessage)
            } catch (e: Exception) {
                Log.e("PrescriptionViewModel", "Error saving prescription", e)
                onError("Falha ao salvar: ${e.localizedMessage}")
            } finally {
                isSaving = false
            }
        }
    }

    fun resetForm() {
        description = ""
        title = ""
        doctorName = ""
        doctorCrm = ""
        institution = ""
        medicationsRaw = ""
        documentFileName = ""
        documentFileURI = null
        selectedPrescriptionDate = System.currentTimeMillis()
    }

    fun processFileAndAutoFillPrescription(
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            isProcessingAI = true
            try {
                val extractionResult: Result<String>? = documentFileURI?.let { uri ->
                    TextExtractionService.extractTextFromUri(uri, context)
                }

                if (extractionResult != null && extractionResult.isFailure) {
                    Log.e("PrescriptionViewModel", "Error extracting text")
                    onError("Falha ao extrair texto: $extractionResult")
                }

                val rawText = extractionResult?.getOrThrow()
                Log.d("PrescriptionViewModel", "ML Extracted Text: $rawText")

                val result = AICloudService.parseExtractedTextToJSON(
                    aiPromptParsePrescription(text = rawText)
                )

                if (result.isSuccess) {
                    val jsonStr = result.getOrThrow()
                    Log.d("PrescriptionViewModel", "AI JSON Result: $jsonStr")

                    val jsonConfig = Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    }
                    val extractedData = jsonConfig.decodeFromString<ExtractedPrescriptionData>(jsonStr)

                    autoFillForm(extractedData)
                    onSuccess("Campos preenchidos automaticamente!")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                    onError("Falha ao processar com IA: $errorMsg")
                }
            } catch (e: Exception) {
                Log.e("PrescriptionViewModel", "Error AI autofill", e)
                onError("Falha ao completar com IA: $e")
            } finally {
                isProcessingAI = false
            }
        }
    }

    @Serializable
    data class ExtractedPrescriptionData(
        val title: String? = null,
        val description: String? = null,
        val prescription_date: String? = null,
        val doctor_name: String? = null,
        val doctor_crm: String? = null,
        val institution: String? = null,
    )

    private fun aiPromptParsePrescription(text: String?): String {
        return """
            Você é um especialista em Receita Médica e Medicamentos. 
            O seguinte texto é uma extração de uma receita médica ou de um medicamento. O texto foi extraido usando com.google.mlkit.vision.text.TextRecognition.
            Analise o texto e estruture conforme um registro de receita médica ou medicamento com informações sobre:
            1) Descrição ou resumo da receita/tratamento, 2) Data da receita (YYYY-MM-DD), 3) Nome do médico, 4) CRM do médico, 5) Instituição/Clínica/Hospital, 6) Medicamentos prescritos com dosagem e posologia.
            
            ---
            $text
            ---
          
            Retorne SOMENTE um JSON válido com esta estrutura:
            {
                "title": "Um título" (eg. Receituário de medicamentos para gripe, Receita de Medicamento X, Medicamento X)
                "description": "Descrição geral da receita médica, indicação clínica, ou medicamento. Possivel Razão da indicação, Lista de medicamentos. Incluir número de comprimidos, dosagem e posologia para cada remedio",
                "prescription_date": "YYYY-MM-DD",
                "doctor_name": "Nome do Médico",
                "doctor_crm": "CRM do médico (número e UF se houver)",
                "institution": "Nome da Instituição, Hospital ou Clínica"
            }
            Não use caixa alta para as strings. Para nomes use tipo título (apenas iniciais maiúsculas).
            Se não conseguir extrair um campo, use string vazia.
        """.trimIndent()
    }

    private fun autoFillForm(extractedData: ExtractedPrescriptionData) {
        title = extractedData.title ?: ""
        description = extractedData.description ?: ""
        extractedData.prescription_date?.let { dateStr ->
            try {
                selectedPrescriptionDate = isoDateFormatter.parse(dateStr)?.time
            } catch (e: Exception) {
                Log.e("PrescriptionViewModel", "Error parsing date: $dateStr", e)
            }
        }
        doctorName = extractedData.doctor_name ?: ""
        doctorCrm = extractedData.doctor_crm ?: ""
        institution = extractedData.institution ?: ""
    }
}
