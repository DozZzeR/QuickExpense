# QuickExpense — Statistics / Analytics Flow Specification

## Status

Draft specification for implementation.

## Source context

This document extends the existing QuickExpense expense split/detailization concept.

Related existing feature specification:

- `Expense_Split_Flow.md`

Known current project structure from repository inspection:

```text
app/src/main/java/dev/keslorod/quickexpense/
├─ data/
│  ├─ dao/
│  ├─ db/
│  ├─ dto/
│  ├─ entities/
│  └─ prefs/
├─ domain/
│  ├─ DateRanges.kt
│  └─ Money.kt
├─ export/
├─ receipt/
├─ ui/
│  ├─ main/
│  ├─ manage/
│  ├─ quickinput/
│  ├─ settings/
│  ├─ split/
│  ├─ theme/
│  └─ widget/
├─ App.kt
└─ MainActivity.kt
```

Recommended new feature packages:

```text
app/src/main/java/dev/keslorod/quickexpense/
├─ domain/
│  ├─ statistics/
│  │  ├─ StatisticsDateRange.kt
│  │  ├─ StatisticsPeriodComparison.kt
│  │  ├─ StatisticsAggregation.kt
│  │  ├─ StatisticsFilter.kt
│  │  ├─ StatisticsSearchFilter.kt
│  │  ├─ StatisticsModels.kt
│  │  └─ StatisticsRules.kt
│  └─ ...
├─ data/
│  ├─ dao/
│  │  ├─ StatisticsDao.kt
│  │  └─ ...
│  ├─ dto/
│  │  ├─ StatisticsRows.kt
│  │  └─ ...
│  └─ ...
└─ ui/
   ├─ statistics/
   │  ├─ StatisticsNavGraph.kt
   │  ├─ StatisticsRoutes.kt
   │  ├─ StatisticsFilterViewModel.kt
   │  ├─ StatisticsDashboardScreen.kt
   │  ├─ StatisticsDashboardViewModel.kt
   │  ├─ StatisticsListScreen.kt
   │  ├─ StatisticsListViewModel.kt
   │  ├─ MerchantDetailsScreen.kt
   │  ├─ CategoryDetailsScreen.kt
   │  ├─ TagDetailsScreen.kt
   │  ├─ TransactionDetailsScreen.kt
   │  ├─ AdvancedSearchScreen.kt
   │  ├─ AdvancedSearchViewModel.kt
   │  ├─ SearchResultsScreen.kt
   │  └─ components/
   │     ├─ DateRangeFilterBar.kt
   │     ├─ PeriodComparisonSummary.kt
   │     ├─ AmountSummaryCard.kt
   │     ├─ StatsPieCard.kt
   │     ├─ StatsRelativeBarRow.kt
   │     ├─ StatsList.kt
   │     ├─ TransactionResultRow.kt
   │     ├─ SplitItemResultRow.kt
   │     └─ EmptyStatisticsState.kt
   └─ ...
```

---

# 1. Product goal

The goal is to add a statistics and analytics section to QuickExpense without damaging the core product principle:

> Fast expense input first, detailed analysis later.

The quick-add screen must remain fast and clean. Statistics must be accessible from the main expense list/history screen and must reuse existing expense, merchant, category, tag, split, and receipt concepts.

Statistics should answer practical user questions:

- Where did my money go during this period?
- Which merchants take the most money?
- Which categories dominate my expenses?
- Which tags explain context, events, warranty lookup, people, trips, guests, gifts, etc.?
- What changed compared with the previous equivalent period?
- Which exact transactions or split items produced a number?
- Can I jump from a statistic to the purchase details and then edit the transaction or split tree?

---

# 2. Core UX principles

## 2.1 Statistics is analysis, not editing

Statistics screens should primarily explain data. Editing must be available, but not be the default accidental action.

Preferred flow:

```text
Statistic / Search result
  -> TransactionDetailsScreen
    -> EditTransactionScreen
    -> SplitEditorScreen
```

Do not open edit mode directly from statistics rows unless the row has a clearly labeled secondary action.

Reason:

- The user often wants to inspect the purchase before changing it.
- Statistics is a read/analysis mode.
- Transaction details can later show receipt photo, split tree, notes, warranty-related tags, and other metadata.

## 2.2 Categories and merchants can be pies

Use pie charts only for mutually exclusive or mostly mutually exclusive totals:

- categories;
- merchants.

Do not use pie charts for tags.

Reason:

- Category totals are intended to be non-overlapping budget buckets.
- Merchant totals are one merchant per expense.
- Tags are overlapping contextual labels. One split item may have multiple tags, and therefore tag totals may exceed total spending.

## 2.3 Tags are ranked lists, not pies

Tags must be shown as a list/table/bar ranking.

Tag total behavior:

```text
Beer 1,200 RSD with tags #beer #guests #leha

#beer   += 1,200
#guests += 1,200
#leha   += 1,200
```

This is intentional.

The UI must not imply that tag totals are part-to-whole.

## 2.4 Every amount summary should support comparison with previous period

Wherever a screen displays a total amount for the selected period, it should also display comparison with the previous equivalent period.

Examples:

```text
This period: 120,000 RSD
Previous period: 95,000 RSD
+25,000 RSD · +26.3%
```

This applies to:

- dashboard total spent;
- merchant list total;
- category list total;
- tag list total;
- merchant details total;
- category details total;
- tag details total;
- advanced search result total;
- transaction/split-item search result total;
- any future screen that shows a period result amount.

This comparison must be derived from the same `DateRangeFilterState`, not implemented separately per screen like a herd of caffeinated cockroaches.

---

# 3. Entry point

## 3.1 Main expense list / history screen

Add entry to statistics from the main expense list screen.

Possible UI options:

```text
[+ Add] [Search] [Statistics]
```

or top app bar action:

```text
Expense list
  top-right: chart / analytics icon
```

or bottom navigation if the app already grows into several major sections:

```text
Quick Add | History | Statistics | Settings
```

