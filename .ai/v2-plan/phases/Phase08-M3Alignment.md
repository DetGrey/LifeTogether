# Phase 8 — Native Material 3 Alignment

**Status:** Implementing _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Replace custom component shells with native Material 3 equivalents where the native component satisfies the product need with light styling. Each replacement requires explicit decision — no component is removed without review.
The alignment target includes shared UI in `ui/common/`, feature-local `components/` packages, and screen files themselves wherever custom visual UI can be replaced with native Material 3.
This review includes visually significant feature-local cards, rows, and layout helpers, not just dialogs and top bars, when a native M3 primitive can cover the same need.

## Scope

The components listed below are current examples of likely candidates. During grill-me and implementation prep, we should inspect the rest of the shared UI surface as well, including feature-local `components/` packages and screen files, and add any other custom shells or wrappers that should be aligned to native M3.
The review must produce an explicit list of components that are intentionally kept as-is or ignored, so the phase can say directly which files were reviewed and which were not worth changing.
Phase 8 excludes non-visual plumbing such as `ViewModel`s, routes, DI helpers, event collectors, and sync lifecycle code even when those files live under `ui/common` or `ui/feature`.

### Reviewed Surface

#### Replace or rebuild

- `ui/common/TopBar.kt` -> native top app bars, defaulting to `CenterAlignedTopAppBar`
- `ui/common/dialog/CustomAlertDialog.kt` -> shared snackbar/banner family
- `ui/common/dialog/ErrorAlertDialog.kt` -> already removed in prior work; no remaining file
- `ui/common/dialog/DatePickerDialog.kt` -> native `DatePickerDialog` with remembered dialog state
- `ui/common/textfield/CustomTextField.kt` -> native `TextField` with shared theme defaults
- `ui/common/textfield/DatePickerTextField.kt` -> read-only `TextField` plus the shared date picker dialog pattern
- `ui/common/textfield/EditableTextField.kt` -> keep only if the editable/static split remains clearer than a single family wrapper
- `ui/common/dialog/ConfirmationDialogWithTextField.kt` -> native dialog primitives plus shared text-field wrappers
- `ui/common/dialog/ConfirmationDialogWithDropdown.kt` -> native dialog primitives plus shared dropdown wrappers
- `ui/common/button/AddButton.kt` -> canonical shared FAB wrapper backed by `FloatingActionButton`
- `ui/common/image/ImageUploadDialog.kt` and `ui/common/image/MediaUploadMultipleDialog.kt` -> native dialog primitives plus native form controls and shared wrappers

#### Review individually, keep if still useful

- `ui/common/image/MediaInfoPanel.kt`
- `ui/common/list/CompletableBox.kt`
- `ui/common/ListItem.kt`
- `ui/common/tagOptionRow/TagOptionRow.kt`
- `ui/common/OverflowMenu.kt`
- `ui/common/button/LoveButton.kt`
- feature `components` cards and layout helpers such as `GuideStepCard`, `StepPlayerOverviewCard`, and `StatsCard`
- feature-local media helper shells only when they are mostly layout around native controls

#### Explicitly kept as canonical shared APIs

- `ui/common/text/TextDefault.kt`
- `ui/common/text/TextHeadingLarge.kt`
- `ui/common/text/TextHeadingMedium.kt`
- `ui/common/text/TextSubHeadingMedium.kt`
- `ui/common/text/TextBodyLarge.kt`
- `ui/common/text/TextDisplayLarge.kt`
- `ui/common/dropdown/Dropdown.kt`
- `ui/common/dialog/ConfirmationDialog.kt`
- `ui/common/image/DisplayImageFromUri.kt`
- `ui/common/image/DisplayVideoFromUri.kt`
- `ui/common/image/rememberObservedImageBitmap`
- `ActionSheet` is an experimental native bottom-sheet path; `OverflowMenu` stays for now until we decide which action-surface pattern the app should standardize on.
- The gallery media-details bottom panel should become a native bottom-sheet pattern rather than a custom offset/snap panel; keep `MediaInfoPanel` only as inline content if needed.

