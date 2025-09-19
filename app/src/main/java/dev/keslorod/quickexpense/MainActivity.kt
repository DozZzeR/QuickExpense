package dev.keslorod.quickexpense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.keslorod.quickexpense.ui.theme.QuickExpenseTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.keslorod.quickexpense.ui.main.MainScreen
import dev.keslorod.quickexpense.ui.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    private val app by lazy { application as App }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickExpenseTheme {
                AppNav(app)
            }
        }
    }
}

@Composable
private fun AppNav(app: App, nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = "main") {
        composable("main") {
            MainScreen(
                onOpenSettings = { nav.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                app = app,
                onBack = { nav.popBackStack() }
            )
        }
    }
}