MVP recommendation:

- Add a clear `Statistics` action to the main expense list/history screen.
- Do not introduce full bottom navigation only for this feature unless the app already needs it.

---

# 4. Navigation structure

## 4.1 Recommended route tree

```text
ExpenseListScreen
 └─ StatisticsNavGraph
     ├─ StatisticsDashboardScreen
     │   ├─ MerchantStatsListScreen
     │   │   └─ MerchantDetailsScreen
     │   │       └─ TransactionDetailsScreen
     │   │           ├─ EditTransactionScreen
     │   │           └─ SplitEditorScreen
     │   │
     │   ├─ CategoryStatsListScreen
     │   │   └─ CategoryDetailsScreen
     │   │       └─ TransactionDetailsScreen
     │   │           ├─ EditTransactionScreen
     │   │           └─ SplitEditorScreen
     │   │
     │   ├─ TagStatsListScreen
     │   │   └─ TagDetailsScreen
     │   │       └─ TransactionDetailsScreen
     │   │           ├─ EditTransactionScreen
     │   │           └─ SplitEditorScreen
     │   │
     │   └─ AdvancedSearchScreen
     │       └─ SearchResultsScreen
     │           └─ TransactionDetailsScreen
     │               ├─ EditTransactionScreen
     │               └─ SplitEditorScreen
```

## 4.2 Suggested route constants

```kotlin
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
```

If the app already uses another route naming convention, adapt names but keep the hierarchy.

---

# 5. Shared date filter

## 5.1 Requirement

All statistics screens must share one logical date filter state.

The filter must not reset when the user navigates between:

```text
Dashboard -> Tags -> Tag details -> Transaction details -> Back
Dashboard -> Merchants -> Merchant details -> Back
Dashboard -> Search -> Results -> Details -> Back
```

## 5.2 Android / Compose implementation approach

In Android, the UI component can be re-created on each screen. What must be shared is not the physical composable instance, but the state.

Use a `StatisticsFilterViewModel` scoped to the statistics navigation graph.

Concept:

```kotlin
data class StatisticsDateRangeState(
    val preset: StatisticsDatePreset,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val comparisonEnabled: Boolean = true,
)
```

```kotlin
enum class StatisticsDatePreset {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    LAST_7_DAYS,
    LAST_30_DAYS,
    CUSTOM,
    ALL_TIME,
}
```

```kotlin
class StatisticsFilterViewModel : ViewModel() {
    private val _state = MutableStateFlow(defaultStatisticsDateRange())
    val state: StateFlow<StatisticsDateRangeState> = _state.asStateFlow()

    fun setPreset(preset: StatisticsDatePreset) { ... }
    fun setCustomRange(start: LocalDate, end: LocalDate) { ... }
    fun setComparisonEnabled(enabled: Boolean) { ... }
}
```

Each statistics screen renders:

```kotlin
DateRangeFilterBar(
    state = filterState,
    onPresetSelected = filterViewModel::setPreset,
    onCustomRangeSelected = filterViewModel::setCustomRange,
)
```

The `StatisticsFilterViewModel` must be obtained from the parent navigation graph back stack entry, not created independently for each screen.

Pseudo-code:

```kotlin
val parentEntry = remember(navController) {
    navController.getBackStackEntry(StatisticsRoutes.ROOT)
}
val filterViewModel: StatisticsFilterViewModel = viewModel(parentEntry)
```

## 5.3 Default period

Recommended default:

```text
This month
```

Reason:

- Expenses are naturally reviewed by month.
- It makes previous-period comparison intuitive.

Alternative:

```text
Last 30 days
```

But for personal finance, calendar month is usually clearer.

## 5.4 Date boundary semantics

Use inclusive local dates in domain/UI:

```kotlin
startDate: LocalDate
endDate: LocalDate
```

In database queries convert to half-open instant/date boundary:

```text
transaction.date >= startDate
transaction.date < endDate + 1 day
```

If expense date is stored as `LocalDate`, use:

```sql
WHERE date >= :startDate AND date <= :endDate
```

Keep this consistent everywhere.

---

# 6. Previous period comparison

## 6.1 Requirement

Every screen that shows a total amount for the active date range should show comparison against the previous equivalent period.

Example for selected range:

```text
2026-06-01 .. 2026-06-30
```

Previous equivalent range:

```text
2026-05-01 .. 2026-05-31
```

Example for custom 10-day range:

```text
Selected: 2026-06-10 .. 2026-06-19
Previous: 2026-05-31 .. 2026-06-09
```

## 6.2 Previous period calculation

```kotlin
data class StatisticsComparisonRange(
    val currentStart: LocalDate,
    val currentEnd: LocalDate,
    val previousStart: LocalDate?,
    val previousEnd: LocalDate?,
)
```

Rules:

### TODAY

```text
Current: today
Previous: yesterday
```

### YESTERDAY

```text
Current: yesterday
Previous: day before yesterday
```

### THIS_WEEK

Use locale-consistent week boundaries. Recommended: ISO week Monday-Sunday.

```text
Current: Monday..Sunday of current week
Previous: Monday..Sunday of previous week
```

### THIS_MONTH

```text
Current: first day of current month..last day of current month
Previous: first day of previous month..last day of previous month
```

### THIS_YEAR

```text
Current: Jan 1..Dec 31 of current year
Previous: Jan 1..Dec 31 of previous year
```

### LAST_7_DAYS

```text
Current: last 7 days including today
Previous: 7 days before that
```

### LAST_30_DAYS

```text
Current: last 30 days including today
Previous: 30 days before that
```

### CUSTOM

Use same number of days directly before the selected range.

```text
periodLength = daysBetween(start, end) + 1
previousEnd = start - 1 day
previousStart = previousEnd - periodLength + 1 day
```

### ALL_TIME

Comparison disabled.

Reason:

- There is no meaningful previous equivalent all-time period unless the app stores a known first transaction date and wants to invent a comparison. Do not invent. Фантазии оставим маркетологам.

