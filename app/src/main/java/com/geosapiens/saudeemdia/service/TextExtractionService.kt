package com.geosapiens.saudeemdia.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
//import com.tom_roush.pdfbox.pdmodel.PDDocument
//import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object TextExtractionService {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromUri(uri: Uri, context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri)
            val text = when {
                mimeType?.startsWith("image/") == true -> extractTextFromImage(uri, context)
                mimeType == "application/pdf" -> throw Exception("Use uma imagem. Preenchimento automático ainda não supportado para PDF")
                else -> throw Exception("Tipo de arquivo não suportado")
            }
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadBitmap(uri: Uri, context: Context): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: throw Exception("Não foi possível carregar a imagem")
        }
    }

    private suspend fun extractTextFromImage(uri: Uri, context: Context): String {
        val bitmap = loadBitmap(uri, context)
        val image = InputImage.fromBitmap(bitmap, 0)

        return suspendCancellableCoroutine { continuation ->
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

        /*FUNCAO PARA EXTRAIR PDF USA com.tom_roush.pdfbox*/
//    private fun extractTextFromPdf(uri: Uri, context: Context): String {
//        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
//            PDDocument.load(inputStream).use { document ->
//                val stripper = PDFTextStripper()
//                stripper.getText(document)
//            }
//        } ?: throw Exception("Não foi possível ler o PDF")
//    }

}