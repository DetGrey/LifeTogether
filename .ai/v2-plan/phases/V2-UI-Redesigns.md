# V2 UI Redesigns

This file tracks planned UI redesigns. Each section covers one screen or component, with all design decisions resolved before implementation begins.

---

## Redesign 1 — Note Entry Screen

**Target:** `notesEntryForm` inside `ListEntryDetailsScreen.kt`
**Status:** Design complete, not yet implemented

### Goal

Replace the current note entry form (plain `CustomTextField` with a separate edit/preview mode toggle) with a full-screen, notes-app-style layout using a custom multiline text area with inline markdown styling.

---

### Architecture

- The `when(val details = contentState.details)` dispatch is **lifted before the LazyColumn** in `ListEntryDetailsScreen`. The `Note` case dispatches to `NoteEntryContent`, the other four entry types (Routine, Wish, Checklist, Meal) keep the existing `LazyColumn` + shared save button structure unchanged.
- `NoteEntryContent` lives in its own file: `ui/feature/lists/entryDetails/NoteEntryContent.kt`, same package as the other entry detail files.
- **Same ViewModel** (`ListEntryDetailsViewModel`) — no new ViewModel.
- The outer shared save button (`PrimaryButton`) is hidden for Note entries. Condition: `contentState.isEditing && contentState.details !is EntryDetailsContent.Note`.

---

### Top Bar