## 6.3 Comparison output model

```kotlin
data class PeriodComparisonSummary(
    val currentAmount: Money,
    val previousAmount: Money?,
    val absoluteDelta: Money?,
    val relativeDeltaPercent: Double?,
    val direction: ComparisonDirection,
    val label: String,
)

enum class ComparisonDirection {
    UP,
    DOWN,
    SAME,
    NO_PREVIOUS_DATA,
    DISABLED,
}
```

Calculation:

```text
absoluteDelta = currentAmount - previousAmount
```

```text
relativeDeltaPercent =
  if previousAmount == 0 and currentAmount == 0 -> 0
  if previousAmount == 0 and currentAmount > 0 -> null / "new spending"
  else (currentAmount - previousAmount) / previousAmount * 100
```

UI examples:

```text
Total: 120,000 RSD
Previous period: 95,000 RSD
+25,000 RSD · +26.3%
```

```text
Total: 0 RSD
Previous period: 12,000 RSD
-12,000 RSD · -100%
```

```text
Total: 5,000 RSD
Previous period: 0 RSD
+5,000 RSD · new spending
```

```text
Total: 120,000 RSD
Comparison unavailable for all time
```

## 6.4 Meaning of good/bad direction

Do not globally color `UP` as good or bad.

For expenses:

- spending up is usually negative;
- spending down is usually positive.

For future income statistics:

- income up would be positive.

For MVP, text is enough:

```text
+26.3% vs previous period
```

Avoid strong semantic colors unless the app already has a clear convention.

---

# 7. Dashboard screen

## 7.1 Purpose

The dashboard gives a quick overview of spending for the selected period.

It should not become a full BI cockpit. We are making a personal expense tracker, not the cockpit of a morally questionable fintech drone.

## 7.2 Layout

```text
Statistics

[DateRangeFilterBar]

[Total spent card]
Current: 123,400 RSD
Previous: 95,000 RSD
+28,400 RSD · +29.9%
Transactions: 57
Average transaction: 2,164 RSD

[Pie card: Categories]
Food        42%
Transport   18%
Alcohol     13%
Other        ...
[View all categories]

[Pie card: Merchants]
Lidl        22%
Maxi        16%
Yandex       9%
Other        ...
[View all merchants]

[Tags card]
Top tags
#beer       9,000 RSD
#guests     7,500 RSD
#warranty  12,000 RSD
[View all tags]

[Advanced search]
```

## 7.3 Dashboard blocks

### Total spent card

Shows:

- current period total expense amount;
- previous period total amount;
- absolute delta;
- relative delta;
- transaction count;
- average transaction amount.

MVP should count expenses only.

### Category pie card

Shows top categories and `Other`.

Rules:

- Use category aggregation based on split-aware category totals.
- Include `Uncategorized` when no effective category exists.
- Use top N + Other.

Recommended top N:

```text
Top 5 + Other
```

### Merchant pie card

Shows top merchants and `Other`.

Rules:

- Group by expense merchant.
- Include `Unknown merchant` when missing.
- Use top N + Other.

Recommended top N:

```text
Top 5 + Other
```

### Tags preview card

Not a pie.

Shows top tags as list rows with amounts.

Must include small note:

```text
Tag totals may overlap
```

Russian copy:

```text
Суммы по меткам могут пересекаться
```

---

# 8. Universal statistics list component

## 8.1 Requirement

Use one reusable list component for:

- merchant statistics;
- category statistics;
- tag statistics.

Suggested composable:

```kotlin
@Composable
fun StatisticsListScreen(
    title: String,
    total: PeriodComparisonSummary,
    items: List<StatsListItemUi>,
    dateRangeState: StatisticsDateRangeState,
    onDateRangeChange: (...),
    onItemClick: (StatsListItemUi) -> Unit,
)
```

or split it into:

```kotlin
StatisticsListScaffold
StatsList
StatsRelativeBarRow
```

## 8.2 Stats row model

```kotlin
data class StatsListItemUi(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val amount: Money,
    val transactionCount: Int? = null,
    val itemCount: Int? = null,
    val shareOfTotalPercent: Double,
    val relativeToMaxPercent: Double,
    val comparison: PeriodComparisonSummary? = null,
)
```

## 8.3 Relative background bar

Each row should show a semi-transparent background bar relative to the largest item in the list.

Calculation:

```text
relativeToMaxPercent = item.amount / maxItem.amount * 100
```

Example:

```text
max item = 200
current item = 20
relativeToMaxPercent = 10%
```

Important:

The visual bar is relative to the largest item, not to the total period spending.

The row can also display `shareOfTotalPercent`, which means:

```text
shareOfTotalPercent = item.amount / totalAmount * 100
```

Suggested row layout:

```text
Lidl
25,000 RSD · 32% of period · 17 transactions
[bar width = relative to largest item]
```

For tags, because totals overlap, avoid `of period` wording if it may confuse.

For tags use:

```text
#beer
9,000 RSD · 8 items
[bar width = relative to largest tag]
```

Optional note at top:

```text
Tag totals overlap; percentages are relative to the largest tag.
```

## 8.4 Minimum bar width

If item amount is positive but very small, a purely proportional bar may be invisible.

Recommended UI rule:

```text
if amount > 0 and relativeToMaxPercent < 3%, draw 3% visual minimum
```

But keep actual numeric percentage unchanged.

---

# 9. Merchant statistics

## 9.1 MerchantStatsListScreen

Purpose:

Show merchants ranked by total spending in selected period.

Layout:

```text
Merchants

[DateRangeFilterBar]

Total: 123,400 RSD
Previous: 95,000 RSD
+28,400 RSD · +29.9%

Lidl
25,000 RSD · 17 transactions · 20.3%
[relative bar]

Maxi
18,000 RSD · 11 transactions · 14.6%
[relative bar]

Unknown merchant
3,000 RSD · 2 transactions · 2.4%
[relative bar]
```

Click row:

```text
MerchantDetailsScreen(merchantId)
```