| Current component                            | M3 candidate                          | Notes                                                 |
|----------------------------------------------|---------------------------------------|-------------------------------------------------------|
| `ui/common/TopBar.kt`                        | `CenterAlignedTopAppBar`              | Default target; exceptions allowed per screen         |
| `ui/common/dialog/CustomAlertDialog.kt`      | Snackbar/banner alert variant         | Replace with the shared snackbar family; remove shell |
| `ui/common/dialog/ErrorAlertDialog.kt`       | Snackbar / banner (from Phase 3)      | Already removed; no remaining file                    |
| `ui/common/dialog/DatePickerDialog.kt`       | `DatePickerDialog` + remembered state | Native shared picker helper                            |
| `ui/common/button/AddButton.kt`              | `FloatingActionButton`                | Same icon/color everywhere; can become canonical FAB  |

## Key Decisions Already Made

- `CenterAlignedTopAppBar` is the default replacement for `TopBar`. `MediumTopAppBar` and `LargeTopAppBar` are allowed only for documented screen-specific hierarchy needs.
- Current `subText` content should move into screen content below the app bar — not forced into the app bar itself.
- Current left/right `TopBar` icons map to `navigationIcon` and `actions` slots natively.
- Custom UI is **never deleted or replaced without explicit user approval first**.
- If a custom wrapper can be expressed fully with native M3 and shared theme tokens, delete it rather than keeping a thin adapter unless it still centralizes non-visual behaviour.
- Shared snackbar family should cover both error and normal alert presentation; use a severity/variant parameter rather than separate bespoke wrappers.
- Native controls must keep the existing visual language through shared theme defaults or canonical wrappers, not repeated per-file color/style duplication.
- Theme defaults should be the first place to encode the app's visual language; if that is not enough, use one canonical shared wrapper/helper rather than duplicating styling in feature files.
- When a control family needs app-specific styling, keep one canonical shared wrapper/helper for that family in `ui/common/` and migrate callers to it.
- Form controls should be organized by family, with one canonical shared wrapper/helper per family when needed rather than a single generic mega-wrapper.
- `EditableTextField` should stay separate if combining editable input and static display into one API makes the wrapper harder to reason about.
- `DatePickerTextField` should be replaced by a standard read-only `TextField` plus the shared date picker dialog pattern unless repetition justifies a canonical helper.
- `Dropdown` should remain the canonical shared select wrapper if the app needs one central place for anchor/menu colors and interaction defaults.
- Keep `Dropdown` in `ui/common/` as the shared select helper for now.
- Cards and layout helpers in feature `components/` packages should be reviewed individually and replaced only when a native M3 primitive can cover the same visual and interaction need.
- Keep one canonical shared add FAB wrapper in `ui/common/button/` if the add action stays visually identical across the app.
- `ConfirmationDialogWithTextField` and `ConfirmationDialogWithDropdown` should be rebuilt from native dialog primitives plus the shared form-control wrappers rather than kept as custom dialog shells.
- `ConfirmationDialog` should remain the canonical shared confirm/cancel `AlertDialog` wrapper.
- The shared snackbar family should keep the existing top-center host placement and dismissal behavior for both error and normal alerts.
- The typography helpers in `ui/common/text/` should stay as the canonical shared text API for now.
- Small shared visual helpers such as `CompletableBox`, `TagOptionRow`, and `OverflowMenu` should be reviewed individually and kept if they encode a repeated interaction or semantic pattern that would otherwise be duplicated.
- `ListItem` should be rebuilt from native M3 row/list primitives, but can remain the canonical shared row wrapper if the app needs one place for completed-item formatting and optional trailing actions.
- `CompletableBox` should stay as the canonical shared completion toggle wrapper, implemented with native M3 building blocks.
- `TagOptionRow` and `TagOption` should stay as the canonical shared chip-row wrapper, rebuilt on native `FilterChip` primitives.
- `LoveButton` should be removed as a shared wrapper and replaced with a native decorative control in `HomeScreen`, with the replacement left ready to become clickable later.
- `MediaInfoPanel` is not a standalone surface anymore; its content should be inlined into the native bottom-sheet implementation for media details.
- Guide feature cards such as `GuideSectionCard`, `GuideHeroCard`, `GuideStepCard`, and `StepPlayerOverviewCard` should stay as feature-local cards, rebuilt on native card/surface primitives.
- Tip-tracker feature components such as `StatsCard`, `TipsList`, and `TipsCalendar` should stay as feature-local components rebuilt on native card/grid/surface primitives.
- `AddNewTipItem` should stay feature-local and reuse the native-backed shared input/date picker patterns rather than becoming a generic shared add-row family.
- `GuideStepRows`, `CommentBubble`, `GuideStepRowText`, `ListEditorContainer`, and `GrocerySuggestionsEditor` should stay feature-local and be rebuilt on native primitives rather than promoted into shared components.
- `ImageUploadDialog` and `MediaUploadMultipleDialog` should stay feature-specific upload dialogs rebuilt on native dialog primitives.
- `DisplayImageFromUri`, `DisplayVideoFromUri`, and `rememberObservedImageBitmap` are implementation utilities, not UI shells, and are intentionally kept.
- The media screen should keep its custom player integration as a rendering utility, not as a component that needs native Material replacement.
- `CountdownRow` should be inlined and removed; `FeatureOverview` should remain the canonical home-tile component on native card/surface primitives.
- `AppTopBar` should live in the `Scaffold.topBar` slot on every screen, with content padded underneath it instead of embedding the bar inside scroll content.
- `AppTopBar` should be the canonical shared wrapper, backed by native Material top app bar primitives, so colors and icon/text styling stay centralized instead of being repeated per screen.
- `TopBar` should be renamed to `AppTopBar` during the migration.
- `AppTopBar` should keep a single optional trailing action icon rather than a list of actions for now.
- `subText` belongs only on `HomeScreen` content and should not remain part of the reusable top-bar contract.
- Every app screen should use a `Scaffold` layout with `AppTopBar` in the top bar slot unless the screen is a deliberate transient full-screen state such as loading.
- Auth screens (`LoginScreen` and `SignupScreen`) should also use the same `Scaffold` + `TopBar` contract as the rest of the app.
- Loading remains the only deliberate full-screen exception.
- The migration target is `Scaffold(topBar = { AppTopBar(...) })` on every non-loading screen.
- Bottom actions that are currently overlays should move into `Scaffold.floatingActionButton` where the screen layout allows it.
- Action sheets and overflow menus remain content-triggered overlay surfaces rather than scaffold slots.
- Phase 8 implementation order: form controls first, then dialogs/snackbars, then top bars, then cards/layout helpers unless a dependency forces a different sequence.
- Before replacing any component, check whether desired visual result can be achieved through shared theme tokens alone.
- Component policy order: (1) native M3 + theme tokens → (2) one canonical shared wrapper if truly needed → (3) feature-specific custom only if the first two are not enough.

