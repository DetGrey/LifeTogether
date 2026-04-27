# Phase 7 — Shared Component Consolidation

**Status:** Complete _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Remove dead code and eliminate the one remaining component duplicate in `ui/common/`. The root-level text field, dropdown, and date picker text field duplicates listed in the original plan were already cleaned up before this phase started — the remaining work is the `DatePickerDialogViewModel`, the unused component files, and the `ConfirmationDialog` / `CustomConfirmationDialog` split.

## Scope

| Action | Target | Reason |
|--------|--------|--------|
| Delete | `ui/common/dropdown/DarkDropdown.kt` | 0 callers; dead code since Phase 6 theming unified dark mode |
| Delete | `ui/common/textfield/EditableDisplayMediumTextField.kt` | 0 callers; superseded by `EditableTextField` with a preset style |
| Merge + delete | `ui/common/dialog/CustomConfirmationDialog.kt` | 1 caller — add optional `content` slot to `ConfirmationDialog`, migrate caller, delete duplicate |
| Refactor | `ui/common/dialog/CustomDatePickerDialog.kt` | Remove `DatePickerDialogViewModel`; replace with `rememberDatePickerState` |

**Out of scope:**
- Text wrapper components (`TextBodyLarge`, `TextHeadingMedium`, etc.) — not duplicates, useful as-is
- `ConfirmationDialogWithTextField` / `ConfirmationDialogWithDropdown` — not duplicates, distinct responsibilities
- `EditableTextField` — kept as-is; `EditableDisplayMediumTextField` callers migrate to it directly
- Button text in `CustomDatePickerDialog` ("Confirm"/"Dismiss") — consistent across all callers, no value in parameterising

## Key Decisions

- The original duplicate file pairs (`ui/common/CustomTextField.kt`, `ui/common/DropDown.kt`, `ui/common/DatePickerTextField.kt`) no longer exist — they were removed before this phase.
- Dead code is deleted outright, not deprecated.
- `ConfirmationDialog` gains an optional `content: @Composable () -> Unit = {}` parameter so all existing callers are unaffected.
- `CustomConfirmationDialog` is deleted once `AlbumDetailsScreen` is migrated to the updated `ConfirmationDialog`.
- `CustomDatePickerDialog` replaces `hiltViewModel<DatePickerDialogViewModel>()` with `rememberDatePickerState(initialSelectedDateMillis = selectedDate)` where `selectedDate: Long = System.currentTimeMillis()` is a new parameter — the caller controls the initial date.
- `DatePickerDialogViewModel` is deleted in the same commit.
- Button labels in `CustomDatePickerDialog` remain hardcoded ("Confirm" / "Dismiss") — all current callers use exactly these strings.

## Subphases

- [x] 7.1 Delete `DarkDropdown.kt` and `EditableDisplayMediumTextField.kt`
- [x] 7.2 Add optional `content` slot to `ConfirmationDialog`; migrate `AlbumDetailsScreen` from `CustomConfirmationDialog`; delete `CustomConfirmationDialog`
- [x] 7.3 Add `selectedDate` parameter to `CustomDatePickerDialog`; replace `DatePickerDialogViewModel` with `rememberDatePickerState`; delete `DatePickerDialogViewModel`

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.

### Acceptance criteria

- [x] `DarkDropdown.kt` and `EditableDisplayMediumTextField.kt` are deleted and no compile errors remain.
- [x] `ConfirmationDialog` has an optional `content` slot; all existing callers compile unchanged.
- [x] `CustomConfirmationDialog.kt` is deleted; `AlbumDetailsScreen` uses `ConfirmationDialog` with the content slot.
- [x] `CustomDatePickerDialog` uses `rememberDatePickerState` directly; no ViewModel injection.
- [x] `DatePickerDialogViewModel` class is deleted.
- [x] Zero new raw color or hardcoded shape references introduced.

### Test cases

- [ ] Verify all confirmation dialogs (Profile, Family, Settings, GroceryList, RecipeDetails, AlbumDetails, GuideDetails, ListDetails, TipTracker) still render and dismiss correctly.
- [ ] Verify the "Move to another album" dialog in `AlbumDetailsScreen` still renders the album picker grid.
- [ ] Verify the date picker dialog in SignUp still opens, selects a date, and calls back with the correct value.
- [ ] Run a full build and confirm zero compile errors.

## GitHub Issues

Create milestone `Phase 7: Shared Component Consolidation` and the following issue assigned to it:

- `[Phase 7] ui/common cleanup — remove dead code, consolidate dialogs, remove DatePickerDialogViewModel`
