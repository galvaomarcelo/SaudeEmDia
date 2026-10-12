package com.geosapiens.saudeemdia.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.geosapiens.saudeemdia.ui.components.getFileNameFromUri


enum class ShareOption(val title: String) {
    EXAM_RESULT_COVER("Registrar Resultado c/ Imagem do Documento"),
    EXAM_REQUEST_DOCUMENT("Registrar Solicitação c/ Imagem do Documento"),
    PRESCRIPTION_DOCUMENT("Registrar Receita/Medicamento c/ Imagem"),
}

class SharedFileViewModel : ViewModel() {

    var sharedUri by mutableStateOf<Uri?>(null)
        private set

    var shouldNavigateToOptions by mutableStateOf(false)

    fun onNewFileShared(uri: Uri) {
        sharedUri = uri
        shouldNavigateToOptions = true
    }

    fun consumeNavigation() {
        shouldNavigateToOptions = false
    }

    fun applySharedFile(
        context: Context,
        option: ShareOption,
        examResultViewModel: ExamResultViewModel,
        examRequestViewModel: ExamRequestViewModel,
        prescriptionViewModel: PrescriptionViewModel,
    ): Boolean {
        val uri = sharedUri ?: return false
        val fileName = getFileNameFromUri(context, uri)

        when (option) {
            ShareOption.EXAM_RESULT_COVER -> {
                examResultViewModel.examCoverFileURI = uri
                examResultViewModel.examCoverFileName = fileName
            }
            ShareOption.EXAM_REQUEST_DOCUMENT -> {
                examRequestViewModel.documentFileURI = uri
                examRequestViewModel.documentFileName = fileName
            }
            ShareOption.PRESCRIPTION_DOCUMENT -> {
                prescriptionViewModel.documentFileURI = uri
                prescriptionViewModel.documentFileName = fileName
            }
        }
        clearSharedFile()
        return true
    }

    fun clearSharedFile() {
        sharedUri = null
        shouldNavigateToOptions = false
    }
}