## Grill Outcomes

The following component decisions have been explicitly agreed and should be treated as settled unless a later implementation issue forces a re-open:

- `ListItem` stays as the canonical shared row wrapper.
- `CompletableBox` stays as the canonical completion toggle wrapper.
- `TagOption` and `TagOptionRow` stay as the canonical shared chip-row wrapper.
- `FeatureOverview` becomes a native `Card` tile.
- `RecipeOverview` becomes a native `Card` tile.
- `AlbumContainer` becomes a native `Card` tile.
- `GrocerySuggestionPopup` becomes a native `Card`-based suggestion surface.
- `AddNewListItem` and `EditListItem` use a native `Card` shell and keep the inline text + `>` action, not a filled button.
- `AddNewString` uses the same native `Card` shell and keeps the inline text + `>` action.
- `AddNewTipItem` uses the same native `Card` family while keeping its one-row layout, date trigger, and inline text + `>` action.
- `AddNewIngredient` uses the same native `Card` family and keeps its current layout logic.
- `ItemCategoryList` and `CompletableCategoryList` stay as shared category wrappers around `ListItem`.
- `GuideStepRowText`, `GuideStepCardBody`, and `GuideStepRows` stay as feature-local text/layout helpers.
- `CommentBubble` stays as a feature-local compact surface built on native primitives.
- `StatsCard`, `TipsList`, `TipsCalendar`, `ListEditorContainer`, and `GrocerySuggestionsEditor` stay feature-local and are rebuilt on native primitives.
- `ProfileDetails` becomes a native `Card` row.
- `SettingsItem` becomes a native `Card` row.
- `OverflowMenu` remains in the codebase for now, but the app should use `ActionSheet` for new action-surface usage.
- `GuideDetailsScreen`, `AlbumDetailsScreen`, and `MediaDetailsScreen` now use `ActionSheet` for their action surfaces while keeping `OverflowMenu` only as the legacy shell.
- `AddNewIngredient`, `RecipeOverview`, `AlbumContainer`, and the other tile-like surfaces should not change their general layout, only their native shell.
- The add/edit rows keep their current visual language, including the shared colors, spacing, and the inline text + `>` affordance.
- The tip entry row keeps its current visual language, including the date trigger and one-row structure.
- `ThumbnailContainer` stays custom because it is a layered media helper, not a simple card tile.
- `GrocerySuggestionPopup` becomes a native `Card`-based suggestion surface.

