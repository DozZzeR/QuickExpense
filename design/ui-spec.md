# QuickExpense — behaviour spec for UI/UX redesign

This describes **what the app must let people do**, the data behind each screen, and how screens
connect. It deliberately does **not** describe the current visual design: layout, navigation
pattern, component choices and information architecture are open. Treat every "screen" below as a
*job to be done*, not as a frame that must exist.

---

## 1. The product in one paragraph

QuickExpense is an offline personal expense tracker for Android phones. Its one principle:
**capturing a purchase must take seconds; organising it can wait.** You enter an amount, pick
where the money came from, and save — categories, receipt photos, splitting one purchase into
parts and tagging are all optional and can be done later, from the history. Everything stays on
the device: no account, no sync, no ads, no network access at all.

Primary user: someone who pays for several small things a day (groceries, coffee, transport) and
wants to know at the end of the month where the money went, without doing bookkeeping.

---

## 2. Hard constraints (please design within these)

- **Android phone**, one-handed use, typically while standing at a till.
- **Offline only.** No login, no cloud, no "sync" affordances, no social features.
- **One display currency** chosen in settings (default RSD). Expenses in other currencies exist in
  history but are excluded from totals and statistics.
- **Amounts are integers of minor units** (cents/para). No fractional cents.
- **Localised in English, Russian and Serbian.** Labels can grow ~40% in translation; Serbian and
  Russian words are long ("Категория", "Нераспределённый остаток").
- Light and dark themes.
- Data is never lost silently: destructive actions need a way back or a confirmation.

Non-goals for now: budgets/limits per category (only one global widget limit exists), income and
transfers between accounts, multi-user, bank import.

---

## 3. Domain model (the vocabulary the UI must express)

| Thing | Meaning | Notes |
|---|---|---|
| **Expense** | One payment: amount, currency, date+time, **source**, **category**, optional **merchant**, optional note, optional receipt photos | The atomic record |
| **Source** | Where the money came from: Cash, Card, … | Required. User-managed list |
| **Category** | What the money went on: Groceries, Home, … | Required; falls back to a built-in "Unsorted" |
| **Merchant** | Where it was spent: shop, café, pharmacy | Optional. In the UI it is currently called "Recipient / To where" |
| **Tag** | Cross-cutting label: "Weekend trip", "Renovation", "For work" | Can be attached to a whole expense or to one split item. Tags overlap, so tag totals don't add up to the period total |
| **Split** | A tree under one expense: the purchase broken into items, each with its own amount, name, category and tags; items can have sub-items | The part of the sum not assigned to items is **"unallocated"** and is always visible |
| **Receipt** | One or more photos attached to an expense | Multi-page capture; stored on device |

Relationships that matter for the UI: an expense has 0..n split items (a tree, usually 1 level,
sometimes 2); a split item's category may differ from the expense's; statistics are computed over
split items when they exist, otherwise over the whole expense.

---

## 4. Jobs, screen by screen

For each: **why it exists → what data it shows → what you can do → states → where you can go**.
Titles are working names only.

### 4.1 Home / recent

- **Why:** answer "how much have I spent lately" and give one-tap access to adding an expense.
- **Data:** total for the chosen period (day / week / month / all time, set in settings) in the
  display currency; a list of recent expenses (category, source, merchant, date-time, amount, and
  whether a receipt is attached).
- **Actions:** add an expense; open one expense; open statistics; export all data of the period as
  CSV inside a ZIP and hand it to the system share sheet; open settings.
- **States:** empty (no expenses yet — currently just a line of text, an onboarding opportunity);
  normal.
- **Goes to:** add-expense, expense details, statistics, settings.

### 4.2 Add / edit an expense ("quick add")

The most important screen. Opened from home, from the home-screen widget (and then it is the only
screen in the task), and from an existing expense to edit it.

- **Data:** amount being typed, display currency, source, merchant, category, chosen date,
  attached receipt pages, number of split items.
