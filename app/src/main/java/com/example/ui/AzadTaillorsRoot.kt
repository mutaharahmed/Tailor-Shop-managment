package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.data.local.entity.MeasurementUnit
import com.example.data.preferences.MeasurementUnitPref
import com.example.data.preferences.ShopSettings
import com.example.ui.navigation.BackupRoute
import com.example.ui.navigation.CustomerDetailRoute
import com.example.ui.navigation.CustomerFormRoute
import com.example.ui.navigation.CustomersRoute
import com.example.ui.navigation.DashboardRoute
import com.example.ui.navigation.ExpensesRoute
import com.example.ui.navigation.InventoryRoute
import com.example.ui.navigation.MeasurementEditorRoute
import com.example.ui.navigation.MoreRoute
import com.example.ui.navigation.OrderDetailRoute
import com.example.ui.navigation.OrderFormRoute
import com.example.ui.navigation.OrdersRoute
import com.example.ui.navigation.ProductDetailRoute
import com.example.ui.navigation.ReceiptRoute
import com.example.ui.navigation.ReportsRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.screens.BackupAndRecoveryScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomerFormScreen
import com.example.ui.screens.CustomersListScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.InventoryListScreen
import com.example.ui.screens.MeasurementEditorScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.OrderFormScreen
import com.example.ui.screens.OrdersListScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ReceiptScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.CustomersViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.FinanceAndBackupViewModel
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.MainSettingsViewModel
import com.example.ui.viewmodel.OrdersViewModel
import kotlinx.coroutines.launch

