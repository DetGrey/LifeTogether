# Phase 3 — Route/Screen Split

**Status:** Complete

## Goal

Separate every feature screen into a route composable (Android wiring) and a screen composable (plain, previewable). Set up the shared `SnackbarHost` at the app root. Standardise lifecycle-aware state collection. Migrate error handling to a snackbar-first approach.

## Scope

- All feature screens under `ui/feature/`
- `ui/navigation/NavHost.kt`
- App root / `MainActivity` scaffold
- `ui/common/event/` (UI event + command contracts and collectors)

## Key Decisions Already Made

- **Route composable** responsibilities: resolves nav args, obtains `ViewModel` via `hiltViewModel()`, reads shared state if needed, passes plain state + callbacks down.
- **Screen composable** responsibilities: receives plain state and callbacks only — no `ViewModel`, no Hilt, fully previewable.
- All route/screen collection layers use `collectAsStateWithLifecycle()` (not `collectAsState()`).
- Plain reusable composables stay free of Android-only collection APIs.
- **All screens must have real `@Preview` support** — `"Preview requires AppSessionViewModel"` is banned.
- **Shared `SnackbarHost`** wired at the app root with a reusable route-level collector.
- Naming convention (locked):
- `UiEvent` means user/UI input flowing into `ViewModel` (for example click, text change, retry tap).
- `NavigationEvent` means user/UI navigation intent emitted by a screen and handled by the route (not by `ViewModel`).
- Keep `UiState`, `UiEvent`, `NavigationEvent`, and `Command` together in the feature `Models.kt` file when the feature has one; do not split navigation events into separate files.
- For screens already migrated in this branch, normalize any remaining model declarations into the feature `Models.kt` file before committing the issue.
- Leave not-yet-refactored screens alone until their route/screen split issue is active.
- `Command` means one-shot output emitted from `ViewModel` to UI (for example `ShowSnackbar`).
- Concrete naming for this phase:
- shared base type is `UiCommand`
- shared command is `UiCommand.ShowSnackbar(message, actionLabel?)`
- feature command types are `<Feature>Command : UiCommand`
- Shared command scope for this phase is **snackbar/error only** (`ShowSnackbar`); navigation and other one-off effects stay feature-local.
- Feature layers emit typed `Command`s (via `Channel`) — they do not render error dialogs directly.
- Input API migration is required in this phase: split screens must migrate `ViewModel` input handling to typed `UiEvent` dispatch (`onEvent(event)`), not keep ad-hoc public method callbacks.
- Per-screen migration contract for this phase:
- `Screen` emits `<Feature>UiEvent` and `<Feature>NavigationEvent` through separate callbacks (`onUiEvent`, `onNavigationEvent`).
- `Route` handles `<Feature>NavigationEvent` directly via `AppNavigator`.
- `Route` maps `<Feature>UiEvent` to `viewModel.onEvent(...)`.
- `ViewModel` exposes lifecycle-collected state (`StateFlow<UiState>`) and one-shot commands (`Flow<UiCommand>`).
- `Screen` never calls `ViewModel` methods directly.
- For screens with both VM-bound actions and nav-only actions, keep separate callbacks instead of folding navigation into `UiEvent`.
- Keep `NavigationEvent` in the feature `Models.kt` file alongside the other models when that file exists.
- Any navigation triggered by `ViewModel` logic is emitted as a `Command`, not a `UiEvent`.
- `ViewModel` startup work uses `init` when no dynamic route argument is required; explicit load events are reserved for argument-driven/dynamic loads.
- For complex screens with repeated gating/layout logic, the route may precompute ordered presentation models (for example sections/tiles) so the screen stays declarative and does not repeat access checks.
- Prefer explicit sealed presentation models over nullable UI strings/flags when the screen has multiple visible states.
- Snackbar design (locked for Phase 3):
- Global root-level snackbar styling (single implementation in app root `SnackbarHost`).
- Error-only visual variant in this phase.
- Top-floating card placement (top-center), not bottom docked default styling.
- Visual parity target with existing `ErrorAlertDialog`: rounded card, error container color, white title/body text.
- Content format: fixed title `"An error occurred"` + error message text.
- Dismissible + auto-dismiss behavior; retain default snackbar queue behavior for repeated errors.
- **Snackbar-first error handling:** validation → inline field errors; operation failures → snackbar/banner; blocking dialogs reserved for rare high-severity interruptions only.
- Each screen has at most one meaningful `Scaffold`; no extra scaffolds just for error handling.
- Pilot screen: `ui/feature/admin/groceryList/suggestions/AdminGrocerySuggestionsScreen.kt`.

## Subphases

- [x] 3.1 Wire shared `SnackbarHost` at app root; define typed `Command` base / collection helper
- [x] 3.2 Pilot split: `AdminGrocerySuggestionsScreen` — route + screen + preview
- [x] 3.3 Split remaining feature screens (one by one or grouped by feature)
- [x] 3.4 Enforce `collectAsStateWithLifecycle()` across all route layers
- [x] 3.5 Migrate `ErrorAlertDialog` usages to snackbar/command pattern

### 3.1 Implementation detail

- Create `ui/common/event/UiCommand.kt` with minimal shared command contract:
- `sealed interface UiCommand`
- `data class ShowSnackbar(...) : UiCommand`
- Create reusable route-level collector in `ui/common/event/` that:
- collects `Flow<UiCommand>`
- handles `UiCommand.ShowSnackbar` by calling shared root `SnackbarHostState`
- Update app root (`MainActivity`) and `ui/navigation/NavHost.kt` so:
- root owns one `SnackbarHostState`
- root scaffold renders one shared `SnackbarHost`
- `NavHost` and route composables can use the shared host for command handling

