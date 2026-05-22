# LifeTogether Architecture (Current State)

This document describes the current implementation reality of the app.
Historical phase decisions remain in `.ai/v2-plan/` and are not duplicated here.

## Layering

- `data/`: local and remote data sources, repository implementations, data-layer logic, and mapping helpers
- `domain/`: domain models, repository interfaces, result types, logic, and use cases
- `ui/`: routes, screens, viewmodels, navigation, theme, and shared UI components
- `di/`: Hilt modules, qualifiers, and dependency wiring

## Theme System

- `LifeTogetherTheme` owns the app's Material 3 theme wiring.
- Feature and common composables should read colors, typography, and shapes from `MaterialTheme`.
- `LifeTogetherTokens` is a foundational token layer for spacing and sizing only.
- Use `LifeTogetherTokens.spacing` for `Spacer` sizes, padding, and other spacing-like gaps.
- Use `LifeTogetherTokens.sizing` for icons and other small UI affordances that need shared sizing.
- Real element heights and widths can be hardcoded when they are part of the actual layout; do not force them through sizing tokens just to satisfy the token layer.
- Raw colors, typography overrides, and ad hoc shapes belong in the theme layer, not in feature screens.

## Shared Components

- Prefer native Material 3 primitives first.
- Keep a shared wrapper only when it centralizes repeated styling or behavior that would otherwise be duplicated across screens.
- Shared wrappers should stay canonical and narrow, not become another layer of custom theming logic.
- When a custom shared component already exists for a need, use it unless it cannot express the requirement. This applies to dialogs, buttons, and similar repeated UI building blocks.
- `AppTopBar` is the canonical shared top-bar wrapper, backed by native Material 3 top app bar primitives, and is the default top bar for non-loading screens.
- `ConfirmationDialog` is the canonical shared confirm/cancel dialog.
- `Dropdown` stays the canonical shared select wrapper.
- `ActionSheet` is the preferred shared action surface for new menu-style actions.
- `OverflowMenu` remains as the legacy action surface for existing callers only.
- Shared text wrappers in `ui/common/text/` stay available as the canonical text API.
- Feature-local helper cards and layout shells should stay feature-local unless they are clearly repeated across screens.
- Trashcan icons should be tinted with `MaterialTheme.colorScheme.error` wherever they appear.

## Motion And Loading

- Full-screen route transitions are centralized in `NavHost` and use one shared subtle slide plus crossfade preset.
- The same route transition applies to top-level routes and nested graph destinations.
- Do not add per-screen navigation transitions unless a later exception is explicitly documented.
- UI that appears or disappears should use `AnimatedVisibility` or `animateContentSize` instead of abrupt instant toggles when the motion is user-facing.
- Skeleton loaders are the default pattern for data-fetching loading states.
- The app-launch auth gate remains the deliberate full-screen loading screen exception.
- Spinner-style loading remains appropriate for inline saving, pull-to-refresh, dialogs, and other compact transient loading actions.
- Skeletons are for initial load and in-place loading placeholders, not for refresh spinners or save progress.
- The shared `Skeletons` family should stay fixed and reusable rather than becoming one-off screen templates.
- Error states stay separate from loading states.
- `SyncUpdatingText` and similar persistent sync-status text are not part of the current architecture.

## App Shell

- `MainActivity` owns the top-level Compose shell.
- The root shell provides:
  - `LifeTogetherTheme`
  - a root `Scaffold`
  - the navigation host
  - the shared snackbar host
  - the progress snackbar overlay
  - the eager `RootCoordinatorViewModel`
- `NavHost` is the single app navigation entry point.
- `RootCoordinatorViewModel` is intentionally created eagerly from `MainActivity` so session and app-level coordination starts even before a screen specifically requests it.

## Session Boundary

- Durable app session state is owned by `SessionRepository` (`domain/repository/SessionRepository.kt`).
- `SessionRepository` is the only source of truth for session state.
- Session contract is `SessionState` with:
  - `Loading`
  - `Unauthenticated`
  - `Authenticated(user: UserInformation)`
- `SessionRepositoryImpl` is a singleton and owns:
  - auth-state observation
  - resolving the active user profile
  - session sign-out orchestration
- `SessionRepositoryImpl` listens to Firebase auth state and resolves the signed-in user through the user repository.
- `RootCoordinatorViewModel` is activity-scoped and reacts to session transitions for root-level coordination concerns such as observer orchestration, guide sync, and FCM token handling.
- Feature viewmodels may observe `SessionRepository` directly when they need session context.
- There is no app-scoped session viewmodel threading through routes anymore.
- `LoadingRoute` is the app's startup/auth gate and only redirects based on `SessionRepository.sessionState`.
- `LoadingRoute` does not own app business logic.