private data class TopLevelDestination(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun AzadTaillorsRoot(
    settings: ShopSettings,
    settingsViewModel: MainSettingsViewModel,
    dashboardViewModel: DashboardViewModel,
    customersViewModel: CustomersViewModel,
    ordersViewModel: OrdersViewModel,
    inventoryViewModel: InventoryViewModel,
    financeBackupViewModel: FinanceAndBackupViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showMessage(msg: String) {
        scope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    val topDestinations = remember {
        listOf(
            TopLevelDestination(
                route = DashboardRoute,
                labelRes = R.string.nav_dashboard,
                selectedIcon = Icons.Filled.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                testTag = "nav_tab_dashboard"
            ),
            TopLevelDestination(
                route = OrdersRoute,
                labelRes = R.string.nav_orders,
                selectedIcon = Icons.Filled.ContentCut,
                unselectedIcon = Icons.Outlined.ContentCut,
                testTag = "nav_tab_orders"
            ),
            TopLevelDestination(
                route = CustomersRoute,
                labelRes = R.string.nav_customers,
                selectedIcon = Icons.Filled.People,
                unselectedIcon = Icons.Outlined.People,
                testTag = "nav_tab_customers"
            ),
            TopLevelDestination(
                route = InventoryRoute,
                labelRes = R.string.nav_inventory,
                selectedIcon = Icons.Filled.Inventory2,
                unselectedIcon = Icons.Outlined.Inventory2,
                testTag = "nav_tab_inventory"
            ),
            TopLevelDestination(
                route = MoreRoute,
                labelRes = R.string.nav_more,
                selectedIcon = Icons.Filled.Menu,
                unselectedIcon = Icons.Outlined.Menu,
                testTag = "nav_tab_more"
            )
        )
    }

    val showChrome = currentDest == null ||
        currentDest.hasRoute<DashboardRoute>() ||
        currentDest.hasRoute<OrdersRoute>() ||
        currentDest.hasRoute<CustomersRoute>() ||
        currentDest.hasRoute<InventoryRoute>() ||
        currentDest.hasRoute<MoreRoute>()

    fun navigateTopLevel(dest: TopLevelDestination) {
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showChrome && !useRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        topDestinations.forEach { dest ->
                            val selected = currentDest?.hasRoute(dest.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && useRail) {
                    NavigationRail {
                        topDestinations.forEach { dest ->
                            val selected = currentDest?.hasRoute(dest.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = DashboardRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<DashboardRoute> {
                        val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                        DashboardScreen(
                            uiState = uiState,
                            onNewOrder = { navController.navigate(OrderFormRoute()) },
                            onNewCustomer = { navController.navigate(CustomerFormRoute()) },
                            onOpenOrders = { navigateTopLevel(topDestinations[1]) },
                            onOpenOrderDetail = { id -> navController.navigate(OrderDetailRoute(id)) },
                            onOpenInventory = { navigateTopLevel(topDestinations[3]) },
                            onOpenExpenses = { navController.navigate(ExpensesRoute) },
                            onOpenBackup = { navController.navigate(BackupRoute) }
                        )
                    }

                    composable<OrdersRoute> {
                        OrdersListScreen(
                            viewModel = ordersViewModel,
                            onCreateOrder = { navController.navigate(OrderFormRoute()) },
                            onOrderClick = { id -> navController.navigate(OrderDetailRoute(id)) }
                        )
                    }

                    composable<OrderDetailRoute> { entry ->
                        val route = entry.toRoute<OrderDetailRoute>()
                        OrderDetailScreen(
                            orderId = route.orderId,
                            viewModel = ordersViewModel,
                            onBack = { navController.popBackStack() },
                            onEditOrder = { id -> navController.navigate(OrderFormRoute(editOrderId = id)) },
                            onOpenCustomer = { custId -> navController.navigate(CustomerDetailRoute(custId)) },
                            onOpenReceipt = { payId -> navController.navigate(ReceiptRoute(payId)) },
                            onShowMessage = ::showMessage
                        )
                    }

                    composable<OrderFormRoute> { entry ->
                        val route = entry.toRoute<OrderFormRoute>()
                        val createdMeasurementId by entry.savedStateHandle
                            .getStateFlow<Long?>("created_measurement_id", null)
                            .collectAsStateWithLifecycle()

                        OrderFormScreen(
                            editOrderId = route.editOrderId.takeIf { it > 0L },
                            preselectedCustomerId = route.preselectedCustomerId.takeIf { it > 0L },
                            newlyCreatedMeasurementId = createdMeasurementId,
                            viewModel = ordersViewModel,
                            onBack = { navController.popBackStack() },
                            onAddMeasurementForCustomer = { custId, garmentName, baseId ->
                                navController.navigate(
                                    MeasurementEditorRoute(
                                        customerId = custId,
                                        preselectedGarmentName = garmentName,
                                        baseProfileId = baseId
                                    )
                                )
                            },
                            onOrderSaved = { orderId, msg ->
                                showMessage(msg)
                                navController.popBackStack()
                                if (route.editOrderId <= 0L) {
                                    navController.navigate(OrderDetailRoute(orderId))
                                }
                            }
                        )
                    }

                    composable<ReceiptRoute> { entry ->
                        val route = entry.toRoute<ReceiptRoute>()
                        ReceiptScreen(
                            paymentId = route.paymentId,
                            shopSettings = settings,
                            viewModel = ordersViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<CustomersRoute> {
                        CustomersListScreen(
                            viewModel = customersViewModel,
                            onAddCustomer = { navController.navigate(CustomerFormRoute()) },
                            onCustomerClick = { id -> navController.navigate(CustomerDetailRoute(id)) }
                        )
                    }

                    composable<CustomerFormRoute> { entry ->
                        val route = entry.toRoute<CustomerFormRoute>()
                        CustomerFormScreen(
                            editCustomerId = route.editCustomerId.takeIf { it > 0L },
                            viewModel = customersViewModel,
                            onBack = { navController.popBackStack() },
                            onSaved = { id, msg ->
                                showMessage(msg)
                                navController.popBackStack()
                                if (route.editCustomerId <= 0L) {
                                    navController.navigate(CustomerDetailRoute(id))
                                }
                            }
                        )
                    }

                    composable<CustomerDetailRoute> { entry ->
                        val route = entry.toRoute<CustomerDetailRoute>()
                        CustomerDetailScreen(
                            customerId = route.customerId,
                            viewModel = customersViewModel,
                            onBack = { navController.popBackStack() },
                            onEditCustomer = { id -> navController.navigate(CustomerFormRoute(editCustomerId = id)) },
                            onNewOrderForCustomer = { id -> navController.navigate(OrderFormRoute(preselectedCustomerId = id)) },
                            onOpenOrderDetail = { orderId -> navController.navigate(OrderDetailRoute(orderId)) },
                            onNewMeasurement = { custId, garment, baseId ->
                                navController.navigate(
                                    MeasurementEditorRoute(
                                        customerId = custId,
                                        preselectedGarmentName = garment,
                                        baseProfileId = baseId
                                    )
                                )
                            },
                            onOpenReceipt = { payId -> navController.navigate(ReceiptRoute(payId)) }
                        )
                    }

                    composable<MeasurementEditorRoute> { entry ->
                        val route = entry.toRoute<MeasurementEditorRoute>()
                        val defUnit = if (settings.defaultUnit == MeasurementUnitPref.CENTIMETERS) {
                            MeasurementUnit.CENTIMETERS
                        } else {
                            MeasurementUnit.INCHES
                        }
                        MeasurementEditorScreen(
                            customerId = route.customerId,
                            preselectedGarmentName = route.preselectedGarmentName,
                            baseProfileId = route.baseProfileId,
                            defaultUnit = defUnit,
                            viewModel = customersViewModel,
                            onBack = { navController.popBackStack() },
                            onSaved = { measurementId, msg ->
                                showMessage(msg)
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("created_measurement_id", measurementId)
                                navController.popBackStack()
                            }
                        )
                    }

                    composable<InventoryRoute> {
                        InventoryListScreen(
                            viewModel = inventoryViewModel,
                            onProductClick = { id -> navController.navigate(ProductDetailRoute(id)) },
                            onShowMessage = ::showMessage
                        )
                    }

                    composable<ProductDetailRoute> { entry ->
                        val route = entry.toRoute<ProductDetailRoute>()
                        ProductDetailScreen(
                            productId = route.productId,
                            viewModel = inventoryViewModel,
                            onBack = { navController.popBackStack() },
                            onShowMessage = ::showMessage
                        )
                    }

                    composable<MoreRoute> {
                        MoreHubScreen(
                            settings = settings,
                            onOpenExpenses = { navController.navigate(ExpensesRoute) },
                            onOpenReports = { navController.navigate(ReportsRoute) },
                            onOpenBackup = { navController.navigate(BackupRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onToggleLanguageQuick = { lang -> settingsViewModel.setLanguage(lang) }
                        )
                    }

                    composable<ExpensesRoute> {
                        ExpensesScreen(
                            viewModel = financeBackupViewModel,
                            onBack = { navController.popBackStack() },
                            onShowMessage = ::showMessage
                        )
                    }

                    composable<ReportsRoute> {
                        ReportsScreen(
                            viewModel = financeBackupViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<BackupRoute> {
                        BackupAndRecoveryScreen(
                            viewModel = financeBackupViewModel,
                            settingsViewModel = settingsViewModel,
                            settings = settings,
                            onBack = { navController.popBackStack() },
                            onShowMessage = ::showMessage
                        )
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(
                            settings = settings,
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            onShowMessage = ::showMessage
                        )
                    }
                }
            }
        }
    }
}
