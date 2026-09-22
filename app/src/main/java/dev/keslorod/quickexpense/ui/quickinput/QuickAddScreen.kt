package dev.keslorod.quickexpense.ui.quickinput

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.receipt.LocalReceiptScanner
import dev.keslorod.quickexpense.receipt.ReceiptScanResult
import dev.keslorod.quickexpense.data.entities.SplitNode
import dev.keslorod.quickexpense.data.entities.Tag
import dev.keslorod.quickexpense.domain.deleteCategoryIfUnused
import dev.keslorod.quickexpense.domain.formatCents
import dev.keslorod.quickexpense.domain.isBuiltIn
import dev.keslorod.quickexpense.ui.split.SplitEditorScreen
import dev.keslorod.quickexpense.voice.VoiceExpenseParser
import dev.keslorod.quickexpense.voice.VoiceParseConfidence
import dev.keslorod.quickexpense.voice.languageCodeToRecognizerTag
import dev.keslorod.quickexpense.voice.rememberVoiceRecognizerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class OperationMode { EXPENSE, INCOME, TRANSFER }
enum class QuickAddType { SOURCE, MERCHANT, CATEGORY, TEMPLATES }
enum class VoiceEntityRole { MERCHANT, CATEGORY, SOURCE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddScreen(
    app: App,
    currency: String,
    sourceOptions: List<Option>,
    categoryOptions: List<Option>,
    merchantOptions: List<Option>,
    // Full (not favorites-only) lists, so voice input can match a category/merchant it hasn't
    // been used often enough to be pinned as a favorite.
    allCategoryOptions: List<Option> = emptyList(),
    allMerchantOptions: List<Option> = emptyList(),
    allSourceOptions: List<Option> = emptyList(),
    languageCode: String = "",
    // Non-null when editing an already-saved expense rather than creating a new one. Its
    // persisted split is loaded into the same in-memory draft a new expense uses
    // (initialSplitNodes/initialNodeTags) and only written back on Save — so cancelling the
    // edit can't leave a split in the DB that was sized for an amount that was never saved.
    editingExpenseId: String? = null,
    initialAmountCents: Long? = null,
    initialSource: Option? = null,
    initialMerchant: Option? = null,
    initialCategory: Option? = null,
    initialDateMillis: Long? = null,
    initialReceiptPaths: List<String> = emptyList(),
    initialSplitNodes: List<SplitNode> = emptyList(),
    initialNodeTags: Map<String, List<Tag>> = emptyMap(),
    initialSplitDiverged: Boolean = false,
    onConfirm: (cents: Long, sourceId: String, merchantId: String?, categoryId: String?, date: Long, receiptPaths: List<String>, splitNodes: List<SplitNode>, nodeTags: Map<String, List<Tag>>) -> Unit,
    onCancel: () -> Unit
) {
    var amountText by remember { mutableStateOf(initialAmountCents?.let { formatCents(it, '.') } ?: "") }
    var selectedDate by remember {
        mutableStateOf(initialDateMillis?.let { Calendar.getInstance().apply { timeInMillis = it } } ?: Calendar.getInstance())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf(initialSource ?: sourceOptions.firstOrNull()) }
    var merchant by remember { mutableStateOf(initialMerchant) }
    var category by remember { mutableStateOf(initialCategory) }
    
    var activePanel by remember { mutableStateOf<QuickAddType?>(null) }
    var showManageType by remember { mutableStateOf<QuickAddType?>(null) }

    // Quick-pick grids (favorites) are seeded once by the caller at activity start,
    // so they go stale after favorites are added/removed/renamed via "Manage" — refresh
    // them whenever that screen is closed.
    var sourceOptionsState by remember { mutableStateOf(sourceOptions) }
    var merchantOptionsState by remember { mutableStateOf(merchantOptions) }
    var categoryOptionsState by remember { mutableStateOf(categoryOptions) }
    val coroutineScope = rememberCoroutineScope()
    suspend fun refreshQuickPickOptions() {
        val (newSources, newMerchants, newCategories) = withContext(Dispatchers.IO) {
            Triple(
                app.db.sources().favorites().map { Option(it.id, it.name) },
                app.db.merchants().favorites().map { Option(it.id, it.name) },
                app.db.categories().favorites().map { Option(it.id, it.name) }
            )
        }
        sourceOptionsState = newSources
        merchantOptionsState = newMerchants
        categoryOptionsState = newCategories
    }
    
    // Split state
    var showSplitEditor by remember { mutableStateOf(false) }
    var draftSplitNodes by remember { mutableStateOf(initialSplitNodes) }
    var draftNodeTags by remember { mutableStateOf(initialNodeTags) }

    // While a split is uniform (every top-level row still carries the category picked
    // above), that top-level category keeps acting as the shared default for new rows.
    // The moment one row's category is changed away from it, the split has "diverged":
    // the shared category no longer means anything, so it's cleared and stops being
    // pre-filled into further rows (see SplitEditorScreen's onDivergedChange).
    var splitCategoryDiverged by remember { mutableStateOf(initialSplitDiverged) }
    LaunchedEffect(draftSplitNodes) {
        if (draftSplitNodes.isEmpty()) splitCategoryDiverged = false
    }
    val hideMainCategoryPicker = draftSplitNodes.isNotEmpty() && splitCategoryDiverged

    // Receipts state. When editing, hydrates from the expense's already-persisted photo(s) —
    // same shape as a fresh scan result, so the "scan receipt" button's existing "already have
    // one" state (its label, the gallery link) picks this up with no extra branching.
    val receiptContext = LocalContext.current
    var lastScan by remember {
        mutableStateOf(
            initialReceiptPaths
                .map { File(it) }
                .filter { it.exists() }
                .takeIf { it.isNotEmpty() }
                ?.let { files ->
                    ReceiptScanResult(
                        uris = files.map { FileProvider.getUriForFile(receiptContext, "${receiptContext.packageName}.fileprovider", it) },
                        files = files,
                        displayName = files.first().name
                    )
                }
        )
    }
    // The paths this screen started with — cancelling must only ever delete files scanned
    // fresh this session (see the Cancel button below); an already-persisted receipt from
    // before this edit began must survive a cancelled edit untouched.
    val initialReceiptFilePaths = remember { initialReceiptPaths.toSet() }
    var showGallery by remember { mutableStateOf(false) }
    val scanner = LocalReceiptScanner.current
    val scannerHandle = scanner.rememberLauncher { result ->
        if (dev.keslorod.quickexpense.BuildConfig.DEBUG) {
            android.util.Log.d("ReceiptDebug", "scanner returned: $result, files=${result?.files?.map { it.absolutePath }}")
        }
        // A cancelled (or empty) re-scan must leave the receipt the form already has alone —
        // clearing it here used to make the next Save delete an edited expense's persisted
        // receipt files outright.
        if (result == null) return@rememberLauncher
        // A successful re-scan replaces the previous one; files scanned earlier in this same
        // session would otherwise be orphaned in storage with nothing ever pointing at them.
        lastScan?.files?.forEach {
            if (it.absolutePath !in initialReceiptFilePaths && it !in result.files) it.delete()
        }
        lastScan = result
    }

    // Voice input state. autoSaveSecondsLeft != null means the parse was unambiguous enough
    // (see VoiceParseConfidence.HIGH) to offer saving automatically — counts down to 0, and
    // dismissing it (Cancel, tapping outside, or back) just stops the countdown, leaving
    // whatever voice already filled in for manual review, same as a PARTIAL/NONE parse.
    var showVoiceHelp by remember { mutableStateOf(false) }
    var autoSaveSecondsLeft by remember { mutableStateOf<Int?>(null) }
    // Phrases voice heard but couldn't match to anything — offered below as "use as merchant/
    // category/source?" chips instead of just being silently dropped. Resolving one (or
    // dismissing it) removes it from this list.
    var voiceLeftoverPhrases by remember { mutableStateOf<List<String>>(emptyList()) }
    // Non-null while resolving one such phrase: which phrase, and which field it's meant to
    // fill. Opens the existing manage screen for that field in SELECT mode with the phrase
    // pre-filled as the search text — its own "matches below, or add new" flow already covers
    // exactly what's needed here, existing-record typos included, not just brand-new names.
    var resolvingVoicePhrase by remember { mutableStateOf<Pair<String, VoiceEntityRole>?>(null) }
    val voiceRecognizer = rememberVoiceRecognizerLauncher(languageTag = languageCodeToRecognizerTag(languageCode)) { transcripts ->
        val result = VoiceExpenseParser.parseBest(transcripts, allCategoryOptions, allMerchantOptions, allSourceOptions)
            ?: return@rememberVoiceRecognizerLauncher
        if (dev.keslorod.quickexpense.BuildConfig.DEBUG) {
            android.util.Log.d(
                "VoiceInput",
                "alternatives=$transcripts -> best: amountCents=${result.amountCents} category=${result.category} " +
                    "merchant=${result.merchant} source=${result.source} confidence=${result.confidence} " +
                    "leftover=${result.leftoverPhrases}"
            )
        }
        result.amountCents?.takeIf { it > 0 }?.let { cents -> amountText = formatCents(cents, '.') }
        result.category?.let { category = it }
        result.merchant?.let { merchant = it }
        result.source?.let { source = it }
        voiceLeftoverPhrases = result.leftoverPhrases
        if (result.confidence == VoiceParseConfidence.HIGH) {
            autoSaveSecondsLeft = 10
        }
    }

    fun cancelAndDiscard() {
        // A scanned-but-unsaved receipt shouldn't linger in persistent storage forever just
        // because this screen was cancelled — nothing will ever reference it. Only ever deletes
        // files scanned fresh this session: a pre-existing receipt loaded for editing (in
        // initialReceiptFilePaths) must survive a cancelled edit.
        lastScan?.files?.forEach { if (it.absolutePath !in initialReceiptFilePaths) it.delete() }
        onCancel()
    }

    // System back steps out of whichever overlay is open first (they're all drawn inside this
    // one screen, not as nav destinations), and only then cancels the form — same cleanup as
    // the Cancel button. The split editor and dialogs register their own, later handlers,
    // which take precedence over this one while they're showing.
    BackHandler {
        when {
            resolvingVoicePhrase != null -> resolvingVoicePhrase = null
            showManageType != null -> {
                showManageType = null
                coroutineScope.launch { refreshQuickPickOptions() }
            }
            activePanel != null -> activePanel = null
            else -> cancelAndDiscard()
        }
    }

    fun toCents(txt: String): Long {
        if (txt.isBlank()) return 0
        val parts = txt.replace(',', '.').split('.', limit = 2)
        val major = parts[0].ifBlank { "0" }
        val minor = (parts.getOrNull(1) ?: "").padEnd(2, '0').take(2)
        return (major.toLongOrNull() ?: 0L) * 100 + (minor.toLongOrNull() ?: 0L)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                // targetSdk 35+ draws edge-to-edge: keep the form clear of the status bar and
                // gesture/nav bar (the top buttons sat under the clock, Cancel under the nav bar).
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Income/Transfer and Templates are hidden for now (see OperationModeSelector,
            // unused below): there's no source-balance tracking yet to make either mean
            // anything, so this screen just always adds a plain expense.
            FlowRow(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Voice and receipt scan are grouped together as the two alternative-input
                // entry points — both fill the form via recognition instead of manual taps.
                OutlinedButton(
                    onClick = { voiceRecognizer.start() },
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.voice_add))
                }
                OutlinedButton(
                    onClick = { scannerHandle.start() },
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (lastScan == null) stringResource(R.string.scan_receipt) else stringResource(R.string.scan_receipt_pages, lastScan!!.files.size))
                }
                IconButton(onClick = { showVoiceHelp = true }) {
                    Icon(Icons.Default.HelpOutline, contentDescription = stringResource(R.string.voice_help))
                }
            }