## UI Command and Snackbar Contract

- The app uses the shared `UiCommand` channel pattern for transient UI effects.
- Feature routes must collect `viewModel.uiCommands` with `CollectUiCommands(...)`.
- `CollectUiCommands` is the default path for:
  - snackbar errors
  - progress snackbars
  - other one-off effects emitted by a viewmodel
- Shared `UiCommand` is reserved for shared UI effects such as snackbar and progress snackbar handling.
- Feature-local one-shot `Command` flows are allowed when a feature needs an output outside the shared `UiCommand` contract, especially for navigation driven by ViewModel logic.
- The root snackbar host is provided through `LocalRootSnackbarHostState`.
- Direct `LocalRootSnackbarHostState.showSnackbar(...)` calls are allowed only for route-level UI guards or other non-viewmodel checks that are inherently local to the route.
- ViewModels should emit snackbar intents through `UiCommand.ShowSnackbar` or `UiCommand.ShowProgressSnackbar` instead of talking to the snackbar host directly.
- `AppSnackbar` is the shared snackbar renderer for both normal and progress snackbars.
- Route-local helpers such as image-loading callbacks may still use the shared snackbar host directly when they are not driven by a ViewModel-owned error flow.

## Navigation

- The app uses Navigation 3 as the navigation foundation.
- `AppRoute` is the typed route contract and also implements `NavKey`.
- `NavHost` owns the root `NavBackStack<NavKey>` and renders destinations through `NavDisplay`.
- `AppNavigator` is a back-stack reducer over Navigation 3 state, not a `NavController` wrapper.
- `AppNavigator` exposes `navigate(...)`, `navigateBack()`, `navigateTopLevel(...)`, `navigateReplacing(...)`, `clearAndNavigate(...)`, and typed `navigateBack(result)` for one-shot return flows.
- The app uses a typed `NavigationResult` channel for back results; the current implemented case is `MealPlannerFocusDate`.
- Destination `ViewModel`s do not read navigation arguments through `SavedStateHandle` string keys.
- Route wrappers own argument extraction and translate typed route keys into screen-specific inputs.
- The only accepted raw destination string path is the notification entry point, where the notification layer maps the string to a typed `AppRoute` before navigation occurs.
- No other navigation path may build or consume raw route strings.
- Login success clears the whole stack and goes to `Home`.
- Logout clears the whole stack and goes to `Login`.
- Loading screen transitions clear the stack instead of leaving `Loading` underneath.
- Home navigation uses a top-level helper so profile, settings, and other top-level destinations do not accumulate duplicate history.
- Profile and Settings keep their local back relationship:
  - `Profile -> Settings` pushes `Settings`
  - `Settings -> Profile` goes back to the existing `Profile` when that is the previous screen
- `TipTrackerGraph` is the one explicit shared-stack marker that keeps `TipTrackerRoute` and `TipStatisticsRoute` on the same `ViewModelStore`.
- Route sync activation is driven directly from the current top route through `RouteSyncBinding`, rather than through graph observer routes.

## Route / Screen / ViewModel Contract

### Route Responsibilities

- Routes collect state and wire side effects.
- Navigation, snackbar collection, lifecycle wiring, and feature command collection belong in routes, not screens.
- Route layers collect `StateFlow` with `collectAsStateWithLifecycle()`, not `collectAsState()`.
- Route layers pass UI input into `ViewModel.onEvent(...)`; feature screens should not call ad hoc public viewmodel methods for screen interaction.
- Feature-local command flows are collected in the route and translated there into navigation or other one-off side effects.
- Navigation arguments are passed into routes as typed route keys, and routes translate those keys into the feature inputs the screen and viewmodel need.

### Screen Responsibilities

- Screens render state and emit callbacks only.
- Screens are expected to stay stateless where possible.

### ViewModel Responsibilities

- `NavigationEvent` is a screen-to-route contract only. Screens emit navigation intents through `onNavigationEvent`, and routes translate those intents into `Navigator` calls.
- ViewModels never emit or consume `NavigationEvent`.
- When a ViewModel needs to navigate, it emits a one-shot command on its own command flow and the route handles that command.
- Feature-local `Command` types live alongside the feature's `UiState`, `UiEvent`, and `NavigationEvent` models when the feature already has a `Models.kt` file.
- Shared UI state should be modeled explicitly instead of inferred from ad hoc booleans when the feature already has a formal state model.
- Feature-specific state that drives a screen should come from the feature ViewModel, not from route-local session extraction.
- ViewModels may still use assisted injection for typed route arguments where a route key needs to be turned into constructor input.

### Composition and Preview Rules

