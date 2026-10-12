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
import kotlin.text.isNullOrBlank


class ExamResultViewModel : ViewModel() {
    var examCoverFileName by mutableStateOf("")
    var examCoverFileURI by mutableStateOf<Uri?>(null)

    var examName by mutableStateOf("")
    var selectedExamDate by mutableStateOf<Long?>(System.currentTimeMillis())
    var summary by mutableStateOf("")

    // New Fields
    var examType by mutableStateOf("")
    var specialty by mutableStateOf("")
    var institution by mutableStateOf("")
    var result by mutableStateOf("")
    var conclusion by mutableStateOf("")
    var repositoryHyperlink by mutableStateOf("")
    var examCode by mutableStateOf("")
    var patientCode by mutableStateOf("")
    var requestingDoctor by mutableStateOf("")
    var examiner by mutableStateOf("")
    var amount by mutableStateOf("1")

    var editingExamId by mutableStateOf<Long?>(null)

    // Document File Picker
    var documentFileName by mutableStateOf("")
    var documentFileUri by mutableStateOf<Uri?>(null)

    fun validateFileSize(context: Context, uri: Uri, maxSizeMB: Int): Boolean {
        val fileSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
            it.length
        } ?: 0
        return fileSize <= maxSizeMB * 1024 * 1024
    }

    var isSaving by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    var isProcessingAI by mutableStateOf(false)

    val isFormValid: Boolean
        get() = examName.isNotBlank() && selectedExamDate != null && examType.isNotBlank()

    var showDatePicker by mutableStateOf(false)

    var lastSavedExamResult by mutableStateOf<ExamResult?>(null)
    var showSuccessCard by mutableStateOf(false)

    // Dropdown options
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
        selectedExamDate?.let { displayDateFormatter.format(Date(it)) } ?: ""
    }

    var exams by mutableStateOf<List<ExamResult>>(
        emptyList()
    )

    fun loadExams() {
        viewModelScope.launch {
            try {
                isLoading = true

                exams =
                    SupabaseService.client.from("view_exam_results").select().decodeList<ExamResult>()
            } catch (e: Exception) {
                Log.e(
                    "ExamResultViewModel", "Error loading exams", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun saveExpense(
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val isoDate = selectedExamDate?.let { isoDateFormatter.format(Date(it)) } ?: ""

        viewModelScope.launch {
            isSaving = true

            try {
                var examCoverFileUrl: String? = null
                var documentFileUrl: String? = null

                // Step 1: Upload Cover if present (only if it's a new Uri)
                examCoverFileURI?.let { uri ->
                    if (examCoverFileName.isNotBlank() && !uri.toString().startsWith("http")) {
                        examCoverFileUrl = SupabaseService.uploadFile(
                            uri,
                            examCoverFileName,
                            "exams",
                            context,
                            compress = true
                        )
                    } else if (uri.toString().startsWith("http")) {
                        examCoverFileUrl = uri.toString()
                    }
                }

                // Step 2: Upload Document if present
                documentFileUri?.let { uri ->
                    if (documentFileName.isNotBlank() && !uri.toString().startsWith("http")) {
                        documentFileUrl = SupabaseService.uploadFile(
                            uri,
                            documentFileName,
                            "exams",
                            context,
                            compress = true
                        )
                    } else if (uri.toString().startsWith("http")) {
                        documentFileUrl = uri.toString()
                    }
                }

                val currentEditingId = editingExamId

                if (currentEditingId == null) {
                    // Create new exam
                    SupabaseService.client.postgrest.rpc(
                        function = "app_create_exam",
                        parameters = buildJsonObject {
                            put("p_patient_id", 0)
                            put("p_exam_date", isoDate)
                            put("p_exam_name", examName)
                            put("p_specialty", specialty)
                            put("p_exam_type", examType)
                            put("p_institution", institution)
                            put("p_result", result)
                            put("p_conclusion", conclusion)
                            put("p_summary", summary)
                            put("p_exam_code", examCode)
                            put("p_requesting_doctor", requestingDoctor)
                            put("p_examiner", examiner)
                            put("p_ammount", amount.toIntOrNull() ?: 1)
                            put("p_patient_code", patientCode)
                            put("p_repository_hyperlink", repositoryHyperlink)

                            put("p_exam_cover_url", examCoverFileUrl)
                            put("p_document_url", documentFileUrl)
                        }
                    )
                } else {
                    // Update existing exam using app_update_exam
                    SupabaseService.client.postgrest.rpc(
                        function = "app_update_exam",
                        parameters = buildJsonObject {
                            put("p_id", currentEditingId)
                            put("p_patient_id", 0)
                            put("p_exam_date", isoDate)
                            put("p_exam_name", examName)
                            put("p_specialty", specialty)
                            put("p_exam_type", examType)
                            put("p_institution", institution)
                            put("p_result", result)
                            put("p_conclusion", conclusion)
                            put("p_summary", summary)
                            put("p_exam_code", examCode)
                            put("p_requesting_doctor", requestingDoctor)
                            put("p_examiner", examiner)
                            put("p_ammount", amount.toIntOrNull() ?: 1)
                            put("p_patient_code", patientCode)
                            put("p_repository_hyperlink", repositoryHyperlink)

                            put("p_exam_cover_url", examCoverFileUrl)
                            put("p_document_url", documentFileUrl)
                        }
                    )
                }


                val newExamResult = ExamResult(
                    id = currentEditingId,
                    patientId = 0,
                    examDate = isoDate,
                    examName = examName,
                    specialty = specialty,
                    examType = examType,
                    institution = institution,
                    result = result,
                    conclusion = conclusion,
                    summary = summary,
                    examCoverUrl = examCoverFileUrl,
                    documentUrl = documentFileUrl,
                    repositoryHyperlink = repositoryHyperlink,
                    examCode = examCode,
                    requestingDoctor = requestingDoctor,
                    examiner = examiner,
                    patientCode = patientCode,
                    amount = amount.toIntOrNull() ?: 1
                )

                lastSavedExamResult = newExamResult
                showSuccessCard = true
                val successMessage = if (currentEditingId == null) "Exame salvo com sucesso!" else "Exame atualizado com sucesso!"
                resetForm()
                loadExams()
                onSuccess(successMessage)
            } catch (e: Exception) {
                Log.e("ExamResultViewModel", "Error saving exam", e)
                onError("Falha ao salvar: ${e.localizedMessage}")
            } finally {
                isSaving = false
            }


        }

    }

    fun deleteExam(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val idToDelete = editingExamId ?: return

        viewModelScope.launch {
            isSaving = true // Reuse isSaving to show progress
            try {
                SupabaseService.client.postgrest.rpc(
                    function = "app_delete_exam",
                    parameters = buildJsonObject {
                        put("p_id", idToDelete)
                    }
                )
                resetForm()
                onSuccess("Exame excluído com sucesso!")
            } catch (e: Exception) {
                Log.e("ExamResultViewModel", "Error deleting exam", e)
                onError("Falha ao excluir: ${e.localizedMessage}")
            } finally {
                isSaving = false
            }
        }
    }

    fun prepareEditExamResult(exam: ExamResult) {
        editingExamId = exam.id
        examName = exam.examName

        selectedExamDate = try {
            isoDateFormatter.parse(exam.examDate.split("T")[0])?.time
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        examType = exam.examType ?: ""
        specialty = exam.specialty ?: ""
        institution = exam.institution ?: ""
        requestingDoctor = exam.requestingDoctor ?: ""
        examiner = exam.examiner ?: ""
        examCode = exam.examCode ?: ""
        amount = (exam.amount ?: 1).toString()
        patientCode = exam.patientCode ?: ""
        repositoryHyperlink = exam.repositoryHyperlink ?: ""
        summary = exam.summary ?: ""
        result = exam.result ?: ""
        conclusion = exam.conclusion ?: ""

        if (!exam.examCoverUrl.isNullOrBlank()) {
            examCoverFileName = "Capa Exame Atual"
            examCoverFileURI = Uri.parse(exam.examCoverUrl)
        } else {
            examCoverFileName = ""
            examCoverFileURI = null
        }

        if (!exam.documentUrl.isNullOrBlank()) {
            documentFileName = "Documento Atual"
            documentFileUri = Uri.parse(exam.documentUrl)
        } else {
            documentFileName = ""
            documentFileUri = null
        }
    }

    fun resetForm() {
        editingExamId = null
        examName = ""
        examType = ""
        specialty = ""
        institution = ""
        requestingDoctor = ""
        examiner = ""
        examCode = ""
        amount = "1"
        patientCode = ""
        repositoryHyperlink = ""
        summary = ""
        result = ""
        conclusion = ""
        examCoverFileName = ""
        examCoverFileURI = null
        documentFileName = ""
        documentFileUri = null
        selectedExamDate = System.currentTimeMillis()
    }


    fun processFileAndAutoFillComprovante(context: Context, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isProcessingAI = true
            try{
                val extractionResult: Result<String>? = examCoverFileURI?.let { uri ->
                    TextExtractionService.extractTextFromUri(uri, context)
                }

                if (extractionResult != null && extractionResult.isFailure) {
                    Log.e("ExpensesViewModel", "Error extracting text")
                    onError("Falha ao extrair text: $extractionResult")
                }

                val rawText = extractionResult?.getOrThrow()
                Log.d("ExpensesViewModel", "ML Extracted Text: ${rawText}")


                Log.d("ExpensesViewModel", "AI Prompt: ${aiPromptParseExamResult(text = rawText)}")
                val result = AICloudService.parseExtractedTextToJSON(
                    aiPromptParseExamResult(text = rawText)
                )

                if (result.isSuccess) {
                    val jsonStr = result.getOrThrow()
                    Log.d("ExpensesViewModel", "AI JSON Result: $jsonStr")

                    val jsonConfig = Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    }
                    val extractedData = jsonConfig.decodeFromString<ExtractedExamData>(jsonStr)

                    autoFillForm(extractedData)
                    onSuccess("Campos preenchidos automaticamente!")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                    onError("Falha ao processar com IA: $errorMsg")
                }
            }
            catch (e: Exception) {
                Log.e("ExpensesViewModel", "Error AI autofill")
                onError("Falha ao completar com IA: $e")
            } finally {
                isProcessingAI = false
            }
        }
    }

    @Serializable
    data class ExtractedExamData(
        val name: String? = null,
        val exam_date: String? = null,
        val exam_type: String? = null,
        val specialty: String? = null,
        val institution: String? = null,
        val requesting_doctor: String? = null,
        val examiner: String? = null,
        val exam_code: String? = null,
        val amount: Int = 1,
        val patient_code: String? = null,
        val repository_url: String? = null,
        val result: String? = null,
        val conclusion: String? = null,
        val summary: String? = null,
        val confidence: Map<String, Float> = emptyMap() // Confiança por campo
    )


    private fun aiPromptParseExamResult(text: String?):String{
        return """
            Você é um especialista em Exame Médico.
            O seguinte texto é uma extração desestruturada da capa ou laudo de um exame médico tipo exame sangue, ultrasonagrafia, rai-x, etc. O texto foi extraido usando com.google.mlkit.vision.text.TextRecognition. 
            Analise os termos do texto e struture conforme um registro de exame médico com informações chave sobre:
            1) Nome ou título do exame, 2) Data de realização, atendimento ou entrega da amostra, 3) Tipo de Exame (Sangue, Raio-x)   
             4) Especilidade fim do exame (Cardilogia, Ginecologia, etc), 5) Nome do Médico Solicitante, 6) Instituição ou Laboratório em que o exame foi realizado, 
             7) Algum codigo ou número do exame, laudo, ordem de serviço, 8) Nome do responsável tecnico, 9) Quantidade de exames no bloco, 10) Algum codigo ou número do pacient ou cliente,
             11) URL da instituiçõa ou repositório dos resultados, 12) informatações basicas sobre resultados, 13) Informações básicas sobre análise, diagnostico, conclusão, opinião
            
            ---
            $text
            ---
          
            Retorne SOMENTE um JSON válido com esta estrutura:
            {
                "name": "Nome ou título do exame",
                "exam_date": "YYYY-MM-DD" (Data de realização, atendimento ou entrega da amostra),
                "exam_type":"Tipo de Exame" (opções possíveis: [${examTypeOptions.joinToString(separator = ",")}], nulo se não identificar)
                "specialty": "Especilidade fim do exame" (opções possíveis: [${ specialtyOptions.joinToString(separator = ",")}], nulo se não identificar),
                "institution": "Instituição, Clínica, ou Laboratório em que o exame foi realizado",
                "requesting_doctor": "Nome do Médico Solicitante",
                "examiner": "Nome do médico examinador ou responsável tecnico",
                "exam_code": "Algum codigo ou número do exame, laudo, ordem de serviço",
                "amount": numerico interio sobre quantidade de exames (1 se não encontrar),
                "patient_code": "Algum codigo ou número do paciente ou cliente",
                "repository_url": "URL dos resultados ou da instituição",
                "result": "informatações basicas sobre resultados" (Máximo 3 frases ou vazio se não encontrar),
                "conclusion": "Informações básicas sobre análise, diagnostico, conclusão, opinião do examinador" (Máximo 3 frases ou vazio se não encontrar),
                "summary": "Um resumo em linguagem coloquial sobre esse exame focado no tipo de exame e conlusões" (Máximo 3 frases),                
                "confidence": {
                    "name": 0.0-1.0,
                    "exam_type": 0.0-1.0,
                    "specialty": 0.0-1.0,
                    "institution": 0.0-1.0
                    "requesting_doctor": 0.0-1.0,
                    "examiner": 0.0-1.0
                }
            }
            Não use caixa alta para as strings. Para nomes (exame, instituição, e médicos) use tipo título (apenas iniciais de nomes maiúsculas)
            Se não conseguir extrair um campo, use string vazia para texto ou 0.00 para valores.
        """.trimIndent()
    }

    private fun autoFillForm(extractedData: ExtractedExamData) {
        examName = extractedData.name?: ""
        extractedData.exam_date?.let { dateStr ->
            try {
                selectedExamDate = isoDateFormatter.parse(dateStr)?.time
            } catch (e: Exception) {
                Log.e("ExpensesViewModel", "Error parsing date: $dateStr", e)
            }
        }
        if (!extractedData.exam_type.isNullOrBlank() && examTypeOptions.contains(extractedData.exam_type)) {
            examType = extractedData.exam_type
        }
        if (!extractedData.specialty.isNullOrBlank() && specialtyOptions.contains(extractedData.specialty)) {
            specialty = extractedData.specialty
        }
        institution = extractedData.institution?: ""
        requestingDoctor = extractedData.requesting_doctor?: ""
        examiner = extractedData.examiner?:""
        examCode = extractedData.exam_code?:""
        amount = extractedData.amount.toString()
        patientCode = extractedData.patient_code?:""
        repositoryHyperlink = extractedData.repository_url?:""
        result = extractedData.result?: ""
        conclusion = extractedData.conclusion?: ""
        summary = extractedData.summary?: ""


    }




}