- **Actions:**
  - enter an amount on an on-screen numeric pad (the system keyboard is deliberately not used);
  - pick **source**, **merchant**, **category** — each opens a short list of favourites, with a
    way to reach the full list, search it, and create a new entry inline;
  - **voice input:** one tap, speak "250 coffee Lidl"; the amount is recognised and the remaining
    words are matched against the user's own categories/merchants/sources. Unmatched words are
    offered as "use as merchant / category / source?" chips. A confident parse offers to save
    automatically after a 10-second countdown the user can cancel;
  - **scan a receipt** (see 4.3);
  - **split** the amount (see 4.4);
  - save with a date: "Today", "Yesterday", or a calendar date (future dates not allowed);
  - cancel (discards the draft and any receipt scanned in this session).
- **Rules:** save is blocked while the amount is 0 or no source is chosen, and while the split's
  top-level items add up to more than the amount (a message explains why).
- **States:** new expense vs editing an existing one (editing shows a single "Save" and prefills
  everything, including the existing split and receipts).
- **Goes back to:** wherever it was opened from.

### 4.3 Receipt scanner

- **Why:** a paper receipt is often longer than one frame; it must be captured in pieces that can
  be read later (warranty, checking a line item).
- **Data:** live camera, number of pages captured, a translucent strip of the previous page's
  bottom edge to help line up the next frame, a hint that ~20% overlap is expected.
- **Actions:** capture a page, finish, cancel (discards the pages of this session).
- **States:** permission not granted yet (camera can only start after it is); capturing.
- **Goes back to:** add/edit expense, which then shows the page count and a gallery preview.

### 4.4 Split editor

- **Why:** one payment often covers several categories ("groceries + household + a gift").
- **Data:** the item being split (its name and amount), its child items, and the **unallocated
  remainder**; for each item: name, amount, category, tags.
- **Actions:** carve a new item off the remainder; edit an item (amount, name, category, tags);
  delete an item (and with it its sub-items); drill into an item to split it further; step back up;
  finish. Leaving with unsaved changes asks for confirmation.
- **Rules:** an item cannot exceed the remaining amount of its parent (over-typing clamps to it).
  The category picked on the add screen acts as the default for top-level items until the user
  changes one of them; from then on the expense-level category stops being offered.
- **States:** nothing split yet (only the unallocated row); partially split; fully allocated.

### 4.5 Expense details

- **Why:** look at one purchase later — what it was, what's inside it, its receipt.
- **Data:** merchant, amount, date-time, source, category, note, the split tree with amounts,
  attached receipt photos.
- **Actions:** open a receipt full-screen (pinch-zoom/pan), save a receipt photo to the phone's
  gallery, edit the expense, open the split editor.
- **States:** with/without split, with/without receipts.

### 4.6 Statistics

A period filter (today, yesterday, this week, this month, this year, last 7/30 days, all time; a
custom range is specified but not yet reachable) applies to **every** statistics screen and is kept
while navigating between them.

- **Dashboard:** total for the period with comparison to the previous period (absolute and %),
  number of transactions, average transaction; spending per day as a line; breakdown by
  **category**, by **merchant**, by **tag** — each as a short top-N list with a way to see all.
- **Full list** (categories / merchants / tags): every entry with amount, share of the period
  total, number of items, sorted by amount.
- **Category details:** total for that category, breakdown by merchant inside it, and the list of
  contributing items (a split item, not always a whole expense).
- **Merchant details:** total, comparison with the previous period, breakdown by category, list of
  transactions.
- **Tag details:** total, the tagged items with their dates and merchants.
- **Advanced search:** free text (matches item names and notes) plus filters by categories,
  merchants and tags (tags combine as "any of" or "all of"); results list amount, title, date,
  merchant, category and tags, and open the expense.
- **Receipts list:** every expense that has a receipt, with a thumbnail, merchant and date.
- **States everywhere:** loading, empty ("no data for this period"), error.

Note for the designer: tag totals intentionally overlap and must not be presented as parts of a
whole; the UI currently explains this with a footnote.

### 4.7 Managing lists (sources, categories, merchants, tags)

One reusable screen with two modes: **manage** (from settings) and **pick** (opened from the add
screen, where tapping an entry selects it and closes).

- **Data:** searchable list; each entry shows whether it is a favourite (favourites are what the
  add screen offers first).
- **Actions:** add, rename, toggle favourite, delete. Deleting is refused with an explanation when
  the entry is used by an expense or a split item, or when it is a built-in ("Unsorted" category,
  "Has receipt" tag).

