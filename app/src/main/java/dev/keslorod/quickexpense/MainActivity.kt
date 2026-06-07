package dev.keslorod.quickexpense

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.keslorod.quickexpense.ui.theme.QuickExpenseTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.keslorod.quickexpense.ui.main.MainScreen
import dev.keslorod.quickexpense.ui.manage.ListScreenMode
import dev.keslorod.quickexpense.ui.manage.ManageCategoriesScreen
import dev.keslorod.quickexpense.ui.manage.ManageMerchantsScreen
import dev.keslorod.quickexpense.ui.manage.ManageSourcesScreen
import dev.keslorod.quickexpense.ui.manage.ManageTagsScreen
import dev.keslorod.quickexpense.ui.split.SplitEditorScreen
import dev.keslorod.quickexpense.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    NavHost(navController = nav, startDestination = "main") {
        composable("main") {
            MainScreen(
                onOpenSettings = { nav.navigate("settings") },
                onOpenSplit = { expenseId -> nav.navigate("split_editor/$expenseId") }
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
        composable("manage_merchants") {
            ManageMerchantsScreen(
                app = app, onBack = { nav.popBackStack() }
            )
        }
        composable("manage_tags") {
            ManageTagsScreen(
                app = app, onBack = { nav.popBackStack() }
            )
        }
        composable("split_editor/{expenseId}") { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable
            val amount = remember { mutableStateOf<Long?>(null) }
            val currency = remember { mutableStateOf<String?>(null) }
            val label = remember { mutableStateOf<String?>(null) }

            LaunchedEffect(expenseId) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val expense = app.db.expenses().getById(expenseId)
                    if (expense != null) {
                        amount.value = expense.amount
                        currency.value = expense.currency
                        label.value = "Транзакция"
                    }
                }
            }

            if (amount.value != null && currency.value != null) {
                SplitEditorScreen(
                    app = app,
                    expenseId = expenseId,
                    totalAmount = amount.value!!,
                    currency = currency.value!!,
                    initialLabel = label.value ?: "Транзакция",
                    onBack = { nav.popBackStack() },
                    onDone = { nodes, tags ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val exp = app.db.expenses().getById(expenseId)
                            if (exp != null) {
                                val anyChildHasCategory = nodes.any { it.parentId == null && it.categoryId != null }
                                if (anyChildHasCategory) {
                                    app.db.expenses().update(exp.copy(categoryId = "unsorted"))
                                }
                            }

                            app.db.splitNodes().deleteByExpenseId(expenseId)
                            nodes.forEach { node ->
                                val toSave = node.copy(expenseId = expenseId)
                                app.db.splitNodes().insert(toSave)
                                tags[node.id]?.forEach { tag ->
                                    app.db.splitNodeTags().insert(dev.keslorod.quickexpense.data.entities.SplitNodeTag(toSave.id, tag.id))
                                }
                            }
                            nav.popBackStack()
                        }
                    }
                )
            }
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