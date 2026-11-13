package com.example.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.domain.PredictionRecord
import com.example.ui.components.BackendStatusChip
import com.example.ui.components.ServerConfigDialog
import com.example.ui.screens.ClassifyScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModelInfoScreen
import com.example.ui.viewmodel.DermaScanViewModel
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_CLASSIFY = "classify"
const val ROUTE_HISTORY = "history"
const val ROUTE_INFO = "info"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val navDestinations = listOf(
    NavDestinationItem(
        route = ROUTE_HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_item_home",
    ),
    NavDestinationItem(
        route = ROUTE_CLASSIFY,
        labelRes = R.string.nav_classify,
        selectedIcon = Icons.Filled.DocumentScanner,
        unselectedIcon = Icons.Outlined.DocumentScanner,
        testTag = "nav_item_classify",
    ),
    NavDestinationItem(
        route = ROUTE_HISTORY,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_item_history",
    ),
    NavDestinationItem(
        route = ROUTE_INFO,
        labelRes = R.string.nav_about,
        selectedIcon = Icons.Filled.Biotech,
        unselectedIcon = Icons.Outlined.Biotech,
        testTag = "nav_item_info",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DermaScanApp(
    viewModel: DermaScanViewModel,
    isDarkMode: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val selectedImage by viewModel.selectedImage.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val backendStatus by viewModel.backendStatus.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val inferenceEngine by viewModel.inferenceEngine.collectAsStateWithLifecycle()
    val metricsState by viewModel.metricsState.collectAsStateWithLifecycle()
    val historyRecords by viewModel.predictionHistory.collectAsStateWithLifecycle()
    val historyFilterCode by viewModel.historyFilterCode.collectAsStateWithLifecycle()
    val selectedHistoryRecord by viewModel.selectedHistoryRecord.collectAsStateWithLifecycle()

    var showServerDialog by remember { mutableStateOf(false) }

    val navigateToTab: (String) -> Unit = { route ->
        if (route != ROUTE_HISTORY) {
            viewModel.closeHistoryRecord()
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val exportReport: (PredictionRecord) -> Unit = { record ->
        scope.launch {
            val uriResult = viewModel.createReportShareUri(record)
            uriResult.fold(
                onSuccess = { uri ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "DermaScan AI Report - ${record.predictedCode}")
                        putExtra(Intent.EXTRA_TEXT, viewModel.buildPlainReportText(record))
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching {
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Export Prediction Report")
                        )
                    }.onFailure {
                        snackbarHostState.showSnackbar("Report saved locally; no share target available.")
                    }
                },
                onFailure = { err ->
                    snackbarHostState.showSnackbar(
                        err.localizedMessage ?: "Failed to prepare exportable report."
                    )
                },
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MedicalServices,
                                    contentDescription = "DermaScan AI",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(18.dp),
                                )
                            }
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    },
                    actions = {
                        BackendStatusChip(
                            status = backendStatus,
                            onClick = { showServerDialog = true },
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("theme_toggle_button"),
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        navDestinations.forEach { item ->
                            val selected = currentRoute == item.route
                            val label = stringResource(id = item.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    if (item.route == ROUTE_HISTORY && historyRecords.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(historyRecords.size.toString())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = label,
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = label,
                                        )
                                    }
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(item.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        navDestinations.forEach { item ->
                            val selected = currentRoute == item.route
                            val label = stringResource(id = item.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    if (item.route == ROUTE_HISTORY && historyRecords.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(historyRecords.size.toString())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = label,
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = label,
                                        )
                                    }
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("${item.testTag}_rail"),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                historyCount = historyRecords.size,
                                onNavigateToClassify = { navigateToTab(ROUTE_CLASSIFY) },
                                onNavigateToHistory = { navigateToTab(ROUTE_HISTORY) },
                                onNavigateToModelInfo = { navigateToTab(ROUTE_INFO) },
                            )
                        }
                        composable(ROUTE_CLASSIFY) {
                            ClassifyScreen(
                                selectedImage = selectedImage,
                                analysisState = analysisState,
                                backendStatus = backendStatus,
                                inferenceEngine = inferenceEngine,
                                serverUrl = serverUrl,
                                onImagePicked = viewModel::onImagePicked,
                                onClearImage = viewModel::clearSelectedImage,
                                onAnalyzeClick = viewModel::analyzeCurrentImage,
                                onOpenServerDialog = { showServerDialog = true },
                                onExportReport = exportReport,
                            )
                        }
                        composable(ROUTE_HISTORY) {
                            HistoryScreen(
                                records = historyRecords,
                                activeFilterCode = historyFilterCode,
                                selectedRecord = selectedHistoryRecord,
                                onSelectFilter = viewModel::setHistoryFilter,
                                onOpenRecord = viewModel::openHistoryRecord,
                                onCloseRecord = viewModel::closeHistoryRecord,
                                onDeleteRecord = viewModel::deleteHistoryRecord,
                                onClearAllHistory = viewModel::clearAllHistory,
                                onNavigateToClassify = { navigateToTab(ROUTE_CLASSIFY) },
                                onExportReport = exportReport,
                            )
                        }
                        composable(ROUTE_INFO) {
                            ModelInfoScreen(
                                metricsState = metricsState,
                                onRefreshMetrics = viewModel::refreshBackendStatus,
                                onOpenServerDialog = { showServerDialog = true },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showServerDialog) {
        ServerConfigDialog(
            currentEngine = inferenceEngine,
            currentUrl = serverUrl,
            backendStatus = backendStatus,
            onDismiss = { showServerDialog = false },
            onSaveConfig = viewModel::updateInferenceConfig,
            onRefreshHealth = viewModel::refreshBackendStatus,
        )
    }
}