### 4.8 Settings

Currency code; which period the home total and the widget show; an optional spending limit for the
widget and a switch to show "left of the limit" instead of "spent"; app language (system/en/ru/sr);
entry points to the four list-management screens and to the receipts list; a button to add the
home-screen widget.

### 4.9 Home-screen widget

Shows a label, the total for the chosen period (or the remainder of the limit) and the period
name, over a dark card with the app's money-bag artwork. Tapping it opens the add-expense screen
directly. It must stay readable at the smallest size (roughly 2×1 cells) and adapt to bigger ones.

---

## 5. Navigation today (a description, not a recommendation)

```
Home ──► Add/Edit expense ──► Receipt scanner
 │            └──► Split editor ──► item editor (amount, name, category, tags)
 │            └──► pick source / merchant / category ──► full list (search, add new)
 ├──► Expense details ──► receipt viewer
 │            └──► Add/Edit expense (edit)
 ├──► Statistics dashboard ──► categories | merchants | tags (full lists) ──► details ──► Expense details
 │            └──► Advanced search ──► Expense details
 ├──► Settings ──► sources | categories | merchants | tags (manage) ; receipts list
 └──► Export (share sheet)

Home-screen widget ──► Add expense (stand-alone task; saving closes it)
```

Facts worth keeping in mind: the widget path must end the moment the expense is saved; the
statistics period filter is shared across all statistics screens; the expense-details screen is the
only place a receipt can be viewed.

---

## 6. End-to-end flows to optimise

1. **Two-tap capture.** Standing at a till: open (home or widget) → amount → save. Target: under
   5 seconds, one hand, no scrolling, no typing text.
2. **Capture now, sort later.** Same as above without category/merchant, then later, from history,
   assign them — ideally in a fast "inbox" style pass over unsorted expenses.
3. **One receipt, several categories.** A supermarket purchase split into 2–4 parts, one of them
   split further, while the remainder stays visible.
4. **"Where did my money go this month?"** From the total down to a category, to a merchant inside
   it, to a specific purchase and its receipt.
5. **"Find that pharmacy purchase in spring."** Search by text plus filters.
6. **Keeping the lists tidy.** Renaming a merchant typo, marking favourites, deleting an unused
   category.

---

## 7. Open problems we would like ideas on

These are current weaknesses — feel free to solve them by restructuring, not just restyling.

1. **Discoverability of splitting and tags.** Both are core to the product but hidden behind a
   button on the add screen and a dialog inside the split editor. Many users never find them.
2. **Category vs tag** is not obvious to newcomers (one is exclusive, the other overlaps).
3. **No onboarding and weak empty states:** a fresh install shows "0,00" and "No transactions yet".
4. **Sorting later has no home.** There is no "unsorted / needs attention" surface, even though the
   whole premise is capture-now-sort-later.
5. **The period filter is split in two:** home + widget use one period (from settings), statistics
   uses its own filter. Users conflate them.
6. **Custom date range** is specified but unreachable.
7. **Editing an amount after splitting** can invalidate the split; today this just blocks saving
   with a message.
8. **Receipts** are only reachable from an expense or a flat list in settings; nothing shows a
   receipt "needs to be photographed" or ages out.
9. **Statistics are read-only dead ends** — you can see a category is too big, but the next useful
   action (rename, re-categorise, set a limit) isn't offered there.
10. **The widget** carries a lot of meaning in a very small area (period, spent vs remaining,
    limit) and is easy to misread.
11. **Accessibility:** large amounts are the primary data; contrast in dark mode, one-handed reach
    and font scaling all matter more than density.
12. **No way to delete an expense** in the UI at all (yes, really).

---

## 8. What is *not* fixed

Everything visual and structural: navigation pattern (bottom bar, drawer, gestures), whether
statistics is a separate area or part of the timeline, whether the numeric pad or a keyboard is
used, whether splitting is a screen, a sheet or inline, how favourites are surfaced, empty states,
motion, typography, colour. The app's current look is intentionally not described here; the money
bag artwork and the dark "glass" card exist in the icon and widget, and may be kept or dropped.

If a proposal requires data the app doesn't have (e.g. bank categories, geolocation, currency
rates, budgets per category), say so explicitly rather than assuming it exists.