## 9.2 MerchantDetailsScreen

Purpose:

Explain where money spent at this merchant went.

Layout:

```text
Lidl

[DateRangeFilterBar]

Total: 25,000 RSD
Previous: 19,000 RSD
+6,000 RSD · +31.6%
Transactions: 17
Average transaction: 1,470 RSD

Categories inside Lidl
Food       18,000 RSD
Alcohol     4,000 RSD
Household   3,000 RSD

Top tags inside Lidl
#beer       2,000 RSD
#guests     1,200 RSD

Transactions
2026-06-01 · Lidl · 10,000 RSD
2026-06-05 · Lidl · 2,500 RSD
...
```

Required sections:

- total summary with previous-period comparison;
- breakdown by categories;
- optional top tags;
- related transactions list.

Click transaction:

```text
TransactionDetailsScreen(expenseId)
```

## 9.3 Merchant aggregation rules

Merchant statistics group by expense-level merchant.

```text
merchant total = sum(expense.amount) for expenses in period grouped by merchantId
```

If merchant is null:

```text
Unknown merchant
```

If merchant is renamed later, prefer current merchant name for management views. If the expense stores a merchant snapshot, details screen can show both when useful:

```text
Current merchant: Lidl
Saved as: LIDL Novi Sad
```

MVP can simply show current merchant name or snapshot.

---

# 10. Category statistics

## 10.1 CategoryStatsListScreen

Purpose:

Show categories ranked by split-aware total spending.

Layout:

```text
Categories

[DateRangeFilterBar]

Total: 123,400 RSD
Previous: 95,000 RSD
+28,400 RSD · +29.9%

Food
60,000 RSD · 48.6%
[relative bar]

Transport
15,000 RSD · 12.1%
[relative bar]

Alcohol
12,000 RSD · 9.7%
[relative bar]

Uncategorized
4,000 RSD · 3.2%
[relative bar]
```

Click row:

```text
CategoryDetailsScreen(categoryId)
```

For `Uncategorized`, use a special virtual id:

```text
__uncategorized__
```

## 10.2 CategoryDetailsScreen

Purpose:

Explain where a category amount came from.

Layout:

```text
Food

[DateRangeFilterBar]

Total: 60,000 RSD
Previous: 52,000 RSD
+8,000 RSD · +15.4%

Merchants inside Food
Lidl       25,000 RSD
Maxi       22,000 RSD
Market      5,000 RSD
Other       8,000 RSD

Top tags inside Food
#child      8,000 RSD
#guests     4,000 RSD
#sweets     2,000 RSD

Items / transactions
Sausage · 500 RSD · Lidl · 2026-06-01
Chocolate · 700 RSD · Lidl · 2026-06-01
Lidl transaction · 4,800 RSD · Rest of Food · 2026-06-01
...
```

Recommended result rows:

- If split exists: show split items/fragments, because category may come from split nodes.
- If no split exists: show transaction-level row.

Click row:

```text
TransactionDetailsScreen(expenseId)
```

## 10.3 Category aggregation rules

Category totals must be non-overlapping.

This is the most important calculation in the feature.

### No split case

If expense has no split children:

```text
category = expense.categoryId or Uncategorized
amount = expense.amount
```

### Split case

Use analytical fragments:

- leaf split nodes;
- virtual unallocated/rest amounts;
- effective category.

Effective category:

```text
effectiveCategory(splitNode) =
  splitNode.categoryId
  or nearest ancestor categoryId
  or expense.categoryId
  or Uncategorized
```

### Partial split case

Example:

```text
Lidl 10,000
├─ Food 6,000 category=Food
│  ├─ Sausage 500 category=null
│  ├─ Chocolate 700 category=null
│  └─ Rest of Food 4,800 virtual
├─ Alcohol 1,200 category=Alcohol
└─ Unallocated root rest 2,800 virtual
```

Category totals:

```text
Food = 6,000
Alcohol = 1,200
Uncategorized / expense category = 2,800
```

Do not double count parent and child amounts.

Preferred algorithm:

1. For every expense in period, build analytical fragments.
2. If node has children, do not count the parent amount directly.
3. Count child leaf nodes.
4. Count virtual rest for any node where `node.amount > sum(children.amount)`.
5. Assign effective category to each counted fragment.

Pseudo-code:

```kotlin
fun buildCategoryFragments(expense: ExpenseWithSplitTree): List<CategoryAmountFragment> {
    if (expense.root == null || expense.root.children.isEmpty()) {
        return listOf(
            CategoryAmountFragment(
                expenseId = expense.id,
                splitNodeId = null,
                amount = expense.amount,
                effectiveCategoryId = expense.categoryId ?: UNCATEGORIZED_ID,
                label = expense.labelForDisplay(),
            )
        )
    }

    return collectFragmentsFromNode(
        expense = expense,
        node = expense.root,
        inheritedCategoryId = expense.categoryId,
    )
}
```

---

# 11. Tag statistics

## 11.1 TagStatsListScreen

Purpose:

Show tags ranked by total amount of tagged split items.

Layout:

```text
Tags

[DateRangeFilterBar]

Tagged total: 32,000 RSD
Previous: 24,000 RSD
+8,000 RSD · +33.3%

Note: tag totals may overlap.

#beer
9,000 RSD · 8 items
[relative bar]

#guests
7,500 RSD · 5 items
[relative bar]

#warranty
12,000 RSD · 1 item
[relative bar]
```

Click row:

```text
TagDetailsScreen(tagId)
```

## 11.2 TagDetailsScreen

Purpose:

Show split items related to a tag.

Layout:

```text
#warranty

[DateRangeFilterBar]

Total: 12,000 RSD
Previous: 0 RSD
+12,000 RSD · new spending
Items: 1

Xiaomi watch
12,000 RSD
Gigatron · 2026-06-01
Category: Electronics
Tags: #xiaomi_watch #warranty
[Open transaction]
```

For tag details, rows should represent matching split nodes/items, not whole transactions, because tags live on split nodes.

