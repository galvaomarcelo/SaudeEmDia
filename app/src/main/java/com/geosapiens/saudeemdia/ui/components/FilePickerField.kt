package com.geosapiens.saudeemdia.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import com.geosapiens.saudeemdia.R
import java.io.File

@Composable
fun FilePickerField(
    label: String,
    fileName: String,
    onFileSelected: (uri: Uri, fileName: String) -> Unit,
    modifier: Modifier = Modifier,
    onClear: () -> Unit = {},
    acceptedMimeTypes: Array<String> = arrayOf("image/*", "application/pdf"),
    maxSizeMB: Int = 4,
    showPreview: Boolean = true,
    fileUri: Uri? = null
) {
    val context = LocalContext.current
    var errorMessage: String? by remember { mutableStateOf<String?>(null) }
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Persist URI permission across app restarts
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val size = getFileSize(context, it)
                if (size > maxSizeMB * 1024 * 1024) {
                    errorMessage = "File too large (max ${maxSizeMB}MB)"
                    return@let
                }

                errorMessage = null
                val fileName = getFileNameFromUri(context, it)
                onFileSelected(it, fileName)
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
            }
        }
    }
    Column(modifier = modifier) {
        OutlinedTextField(
            value = fileName,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row {
                    // Clear button (only show if file is selected)
                    if (fileName.isNotBlank()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                painter = painterResource(id = R.drawable.close_24px),
                                contentDescription = "Clear file"
                            )
                        }
                    }

                    // File picker button
                    IconButton(onClick = {
                        filePicker.launch(acceptedMimeTypes)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.add_24px),
                            contentDescription = "Select file"
                        )
                    }
                }
            },
            isError = errorMessage != null,
            supportingText = {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                } else if (fileName.isNotBlank()) {
                    Text("✓ File ready for upload", color = Color.Green)
                } else {
                    Text("Supported: Images and PDFs (max ${maxSizeMB}MB)")
                }
            }
        )

    }
}

// Helper function to get filename from URI
fun getFileNameFromUri(context: Context, uri: Uri): String {
    var fileName = "unknown"

    if (uri.scheme == "content") {
        // Use ContentResolver to query filename
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameColumn != -1) {
                    fileName = cursor.getString(nameColumn)
                }
            }
        }
    } else if (uri.scheme == "file") {
        // Direct file path (older Android versions)
        fileName = uri.path?.let { File(it).name } ?: "unknown"
    }

    return fileName
}

fun getFileSize(context: Context, uri: Uri): Long {
    return context.contentResolver.openFileDescriptor(uri, "r")?.use {
        it.statSize
    } ?: 0
}
