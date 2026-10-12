package com.geosapiens.saudeemdia.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.geosapiens.saudeemdia.ui.viewmodel.ExamRequestViewModel
import com.geosapiens.saudeemdia.ui.viewmodel.ExamResultViewModel
import com.geosapiens.saudeemdia.ui.viewmodel.PrescriptionViewModel
import com.geosapiens.saudeemdia.ui.viewmodel.ShareOption
import com.geosapiens.saudeemdia.ui.viewmodel.SharedFileViewModel


@Composable
fun ShareFileOptionsScreen(
    sharedFileViewModel: SharedFileViewModel,
    examResultViewModel: ExamResultViewModel,
    examRequestViewModel: ExamRequestViewModel,
    prescriptionViewModel: PrescriptionViewModel,
    onNavigateToExamResult: () -> Unit,
    onNavigateToExamRequest: () -> Unit,
    onNavigateToPrescription: () -> Unit,
) {
    val context = LocalContext.current
    val options = ShareOption.entries // Usa as opções definidas no Enum

    LazyColumn {
        items(options) { option ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        val handled = sharedFileViewModel.applySharedFile(
                            context = context,
                            option = option,
                            examResultViewModel = examResultViewModel,
                            examRequestViewModel = examRequestViewModel,
                            prescriptionViewModel = prescriptionViewModel,
                        )
                        if (handled) {
                            when (option) {
                                ShareOption.EXAM_REQUEST_DOCUMENT -> onNavigateToExamRequest()
                                ShareOption.PRESCRIPTION_DOCUMENT -> onNavigateToPrescription()
                                else -> onNavigateToExamResult()
                            }
                        }
                    }
            ) {
                Text(
                    text = option.title,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}
