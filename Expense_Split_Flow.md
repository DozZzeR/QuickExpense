# Task: Implement expense split/detailization flow

## Context

We are building an Android personal expense tracker app focused on very fast expense input, but with optional detailed breakdown.

The main quick expense screen already exists. It contains:

- operation mode selector;
- amount input;
- source picker;
- merchant picker;
- optional category picker;
- date save buttons;
- reusable quick grid/list picker components.

Now we need to add price splitting / detailization.

The goal is to let the user save an expense quickly, but optionally split the total amount into meaningful parts later or immediately.

This feature must not turn the quick-add screen into a monster.

The quick-add screen remains fast. Split/detailization lives in a separate screen or large modal flow.

## Core UX principle

**Fast first, detailed later.**

The user must be able to:

1. quickly create an expense without splitting;
2. optionally open split editor before saving;
3. open an already saved transaction later and split/edit it;
4. partially split only the interesting part of a transaction;
5. leave the rest as unallocated/uncategorized.

Do not force the user to categorize or tag every line.

## Main screen changes

### Add two secondary buttons

On the existing quick-add expense screen, add two buttons near the current receipt/action area:

```text
[Split] [Scan]
```

or:

```text
[Detailize] [Scan receipt]
```

Preferred labels for MVP:

- `Split`
- `Scan`

### Behavior

#### Split button

Opens the split editor for the current in-progress transaction draft.

If the transaction is not saved yet, the split editor receives current draft data:

- amount;
- currency;
- source;
- merchant;
- selected category if any;
- date if selected;
- operation type.

If amount is empty or zero, either:

- disable Split button;
- or open editor with warning that total amount is required.

Preferred MVP behavior:

> Disable Split until amount > 0.

#### Scan button

For now this can be stubbed, disabled, or open a placeholder.

Important:

- OCR/receipt scanning is not part of this task.
- Do not implement AI receipt parsing here.
- Do not mix Scan logic with manual Split logic.

## Expense list integration

In the saved expense/history list, each expense item must allow opening the same edit/split flow.

Required actions from expense list item:

1. Tap expense -> open Expense Details / Edit screen
2. Or contextual action: Split / Detailize

The opened screen must be pre-filled with existing transaction data and existing split tree if any.

The user should be able to:

- edit basic transaction data;
- open split editor;
- add/remove/update split nodes;
- save changes.

## Entities

The app has these user-managed list entities:

- Merchant
- Category
- Tag

For MVP:

- merchants are a flat list;
- categories are a flat list;
- tags are a flat list.

Later categories may become a tree, but MVP must not require category tree logic.

## Reusable list management

There is already a reusable parameterized component:

```text
ManageListScreen
```

It must be used for all simple list management.

Create thin wrapper screens around it:

- `ManageMerchantsScreen`
- `ManageCategoriesScreen`
- `ManageTagsScreen`

These wrappers only configure entity-specific labels, fields, validation, repository/use-case, and navigation.

Do not duplicate list CRUD UI.

### ManageListScreen usage

#### ManageMerchantsScreen

Entity:

- Merchant

Fields:

- name;
- optional note later;
- archived flag later.

MVP:

- name only.

Labels:

- title: `Merchants`
- create button: `Add merchant`
- empty state: `No merchants yet`

#### ManageCategoriesScreen

Entity:

- Category

Fields:

- name;
- optional color/icon later;
- optional parentId later, but not MVP.

MVP:

- name only;
- flat list.

Labels:

- title: `Categories`
- create button: `Add category`
- empty state: `No categories yet`

Important: Categories are budget buckets. They should be mutually exclusive in main budget statistics.

#### ManageTagsScreen

Entity:

- Tag

Fields:

- name;
- normalizedName;
- usageCount optional;
- archived flag later.

MVP:

- name only;
- normalize internally.

Labels:

- title: `Tags`
- create button: `Add tag`
- empty state: `No tags yet`

Important: Tags are overlapping labels for search, context, warranty lookup, people, events, and detailed analysis.

Examples:

- guests
- leha
- beer
- xiaomi_watch
- warranty
- gift

