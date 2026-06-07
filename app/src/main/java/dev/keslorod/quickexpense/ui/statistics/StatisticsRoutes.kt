package dev.keslorod.quickexpense.ui.statistics

object StatisticsRoutes {
    const val ROOT = "statistics"
    const val DASHBOARD = "statistics/dashboard"
    const val MERCHANTS = "statistics/merchants"
    const val CATEGORIES = "statistics/categories"
    const val TAGS = "statistics/tags"
    const val MERCHANT_DETAILS = "statistics/merchants/{merchantId}"
    const val CATEGORY_DETAILS = "statistics/categories/{categoryId}"
    const val TAG_DETAILS = "statistics/tags/{tagId}"
    const val SEARCH = "statistics/search"
    const val SEARCH_RESULTS = "statistics/search/results"
    const val TRANSACTION_DETAILS = "statistics/transactions/{expenseId}"
}
