package com.geosapiens.saudeemdia

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geosapiens.saudeemdia.ui.screen.ExamRequestForm
import com.geosapiens.saudeemdia.ui.screen.ExamResultForm
import com.geosapiens.saudeemdia.ui.screen.HomeScreen
import com.geosapiens.saudeemdia.ui.screen.ListVisualizationScreen
import com.geosapiens.saudeemdia.ui.screen.PrescriptionForm
import com.geosapiens.saudeemdia.ui.screen.ShareFileOptionsScreen
import com.geosapiens.saudeemdia.ui.theme.SaudeEmDiaTheme
import com.geosapiens.saudeemdia.ui.viewmodel.ExamRequestViewModel
import com.geosapiens.saudeemdia.ui.viewmodel.ExamResultViewModel
import com.geosapiens.saudeemdia.ui.viewmodel.PrescriptionViewModel
import com.geosapiens.saudeemdia.ui.viewmodel.SharedFileViewModel
import kotlinx.coroutines.launch
import kotlin.getValue

enum class MenuOptions {
    Home,
    ExamResult, ExamRequest, Prescription,
    ListVisualization,
    ShareFileOptions

}


class MainActivity : ComponentActivity() {
    private val examResultViewModel: ExamResultViewModel by viewModels()
    private val examRequestViewModel: ExamRequestViewModel by viewModels()

    private val prescriptionViewModel: PrescriptionViewModel by viewModels()

    private val sharedFileViewModel: SharedFileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            SaudeEmDiaTheme {
                MainScreen(
                    examResultViewModel, examRequestViewModel, prescriptionViewModel, sharedFileViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            uri?.let {
                sharedFileViewModel.onNewFileShared(it)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(examResultViewModel: ExamResultViewModel= viewModel(),
               examRequestViewModel: ExamRequestViewModel= viewModel(),
               prescriptionViewModel: PrescriptionViewModel= viewModel(),
               sharedFileViewModel: SharedFileViewModel = viewModel()){

    //drawerState control how the drawer opens and closes
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(MenuOptions.Home) }

    LaunchedEffect(sharedFileViewModel.shouldNavigateToOptions) {
        if (sharedFileViewModel.shouldNavigateToOptions) {
            currentScreen = MenuOptions.ShareFileOptions
            drawerState.close()
            sharedFileViewModel.consumeNavigation()
        }
    }


    ModalNavigationDrawer(
        //pass a DrawerState to ModalNavigationDrawer using the drawerState parameter
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Saúde em Dia",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("Início") },
                    icon = {Icon(painter = painterResource(id = R.drawable.home_24px), contentDescription = "Home")},
                    selected = currentScreen == MenuOptions.Home,
                    onClick = {
                        currentScreen = MenuOptions.Home
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("Ver Dados") },
                    icon = {Icon(painter = painterResource(id = R.drawable.list_alt_24px), contentDescription = "View data")},
                    selected = currentScreen == MenuOptions.ListVisualization,
                    onClick = {
                        currentScreen = MenuOptions.ListVisualization
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )


                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("Registrar Resultado") },
                    icon = {Icon(painter = painterResource(id = R.drawable.monitoring_24px), modifier = Modifier.rotate(180f), contentDescription = "Results Form")},
                    selected = currentScreen == MenuOptions.ExamResult,
                    onClick = {
                        currentScreen = MenuOptions.ExamResult
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Registrar Solicitação") },
                    icon = {Icon(painter = painterResource(id = R.drawable.checkbook_24px), contentDescription = "Requests Form")},
                    selected = currentScreen == MenuOptions.ExamRequest,
                    onClick = {
                        currentScreen = MenuOptions.ExamRequest
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Registrar Receita") },
                    icon = {Icon(painter = painterResource(id = R.drawable.checkbook_24px), contentDescription = "Prescription Form")},
                    selected = currentScreen == MenuOptions.Prescription,
                    onClick = {
                        currentScreen = MenuOptions.Prescription
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

            }
        }
    ) {
        // Screen content
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                MenuOptions.Home -> "Saúde em Dia"
                                MenuOptions.ExamResult -> "Saúde em Dia"
                                MenuOptions.ExamRequest -> "Saúde em Dia"
                                MenuOptions.Prescription -> "Saúde em Dia"
                                MenuOptions.ListVisualization -> "Ver Dados"
                                MenuOptions.ShareFileOptions -> "Opções para Compartilhar"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(painter = painterResource(id = R.drawable.menu_24px), contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    )
                )
            }
        )
        { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                when (currentScreen) {
                    MenuOptions.Home -> HomeScreen()
                    MenuOptions.ExamResult -> ExamResultForm(snackbarHostState, examResultViewModel)
                    MenuOptions.ExamRequest -> ExamRequestForm(snackbarHostState, examRequestViewModel)
                    MenuOptions.Prescription -> PrescriptionForm(snackbarHostState, prescriptionViewModel)
                    MenuOptions.ListVisualization -> ListVisualizationScreen(
                        onEditExamResult = { exam ->
                            examResultViewModel.prepareEditExamResult(exam)
                            currentScreen = com.geosapiens.saudeemdia.MenuOptions.ExamResult
                        }
                    )
                    MenuOptions.ShareFileOptions -> ShareFileOptionsScreen(
                        sharedFileViewModel = sharedFileViewModel,
                        examResultViewModel = examResultViewModel,
                        examRequestViewModel = examRequestViewModel,
                        prescriptionViewModel = prescriptionViewModel,
                        onNavigateToExamResult = { currentScreen = MenuOptions.ExamResult },
                        onNavigateToExamRequest = { currentScreen = MenuOptions.ExamRequest },
                        onNavigateToPrescription = { currentScreen = MenuOptions.Prescription },
                    )
                }
            }
        }
    }

}