Tags may overlap. One split item can have multiple tags.

## Conceptual model

### Category

Category is a budget bucket.

Rules:

- flat list in MVP;
- one effective category per amount fragment;
- category totals are part-to-whole and should not double count;
- category reports can use pie/bar charts.

Example categories:

- Food
- Alcohol
- Household
- Transport
- Health
- Subscriptions
- Tools
- Electronics
- Other

### Tag

Tag is an overlapping contextual label.

Rules:

- flat list;
- many tags per split node are allowed;
- tag reports are not part-to-whole;
- tag totals may overlap and may exceed total spending;
- tag reports should be ranked lists/tables/bars, not pie charts;
- when filtering by multiple tags, matching split nodes must be counted once.

Examples:

- guests
- leha
- beer
- sweets
- xiaomi_watch
- warranty
- child
- trip

### Split node

Split node is a fragment of an expense amount.

A split node may represent:

- a top-level budget bucket;
- a detail line inside a budget bucket;
- a partially detailed item;
- a product/service line;
- a contextual fragment.

Split depth:

- max depth = 3.

Important: Split depth and category depth are independent. For MVP categories are flat, but split tree can have up to 3 levels.

## Data model

Use exact implementation details suitable for the existing app architecture, but preserve these concepts.

### Expense / Transaction

Pseudo-structure:

```text
Expense {
  id: String
  type: OperationType // EXPENSE, INCOME, TRANSFER
  amount: MoneyAmount
  currency: String
  sourceId: String?
  merchantId: String?
  merchantNameSnapshot: String?
  categoryId: String?
  date: LocalDate
  note: String?
  splitStatus: SplitStatus
  createdAt: Instant
  updatedAt: Instant
}
```

Notes:

- `categoryId` on expense is optional quick-level category.
- `merchantNameSnapshot` can be useful if merchant is renamed later.
- Currency comes from selected source/wallet in MVP.
- Multi-currency wallet is not part of this task.

### SplitNode

```text
SplitNode {
  id: String
  expenseId: String
  parentId: String? // null for root node
  amount: MoneyAmount
  label: String?
  categoryId: String? // optional; can be inherited from nearest parent
  depth: Int
  sortOrder: Int
  createdAt: Instant
  updatedAt: Instant
}
```

Root node:

- one root node per expense;
- root amount equals expense total amount;
- root label defaults to merchant name or `Transaction`;
- root category may be expense category or null.

Child nodes:

- user-created split fragments;
- each child amount must be positive;
- sum(children.amount) must be <= parent.amount.

### SplitNodeTag

Many-to-many relation:

```text
SplitNodeTag {
  splitNodeId: String
  tagId: String
}
```

A split node may have zero, one, or many tags.

### Merchant

```text
Merchant {
  id: String
  name: String
  normalizedName: String
  usageCount: Int?
  lastUsedAt: Instant?
  archivedAt: Instant?
}
```

MVP required:

- id;
- name;
- normalizedName.

### Category

```text
Category {
  id: String
  name: String
  normalizedName: String
  sortOrder: Int?
  archivedAt: Instant?
}
```

MVP:

- flat list;
- no parentId required now.

Future-compatible optional field:

```text
parentId: String?
```

Do not implement category tree UI in this task unless already trivial.

### Tag

```text
Tag {
  id: String
  name: String
  normalizedName: String
  usageCount: Int?
  lastUsedAt: Instant?
  archivedAt: Instant?
}
```

Normalize tag names:

- trim spaces;
- lowercase for comparison;
- prevent duplicate normalized tags.

Display can preserve original user-friendly name if desired.

## Virtual unallocated amount

Do not store `Unallocated` as a real `SplitNode`.

For each node:

```text
remainingAmount = node.amount - sum(child.amount)
```

If `remainingAmount > 0`, UI displays a virtual row:

- Rest / Unallocated

Examples:

- At root level: `Unallocated`
- Inside Food category: `Rest of Food`
- Inside merchant/root without category: `Unallocated`

The virtual row is calculated, not persisted.

