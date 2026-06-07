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

- Room DB: `AppDatabase` (version 1) with entities:
  - `Expense`, `Category`, `Source`
- DAOs: `ExpenseDao`, `CategoryDao`, `SourceDao`.
- DB singleton: `AppDatabase.get(context)`.

## Preferences (DataStore)

- `AppPrefs`: currency, widget period, limit, show remainder, language code, seeded flag.

## Widget

- Glance widget: `QuickExpenseWidget` + receiver.
- State stored in Preferences (Glance state).
- `WidgetRecomputeAndRedraw` schedules updates.

## Export

- `ExportWorker` creates CSV + ZIP, returns file URI.
- CSV output includes CSV escaping + formula injection mitigation.
- Export stored in cache/exports.

## Receipt scanning (current implementation)

- Entry point: Quick Input screen has "Scan receipt" button.
- Scanner abstraction: `ReceiptScanner` with `LocalReceiptScanner`.
- Current implementation: CameraX activity `ReceiptScanActivity`.
- Multi-page capture with overlay (top 20% shows previous page bottom for overlap).
- Saves pages to cache/receipts as JPG: `receipt_page_yyyyMMdd_HHmmss.jpg`.
- Returns list of files/URIs via `EXTRA_RECEIPT_PATHS`.
- Quick Input shows number of pages and gallery dialog.

## Permissions

- `android.permission.CAMERA` used for receipt scan.

## FileProvider

- Authority: `${applicationId}.fileprovider`.
- Paths: cache/exports and cache/receipts.

## Security changes

- `allowBackup=false` in manifest.
- `QuickInputActivity` not exported.
- Debug logs gated by `BuildConfig.DEBUG`.

## Notes

- BuildConfig generation explicitly enabled in app/build.gradle.kts.
- Camera preview forced to 16:9 and full width to avoid side padding.
- Gallery dialog uses Coil to preview pages.
