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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import dev.keslorod.quickexpense.ui.main.MainScreen
import dev.keslorod.quickexpense.ui.manage.ListScreenMode
import dev.keslorod.quickexpense.ui.manage.ManageCategoriesScreen
import dev.keslorod.quickexpense.ui.manage.ManageMerchantsScreen
import dev.keslorod.quickexpense.ui.manage.ManageSourcesScreen
import dev.keslorod.quickexpense.ui.manage.ManageTagsScreen
import dev.keslorod.quickexpense.ui.statistics.StatisticsRoutes
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
                onOpenSplit = { expenseId -> nav.navigate("split_editor/$expenseId") },
                onOpenStatistics = { nav.navigate("statistics") }
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
        navigation(startDestination = StatisticsRoutes.DASHBOARD, route = StatisticsRoutes.ROOT) {
            composable(StatisticsRoutes.DASHBOARD) { backStackEntry ->
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                
                dev.keslorod.quickexpense.ui.statistics.StatisticsDashboardScreen(
                    filterViewModel = filterViewModel,
                    onBack = { nav.popBackStack() },
                    onViewMerchants = { nav.navigate(StatisticsRoutes.MERCHANTS) },
                    onViewCategories = { nav.navigate(StatisticsRoutes.CATEGORIES) },
                    onViewTags = { nav.navigate(StatisticsRoutes.TAGS) },
                    onAdvancedSearch = { nav.navigate(StatisticsRoutes.SEARCH) },
                    onMerchantClick = { nav.navigate("statistics/merchants/$it") },
                    onCategoryClick = { nav.navigate("statistics/categories/$it") },
                    onTagClick = { nav.navigate("statistics/tags/$it") }
                )
            }
            composable(StatisticsRoutes.MERCHANTS) {
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                val listViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsListViewModel = viewModel(factory = dev.keslorod.quickexpense.ui.statistics.StatisticsListViewModelFactory(
                    application = app,
                    filterViewModel = filterViewModel,
                    type = dev.keslorod.quickexpense.ui.statistics.StatsListType.MERCHANTS
                ))
                val items by listViewModel.items.collectAsState()
                val currency by listViewModel.currency.collectAsState()
                
                dev.keslorod.quickexpense.ui.statistics.StatisticsListScreen(
                    title = stringResource(R.string.merchants),
                    filterViewModel = filterViewModel,
                    items = items,
                    currency = currency,
                    onBack = { nav.popBackStack() },
                    onItemClick = { nav.navigate("statistics/merchants/$it") }
                )
            }
            composable(StatisticsRoutes.CATEGORIES) {
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                val listViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsListViewModel = viewModel(factory = dev.keslorod.quickexpense.ui.statistics.StatisticsListViewModelFactory(
                    application = app,
                    filterViewModel = filterViewModel,
                    type = dev.keslorod.quickexpense.ui.statistics.StatsListType.CATEGORIES
                ))
                val items by listViewModel.items.collectAsState()
                val currency by listViewModel.currency.collectAsState()
                
                dev.keslorod.quickexpense.ui.statistics.StatisticsListScreen(
                    title = stringResource(R.string.categories),
                    filterViewModel = filterViewModel,
                    items = items,
                    currency = currency,
                    onBack = { nav.popBackStack() },
                    onItemClick = { nav.navigate("statistics/categories/$it") }
                )
            }
            composable(StatisticsRoutes.TAGS) {
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                val listViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsListViewModel = viewModel(factory = dev.keslorod.quickexpense.ui.statistics.StatisticsListViewModelFactory(
                    application = app,
                    filterViewModel = filterViewModel,
                    type = dev.keslorod.quickexpense.ui.statistics.StatsListType.TAGS
                ))
                val items by listViewModel.items.collectAsState()
                val currency by listViewModel.currency.collectAsState()
                
                dev.keslorod.quickexpense.ui.statistics.StatisticsListScreen(
                    title = stringResource(R.string.tags),
                    filterViewModel = filterViewModel,
                    items = items,
                    currency = currency,
                    showPercent = false,
                    onBack = { nav.popBackStack() },
                    onItemClick = { nav.navigate("statistics/tags/$it") }
                )
            }
            composable(StatisticsRoutes.MERCHANT_DETAILS) { backStackEntry ->
                val merchantId = backStackEntry.arguments?.getString("merchantId") ?: return@composable
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                
                dev.keslorod.quickexpense.ui.statistics.MerchantDetailsScreen(
                    merchantId = merchantId,
                    filterViewModel = filterViewModel,
                    onBack = { nav.popBackStack() },
                    onTransactionClick = { nav.navigate("statistics/transactions/$it") }
                )
            }
            composable(StatisticsRoutes.CATEGORY_DETAILS) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: return@composable
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                
                dev.keslorod.quickexpense.ui.statistics.CategoryDetailsScreen(
                    categoryId = categoryId,
                    filterViewModel = filterViewModel,
                    onBack = { nav.popBackStack() },
                    onTransactionClick = { nav.navigate("statistics/transactions/$it") }
                )
            }
            composable(StatisticsRoutes.TAG_DETAILS) { backStackEntry ->
                val tagId = backStackEntry.arguments?.getString("tagId") ?: return@composable
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                
                dev.keslorod.quickexpense.ui.statistics.TagDetailsScreen(
                    tagId = tagId,
                    filterViewModel = filterViewModel,
                    onBack = { nav.popBackStack() },
                    onTransactionClick = { nav.navigate("statistics/transactions/$it") }
                )
            }
            composable(StatisticsRoutes.SEARCH) {
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                
                dev.keslorod.quickexpense.ui.statistics.AdvancedSearchScreen(
                    filterViewModel = filterViewModel,
                    onBack = { nav.popBackStack() },
                    onSearch = { nav.navigate(StatisticsRoutes.SEARCH_RESULTS) }
                )
            }
            composable(StatisticsRoutes.TRANSACTION_DETAILS) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable
                dev.keslorod.quickexpense.ui.statistics.TransactionDetailsScreen(
                    expenseId = expenseId,
                    onBack = { nav.popBackStack() },
                    onEditTransaction = { /* TODO */ },
                    onEditSplit = { nav.navigate("split_editor/$it") }
                )
            }
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
            composable(StatisticsRoutes.MERCHANTS) {
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                val listViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsListViewModel = viewModel(factory = dev.keslorod.quickexpense.ui.statistics.StatisticsListViewModelFactory(
                    application = app,
                    filterViewModel = filterViewModel,
                    type = dev.keslorod.quickexpense.ui.statistics.StatsListType.MERCHANTS
                ))
                val items by listViewModel.items.collectAsState()
                val currency by listViewModel.currency.collectAsState()
                
                dev.keslorod.quickexpense.ui.statistics.StatisticsListScreen(
                    title = stringResource(R.string.merchants),
                    filterViewModel = filterViewModel,
                    items = items,
                    currency = currency,
                    onBack = { nav.popBackStack() },
                    onItemClick = { nav.navigate("statistics/merchants/$it") }
                )
            }
            composable(StatisticsRoutes.CATEGORIES) {
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                val listViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsListViewModel = viewModel(factory = dev.keslorod.quickexpense.ui.statistics.StatisticsListViewModelFactory(
                    application = app,
                    filterViewModel = filterViewModel,
                    type = dev.keslorod.quickexpense.ui.statistics.StatsListType.CATEGORIES
                ))
                val items by listViewModel.items.collectAsState()
                val currency by listViewModel.currency.collectAsState()
                
                dev.keslorod.quickexpense.ui.statistics.StatisticsListScreen(
                    title = stringResource(R.string.categories),
                    filterViewModel = filterViewModel,
                    items = items,
                    currency = currency,
                    onBack = { nav.popBackStack() },
                    onItemClick = { nav.navigate("statistics/categories/$it") }
                )
            }
            composable(StatisticsRoutes.TAGS) {
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                val listViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsListViewModel = viewModel(factory = dev.keslorod.quickexpense.ui.statistics.StatisticsListViewModelFactory(
                    application = app,
                    filterViewModel = filterViewModel,
                    type = dev.keslorod.quickexpense.ui.statistics.StatsListType.TAGS
                ))
                val items by listViewModel.items.collectAsState()
                val currency by listViewModel.currency.collectAsState()
                
                dev.keslorod.quickexpense.ui.statistics.StatisticsListScreen(
                    title = stringResource(R.string.tags),
                    filterViewModel = filterViewModel,
                    items = items,
                    currency = currency,
                    showPercent = false,
                    onBack = { nav.popBackStack() },
                    onItemClick = { nav.navigate("statistics/tags/$it") }
                )
            }
            composable(StatisticsRoutes.MERCHANT_DETAILS) { backStackEntry ->
                val merchantId = backStackEntry.arguments?.getString("merchantId") ?: return@composable
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                
                dev.keslorod.quickexpense.ui.statistics.MerchantDetailsScreen(
                    merchantId = merchantId,
                    filterViewModel = filterViewModel,
                    onBack = { nav.popBackStack() },
                    onTransactionClick = { nav.navigate("statistics/transactions/$it") }
                )
            }
            composable(StatisticsRoutes.CATEGORY_DETAILS) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: return@composable
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                
                dev.keslorod.quickexpense.ui.statistics.CategoryDetailsScreen(
                    categoryId = categoryId,
                    filterViewModel = filterViewModel,
                    onBack = { nav.popBackStack() },
                    onTransactionClick = { nav.navigate("statistics/transactions/$it") }
                )
            }
            composable(StatisticsRoutes.TAG_DETAILS) { backStackEntry ->
                val tagId = backStackEntry.arguments?.getString("tagId") ?: return@composable
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                
                dev.keslorod.quickexpense.ui.statistics.TagDetailsScreen(
                    tagId = tagId,
                    filterViewModel = filterViewModel,
                    onBack = { nav.popBackStack() },
                    onTransactionClick = { nav.navigate("statistics/transactions/$it") }
                )
            }
            composable(StatisticsRoutes.SEARCH) {
                val parentEntry = remember(nav) { nav.getBackStackEntry(StatisticsRoutes.ROOT) }
                val filterViewModel: dev.keslorod.quickexpense.ui.statistics.StatisticsFilterViewModel = viewModel(parentEntry)
                
                dev.keslorod.quickexpense.ui.statistics.AdvancedSearchScreen(
                    filterViewModel = filterViewModel,
                    onBack = { nav.popBackStack() },
                    onSearch = { nav.navigate(StatisticsRoutes.SEARCH_RESULTS) }
                )
            }
            composable(StatisticsRoutes.TRANSACTION_DETAILS) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable
                dev.keslorod.quickexpense.ui.statistics.TransactionDetailsScreen(
                    expenseId = expenseId,
                    onBack = { nav.popBackStack() },
                    onEditTransaction = { /* TODO */ },
                    onEditSplit = { nav.navigate("split_editor/$it") }
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