- `AppTopBar` gets a new optional parameter: `rightText: String?` (defaults to `null`). When set, renders a `TextButton` in the actions slot instead of `IconButton`. All existing call sites are unaffected.
- For the note entry:
  - Top bar `text = ""` (empty — the note's own title field provides identity)
  - `rightText = "Done"` when `isEditing = true`, no right action when `isEditing = false`
  - Back arrow always shown

---

### Layout Structure

```
Scaffold (AppTopBar with ← and optional "Done")
└── NoteEntryContent
    └── Column(fillMaxSize + verticalScroll + padding horizontal=medium)
        ├── BasicTextField  ← Title (headlineMedium, ImeAction.Next)
        ├── HorizontalDivider
        └── BasicTextField  ← Body (bodyLarge, ImeAction.Default, heightIn(min=remainingHeight))
```

- The outer `Column` uses `Modifier.verticalScroll(rememberScrollState())` — the whole screen scrolls as one unit, no nested scroll issues.
- The body `BasicTextField` has `Modifier.heightIn(min = ...)` derived from `LocalConfiguration.current.screenHeightDp` minus the title area and top bar height. This ensures tapping below the text always focuses the body field.
- Both fields use `BasicTextField` inline (no extracted `NoteTextField` function). Styling is applied directly.

---

### Edit vs View Mode

- **Edit mode (`isEditing = true`):** Both fields are active and editable. Top bar shows "Done".
- **View mode (`isEditing = false`):** Both fields render with `enabled = false` (shows content, no cursor). The body area has `Modifier.clickable { onUiEvent(EnterEditMode) }` — tapping anywhere enters edit mode. No explicit edit icon in the top bar.
- The `isPreviewMode` flag on `NoteEntryFormState` is **removed**. The mode toggle row is **removed**. `PreviewModeChanged` event is **removed**.

---

### Keyboard & Focus

- When creating a new entry (`isEditing = true && !isExistingEntry`): the title field is auto-focused via `FocusRequester` + `LaunchedEffect(Unit)`, opening the keyboard immediately.
- Title: `ImeAction.Next` — pressing Next moves focus to the body field.
- Body: `ImeAction.Default` — the return key inserts newlines.

---

### Placeholder Text

- Title field: `"Title"` shown when value is blank (via `decorationBox` overlay `Text`)
- Body field: `"Start writing…"` shown when value is blank

---

### Markdown Visual Transformation

- File: `ui/common/textfield/MarkdownTextField.kt`
- Contains a `MarkdownVisualTransformation` class implementing `VisualTransformation`.
- Applied to the body `BasicTextField` via `visualTransformation = MarkdownVisualTransformation()`.
- Supported patterns (line-start only):

| Input           | Display                                                    |
|-----------------|------------------------------------------------------------|
| `# Heading`     | bold + `headlineSmall` style                               |
| `## Subheading` | bold + `titleLarge` style                                  |
| `- item`        | bullet point style (same weight, slightly indented colour) |

- Inline patterns (`**bold**`, `*italic*`) are **not** included in this iteration.
- The `VisualTransformation` approach means syntax characters remain visible but styled — no cursor offset mapping issues.

---

### Domain Model Changes

- `NoteEntry.markdownBody: String` → `NoteEntry.body: String` (full rename)
- `NoteEntryFormState.markdownBody` → `NoteEntryFormState.body`
- Firestore field key also renamed to `"body"` — no migration needed (no existing entries)
- All references updated: `NoteEntry.kt`, `NoteEntryEntity.kt`, `UserListFirestoreDataSource.kt`, `UserListLocalDataSource.kt`, `UserListRepositoryImpl.kt`, `ListEntryDetailsModels.kt`, `ListEntryDetailsSaver.kt`, `ListEntryDetailsFormReducer.kt`, `ListEntryDetailsScreen.kt`, `ListDetailsScreen.kt`

---

### Files Changed

| File                                                           | Change                                                                       |
|----------------------------------------------------------------|------------------------------------------------------------------------------|
| `ui/common/AppTopBar.kt`                                       | Add `rightText: String?` parameter                                           |
| `ui/common/textfield/MarkdownTextField.kt`                     | **New file** — `MarkdownVisualTransformation`                                |
| `ui/feature/lists/entryDetails/NoteEntryContent.kt`            | **New file** — note entry composable                                         |
| `ui/feature/lists/entryDetails/ListEntryDetailsScreen.kt`      | Lift `when` dispatch, remove `notesEntryForm`, hide save button for Note     |
| `ui/feature/lists/entryDetails/ListEntryDetailsModels.kt`      | Remove `isPreviewMode`, `PreviewModeChanged`; rename `markdownBody` → `body` |
| `domain/model/lists/NoteEntry.kt`                              | Rename field                                                                 |
| `data/model/NoteEntryEntity.kt`                                | Rename field                                                                 |
| `data/remote/UserListFirestoreDataSource.kt`                   | Rename Firestore key                                                         |
| `data/local/source/UserListLocalDataSource.kt`                 | Rename field                                                                 |
| `data/repository/UserListRepositoryImpl.kt`                    | Rename field                                                                 |
| `ui/feature/lists/listDetails/ListDetailsScreen.kt`            | Rename field reference                                                       |
| `ui/feature/lists/entryDetails/ListEntryDetailsSaver.kt`       | Rename field                                                                 |
| `ui/feature/lists/entryDetails/ListEntryDetailsFormReducer.kt` | Rename field                                                                 |

---

## Redesign 2 — Meal Planner Entry Screen

**Target:** `mealEntryForm` inside `ListEntryDetailsScreen.kt`
**Status:** Design in progress

### Goal

Create a focused meal-planner-style entry screen that makes it fast to schedule a recipe or a custom meal name for a specific date. The UI should feel consistent with the `NoteEntryContent` approach (its own file) and reuse existing primitives (the TipTracker date picker and `TagOptionRow` chips).

---

### Architecture

- Lift the `when(val details = contentState.details)` dispatch before the `LazyColumn` in `ListEntryDetailsScreen` and dispatch the `Meal` case to a new `MealPlannerEntryContent` in its own file: `ui/feature/lists/entryDetails/MealPlannerEntryContent.kt`.
- **Same ViewModel** (`ListEntryDetailsViewModel`) — no dedicated ViewModel. UI writes updates via `ListEntryDetailsUiEvent.Meal.*` events already present in `ListEntryDetailsModels.kt`.
- The outer shared save button (`PrimaryButton`) remains in use (unlike Note entries) — the meal entry keeps the normal flow: form edits are saved by the shared save action.

---

### Fields

- **Date**: Required. Defaults to today. Uses the existing `DatePickerTextField` / `DatePickerDialog` pattern from `TipTracker` and `Signup` screens.
- **Mode**: `Recipe` vs `Custom` — shown as a compact segmented control (two adjacent chip-style buttons). Only the active mode's input is visible.
- **Recipe selector** (visible when `Recipe` mode): an as-you-type search field that shows suggestions (name + prep time). Selecting a suggestion sets `recipeId` in the ViewModel. No thumbnails, no servings shown.
- **Custom name** (visible when `Custom` mode): a single-line text field for a free-form meal name.
- **Meal type**: single-select `TagOptionRow` chips: Breakfast / Lunch / Dinner / Snack / Other.
- **Notes**: optional multiline notes area.

---

### Top Bar & Navigation

- `AppTopBar` behavior follows the Note pattern: show back arrow; when editing show a `rightText = "Done"` action that exits edit mode. Title is empty (entry has its own name).

---

### Edit / View Mode

- Edit mode (`isEditing = true`): form inputs are active. Date picker opens the `DatePickerDialog`.
- View mode (`isEditing = false`): fields are read-only; tapping the main content enters edit mode.

---

### Validation & Save

- Minimal validation: `date` must be present and either `recipeId` (for `Recipe` mode) OR `customMealName` (for `Custom` mode) must be non-empty. The shared `SaveClicked` should be no-op / show inline validation until satisfied.

---

### Implementation Notes

- Use `MealPlanEntryFormState` and `ListEntryDetailsUiEvent.Meal.*` events already defined in `ListEntryDetailsModels.kt`.
- The recipe search dropdown can be implemented gradually: start with a local suggestions list (name + prep time) surfaced from the `RecipesRepository`; wire selection to `RecipeIdChanged`.
- Keep visuals simple for this iteration: no thumbnails, only name + prep time.