If in future tags can also be attached directly to expenses, normalize result model as:

```kotlin
sealed interface StatisticsResultRow {
    data class SplitItem(...): StatisticsResultRow
    data class Transaction(...): StatisticsResultRow
}
```

## 11.3 Tag aggregation rules

Tag totals are overlapping.

```text
For each split node with tags:
  for each tag on node:
    tagTotal[tag] += splitNode.amount
```

Do not include virtual unallocated rows in tag totals because virtual rows cannot have tags.

Do not infer tags from parent nodes to children for MVP.

If a parent node has a tag and children:

- MVP option A: count parent tag amount once at parent amount.
- MVP option B: if parent is expanded into children, still count the tagged parent as its own tagged amount.

Recommended MVP rule:

```text
Tags apply to the exact node where they are assigned.
```

If a parent node has children and a tag, the parent tag contributes parent amount. This is simple and predictable, but can overlap with child tags. That is acceptable because tags are contextual and overlapping.

UI note remains mandatory:

```text
Tag totals may overlap.
```

---

# 12. Transaction details screen

## 12.1 Purpose

A read-only details screen for a transaction/purchase.

It acts as the safe bridge between analytics/search and editing.

## 12.2 Layout

```text
Lidl
10,000 RSD
2026-06-01
Source: Card
Merchant: Lidl
Quick category: Groceries
Note: optional

Receipt
[thumbnail / no receipt]

Split
Food 6,000 RSD
  Sausage 500 RSD #meat
  Chocolate 700 RSD #sweets
  Rest of Food 4,800 RSD
Alcohol 1,200 RSD
  Beer 1,200 RSD #beer #guests #leha
Household 800 RSD
Unallocated 2,000 RSD

[Edit transaction]
[Edit split]
```

## 12.3 Required behavior

- Open from merchant/category/tag/search result rows.
- Show basic expense fields.
- Show receipt thumbnail placeholder if receipt storage exists or is planned.
- Show split tree using existing split model.
- Show virtual unallocated rows calculated, not stored.
- Allow navigation to edit transaction.
- Allow navigation to split editor.

## 12.4 Why not jump directly to edit?

Because statistics is an inspection mode.

The user may search `#xiaomi_watch`, open a result two years later, and wants to see:

- purchase date;
- merchant;
- source;
- amount;
- receipt photo;
- split context;
- warranty-related tags.

Only after that should editing be offered.

---

# 13. Advanced search

## 13.1 Purpose

Advanced search finds transactions or split items using optional filters.

It should work as a practical “financial microscope”.

## 13.2 Entry point

Accessible from dashboard:

```text
StatisticsDashboardScreen -> AdvancedSearchScreen
```

Optional also from main expense list:

```text
ExpenseListScreen -> AdvancedSearchScreen
```

But for MVP, dashboard entry is enough.

## 13.3 Shared date filter

Advanced search must use the same shared `StatisticsFilterViewModel`.

Changing the date range on the search screen affects dashboard/lists/details when navigating back.

## 13.4 Fields

All fields are optional.

```text
[DateRangeFilterBar]

Merchant
- optional
- single-select for MVP
- multi-select later if needed

Category
- optional
- single-select for MVP
- flat category list

Tags
- optional multi-select
- match mode:
  - all selected tags
  - any selected tag

Split item name contains
- optional text input
- partial case-insensitive match
- suggestions from existing split labels if feasible

Amount
- optional from / to, later

Source
- optional source/wallet filter, later

Split status
- optional: none / partial / full, later

Has receipt
- optional, later
```

MVP fields:

- merchant;
- category;
- tags;
- tag match mode `ALL` / `ANY`;
- split item name contains;
- shared date range.

## 13.5 Search filter model

```kotlin
data class StatisticsSearchFilter(
    val dateRange: StatisticsDateRangeState,
    val merchantIds: Set<String> = emptySet(),
    val categoryIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
    val tagMatchMode: TagMatchMode = TagMatchMode.ALL,
    val splitLabelQuery: String = "",
    val sourceIds: Set<String> = emptySet(),
    val amountFrom: Money? = null,
    val amountTo: Money? = null,
    val splitStatuses: Set<SplitStatus> = emptySet(),
    val hasReceipt: Boolean? = null,
)

enum class TagMatchMode {
    ALL,
    ANY,
}
```

## 13.6 Search behavior

### Empty filters

If all filters are empty except date range:

```text
Return all analytical rows for the selected period with pagination.
```

This is allowed.

But UI should make it clear:

```text
No filters selected. Showing all period results.
```

### Merchant filter

Matches expenses by merchantId.

### Category filter

Matches analytical fragments by effective category.

Important:

If a transaction has several split items in different categories, searching by category should return only matching fragments, not necessarily the whole transaction amount.

### Tags filter

Tags live on split nodes.

`ALL` mode:

```text
A split node matches if it has every selected tag.
```

`ANY` mode:

```text
A split node matches if it has at least one selected tag.
```

When filtering by multiple tags, the same split node must be counted once.

### Split item name contains

Search in split node label/name.

Recommended matching:

```text
case-insensitive partial match
trimmed query
```

Optional later:

- search merchant name;
- search category name;
- search tag name;
- search transaction note.

MVP should keep label query specifically about split item name to avoid ambiguous result scope.

## 13.7 Search results screen

Layout:

```text
Search results

[DateRangeFilterBar]

Found: 7 items
Total: 18,400 RSD
Previous period: 10,000 RSD
+8,400 RSD · +84%

Xiaomi watch
12,000 RSD
Gigatron · 2026-06-01
Category: Electronics
Tags: #xiaomi_watch #warranty

Beer
1,200 RSD
Lidl · 2026-05-28
Category: Alcohol
Tags: #beer #guests

...
```

Result row click:

```text
TransactionDetailsScreen(expenseId)
```

## 13.8 Result row model