            if (lastScan != null) {
                TextButton(onClick = { showGallery = true }) {
                    Text(stringResource(R.string.open_receipt_gallery))
                }
            }

            Spacer(Modifier.height(32.dp))

            // Amount display
            AmountDisplay(amountText, currency)

            Spacer(Modifier.height(32.dp))

            // Picker rows
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PickerRow(
                    label = stringResource(R.string.source),
                    value = source?.label ?: stringResource(R.string.choose_source),
                    onClick = { activePanel = QuickAddType.SOURCE }
                )
                PickerRow(
                    label = stringResource(R.string.to_where),
                    value = merchant?.label ?: stringResource(R.string.choose_or_add),
                    onClick = { activePanel = QuickAddType.MERCHANT }
                )
                if (!hideMainCategoryPicker) {
                    PickerRow(
                        label = stringResource(R.string.category),
                        value = category?.label ?: stringResource(R.string.optional),
                        onClick = { activePanel = QuickAddType.CATEGORY }
                    )
                }
            }

            // Phrases voice heard but couldn't match — a tap opens that field's own picker,
            // pre-filled with the phrase, to either pick an existing near-match or add it as new.
            if (voiceLeftoverPhrases.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    voiceLeftoverPhrases.forEach { phrase ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(R.string.voice_unmatched_word_fmt, phrase),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = { resolvingVoicePhrase = phrase to VoiceEntityRole.MERCHANT },
                                    label = { Text(stringResource(R.string.to_where)) }
                                )
                                AssistChip(
                                    onClick = { resolvingVoicePhrase = phrase to VoiceEntityRole.CATEGORY },
                                    label = { Text(stringResource(R.string.category)) }
                                )
                                AssistChip(
                                    onClick = { resolvingVoicePhrase = phrase to VoiceEntityRole.SOURCE },
                                    label = { Text(stringResource(R.string.source)) }
                                )
                                AssistChip(
                                    onClick = { voiceLeftoverPhrases = voiceLeftoverPhrases - phrase },
                                    label = { Text(stringResource(R.string.ignore)) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Custom numpad
            CustomNumPad(
                onDigit = { digit -> if (amountText.length < 10) amountText += digit },
                onDot = { 
                    if (!amountText.contains(".")) {
                        amountText = if (amountText.isEmpty()) "0." else "$amountText."
                    }
                },
                onDelete = { if (amountText.isNotEmpty()) amountText = amountText.dropLast(1) }
            )

            Spacer(Modifier.height(24.dp))
            
            // Split button (Scan moved up next to Voice — see top of screen)
            run {
                val cents = toCents(amountText)
                OutlinedButton(
                    onClick = { showSplitEditor = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cents > 0,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.CallSplit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (draftSplitNodes.isEmpty()) stringResource(R.string.split_action)
                        else stringResource(R.string.split_action_count_fmt, draftSplitNodes.size)
                    )
                }
            }

    Spacer(Modifier.height(24.dp))

    // Save/date buttons
    // The amount can be lowered after splitting; a split whose top-level items then add up to
    // more than the expense itself can't be saved as-is — it has to be fixed in the editor.
    val splitExceedsAmount = draftSplitNodes.filter { it.parentId == null }.sumOf { it.amount } > toCents(amountText)
    val isFormValid = source != null && amountText.isNotEmpty() && toCents(amountText) > 0 && !splitExceedsAmount
    if (splitExceedsAmount) {
        Text(
            stringResource(R.string.split_exceeds_amount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
    }

    // Yesterday/Today apply their offset to selectedDate, not to the real calendar day — so once
    // the user has picked a custom date, "Today" silently means "the picked date", not today.
    // Surface the picked date so that isn't invisible (see conversation: no on-screen indicator
    // meant a Calendar-picked date could be mistaken for using the real current day).
    val isCustomDate = remember(selectedDate) {
        val today = Calendar.getInstance()
        selectedDate.get(Calendar.YEAR) != today.get(Calendar.YEAR) ||
            selectedDate.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR)
    }
    if (isCustomDate) {
        val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showDatePicker = true }
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = stringResource(R.string.selected_date_fmt, dateFormatter.format(selectedDate.time)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    fun performSave(daysOffset: Int) {
        val cal = (selectedDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, daysOffset) }
        val receiptPaths = lastScan?.files?.map { it.absolutePath }.orEmpty()
        if (dev.keslorod.quickexpense.BuildConfig.DEBUG) {
            android.util.Log.d("ReceiptDebug", "performSave: lastScan=$lastScan, receiptPaths=$receiptPaths")
        }
        // TODO: handle split saving in onConfirm if needed,
        // but for now we just pass data back to activity
        onConfirm(
            toCents(amountText),
            source!!.id,
            merchant?.id,
            category?.id,
            cal.timeInMillis,
            receiptPaths,
            draftSplitNodes,
            draftNodeTags
        )
    }

    // Ticks the voice auto-save countdown down to 0, then performs the save — same path as
    // tapping "Today" by hand. Re-runs on every change to autoSaveSecondsLeft (each tick, and
    // being cancelled/restarted), which is exactly what's wanted here.
    LaunchedEffect(autoSaveSecondsLeft) {
        val secondsLeft = autoSaveSecondsLeft ?: return@LaunchedEffect
        if (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            autoSaveSecondsLeft = secondsLeft - 1
        } else {
            autoSaveSecondsLeft = null
            if (isFormValid) performSave(0)
        }
    }

    if (autoSaveSecondsLeft != null) {
        AlertDialog(
            onDismissRequest = { autoSaveSecondsLeft = null },
            title = { Text(stringResource(R.string.voice_understood_title)) },
            text = { Text(stringResource(R.string.voice_autosave_countdown_fmt, autoSaveSecondsLeft ?: 0)) },
            confirmButton = {
                TextButton(onClick = {
                    autoSaveSecondsLeft = null
                    performSave(0)
                }) { Text(stringResource(R.string.voice_save_now)) }
            },
            dismissButton = {
                TextButton(onClick = { autoSaveSecondsLeft = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showVoiceHelp) {
        AlertDialog(
            onDismissRequest = { showVoiceHelp = false },
            title = { Text(stringResource(R.string.voice_help_title)) },
            text = { Text(stringResource(R.string.voice_help_message)) },
            confirmButton = {
                TextButton(onClick = { showVoiceHelp = false }) { Text(stringResource(R.string.confirm)) }
            }
        )
    }

    if (editingExpenseId != null) {
        // "Yesterday"/"Today" are relative-day shortcuts for logging a fresh expense — while
        // editing, selectedDate is already the expense's real date (changeable via the
        // calendar chip above), so a plain Save is the only button that makes sense here.
        Button(
            onClick = { performSave(0) },
            enabled = isFormValid,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
        }
    } else {
        SaveDateButtons(
            enabled = isFormValid,
            onSave = { daysOffset -> performSave(daysOffset) },
            onOpenCalendar = { showDatePicker = true }
        )
    }

    if (showDatePicker) {
        // DatePicker speaks in UTC-midnight millis for a calendar day, not in instants — feed
        // it the local day as such, and map its answer back onto the local day (keeping the
        // time of day) instead of reading the UTC midnight as a local instant, which lands on
        // the previous day anywhere west of UTC.
        val todayUtcMillis = remember { localDayToUtcMillis(Calendar.getInstance()) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDayToUtcMillis(selectedDate),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayUtcMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        selectedDate = utcMillisToLocalDay(utcMillis, timeOfDayFrom = selectedDate)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
            
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { cancelAndDiscard() }) {
                Text(stringResource(R.string.cancel))
            }
        }

        // Overlay panel: QuickGridPanel
        if (activePanel != null) {
            QuickGridPanel(
                type = activePanel!!,
                sourceOptions = sourceOptionsState,
                merchantOptions = merchantOptionsState,
                categoryOptions = categoryOptionsState,
                onDismiss = { activePanel = null },
                onSelect = { type, option ->
                    when (type) {
                        QuickAddType.SOURCE -> source = option
                        QuickAddType.MERCHANT -> merchant = option
                        QuickAddType.CATEGORY -> category = option
                        QuickAddType.TEMPLATES -> {
                            // Apply template logic could go here
                        }
                    }
                    activePanel = null
                },
                onManage = { type ->
                    showManageType = type
                    activePanel = null
                }
            )
        }

        if (showManageType != null) {
            val type = showManageType!!
            val title = when (type) {
                QuickAddType.SOURCE -> stringResource(R.string.select_source)
                QuickAddType.MERCHANT -> stringResource(R.string.select_merchant)
                QuickAddType.CATEGORY -> stringResource(R.string.select_category)
                else -> ""
            }
            
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (type) {
                    QuickAddType.SOURCE -> {
                        dev.keslorod.quickexpense.ui.manage.ManageListScreen(
                            title = title,
                            onBack = { showManageType = null; coroutineScope.launch { refreshQuickPickOptions() } },
                            mode = dev.keslorod.quickexpense.ui.manage.ListScreenMode.SELECT,
                            onSelect = { source = Option(it.id, it.name); showManageType = null; coroutineScope.launch { refreshQuickPickOptions() } },
                            getName = { it.name },
                            isFavorite = { it.isFavorite },
                            itemKey = { it.id },
                            loadAll = { app.db.sources().all() },
                            addNew = { app.db.sources().insert(dev.keslorod.quickexpense.data.entities.Source(name = it, isFavorite = false)) },
                            toggleFavorite = { app.db.sources().update(it.copy(isFavorite = !it.isFavorite)) },
                            rename = { item, newName -> app.db.sources().update(item.copy(name = newName)) },
                            deleteIfUnused = { item ->
                                if (app.db.expenses().countBySource(item.id) == 0L) {
                                    app.db.sources().delete(item)
                                    true
                                } else false
                            }
                        )
                    }
                    QuickAddType.MERCHANT -> {
                        dev.keslorod.quickexpense.ui.manage.ManageListScreen(
                            title = title,
                            onBack = { showManageType = null; coroutineScope.launch { refreshQuickPickOptions() } },
                            mode = dev.keslorod.quickexpense.ui.manage.ListScreenMode.SELECT,
                            onSelect = { merchant = Option(it.id, it.name); showManageType = null; coroutineScope.launch { refreshQuickPickOptions() } },
                            getName = { it.name },
                            isFavorite = { it.isFavorite },
                            itemKey = { it.id },
                            loadAll = { app.db.merchants().all() },
                            addNew = { app.db.merchants().insert(dev.keslorod.quickexpense.data.entities.Merchant(name = it, isFavorite = false)) },
                            toggleFavorite = { app.db.merchants().update(it.copy(isFavorite = !it.isFavorite)) },
                            rename = { item, newName -> app.db.merchants().update(item.copy(name = newName)) },
                            deleteIfUnused = { item ->
                                if (app.db.expenses().countByMerchant(item.id) == 0L) {
                                    app.db.merchants().delete(item)
                                    true
                                } else false
                            }
                        )
                    }
                    QuickAddType.CATEGORY -> {
                        dev.keslorod.quickexpense.ui.manage.ManageListScreen(
                            title = title,
                            onBack = { showManageType = null; coroutineScope.launch { refreshQuickPickOptions() } },
                            mode = dev.keslorod.quickexpense.ui.manage.ListScreenMode.SELECT,
                            onSelect = { category = Option(it.id, it.name); showManageType = null; coroutineScope.launch { refreshQuickPickOptions() } },
                            getName = { it.name },
                            isFavorite = { it.isFavorite },
                            itemKey = { it.id },
                            loadAll = { app.db.categories().all() },
                            addNew = { app.db.categories().insert(dev.keslorod.quickexpense.data.entities.Category(name = it, isFavorite = false)) },
                            toggleFavorite = { app.db.categories().update(it.copy(isFavorite = !it.isFavorite)) },
                            rename = { item, newName -> app.db.categories().update(item.copy(name = newName)) },
                            deleteIfUnused = { item -> app.deleteCategoryIfUnused(item) },
                            isBuiltIn = { it.isBuiltIn() }
                        )
                    }
                    else -> {}
                }
            }
        }

        // Resolving one "not recognized" voice phrase: the same manage/select screens as
        // above, just pre-filled with the phrase so its own "pick a match below, or add new"
        // flow does the resolving — a typo-tolerant existing record beats minting a near-
        // duplicate, and a genuinely new name still gets created with one tap either way.
        if (resolvingVoicePhrase != null) {
            val (phrase, role) = resolvingVoicePhrase!!
            val title = when (role) {
                VoiceEntityRole.MERCHANT -> stringResource(R.string.select_merchant)
                VoiceEntityRole.CATEGORY -> stringResource(R.string.select_category)
                VoiceEntityRole.SOURCE -> stringResource(R.string.select_source)
            }
            fun dismissResolved() {
                voiceLeftoverPhrases = voiceLeftoverPhrases - phrase
                resolvingVoicePhrase = null
            }
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (role) {
                    VoiceEntityRole.MERCHANT -> {
                        dev.keslorod.quickexpense.ui.manage.ManageListScreen(
                            title = title,
                            onBack = { resolvingVoicePhrase = null },
                            mode = dev.keslorod.quickexpense.ui.manage.ListScreenMode.SELECT,
                            initialQuery = phrase,
                            onSelect = { merchant = Option(it.id, it.name); dismissResolved() },
                            getName = { it.name },
                            isFavorite = { it.isFavorite },
                            itemKey = { it.id },
                            loadAll = { app.db.merchants().all() },
                            addNew = { app.db.merchants().insert(dev.keslorod.quickexpense.data.entities.Merchant(name = it, isFavorite = false)) },
                            toggleFavorite = { app.db.merchants().update(it.copy(isFavorite = !it.isFavorite)) },
                            rename = { item, newName -> app.db.merchants().update(item.copy(name = newName)) },
                            deleteIfUnused = { item ->
                                if (app.db.expenses().countByMerchant(item.id) == 0L) {
                                    app.db.merchants().delete(item)
                                    true
                                } else false
                            }
                        )
                    }
                    VoiceEntityRole.CATEGORY -> {
                        dev.keslorod.quickexpense.ui.manage.ManageListScreen(
                            title = title,
                            onBack = { resolvingVoicePhrase = null },
                            mode = dev.keslorod.quickexpense.ui.manage.ListScreenMode.SELECT,
                            initialQuery = phrase,
                            onSelect = { category = Option(it.id, it.name); dismissResolved() },
                            getName = { it.name },
                            isFavorite = { it.isFavorite },
                            itemKey = { it.id },
                            loadAll = { app.db.categories().all() },
                            addNew = { app.db.categories().insert(dev.keslorod.quickexpense.data.entities.Category(name = it, isFavorite = false)) },
                            toggleFavorite = { app.db.categories().update(it.copy(isFavorite = !it.isFavorite)) },
                            rename = { item, newName -> app.db.categories().update(item.copy(name = newName)) },
                            deleteIfUnused = { item -> app.deleteCategoryIfUnused(item) },
                            isBuiltIn = { it.isBuiltIn() }
                        )
                    }
                    VoiceEntityRole.SOURCE -> {
                        dev.keslorod.quickexpense.ui.manage.ManageListScreen(
                            title = title,
                            onBack = { resolvingVoicePhrase = null },
                            mode = dev.keslorod.quickexpense.ui.manage.ListScreenMode.SELECT,
                            initialQuery = phrase,
                            onSelect = { source = Option(it.id, it.name); dismissResolved() },
                            getName = { it.name },
                            isFavorite = { it.isFavorite },
                            itemKey = { it.id },
                            loadAll = { app.db.sources().all() },
                            addNew = { app.db.sources().insert(dev.keslorod.quickexpense.data.entities.Source(name = it, isFavorite = false)) },
                            toggleFavorite = { app.db.sources().update(it.copy(isFavorite = !it.isFavorite)) },
                            rename = { item, newName -> app.db.sources().update(item.copy(name = newName)) },
                            deleteIfUnused = { item ->
                                if (app.db.expenses().countBySource(item.id) == 0L) {
                                    app.db.sources().delete(item)
                                    true
                                } else false
                            }
                        )
                    }
                }
            }
        }

        val scanToShow = lastScan
        if (showGallery && scanToShow != null) {
            AlertDialog(
                onDismissRequest = { showGallery = false },
                confirmButton = {
                    TextButton(onClick = { showGallery = false }) {
                        Text(stringResource(R.string.close))
                    }
                },
                title = { Text(stringResource(R.string.receipt_gallery_title)) },
                text = {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(scanToShow.files) { file ->
                            AsyncImage(
                                model = file,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(180.dp)
                            )
                        }
                    }
                }
            )
        }

        if (showSplitEditor) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                SplitEditorScreen(
                    app = app,
                    totalAmount = toCents(amountText),
                    currency = currency,
                    initialNodes = draftSplitNodes,
                    initialTags = draftNodeTags,
                    initialLabel = merchant?.label ?: stringResource(R.string.transaction_default),
                    defaultCategoryId = category?.id,
                    initialDiverged = splitCategoryDiverged,
                    onDivergedChange = { diverged ->
                        splitCategoryDiverged = diverged
                        if (diverged) category = null
                    },
                    onBack = { showSplitEditor = false },
                    onDone = { nodes, tags ->
                        draftSplitNodes = nodes
                        draftNodeTags = tags
                        showSplitEditor = false
                    }
                )
            }
        }
    }
}

@Composable
fun OperationModeSelector(
    selectedMode: OperationMode,
    onModeChange: (OperationMode) -> Unit,
    onOpenTemplates: () -> Unit
) {
    val modes = listOf(
        OperationMode.EXPENSE to stringResource(R.string.expense),
        OperationMode.INCOME to stringResource(R.string.income),
        OperationMode.TRANSFER to stringResource(R.string.transfer)
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.height(48.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            modes.forEach { (mode, label) ->
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { 
                            if (isSelected) onOpenTemplates() else onModeChange(mode)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AmountDisplay(amount: String, currency: String) {
    val displayAmount = if (amount.isEmpty()) "0" else amount
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$displayAmount $currency",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun PickerRow(label: String, value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CustomNumPad(
    onDigit: (String) -> Unit,
    onDot: () -> Unit,
    onDelete: () -> Unit
) {
    val buttons = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        ".", "0", "⌫"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until 4) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (j in 0 until 3) {
                    val text = buttons[i * 3 + j]
                    NumButton(
                        text = text,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (text) {
                                "." -> onDot()
                                "⌫" -> onDelete()
                                else -> onDigit(text)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NumButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = modifier.height(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun SaveDateButtons(enabled: Boolean, onSave: (Int) -> Unit, onOpenCalendar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SecondaryDateButton(
            text = stringResource(R.string.yesterday),
            enabled = enabled,
            onClick = { onSave(-1) },
            modifier = Modifier.weight(1f)
        )
        PrimaryDateButton(
            text = stringResource(R.string.today),
            enabled = enabled,
            onClick = { onSave(0) },
            modifier = Modifier.weight(1.2f)
        )
        SecondaryDateButton(
            icon = Icons.Default.CalendarToday,
            text = stringResource(R.string.calendar),
            enabled = enabled,
            onClick = onOpenCalendar,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PrimaryDateButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier.height(56.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryDateButton(text: String = "", icon: ImageVector? = null, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        // The default 24dp side padding left too little room in a third of the row: "Calendar"
        // wrapped onto two lines.
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = modifier.height(56.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            if (text.isNotEmpty()) Spacer(Modifier.width(4.dp))
        }
        if (text.isNotEmpty()) {
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun QuickGridPanel(
    type: QuickAddType,
    sourceOptions: List<Option>,
    merchantOptions: List<Option>,
    categoryOptions: List<Option>,
    onDismiss: () -> Unit,
    onSelect: (QuickAddType, Option) -> Unit,
    onManage: (QuickAddType) -> Unit
) {
    val title = when (type) {
        QuickAddType.SOURCE -> stringResource(R.string.select_source)
        QuickAddType.MERCHANT -> stringResource(R.string.quick_choice_to_where)
        QuickAddType.CATEGORY -> stringResource(R.string.select_category)
        QuickAddType.TEMPLATES -> stringResource(R.string.templates_expense)
    }

    val items = when (type) {
        QuickAddType.SOURCE -> sourceOptions
        QuickAddType.MERCHANT -> merchantOptions
        QuickAddType.CATEGORY -> categoryOptions
        QuickAddType.TEMPLATES -> listOf(
            Option("t_lidl", "Lidl"),
            Option("t_maxi", "Maxi"),
            Option("t_coffee", "Coffee"),
            Option("t_taxi", "Taxi"),
            Option("t_netflix", "Netflix"),
            Option("t_salary", "Salary")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                Modifier
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Box(
                    Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(24.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(items) { item ->
                        GridCard(item.label, onClick = { onSelect(type, item) })
                    }
                    if (type != QuickAddType.TEMPLATES) {
                        item {
                            ActionCard(
                                title = if (items.isEmpty()) "+" else "…",
                                onClick = { onManage(type) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun GridCard(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.height(72.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ActionCard(title: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** The local calendar day of [cal] as Material3 DatePicker represents a day: UTC midnight. */
internal fun localDayToUtcMillis(cal: Calendar): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis

/** Inverse of [localDayToUtcMillis]: that day in the local zone, at [timeOfDayFrom]'s time. */
internal fun utcMillisToLocalDay(utcMillis: Long, timeOfDayFrom: Calendar): Calendar {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return (timeOfDayFrom.clone() as Calendar).apply {
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }
}
