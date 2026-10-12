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
import com.geosapiens.saudeemdia.model.ExamRequest
import com.geosapiens.saudeemdia.model.ExamResult
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

class ExamRequestViewModel : ViewModel() {

    var documentFileName by mutableStateOf("")
    var documentFileURI by mutableStateOf<Uri?>(null)

    var examName by mutableStateOf("")
    var examType by mutableStateOf("")
    var speciality by mutableStateOf("")
    var institution by mutableStateOf("")
    var requestingDoctor by mutableStateOf("")

    var summary by mutableStateOf("")

    var selectedRequestDate by mutableStateOf<Long?>(System.currentTimeMillis())

    var showDatePicker by mutableStateOf(false)
    var isProcessingAI by mutableStateOf(false)
    var isSaving by mutableStateOf(false)
    var lastSavedExamRequest by mutableStateOf<ExamRequest?>(null)
    var showSuccessCard by mutableStateOf(false)

    val isFormValid: Boolean
        get() = examName.isNotBlank() && selectedRequestDate != null && examType.isNotBlank()
    val examTypeOptions = listOf(
        // Laboratoriais
        "Exame de Sangue",
        "Exame de Urina",
        "Exame de Fezes",
        "Exame Hormonal",
        "Exame Imunológico",
        "Marcador Tumoral",

        // Imagem
        "Ultrassonografia",
        "Ressonância Magnética",
        "Tomografia Computadorizada",
        "Radiografia (Raio-X)",
        "Mamografia",
        "Densitometria Óssea",

        // Cardiologia
        "Eletrocardiograma",
        "Ecocardiograma",
        "Teste Ergométrico",
        "Holter",
        "MAPA",

        // Endoscopia / procedimentos
        "Endoscopia",
        "Colonoscopia",
        "Biópsia",

        // Anatomia / patologia
        "Exame Anatomopatológico",
        "Citologia",

        // Especializados
        "Exame Oftalmológico",
        "Exame Dermatológico",
        "Teste Funcional",

        "Outro"
    )
    val specialtyOptions = listOf(
        "Cardiologia",
        "Clínica Médica",
        "Dermatologia",
        "Endocrinologia",
        "Gastroenterologia",
        "Ginecologia",
        "Hematologia",
        "Neurologia",
        "Oftalmologia",
        "Ortopedia",
        "Otorrinolaringologia",
        "Pneumologia",
        "Psiquiatria",
        "Reumatologia",
        "Urologia",
        "Oncologia",
        "Nefrologia",
        "Infectologia",
        "Cirurgia Geral",
        "Outro"
    )

    private val displayDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val formattedDate = derivedStateOf {
        selectedRequestDate?.let { displayDateFormatter.format(Date(it)) } ?: ""
    }

    var isLoading by mutableStateOf(false)
    var requests by mutableStateOf<List<ExamRequest>>(emptyList())

    fun loadRequests() {
        viewModelScope.launch {
            try {
                isLoading = true
                requests = SupabaseService.client.from("view_exam_requests")
                    .select()
                    .decodeList<ExamRequest>()
            } catch (e: Exception) {
                Log.e("ExamRequestViewModel", "Error loading exam requests", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteExamRequest(
        id: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                SupabaseService.client.postgrest.rpc(
                    function = "app_delete_exam_request",
                    parameters = buildJsonObject {
                        put("p_exam_request_id", id)
                    }
                )
                loadRequests()
                onSuccess()
            } catch (e: Exception) {
                Log.e("ExamRequestViewModel", "Error deleting exam request", e)
                onError(e.localizedMessage ?: "Erro ao excluir solicitação")
            }
        }
    }

    fun saveExamRequest(
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val isoDate = selectedRequestDate?.let { isoDateFormatter.format(Date(it)) } ?: ""

        viewModelScope.launch {
            isSaving = true

            try {
                var documentFileUrl: String? = null

                // Step 1: Upload Cover if present (only if it's a new Uri)
                documentFileURI?.let { uri ->
                    if (documentFileName.isNotBlank() && !uri.toString().startsWith("http")) {
                        documentFileUrl = SupabaseService.uploadFile(
                            uri,
                            documentFileName,
                            "requests",
                            context,
                            compress = true
                        )
                    } else if (uri.toString().startsWith("http")) {
                        documentFileUrl = uri.toString()
                    }
                }


                // Create new exam
                SupabaseService.client.postgrest.rpc(
                    function = "app_create_exam_request",
                    parameters = buildJsonObject {
                        put("p_patient_id", 0)
                        put("p_request_date", isoDate)
                        put("p_exam_name", examName)
                        put("p_speciality", speciality)
                        put("p_exam_type", examType)
                        put("p_requesting_doctor", requestingDoctor)
                        put("p_institution", institution)
                        put("p_summary", summary)
                        put("p_status", "pendente")
                        put("p_document_url", documentFileUrl)
                    }
                )



                val newExamRequest = ExamRequest(
                    patientId = 0,
                    requestDate = isoDate,
                    examName = examName,
                    speciality = speciality,
                    examType = examType,
                    institution = institution,
                    summary = summary,
                    documentUrl = documentFileUrl,
                    requestingDoctor = requestingDoctor
                )

                lastSavedExamRequest = newExamRequest
                showSuccessCard = true
                val successMessage = "Solicitação salva com sucesso!"
                resetForm()
                loadRequests()
                onSuccess(successMessage)
            } catch (e: Exception) {
                Log.e("ExamResultViewModel", "Error saving exam", e)
                onError("Falha ao salvar: ${e.localizedMessage}")
            } finally {
                isSaving = false
            }


        }

    }

    fun resetForm() {
        examName = ""
        examType = ""
        speciality = ""
        institution = ""
        requestingDoctor = ""
        summary = ""
        documentFileName = ""
        documentFileURI = null
        selectedRequestDate = System.currentTimeMillis()
    }


    fun processFileAndAutoFillExamRequest(context: Context, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isProcessingAI = true
            try{
                val extractionResult: Result<String>? = documentFileURI?.let { uri ->
                    TextExtractionService.extractTextFromUri(uri, context)
                }

                if (extractionResult != null && extractionResult.isFailure) {
                    Log.e("ExamRequestViewModel", "Error extracting text")
                    onError("Falha ao extrair text: $extractionResult")
                }

                val rawText = extractionResult?.getOrThrow()
                Log.d("ExamRequestViewModel", "ML Extracted Text: ${rawText}")


                Log.d("ExamRequestViewModel", "AI Prompt: ${aiPromptParseExamResult(text = rawText)}")
                val result = AICloudService.parseExtractedTextToJSON(
                    aiPromptParseExamResult(text = rawText)
                )

                if (result.isSuccess) {
                    val jsonStr = result.getOrThrow()
                    Log.d("ExamRequestViewModel", "AI JSON Result: $jsonStr")

                    val jsonConfig = Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    }
                    val extractedData = jsonConfig.decodeFromString<ExtractedExamRequestData>(jsonStr)

                    autoFillForm(extractedData)
                    onSuccess("Campos preenchidos automaticamente!")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                    onError("Falha ao processar com IA: $errorMsg")
                }
            }
            catch (e: Exception) {
                Log.e("ExamRequestViewModel", "Error AI autofill")
                onError("Falha ao completar com IA: $e")
            } finally {
                isProcessingAI = false
            }
        }


    }