```kotlin
data class StatisticsSearchResultUi(
    val expenseId: String,
    val splitNodeId: String?,
    val title: String,
    val amount: Money,
    val merchantName: String?,
    val date: LocalDate,
    val effectiveCategoryName: String?,
    val tags: List<String>,
    val isVirtualUnallocated: Boolean = false,
)
```

If search result comes from an unsplit transaction:

```text
splitNodeId = null
```

If search result comes from virtual unallocated/rest amount:

```text
isVirtualUnallocated = true
```

MVP can exclude virtual unallocated rows from text/tag search, but category search must account for virtual unallocated amounts if category totals include them.

---

# 14. Data and domain models

## 14.1 Existing concepts assumed

From split/detailization specification:

- Expense / Transaction;
- SplitNode;
- SplitNodeTag;
- Merchant;
- Category;
- Tag;
- split status;
- effective category calculation;
- virtual unallocated amount.

Statistics must reuse these concepts and must not duplicate business logic inside UI.

## 14.2 New domain models

```kotlin
data class StatisticsFilter(
    val dateRange: StatisticsDateRangeState,
    val operationTypes: Set<OperationType> = setOf(OperationType.EXPENSE),
    val sourceIds: Set<String> = emptySet(),
)
```

```kotlin
data class StatsAmountSummary(
    val currentAmount: Money,
    val previousAmount: Money?,
    val currentCount: Int? = null,
    val previousCount: Int? = null,
    val comparison: PeriodComparisonSummary,
)
```

```kotlin
data class StatsBreakdownItem(
    val id: String,
    val label: String,
    val amount: Money,
    val count: Int,
    val shareOfTotal: Double,
    val relativeToMax: Double,
)
```

```kotlin
data class DashboardStatistics(
    val totalSpent: StatsAmountSummary,
    val categoryPie: List<StatsBreakdownItem>,
    val merchantPie: List<StatsBreakdownItem>,
    val topTags: List<StatsBreakdownItem>,
)
```

```kotlin
data class CategoryAmountFragment(
    val expenseId: String,
    val splitNodeId: String?,
    val amount: Money,
    val effectiveCategoryId: String,
    val label: String,
    val date: LocalDate,
    val merchantId: String?,
    val isVirtualRest: Boolean,
)
```

```kotlin
data class TagAmountFragment(
    val expenseId: String,
    val splitNodeId: String,
    val tagId: String,
    val amount: Money,
    val label: String,
    val date: LocalDate,
    val merchantId: String?,
    val effectiveCategoryId: String,
)
```

---

# 15. Repository / use-case layer

## 15.1 Do not put statistics rules only in Compose

Rules like:

- effective category;
- virtual unallocated amount;
- previous period calculation;
- split-aware category totals;
- tag overlap behavior;
- row percentage calculation;

must live in domain/data/use-case layer, not only in composables.

Composables should render prepared UI state.

## 15.2 Suggested use cases

```text
GetDashboardStatisticsUseCase
GetMerchantStatsUseCase
GetMerchantDetailsUseCase
GetCategoryStatsUseCase
GetCategoryDetailsUseCase
GetTagStatsUseCase
GetTagDetailsUseCase
SearchStatisticsUseCase
GetTransactionDetailsUseCase
CalculatePreviousPeriodUseCase
BuildCategoryFragmentsUseCase
BuildTagFragmentsUseCase
```

If the project does not use explicit use-case classes yet, repositories/ViewModels may be used pragmatically, but keep calculation functions testable and outside UI.

## 15.3 Suggested repository interface

```kotlin
interface StatisticsRepository {
    fun observeDashboard(filter: StatisticsFilter): Flow<DashboardStatistics>

    fun observeMerchantStats(filter: StatisticsFilter): Flow<List<StatsBreakdownItem>>
    fun observeMerchantDetails(merchantId: String, filter: StatisticsFilter): Flow<MerchantStatisticsDetails>

    fun observeCategoryStats(filter: StatisticsFilter): Flow<List<StatsBreakdownItem>>
    fun observeCategoryDetails(categoryId: String, filter: StatisticsFilter): Flow<CategoryStatisticsDetails>

    fun observeTagStats(filter: StatisticsFilter): Flow<List<StatsBreakdownItem>>
    fun observeTagDetails(tagId: String, filter: StatisticsFilter): Flow<TagStatisticsDetails>

    fun search(filter: StatisticsSearchFilter, page: Int, pageSize: Int): Flow<StatisticsSearchPage>

    fun observeTransactionDetails(expenseId: String): Flow<TransactionDetails>
}
```

---

# 16. DAO / SQL considerations

## 16.1 MVP choice: SQL aggregation vs Kotlin aggregation

There are two possible approaches.

### Approach A: SQL-heavy aggregation

Pros:

- faster for large datasets;
- less memory usage;
- good for simple merchant totals.

Cons:

- split-aware category totals with virtual unallocated fragments can become complex;
- harder to test;
- SQL may become cursed spaghetti if rushed.

### Approach B: load period rows and aggregate in Kotlin

Pros:

- easier to implement correctly for split trees;
- easier to unit test;
- simpler to express virtual unallocated logic;
- safer for MVP.

Cons:

- may be slower with very large datasets.

MVP recommendation:

```text
Use SQL for simple base loading by date, then aggregate split-aware statistics in Kotlin.
```

Personal expense tracker data volume is likely manageable. Correctness matters more than premature SQL heroics.

Later optimization:

- add SQL views;
- add materialized/statistics cache;
- precompute monthly aggregates;
- add indexes.

## 16.2 Required indexes

Ensure indexes exist or add them during implementation:

```text
expenses(date)
expenses(merchantId)
expenses(categoryId)
expenses(sourceId)
split_nodes(expenseId)
split_nodes(parentId)
split_nodes(categoryId)
split_node_tags(splitNodeId)
split_node_tags(tagId)
tags(normalizedName)
merchants(normalizedName)
categories(normalizedName)
```

If table names differ, adapt accordingly.

## 16.3 Pagination

Search results and large transaction lists must be paginated or lazily loaded.