- Every screen composable must have a real `@Preview` that renders the actual screen inside `LifeTogetherTheme` with representative state and callbacks.
- Comment-only placeholder previews or previews that do not execute the real screen composable are not acceptable.
- Each screen should have at most one meaningful `Scaffold`.
- Do not add extra scaffolds only for error or snackbar handling; those concerns belong to the shared root snackbar host and route command collection.

## Session-Driven Startup

- App startup is driven from the root shell and the root coordinator rather than through per-screen session bootstrapping.
- Startup/auth gating uses feature-owned loading state handling.
- Feature routes should not recreate session orchestration or app-level startup logic.
- Routes may still do direct session checks for route-local guards, but those checks should stay narrow and not become a second session layer.

## Sync Orchestration

- `SyncCoordinator` owns remote sync orchestration.
- The root coordinator ViewModel triggers sync context changes in response to session changes.
- The old graph observer route layer is gone.
- `RouteSyncBinding` maps the current top route to the active `SyncKey` set at the nav-host level.
- `FeatureSyncLifecycleBinding` is still used by feature routes that need explicit sync-key ownership, and it acquires/releases `SyncKey`s based on the composable lifecycle.
- Sync work still lives in `SyncCoordinator`; routes and bindings only activate or release the relevant keys.

## Logging Policy

- `println(...)` is banned across the entire codebase.
- Use `Log.d`, `Log.w`, or `Log.e` instead, depending on severity.
- Do not log sensitive payloads or secrets.

## Local Data Sources

- The old `LocalDataSource` god-object is gone.
- Local persistence is split into focused feature-local sources under `data/local/source/`.
- Repositories depend on these focused sources directly.
- No new production code should depend on a central local-source facade.

## Repository Contract

- Repository observation APIs commonly expose `Flow<Result<T, AppError>>`, not plain local flows.
- One-shot writes and commands return `Result` and are responsible for their own optimistic local persistence, remote write, and rollback behavior.
- Repositories own the mapping/orchestration logic between remote data, local data sources, and domain models.
- Create, update, and delete operations should be local-first and optimistic so the UI can reflect the change immediately.
- Every persisted app-managed model must carry and maintain `lastUpdated`.
- Every local-first write must stamp `lastUpdated` before writing to Room, and the corresponding remote write must persist the same logical change with an updated `lastUpdated` as well.
- Narrow field updates such as image URL changes, family membership changes, counters, and embedded-member updates are not exempt; they must update `lastUpdated` in the same write path instead of relying on callers to remember.
- If a remote write fails, the repository is responsible for rolling back the local optimistic change where that rollback is meaningful.
- Phase 13 is the current write intent: repositories write to Room first, the UI observes that local change immediately, then the remote Firestore write follows.
- Firestore snapshot listeners are reconciliation-only; they correct divergence from other devices or server-side changes and are not the source of immediate UI truth.
- Do not reintroduce a write-then-wait flow where the screen stays stale until the snapshot listener echoes the change back from the server.

## Timestamp Stamping Rules

- `lastUpdated` on all domain models is an immutable `val` with a `= Date()` default so construction sites do not need to supply it.
- `Date()` must only be instantiated in the **repository layer**. Data sources (Firestore and Room) must never call `Date()` internally; they receive the timestamp as an explicit parameter.
- The same `Date` instance must flow into both the local Room write and the Firestore write for any given operation, so both stores carry an identical timestamp.
- `stampNow()` extension functions in `data/repository/internal/LastUpdatedPolicy.kt` are the canonical way to stamp a complete domain object before saving it (e.g. `val stamped = item.stampNow()`). Each domain type has its own overload so `.copy(lastUpdated = now)` compiles correctly for sealed types.
- `val now = Date()` directly in the repository is correct when the same timestamp must be applied across multiple partial `.copy()` calls or multiple entity types at once (e.g. updating `albumId` + `lastUpdated` on media entities and `count` + `lastUpdated` on album entities in the same operation). This is a deliberate choice, not a gap — `stampNow()` only sets `lastUpdated` and requires a domain object.

## Room Migration Rules

- Every new field added to a domain model must have a corresponding `ALTER TABLE ADD COLUMN` statement in `AppDatabaseMigrations.kt`.
- When a domain model with new fields is used as an `@Embedded` type inside a Room entity, **every** table that embeds it (with any `@Embedded(prefix = ...)`) needs its own `ALTER TABLE` statement — one per embedding table, not just one for the type's own table.
- Always verify the exact column name Room expects by checking the generated `AppDatabase_Impl.kt` before writing the migration SQL.

## Current-State Notes

- The architecture file records how the app currently behaves, not the original v2 plan text.
- If a future phase changes current behavior, this file should be updated to match the new implementation reality.