Reason: Persisting unallocated as a real row causes sync bugs, double counting, and annoying recalculation issues.

## Category inheritance

A split node may have `categoryId = null`.

For statistics, calculate effective category:

```text
effectiveCategory(node) =
  node.categoryId
  or nearest ancestor categoryId
  or expense.categoryId
  or Uncategorized
```

This allows this flow:

```text
Lidl 10000
├─ Food 6000 category=Food
│  ├─ Sausage 500 category=null, tags=[meat]
│  ├─ Chocolate 700 category=null, tags=[sweets]
│  └─ Rest of Food 4800
├─ Alcohol 1200 category=Alcohol
│  └─ Beer 1200 tags=[beer, guests, leha]
└─ Household 800 category=Household
```

Category statistics:

- Food = 6000
- Alcohol = 1200
- Household = 800

Tag statistics:

- meat = 500
- sweets = 700
- beer = 1200
- guests = 1200
- leha = 1200

If one node has multiple tags, it contributes full amount to each tag report. This is intended.

## Split status

Expense should expose or compute split status:

```text
enum SplitStatus {
  NONE,
  PARTIAL,
  FULL
}
```

Suggested calculation:

```text
root = split root node

if no child nodes under root:
  NONE
else if root remainingAmount > 0:
  PARTIAL
else:
  FULL
```

Optional nuance: If root is fully split, but some child nodes are partially split internally, status can still be `FULL` at root level. For MVP, root-level full split is enough.

If deeper strictness is needed:

```text
FULL only if every expanded node has remainingAmount = 0
```

But this may be too strict for UX. Prefer root-level status for MVP.

## Split editor screen

### Purpose

A dedicated screen or large modal flow for splitting an expense amount.

Do not implement recursive inline tree inside the quick-add screen.

Preferred UI:

- Full screen editor

or:

- Large modal bottom sheet

Full screen is recommended for MVP because split editing can be complex.

### Screen title and header

Header must show current context node.

Root level example:

```text
Lidl
10,000 RSD
Remaining: 8,100 RSD
```

If inside a nested node:

```text
Lidl > Food
6,000 RSD
Remaining: 4,800 RSD
```

Header components:

- back button;
- breadcrumb/context title;
- current node amount;
- remaining amount.

If breadcrumb is too long:

- show last 2 levels;
- truncate middle.

Examples:

```text
Lidl > Food
... > Alcohol > Beer
```

### Main list

Display direct children of the current node.

Each child row/card shows:

- label;
- amount;
- effective category or category context;
- tags chips;
- chevron or action for deeper split.

Example root level:

```text
Food
6,000 RSD
Category

Alcohol
1,200 RSD
Category

Household
800 RSD
Category

Unallocated
2,000 RSD
```

Example inside Food:

```text
Sausage
500 RSD
#meat

Chocolate
700 RSD
#sweets

Vegetables
1,000 RSD
#vegetables

Rest of Food
3,800 RSD
```

### Row behavior

#### Tap normal row

Opens Item Editor Sheet for this split node.

#### Tap chevron / Split deeper

Navigates into this node and shows its children.

This is drill-down navigation, not inline nested tree.

Reason: Showing all nested levels at once creates visual overload.

#### Long press row

Optional MVP:

- can show context menu: edit/delete/move;
- not required.

### Virtual unallocated row behavior

The virtual unallocated row is displayed when `remainingAmount > 0`.

It is not a real persisted node.

Tap behavior options:

Preferred:

- tap `Unallocated` opens quick action:
  - `Assign rest`
  - `Add item from rest`

Alternative:

- it does nothing and bottom action handles assignment.

Recommended MVP: Show unallocated row as informational, and use bottom button:

```text
Assign rest
```

### Bottom sticky action bar

At bottom of split editor:

```text
[+ Add item] [Assign rest] [Done]
```

#### + Add item

Opens Item Editor Sheet for a new child node under current node.

Defaults:

- amount empty;
- label empty;
- category:
  - if current node has effective category, inherit category context;
  - at root level allow choosing category;
- tags empty.

#### Assign rest