    @Serializable
    data class ExtractedExamRequestData(
        val name: String? = null,
        val request_date: String? = null,
        val exam_type: String? = null,
        val specialty: String? = null,
        val institution: String? = null,
        val requesting_doctor: String? = null,
        val summary: String? = null,
        val confidence: Map<String, Float> = emptyMap() // Confiança por campo
    )


    private fun aiPromptParseExamResult(text: String?):String{
        return """
            Você é um especialista em Exame Médico.
            O seguinte texto é uma extração desestruturada de um solicitação de exame médico tipo exame sangue, ultrasonagrafia, rai-x, etc. O texto foi extraido usando com.google.mlkit.vision.text.TextRecognition. 
            Analise os termos do texto e struture conforme um registro de solicitações de exames médico com informações chave sobre:
            1) Nome ou título do exame, 2) Data da solicitação, 3) Tipo de Exame (Sangue, Raio-x, etc)   
             4) Especilidade fim do exame (Cardilogia, Ginecologia, etc), 5) Nome do Médico Solicitante, 6) Instituição ou Clinica ou Hospital da solicitação, 
              7) informatações basicas sobre pedido: detalhes dos exames, indicação, razão, instruçoes para realizar o exame 
            
            ---
            $text
            ---
          
            Retorne SOMENTE um JSON válido com esta estrutura:
            {
                "name": "Nome ou título do exame",
                "request_date": "YYYY-MM-DD" (Data da solicitação),
                "exam_type":"Tipo de Exame" (opções possíveis: [${examTypeOptions.joinToString(separator = ",")}], nulo se não identificar)
                "specialty": "Especilidade fim do exame" (opções possíveis: [${ specialtyOptions.joinToString(separator = ",")}], nulo se não identificar),
                "institution": "Instituição, Clínica, ou Laboratório em que o exame foi realizado",
                "requesting_doctor": "Nome do Médico Solicitante",               
                "summary": "Um resumo com mais detalhes sobre o exames (inclusive se houver mais de um), instruções de preparação para realizar os exames,                 
                "confidence": {
                    "name": 0.0-1.0,
                    "exam_type": 0.0-1.0,
                    "specialty": 0.0-1.0,
                    "institution": 0.0-1.0
                    "requesting_doctor": 0.0-1.0
                }
            }
            Não use caixa alta para as strings. Para nomes (exame, instituição, e médicos) use tipo título (apenas iniciais de nomes maiúsculas)
            Se não conseguir extrair um campo, use string vazia para texto ou 0.00 para valores.
        """.trimIndent()
    }

    private fun autoFillForm(extractedData: ExtractedExamRequestData) {
        examName = extractedData.name?: ""
        extractedData.request_date?.let { dateStr ->
            try {
                selectedRequestDate = isoDateFormatter.parse(dateStr)?.time
            } catch (e: Exception) {
                Log.e("ExpensesViewModel", "Error parsing date: $dateStr", e)
            }
        }
        if (!extractedData.exam_type.isNullOrBlank() && examTypeOptions.contains(extractedData.exam_type)) {
            examType = extractedData.exam_type
        }
        if (!extractedData.specialty.isNullOrBlank() && specialtyOptions.contains(extractedData.specialty)) {
            speciality = extractedData.specialty
        }
        institution = extractedData.institution?: ""
        requestingDoctor = extractedData.requesting_doctor?: ""
        summary = extractedData.summary?: ""

    }

}