MVP options:

- Room `PagingSource` if Paging is already used;
- simple `LIMIT/OFFSET` if not;
- in-memory paging for MVP only if dataset is tiny.

Recommended:

```text
Use lazy list in UI and repository-level paging for search results.
```

---

# 17. UI components

## 17.1 DateRangeFilterBar

Purpose:

Reusable visible control for changing shared period.

UI examples:

```text
[This month ▼]  Jun 1 — Jun 30
```

Expanded:

```text
Today
Yesterday
This week
This month
This year
Last 7 days
Last 30 days
Custom range
All time
```

For custom range, open date range picker.

Required props:

```kotlin
@Composable
fun DateRangeFilterBar(
    state: StatisticsDateRangeState,
    onPresetSelected: (StatisticsDatePreset) -> Unit,
    onCustomRangeSelected: (LocalDate, LocalDate) -> Unit,
)
```

## 17.2 PeriodComparisonSummary

Purpose:

Reusable comparison display attached to amount summary.

Examples:

```text
vs previous period: +25,000 RSD · +26.3%
```

```text
vs previous period: -12,000 RSD · -100%
```

```text
Previous period: no spending
```

Required props:

```kotlin
@Composable
fun PeriodComparisonSummary(
    comparison: PeriodComparisonSummaryUi,
)
```

## 17.3 AmountSummaryCard

Purpose:

Reusable total block.

Used on:

- dashboard;
- merchant details;
- category details;
- tag details;
- search results;
- stats list screens.

Required props:

```kotlin
@Composable
fun AmountSummaryCard(
    title: String,
    amount: Money,
    comparison: PeriodComparisonSummaryUi?,
    countLabel: String? = null,
)
```

## 17.4 StatsPieCard

Purpose:

Reusable pie card for categories and merchants only.

Required props:

```kotlin
@Composable
fun StatsPieCard(
    title: String,
    items: List<StatsPieSliceUi>,
    onSliceClick: (StatsPieSliceUi) -> Unit,
    onViewAllClick: () -> Unit,
)
```

Do not use this for tags.

## 17.5 StatsRelativeBarRow

Purpose:

Reusable list row with semi-transparent relative background.

Required props:

```kotlin
@Composable
fun StatsRelativeBarRow(
    title: String,
    amountText: String,
    subtitle: String?,
    relativeToMaxPercent: Double,
    onClick: () -> Unit,
)
```

The bar must be visual only and must not replace numeric values.

## 17.6 TransactionResultRow

For whole transaction rows:

```text
Lidl
10,000 RSD
2026-06-01 · Card · Groceries
```

## 17.7 SplitItemResultRow

For split/search/tag/category item rows:

```text
Xiaomi watch
12,000 RSD
Gigatron · 2026-06-01
Electronics · #xiaomi_watch #warranty
```

---

# 18. Empty states

## 18.1 Dashboard empty state

If no expenses in selected period:

```text
No expenses for this period
Try another date range or add your first expense.
```

Russian:

```text
За этот период расходов нет
Выберите другой период или добавьте первую трату.
```

## 18.2 List empty state

Merchants:

```text
No merchant spending for this period
```

Categories:

```text
No category spending for this period
```

Tags:

```text
No tagged items for this period
```

Search:

```text
No results
Try changing filters or date range.
```

---

# 19. User-facing copy

## 19.1 English labels

```text
Statistics
Analytics
This month
This week
This year
Custom range
All time
Total spent
Previous period
Compared with previous period
Categories
Merchants
Tags
Advanced search
View all
Transactions
Items
Uncategorized
Unknown merchant
No tags
Tag totals may overlap
Search results
Found
Open transaction
Edit transaction
Edit split
```

## 19.2 Russian labels

```text
Статистика
Аналитика
Этот месяц
Эта неделя
Этот год
Произвольный период
Всё время
Всего потрачено
Прошлый период
По сравнению с прошлым периодом
Категории
Магазины
Метки
Расширенный поиск
Показать все
Транзакции
Позиции
Без категории
Без магазина
Без меток
Суммы по меткам могут пересекаться
Результаты поиска
Найдено
Открыть покупку
Редактировать покупку
Редактировать разбивку
```

Note: If the app uses `merchant` as a wider concept than store, Russian `Магазины` may be too narrow. Alternative:

```text
Продавцы
Места трат
Мерчанты
```

Recommended for friendly Russian UI:

```text
Места трат
```

---

# 20. Non-goals for MVP

Do not implement in this task:

- budgets and limits;
- recurring payments;
- OCR receipt parsing;
- AI categorization;
- warranty expiration tracking;
- multi-currency conversion;
- category tree UI;
- tag taxonomy/types;
- complex BI matrix screen;
- trend charts by day/week/month;
- cloud sync;
- predictive analytics;
- exporting statistics.

Do not redesign the quick-add screen.

Do not move split editing into statistics screens.

---

# 21. Future extensions

## 21.1 Merchant × Category matrix

Later screen:

```text
          Food   Alcohol   Household   Total
Lidl      6000   1200      800         8000
Maxi      5800   1200      0           7000
Gigatron  0      0         0           12000
```

Useful, but not MVP.

## 21.2 Trends

Possible later charts:

- daily spending;
- weekly spending;
- monthly spending;
- category trend over time;
- merchant trend over time.

## 21.3 Budget warnings

Later:

```text
Food budget: 80% used
Alcohol: +30% vs last month
```

## 21.4 Receipt integration

Later transaction details may show:

- receipt image;
- OCR text;
- receipt item mapping to split nodes;
- warranty-related files.

## 21.5 Warranty mode

Tags like:

```text
#warranty
#xiaomi_watch
```

can later support:

- warranty expiration date;
- receipt reminder;
- product lookup.

Not MVP.

---

# 22. Acceptance criteria

## 22.1 Navigation