Creates or edits one child node with amount equal to current remaining amount.

Flow:

1. User taps `Assign rest`.
2. Open Item Editor Sheet.
3. Amount prefilled with `remainingAmount`.
4. User enters label/category/tags.
5. Save creates a child node.

If `remainingAmount <= 0`:

- disable Assign rest.

#### Done

Returns to previous screen.

If there are invalid nodes:

- show validation errors;
- do not close.

If split is partial:

- allow closing;
- transaction remains `PARTIAL`.

## Item Editor Sheet

### Purpose

Edit or create one split node.

Use a bottom sheet or modal sheet.

Fields:

- Amount
- Name / label
- Category
- Tags

Actions:

- Save
- Delete
- Split deeper

Delete only when editing existing node.

Split deeper only if:

- node exists;
- depth < maxDepth;
- node amount > 0.

At max depth:

- hide or disable Split deeper.

### Amount field

Use the same custom numeric input style as the main expense screen if possible.

Validation:

- amount required;
- amount > 0;
- total siblings amount must not exceed parent amount.

If saving would exceed parent amount, show error:

```text
Items exceed available amount by 250 RSD.
```

### Label field

Optional but recommended.

Examples:

- Food
- Beer
- Sausage
- Xiaomi watch
- Taxi to airport

If label is empty:

- display category name;
- or display `Item`.

### Category field

MVP behavior:

At root level:

- category field is available;
- category comes from flat category list.

Inside a categorized parent:

- category can be shown as inherited context;
- editing category is optional.

Recommended UX: If current parent has an effective category:

```text
Category: Food from parent
[Change]
```

But for MVP, simpler behavior is allowed:

```text
Category: Food
```

where Food is prefilled/inherited and can be changed only if explicitly allowed.

Important: Do not force category selection on every nested detail line. Nested detail lines can rely on inherited category.

### Tags field

Tags are multi-select chips.

UI:

- show selected tags as chips;
- button/chip `+ Add tag`;
- tag picker opens using the existing reusable list picker/grid component.

Rules:

- zero or many tags allowed;
- tags are optional;
- tags are for search/context;
- tags may overlap in reports.

Examples:

- beer
- guests
- leha
- warranty
- xiaomi_watch

### Tag picker behavior

The tag picker should use existing reusable quick picker/list infrastructure where possible.

Behavior:

- show recent tags;
- search tags;
- create tag if not found;
- prevent duplicate normalized tag names.

When user types a new tag:

```text
+ Create "#typed_tag"
```

Normalize:

- trim spaces;
- optionally replace spaces with underscore or allow spaces internally but display with `#` prefix.

Suggested MVP: Allow human-readable tag names, display them as chips with `#`.

Examples:

```text
name: "часы хаоми"
display: "#часы хаоми"
```

or normalized:

```text
normalizedName: "часы_хаоми"
```

Pick one convention and keep it consistent.

## Full edit from expense list

When opening split editor from saved expense list:

1. Load expense by id.
2. Load split root and children.
3. If no root exists, create an in-memory root from expense data:
   - amount = expense.amount;
   - label = merchant name or `Transaction`;
   - categoryId = expense.categoryId.
4. Show root level split editor.
5. On save, persist root and split nodes.

Important: Do not lose existing split data when editing basic transaction data.

If expense amount changes:

- update root amount;
- revalidate children;
- if children sum > new amount, show error or mark overspent.

Preferred: Do not allow saving expense amount smaller than sum of root children unless user adjusts split.

## Reports/statistics implications

This task does not require implementing reports, but data must support them.

### Category reports

Category totals are non-overlapping.

Use:

- leaf split nodes;
- virtual unallocated amounts;
- effectiveCategory.

Category reports can be pie/bar charts.

### Tag reports

Tag totals are overlapping.

Use:

- split nodes with tag relations;
- each node contributes full amount to each assigned tag.

Important report note:

- Tags may overlap. One expense item can appear under multiple tags.
- When filtering by multiple tags, each matching split node must be counted once.

## Validation rules

### Split node amount validation

For every parent node:

```text
sum(children.amount) <= parent.amount
```

