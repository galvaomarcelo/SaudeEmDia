package com.geosapiens.saudeemdia.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geosapiens.saudeemdia.model.ExamResult
import com.geosapiens.saudeemdia.ui.components.ExamRequestList
import com.geosapiens.saudeemdia.ui.components.ExamResultList
import com.geosapiens.saudeemdia.ui.components.PrescriptionList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListVisualizationScreen( onEditExamResult: (com.geosapiens.saudeemdia.model.ExamResult) -> Unit = {}){

    val topics = listOf("Exames","Pedidos", "Receitas")
    val pagerState = rememberPagerState(pageCount = { topics.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    divider = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    topics.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    style = if (pagerState.currentPage == index)
                                        MaterialTheme.typography.titleSmall
                                    else
                                        MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }
                }
            }
        }
    ){ padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { pageIndex ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                ListPlaceholder(topics[pageIndex], onEditExamResult)
            }
        }
    }


}

@Composable
fun ListPlaceholder(
    topic: String,
    onEditExamResult: (ExamResult) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth())
    {
        when (topic) {
            "Exames" -> ExamResultList(onEditClick = onEditExamResult)
            "Pedidos" -> ExamRequestList()
            "Receitas" -> PrescriptionList()
            else -> {
                Text(text = "Em breve")
            }
        }
    }
}
