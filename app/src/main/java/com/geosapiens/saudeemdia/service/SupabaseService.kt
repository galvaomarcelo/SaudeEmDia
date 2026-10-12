package com.geosapiens.saudeemdia.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.geosapiens.saudeemdia.BuildConfig
import com.geosapiens.saudeemdia.util.ImageUtils
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object SupabaseService {


    private const val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private const val SUPABASE_KEY = BuildConfig.SUPABASE_KEY

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY,
        ) {
            install(Postgrest)
            install(Auth) {
                autoLoadFromStorage = false
            }
            install(Storage)
        }

    }

    // Upload file and return the public URL
    suspend fun uploadFile(uri: Uri, fileName: String, folderName: String, context: Context, compress: Boolean = false): String {

        return withContext(Dispatchers.IO) {
            try {
                // Read file as bytes (with optional compression)
                val bytes = if (compress && (context.contentResolver.getType(uri)?.startsWith("image/") == true)) {
                    ImageUtils.compressImage(context, uri) ?: throw Exception("Failed to compress image")
                } else {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes()
                    } ?: throw Exception("Failed to read file")
                }

                // Create unique path to avoid collisions
                val uniquePath = "$folderName/${System.currentTimeMillis()}_$fileName"

                // Upload to Supabase Storage
                client.storage.from("documents-uploads").upload(
                    path = uniquePath,
                    data = bytes,
                )



                // Return public URL (for public buckets) because is last command in supended function
                client.storage.from("documents-uploads").publicUrl(uniquePath)

                // If bucket is PRIVATE, use signed URL instead:
                // storage.createSignedUrl(uniquePath, 3600) // 1 hour expiry

            } catch (e: Exception) {
                Log.e("SupabaseManager", "Upload failed", e)
                throw Exception("File upload failed: ${e.message}")
            }
        }
    }

    // Upload multiple files
    suspend fun uploadMultipleFiles(
        files: Map<Uri, String>, // uri to desired filename
        folderName: String,
        context: Context
    ): Map<String, String> { // Returns map of original filename -> uploaded URL
        val results = mutableMapOf<String, String>()

        for ((uri, fileName) in files) {
            val url = uploadFile(uri, fileName, folderName,context)
            results[fileName] = url
        }

        return results
    }

    // Delete file from storage (useful for cleanup on error)
    suspend fun deleteFile(fileUrl: String) {
        try {
            // Extract path from URL
            val path = fileUrl.substringAfter("/documents-uploads/")
            client.storage.from("documents-uploads").delete(listOf(path))
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Delete failed", e)
        }
    }
}