If sum is greater:

- show error;
- do not save.

If sum is less:

- valid;
- show remaining amount.

If sum equals:

- fully allocated at this node.

### Depth validation

```text
depth <= 3
```

At max depth:

- no deeper split action.

### Delete validation

When deleting a node:

- delete its children too;
- or ask confirmation if it has children.

Preferred confirmation:

```text
Delete this item and all nested details?
```

## UX copy

Use clear, non-technical labels.

Preferred English labels:

- Split
- Scan
- Add item
- Assign rest
- Done
- Remaining
- Unallocated
- Rest of Food
- Tags
- Category

Alternative Russian labels if app is Russian:

- Разбить
- Скан
- Добавить
- Добить остаток
- Готово
- Осталось
- Нераспределено
- Остаток продуктов
- Метки
- Категория

Avoid:

- `subcategories` for nested detail lines;
- `node`;
- `allocation` in user-facing UI;
- technical words like `inheritedCategory`.

## UX examples

### Example 1: Simple split

Expense:

```text
Lidl
10,000 RSD
```

Root split:

- Food 6,000
- Alcohol 1,200
- Household 800
- Unallocated 2,000

Category stats:

- Food 6,000
- Alcohol 1,200
- Household 800
- Uncategorized 2,000

### Example 2: Detail only alcohol

Root:

```text
Maxi 7,000
├─ Food 5,800
└─ Alcohol 1,200
```

Inside Alcohol:

```text
Beer 800 #beer #guests #leha
Wine 400 #wine #guests
```

Category stats:

- Food 5,800
- Alcohol 1,200

Tag stats:

- beer 800
- wine 400
- guests 1,200
- leha 800

### Example 3: Warranty/product lookup

Expense:

```text
Gigatron
12,000 RSD
Electronics
```

Split/details:

```text
Xiaomi watch 12,000
Tags: #xiaomi_watch #warranty
```

Later user can search by:

```text
#xiaomi_watch
```

and find:

- purchase date;
- merchant;
- amount;
- source;
- receipt/scan if attached later.

This is an intended use-case.

## Non-goals for this task

Do not implement:

- OCR receipt parsing;
- AI categorization;
- category tree UI;
- recurring payments;
- warranty date tracking;
- attachment storage, unless already present;
- advanced analytics screens;
- tag type taxonomy;
- budgets/limits.

Do not redesign the entire quick-add screen.

## Architecture expectations

Keep the implementation layered.

Suggested separation:

### Domain

- Expense
- SplitNode
- Merchant
- Category
- Tag
- SplitNodeTag
- Split validation logic
- Effective category calculation

### Data

- repositories / DAOs
- persistence models
- mapping

### UI

- Quick Add button integration
- SplitEditorScreen
- SplitItemEditorSheet
- ManageMerchantsScreen
- ManageCategoriesScreen
- ManageTagsScreen

### Reusable UI

- ManageListScreen
- QuickGridPanel / picker components

Split validation and effective category calculation should not live only in composables.

## Acceptance criteria

1. Quick Add screen has two secondary actions:
   - Split
   - Scan
2. Split opens a dedicated split editor for the current transaction/draft.
3. Saved expenses can open the same split editor pre-filled with existing data.
4. Split editor shows:
   - current context title;
   - total amount;
   - remaining amount;
   - list of child split rows;
   - virtual unallocated row when needed.
5. User can add, edit, and delete split nodes.
6. User can drill down into a split node up to max depth 3.
7. User can assign categories from flat category list.
8. User can assign multiple tags from flat tag list.
9. Merchants, categories, and tags are managed through wrappers around reusable `ManageListScreen`:
   - `ManageMerchantsScreen`
   - `ManageCategoriesScreen`
   - `ManageTagsScreen`
10. Unallocated amount is calculated, not stored as a real node.
11. Validation prevents children amount sum from exceeding parent amount.
12. Partial split is allowed.
13. Category totals remain non-overlapping.
14. Tag totals are allowed to overlap conceptually.
15. No OCR/AI/category-tree implementation is required in this task.
