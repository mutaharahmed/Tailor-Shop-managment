package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.AppLanguage
import com.example.ui.AzadTaillorsRoot
import com.example.ui.theme.AzadTaillorsTheme
import com.example.ui.viewmodel.AppViewModelFactory
import com.example.ui.viewmodel.CustomersViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.FinanceAndBackupViewModel
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.MainSettingsViewModel
import com.example.ui.viewmodel.OrdersViewModel
import com.example.util.ProvideLocalizedApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Strict Light-Only System Bars — never switch to dark mode
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.WHITE,
                Color.WHITE
            )
        )

        val app = application as AzadTaillorsApp
        val factory = AppViewModelFactory(app.container)

        setContent {
            val settingsVm: MainSettingsViewModel = viewModel(factory = factory)
            val dashboardVm: DashboardViewModel = viewModel(factory = factory)
            val customersVm: CustomersViewModel = viewModel(factory = factory)
            val ordersVm: OrdersViewModel = viewModel(factory = factory)
            val inventoryVm: InventoryViewModel = viewModel(factory = factory)
            val financeBackupVm: FinanceAndBackupViewModel = viewModel(factory = factory)

            val settings by settingsVm.settings.collectAsStateWithLifecycle()

            ProvideLocalizedApp(language = settings.language) {
                AzadTaillorsTheme(isSindhi = settings.language == AppLanguage.SINDHI) {
                    AzadTaillorsRoot(
                        settings = settings,
                        settingsViewModel = settingsVm,
                        dashboardViewModel = dashboardVm,
                        customersViewModel = customersVm,
                        ordersViewModel = ordersVm,
                        inventoryViewModel = inventoryVm,
                        financeBackupViewModel = financeBackupVm
                    )
                }
            }
        }
    }
}
