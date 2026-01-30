package dev.keslorod.quickexpense

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.keslorod.quickexpense.ui.theme.QuickExpenseTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.keslorod.quickexpense.ui.main.MainScreen
import dev.keslorod.quickexpense.ui.manage.ListScreenMode
import dev.keslorod.quickexpense.ui.manage.ManageCategoriesScreen
import dev.keslorod.quickexpense.ui.manage.ManageSourcesScreen
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
        if (BuildConfig.DEBUG) {
            Log.d("language main screen", resources.configuration.locales[0].toLanguageTag())
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
                onBack = { nav.popBackStack() },
                nav = nav
            )
        }
        composable("manage_sources") {
            ManageSourcesScreen(
                app = app, onBack = { nav.popBackStack() }
            )
        }
        composable("manage_categories") {
            ManageCategoriesScreen(
                app = app, onBack = { nav.popBackStack() }
            )
        }
        composable("select_category") {
            ManageCategoriesScreen(
                app = app,
                mode = ListScreenMode.SELECT,
                onSelectCategory = { category ->
                    // Возвращаем в QuickInputActivity
                    nav.previousBackStackEntry?.savedStateHandle?.set("selected_category", category.id)
                    nav.popBackStack()
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable("select_source") {
            ManageSourcesScreen(
                app = app,
                mode = ListScreenMode.SELECT,
                onSelectSource = { source ->
                    // Возвращаем в QuickInputActivity
                    nav.previousBackStackEntry?.savedStateHandle?.set("selected_source", source.id)
                    nav.popBackStack()
                },
                onBack = { nav.popBackStack() }
            )
        }

    }
}