1. Main expense list has an entry to statistics.
2. Statistics opens `StatisticsDashboardScreen`.
3. Dashboard links to merchant, category, tag lists and advanced search.
4. Merchant/category/tag list rows open corresponding details screens.
5. Statistic/search result rows open `TransactionDetailsScreen`.
6. `TransactionDetailsScreen` can navigate to transaction edit and split editor.

## 22.2 Shared date filter

1. All statistics screens display or have access to the same date range filter.
2. Changing the date range on one statistics screen updates data on all statistics screens.
3. Date range does not reset during navigation inside the statistics graph.
4. Default period is `This month`, unless app settings specify otherwise.
5. `All time` is supported but disables previous-period comparison.

## 22.3 Previous period comparison

1. Every screen with a total amount shows previous-period comparison.
2. Previous period is calculated from the active date range.
3. Custom ranges compare against the same number of days immediately before the selected range.
4. Month/week/year presets compare against previous calendar month/week/year.
5. Division by zero is handled gracefully.
6. If previous amount is zero and current amount is positive, UI shows `new spending` or equivalent.
7. If comparison is disabled/unavailable, UI explains that comparison is unavailable.

## 22.4 Dashboard

1. Dashboard shows total spent for selected period.
2. Dashboard shows transaction count.
3. Dashboard shows average transaction amount.
4. Dashboard shows category pie chart.
5. Dashboard shows merchant pie chart.
6. Dashboard does not show tag pie chart.
7. Dashboard shows top tags as list preview.
8. Pie charts use top N + Other when there are many items.

## 22.5 Stats lists

1. Merchants list shows merchant totals ranked descending.
2. Categories list shows split-aware category totals ranked descending.
3. Tags list shows tag totals ranked descending.
4. All three lists use reusable row/list component.
5. Rows show amount.
6. Rows show visual background bar relative to the largest item.
7. Rows do not confuse relative-to-max bar with share-of-total percentage.
8. Tag list includes note that tag totals may overlap.

## 22.6 Merchant details

1. Merchant details show total spending for merchant.
2. Merchant details show previous-period comparison.
3. Merchant details show breakdown by categories.
4. Merchant details show related transactions.
5. Related transactions open transaction details.

## 22.7 Category details

1. Category details show total spending for category.
2. Category details show previous-period comparison.
3. Category details show breakdown by merchants.
4. Category details show matching transaction/split item rows.
5. Category aggregation is split-aware and does not double count parent and child nodes.
6. Virtual unallocated/rest amounts are included where category rules require them.

## 22.8 Tag details

1. Tag details show total tagged amount.
2. Tag details show previous-period comparison.
3. Tag details show matching split nodes/items, not only whole transactions.
4. Same split node with multiple selected tags is counted once in filtered results.
5. Tag totals are allowed to overlap conceptually.

## 22.9 Advanced search

1. Advanced search uses shared date filter.
2. Merchant filter is optional.
3. Category filter is optional.
4. Tags filter is optional and supports `ALL` and `ANY` modes.
5. Split label partial search is optional.
6. Empty filters return period results with pagination or lazy loading.
7. Search results show total amount for found rows.
8. Search results show previous-period comparison for the same filter logic.
9. Search result rows open transaction details.

## 22.10 Transaction details

1. Transaction details show basic expense data.
2. Transaction details show split tree if available.
3. Transaction details show virtual unallocated rows calculated, not stored.
4. Transaction details has action to edit transaction.
5. Transaction details has action to edit split.
6. Transaction details can later show receipt photo/thumbnail without redesign.

## 22.11 Architecture

1. Statistics calculations are not implemented only inside composables.
2. Previous period calculation is centralized.
3. Effective category calculation is reused from split/domain logic or centralized in domain.
4. Virtual unallocated/rest calculation is centralized.
5. UI renders prepared state from ViewModels/use cases.
6. DAO/repository layer exposes observable statistics data.
7. Tests cover previous period calculation.
8. Tests cover split-aware category aggregation.
9. Tests cover tag overlap behavior.
10. Tests cover search tag `ALL` / `ANY` behavior.

---

# 23. Implementation phases

## Phase 1 — Domain rules and shared filter

- Add statistics date range model.
- Add previous-period calculation.
- Add period comparison model.
- Add shared `StatisticsFilterViewModel`.
- Add `DateRangeFilterBar`.
- Add unit tests for date range and previous period logic.

## Phase 2 — Base dashboard

- Add statistics entry from main expense list.
- Add statistics navigation graph.
- Add dashboard screen.
- Add total spent summary with comparison.
- Add merchant pie.
- Add category pie.
- Add top tags preview list.

## Phase 3 — Universal stats lists

- Add reusable `StatsList` and `StatsRelativeBarRow`.
- Add merchant stats list.
- Add category stats list.
- Add tag stats list.
- Add row navigation to details.

## Phase 4 — Details screens

- Add merchant details.
- Add category details.
- Add tag details.
- Add related transaction/split item lists.
- Add transaction details screen.
- Connect transaction details to edit/split flows.

## Phase 5 — Advanced search

- Add advanced search screen.
- Add optional filters.
- Add search result total with previous-period comparison.
- Add result list and navigation to transaction details.

## Phase 6 — Polish and validation

- Add empty states.
- Add loading/error states.
- Add string resources.
- Add tests for aggregation/search.
- Add indexes/migrations as needed.
- Check performance on realistic local dataset.

---

# 24. Final conceptual model

```text
Expense = purchase / transaction document
SplitNode = analytical money fragment
Category = mutually exclusive budget bucket
Merchant = place/person/service where money was spent
Tag = overlapping contextual/search label
DateRangeFilter = shared statistics lens
PreviousPeriodComparison = built-in context for every amount
TransactionDetails = safe read-only bridge from analytics to editing
SplitEditor = surgical editing tool
```

The important design line:

> Dashboard explains the big picture. Lists rank entities. Details explain one entity. Search hunts exact fragments. Transaction details shows the purchase passport. Split editor changes the anatomy.

This keeps the app fast at input time and powerful at analysis time without turning the main screen into a мутировавший бухгалтерский комбайн.