### 3.2 Implementation detail

- Split `AdminGrocerySuggestionsScreen` into:
- route composable that owns `hiltViewModel`, state/command collection, setup side-effects
- pure screen composable that receives plain `UiState` + event callbacks
- Replace direct VM method callbacks with `<Feature>UiEvent` callbacks and `onEvent(...)` dispatch
- Replace `ErrorAlertDialog` behavior with `UiCommand.ShowSnackbar`
- Keep and verify working `@Preview` that does not require `ViewModel` or Hilt

### 3.3 Implementation detail

- Apply same route/screen + UiEvent/UiCommand contract to all listed feature groups/issues
- Keep split issues on one shared phase branch (per explicit phase strategy)
- Execute low/medium complexity groups first, then explicit complex-last screens

### 3.4 Implementation detail

- Replace route-layer `collectAsState()` with `collectAsStateWithLifecycle()` for screen state
- Keep non-route reusable composables free from Android lifecycle collection APIs
- Ensure no route/screen pair regresses to direct `collectAsState()` in route scope

### 3.5 Implementation detail

- Remove screen-level `ErrorAlertDialog` usage in migrated feature screens
- Route collectors consume `UiCommand.ShowSnackbar` for operation failures
- Keep inline validation errors inline (field-level) and do not route them to snackbar unless non-field/global
- Delete `ErrorAlertDialog` component only after confirming no remaining usages

### Migration order decision (agreed)

- Split low/medium complexity route/screen pairs first.
- Tackle highest-complexity screens last to reduce risk during pattern stabilization.
- Explicit late-stage screens for this phase: `RecipeDetailsScreen`, `FamilyScreen`, `ListEntryDetailsScreen`, `ProfileScreen`, `AlbumDetailsScreen`.

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria

- [x] Shared root `SnackbarHost` is wired once at app root and used by route command collectors
- [x] `UiCommand` infrastructure exists in `ui/common/event/` with shared `ShowSnackbar` command
- [x] Every split screen follows route/screen separation (`Route` Android wiring, `Screen` plain previewable UI)
- [x] Every split screen migrates input handling to typed `<Feature>UiEvent` via `viewModel.onEvent(...)`
- [x] Every split screen emits navigation through typed `<Feature>NavigationEvent` handled in route (not forwarded to `ViewModel`)
- [x] Every split screen emits one-shot output via `Flow<UiCommand>` from `ViewModel`
- [x] Route layers use `collectAsStateWithLifecycle()` for state collection (no remaining route `collectAsState()` uses)
- [x] `ErrorAlertDialog` usage is removed from migrated split screens and replaced by snackbar command handling
- [x] `ErrorAlertDialog` file is removed only if no usages remain after migration
- [x] All Phase 3 split issues are implemented on the same branch
- [x] No PR is opened before all split issues are complete; one consolidated PR is opened at the end

### Test cases

- [x] Build compiles after setup subphase (shared host + command infra)
- [x] Pilot (`AdminGrocerySuggestions`) renders and works with route/screen split and event/command contract
- [x] For each feature-group issue, representative screen interactions dispatch `UiEvent` and update UI state correctly
- [x] For each migrated screen, operation failure triggers `UiCommand.ShowSnackbar` and snackbar appears via shared host
- [x] Inline validation errors remain inline and are not incorrectly converted to snackbar-only feedback
- [x] Navigation behaviors remain unchanged after route/screen split across all migrated screens
- [x] Existing `@Preview` functions for split screens render without Hilt/`ViewModel` requirements
- [x] Search confirms no remaining `collectAsState()` calls in route-layer state collection
- [x] Search confirms no remaining `ErrorAlertDialog(` usages in completed migration scope

## GitHub Issues

Create milestone `Phase 3: Route/Screen Split` and the following issues assigned to it:

- `[Phase 3] Setup: shared SnackbarHost and Command infrastructure`
- `[Phase 3] Pilot: AdminGrocerySuggestionsScreen`
- `[Phase 3] Route/screen split — Auth+Core` (`Loading`, `Login`, `Signup`, `Home`, `Settings`)
- `[Phase 3] Route/screen split — Family+Profile` (`Family`, `Profile`)
- `[Phase 3] Route/screen split — Grocery+Admin` (`GroceryList`, `AdminGroceryCategories`)
- `[Phase 3] Route/screen split — Recipes` (`Recipes`, `RecipeDetails`)
- `[Phase 3] Route/screen split — Guides` (`Guides`, `GuideCreate`, `GuideDetails`, `GuideStepPlayer`)
- `[Phase 3] Route/screen split — Lists` (`Lists`, `ListDetails`, `ListEntryDetails`)
- `[Phase 3] Route/screen split — Gallery` (`Gallery`, `AlbumDetails`, `MediaDetails`)
- `[Phase 3] Route/screen split — TipTracker` (`TipTracker`, `TipStatistics`)
- `[Phase 3] ErrorAlertDialog migration`

### Phase 3 execution strategy (explicit override)

- All route/screen split issues in this phase are implemented on the **same branch**.
- Use **multiple commits** and keep issue references in commit messages and issue comments for traceability.
- **Do not open any PR** for split work until all split issues are implemented.
- Open **one consolidated PR** for the full split scope after the last split issue is complete.
- The setup/pilot/migration issues remain separate for tracking, but they still use the same shared Phase 3 implementation branch.
