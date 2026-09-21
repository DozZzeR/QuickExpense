# QuickExpense — agent notes

## App overview

- Android app for quick expense entry, export, widgets.
- Package: dev.keslorod.quickexpense
- UI: Jetpack Compose.
- Persistence: Room + DataStore.

## Key flows

- Main screen: summary + recent transactions, export via WorkManager.
- Quick input: choose source/category, enter amount, save.
- Settings: manage sources/categories, widget settings, language, currency.

## Database

- Room DB: `AppDatabase` (version 5, migrations 1→5) with entities:
  - `Expense`, `Category`, `Source`, `Merchant`, `Tag`, `SplitNode`, `SplitNodeTag`, `ExpenseTag`
- DAOs: one per entity (`ExpenseDao`, `CategoryDao`, …).
- Built-ins referenced by fixed id: category `unsorted`, tag `has_receipt` — never deletable;
  categories/tags in use (incl. by split items) can't be deleted either (`domain/Deletion.kt`).
- Totals and statistics only cover expenses in the currency selected in Settings.
- DB singleton: `AppDatabase.get(context)`.

## Preferences (DataStore)

- `AppPrefs`: currency, widget period, limit, show remainder, language code, seeded flag.

## Widget

- Glance widget: `QuickExpenseWidget` + receiver.
- State stored in Preferences (Glance state).
- `WidgetRecomputeAndRedraw` schedules updates.
- Each refresh re-arms an inexact alarm for the next local midnight (period rollover);
  the receiver also refreshes on TIME_SET / TIMEZONE_CHANGED.

## Export

- `ExportWorker` creates CSV + ZIP (all rows in range, no cap), returns file URI.
- CSV output includes CSV escaping + formula injection mitigation.
- Export stored in cache/exports; previous exports are deleted on each new export.

## Receipt scanning (current implementation)

- Entry point: Quick Input screen has "Scan receipt" button.
- Scanner abstraction: `ReceiptScanner` with `LocalReceiptScanner`.
- Current implementation: CameraX activity `ReceiptScanActivity`.
- Multi-page capture with overlay (top 20% shows previous page bottom for overlap).
- Saves pages to files/receipts (not cache — receipts back warranty claims) as JPG:
  `receipt_page_yyyyMMdd_HHmmss_<random>.jpg`.
- Unreferenced receipt files older than a day are deleted on app start.
- Returns list of files/URIs via `EXTRA_RECEIPT_PATHS`.
- Quick Input shows number of pages and gallery dialog.

## Permissions

- `android.permission.CAMERA` used for receipt scan.

## FileProvider

- Authority: `${applicationId}.fileprovider`.
- Paths: cache/exports and files/receipts.

## Security changes

- `allowBackup=false` in manifest.
- `QuickInputActivity` not exported.
- Debug logs gated by `BuildConfig.DEBUG`.

## Notes

- BuildConfig generation explicitly enabled in app/build.gradle.kts.
- Camera preview forced to 16:9 and full width to avoid side padding.
- Gallery dialog uses Coil to preview pages.