## Subphases

- [x] 8.1 Rebuild the shared input family on native M3 controls with centralized defaults: `CustomTextField`, `DatePickerTextField`, `EditableTextField`, `Dropdown`, and the canonical add FAB wrapper
- [x] 8.2 Rebuild alert, confirmation, and upload surfaces on native dialog/sheet primitives: `CustomAlertDialog`, residual `ErrorAlertDialog`, `ConfirmationDialogWithTextField`, `ConfirmationDialogWithDropdown`, `ImageUploadDialog`, `MediaUploadMultipleDialog`, and the media-details bottom panel
- [x] 8.3 Standardize every non-loading screen on `Scaffold(topBar = { AppTopBar(...) })`, move `subText` into `HomeScreen` content, and keep loading as the only full-screen exception
- [x] 8.4 Review and normalize shared row/chip/action helpers: `ListItem`, `CompletableBox`, `TagOptionRow`/`TagOption`, `OverflowMenu`, `ActionSheet`, `LoveButton`, `CountdownRow`, and `FeatureOverview`
- [x] 8.5 Rebuild feature-local cards and layout helpers on native card/surface/grid primitives: guide cards, tip-tracker cards, guide step rows, grocery editors, and `AddNewTipItem`

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria

- [x] Every non-loading screen uses `Scaffold` with `AppTopBar` in `topBar`.
- [x] `HomeScreen` renders `subText` in content instead of the shared top-bar contract.
- [x] Shared input controls use native M3 primitives plus centralized styling, without repeating color or shape overrides at every call site.
- [x] Shared alert presentation uses one snackbar/banner family for both error and normal alerts, with the existing top-center host behavior.
- [x] Shared confirmation and upload surfaces are rebuilt from native dialog primitives and the shared input/select wrappers.
- [x] Shared helper rows and chips are either kept as canonical wrappers on native primitives or inlined where they are single-use.
- [x] Feature-local cards and layout helpers remain feature-local but are rebuilt on native card/surface/grid primitives where appropriate.
- [ ] `TextDefault` and the other typography helpers remain available as the shared text API and continue to render consistently after the alignment work.

### Test cases

- [ ] Manually inspect representative scaffolded screens to confirm `AppTopBar`, padding, and bottom actions all still match the existing app visual language.
- [x] Verify a representative text-field screen, dropdown screen, and date-input flow still work with the native-backed wrappers.
- [x] Verify the shared alert snackbar renders both error and normal alert variants with the same host placement and dismissal behavior.
- [x] Verify the revised dialog flows still allow selecting text, dropdown values, and dates before confirming.
- [ ] Verify media detail screens still render the image/video area and the new bottom-panel behavior correctly.
- [x] Verify feature-local guide and tip-tracker card screens still render correctly after the native-primitive rebuilds.
- [x] Verify every updated component preview still compiles and reflects the intended native M3 styling.

## GitHub Issues

Create milestone `Phase 8: Native Material 3 Alignment` and the following issues assigned to it:

- [Phase 8] [8.1] Rebuild shared input controls and add FAB -> issue `#68`
- [Phase 8] [8.2] Rebuild dialogs, uploads, and the media bottom sheet -> issue `#69`
- [Phase 8] [8.3] Standardize screens on Scaffold and AppTopBar -> issue `#70`
- [Phase 8] [8.4] Normalize helper rows, chips, and action surfaces -> issue `#72`
- [Phase 8] [8.5] Rebuild feature-local cards and layout helpers -> issue `#71`

> Pattern: one issue per implementation surface, not one issue per component. The issue list should match the grouped subphases and preserve the implementation detail already captured in the phase file.
