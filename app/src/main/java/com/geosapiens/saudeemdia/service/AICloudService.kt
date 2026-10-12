package com.geosapiens.saudeemdia.service


import com.geosapiens.saudeemdia.BuildConfig
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.Content
import dev.shreyaspatil.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object AICloudService {

    private const val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
    private const val MODEL_NAME = "gemini-3.1-flash-lite"
    //private const val MODEL_NAME = "gemini-1.5-flash"
    private val generativeModel = GenerativeModel(
                    modelName = MODEL_NAME, // Use a flash model for speed/JSON
                    apiKey = GEMINI_API_KEY,
                    // 2. Configure for JSON output
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                    }
    )

    /**
     * Parseia texto extraído para dados estruturados da despesa
     * @param rawText Texto bruto extraído do arquivo
     * @return Result<ExtractedExpenseData> contendo os dados parseados
     */
    suspend fun parseExtractedTextToJSON(promptText: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {


                val response = generativeModel.generateContent(promptText)

                println(response.text)
                val jsonText = response.text?.trim() ?: throw Exception("Resposta vazia da IA")
                //val expenseData = parseResponseToExpenseData(jsonText)

                Result.success(jsonText)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }


        suspend fun getStoryDataFromJSON(promptText: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {


                val response = generativeModel.generateContent(promptText)

                println(response.text)
                val jsonText = response.text?.trim() ?: throw Exception("Resposta vazia da IA")
                //val expenseData = parseResponseToExpenseData(jsonText)

                Result.success(jsonText)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }



}