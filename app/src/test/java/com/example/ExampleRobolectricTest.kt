package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AppLanguage
import com.example.util.createLocalizedContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches Azad Taillors in English and Sindhi`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Azad Taillors", context.getString(R.string.app_name))

        val sindhiContext = context.createLocalizedContext(AppLanguage.SINDHI)
        assertEquals("آزاد ٽيلرز", sindhiContext.getString(R.string.app_name))
        assertEquals("آرڊر", sindhiContext.getString(R.string.nav_orders))
    }

    @Test
    fun `no internet permission declared in manifest`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS
        )
        val perms = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertTrue(
            "Strictly offline app must not request INTERNET permission",
            !perms.contains(android.Manifest.permission.INTERNET)
        )
    }

    @Test
    fun `dashboard renders and bottom navigation switches screens and language toggles`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()

        // Navigate to Customers tab
        composeRule.onNodeWithTag("nav_tab_customers").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_customer_fab").assertIsDisplayed()

        // Navigate to More tab and test Sindhi instant switch
        composeRule.onNodeWithTag("nav_tab_more").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("more_hub_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("lang_chip_sd").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("آرڊر")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("آرڊر").assertIsDisplayed()

        // Switch back to English
        composeRule.onNodeWithTag("lang_chip_en").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Orders")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Orders").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "night")
    fun `app stays light even when system night mode qualifier is active`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        assertEquals(Configuration.UI_MODE_NIGHT_YES, nightMode)

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
    }
}
