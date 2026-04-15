# Project Improvement Plan

This is a living document for the next major cleanup and modernization pass of the project. It is intentionally specific, but it does not authorize implementation yet. We will keep refining this file together until the scope, priorities, and tradeoffs are clear enough to execute safely.

Best-practice references in this file were re-checked against official Android documentation on 2026-04-15.

## Goal

Improve the whole project in controlled phases so it becomes:

- easier to read
- easier to test
- easier to preview in Compose
- more reusable without creating shallow abstractions
- closer to native Android and Material 3 behavior where custom UI is not giving us a clear product advantage
- less dependent on large shared ViewModel parameters being threaded through the UI tree

## Non-goals for this document

- no refactor implementation yet
- no mass renaming yet
- no architecture rewrite in one step
- no custom design-system work unless it clearly improves consistency and removes duplication

## Current Baseline

The current `Architecture.md` is too generic for the actual codebase. It describes a clean package structure, but it does not explain the current architectural friction, concrete refactor targets, or the migration path we want to take.

This plan will be the practical companion to `Architecture.md`.

## Guiding Principles

### 1. Prefer native Material 3 first

When a stock Material 3 component can satisfy the product need with light styling, prefer that over maintaining a custom wrapper or a fully custom dialog/surface.

This especially applies to:

- top app bars
- text fields
- dropdowns and exposed menus
- dialogs
- date pickers
- buttons and floating action buttons
- navigation surfaces

Custom wrappers should stay only when they provide one or more of the following:

- shared business behavior
- accessibility improvements
- stable app-specific tokens or layout defaults
- a product-specific interaction that native components do not handle well

Change-control rule:

- custom UI should not be deleted or replaced without explicit user approval first
- before replacing a custom component, first check whether the desired size, color, shape, spacing, or typography can be moved into shared theme tokens or centralized styling instead
- if a native Material 3 component can match the desired result through shared theme or design-token configuration, that should usually be preferred long term

Decision made:

- prefer solving visual consistency at the theme or design-token level before creating, keeping, or replacing custom wrappers
- component policy order should be:
  1. native Material 3 plus shared theme tokens
  2. one canonical shared wrapper when truly needed
  3. feature-specific custom component only when the first two are not enough

### 2. Keep state closest to where it is consumed

For Compose, UI state should be hoisted to the lowest common ancestor that actually needs it. If business logic is involved, a screen-level state holder such as a `ViewModel` is appropriate. If it is only UI logic, a plain state holder or local composable state is often better.

### 3. Do not pass ViewModels deep into reusable composables

Reusable composables should receive plain state and callbacks, not `ViewModel` instances. This is important for previews, tests, and long-term reuse.

### 4. Reuse through deeper modules, not wrapper sprawl

If multiple features share the same concept, prefer consolidating that concept behind a stronger boundary instead of creating many tiny wrappers or helper files that still leak the same complexity everywhere.

### 5. Standardize feature screens around clear boundaries

Long term, each feature should trend toward:

- route-level composable: resolves navigation args, obtains ViewModel, reads shared app/session state if needed
- screen-level composable: receives plain state and callbacks
- feature ViewModel: owns screen UI state and business interaction
- common UI components: stateless and previewable

This is the preferred target because it aligns with Compose guidance:

- the route-level composable is the right place for Android-specific wiring
- the screen-level composable should stay plain and previewable
- `ViewModel` instances should not be passed deep into reusable UI

## Concrete Friction In The Current Codebase

### 1. `AppSessionViewModel` is too broadly threaded through the app

Observed references:

- root injection in `app/src/main/java/com/example/lifetogether/MainActivity.kt`
- parameter propagation through `app/src/main/java/com/example/lifetogether/ui/navigation/NavHost.kt`
- route and screen usage across lists, grocery, guides, gallery, family, profile, settings, recipes, tip tracker, and loading
- observer lifecycle ownership in `app/src/main/java/com/example/lifetogether/ui/common/observer/ObserverLifecycle.kt`

Measured scope:

- `AppSessionViewModel` is referenced in 43 files

Current responsibilities inside `app/src/main/java/com/example/lifetogether/ui/viewmodel/AppSessionViewModel.kt` include:

- auth state observation
- user information loading
- global observer coordination
- guide progress sync loop
- FCM token storage trigger
- shared `itemCount` UI-ish state

This is a sign that the class is acting as a mixed app session holder, app coordinator, observer manager, and shared UI convenience object.

Decision made:

- `AppSessionViewModel` (and ViewModels in general) must NEVER be passed as parameters to other screens or composables.
- App-wide state (like Auth, Family ID, User Info) must live in a Singleton `SessionRepository`.
- Feature ViewModels that need this data must inject the `SessionRepository` via Hilt, completely decoupling the UI tree from session management.

### 2. Screen previews are blocked by ViewModel coupling

Observed example:

- `app/src/main/java/com/example/lifetogether/ui/feature/admin/groceryList/AdminGrocerySuggestionsScreen.kt` preview currently renders `"Preview requires AppSessionViewModel"`

This means the screen API is not clean enough for design-time rendering. Official Compose guidance explicitly warns against passing `ViewModel` instances down to child composables because it hurts reuse, tests, and previews.

### 3. UI component duplication already exists

Examples:

- `ui/common/CustomTextField.kt`
- `ui/common/textfield/CustomTextField.kt`
- `ui/common/DatePickerTextField.kt`
- `ui/common/textfield/DatePickerTextField.kt`
- `ui/common/DropDown.kt`
- `ui/common/dropdown/Dropdown.kt`

These duplicates create uncertainty about which component is canonical and make future cleanup harder.

### 4. Some custom components replace native behavior without a strong reason

Examples to review first:

- `ui/common/TopBar.kt`
  - likely candidate to compare against `TopAppBar` variants
- `ui/common/dialog/CustomAlertDialog.kt`
  - fully custom dialog shell instead of standard `AlertDialog` or `BasicAlertDialog`
- `ui/common/dialog/ErrorAlertDialog.kt`
  - custom full-screen non-dismissible error shell
- `ui/common/textfield/CustomTextField.kt`
  - strongly styled `TextField` wrapper that may be doing design and behavior work together
- `ui/common/dropdown/Dropdown.kt`
  - custom wrapper over `ExposedDropdownMenuBox`
- `ui/common/dialog/CustomDatePickerDialog.kt`
  - currently uses `hiltViewModel()` just to hold selected date state

These are not automatically wrong, but each should justify why native Material 3 behavior is not enough.

Decision made:

- date-picker dialog UI state should not use a `ViewModel` by default when the state is only ephemeral UI element state
- the default preference is remembered local UI state or a plain UI state holder close to the composable
- a `ViewModel` should only be used there if the date-picker state truly participates in broader screen business logic

### 5. ViewModel style is inconsistent across features

There are already examples of a stronger direction:

- `ui/feature/gallery/GalleryViewModel.kt`
  - `data class` UI state + `MutableStateFlow`
- `ui/feature/lists/entryDetails/ListEntryDetailsViewModel.kt`
  - combined screen state + form state, better separation of concerns
- `ui/feature/groceryList/GroceryListViewModel.kt`
  - state container is better, although setup and fetch logic are still broad

There are also examples that should be migrated later:

- `ui/feature/recipes/RecipesViewModel.kt`
  - mutable public fields, ad hoc setup flags, repeated collectors, debug prints
- `ui/feature/profile/ProfileViewModel.kt`
- `ui/feature/family/FamilyViewModel.kt`
- `ui/feature/guides/GuidesViewModel.kt`

### 6. Lifecycle-aware state collection is not standardized

The codebase currently uses many `collectAsState()` calls in Android UI. We should consider standardizing Android-specific screen collection to `collectAsStateWithLifecycle()` where appropriate, while keeping platform-agnostic composables free of Android-only requirements.

Decision made:

- Android screen and route layers should prefer `collectAsStateWithLifecycle()` by default
- plain reusable composables should stay free of Android-only collection APIs
- exceptions should be rare and documented when they exist

### 7. Debug logging and temporary plumbing are leaking into production code

There are many `println(...)` calls across screens and ViewModels. They should be reviewed and either:

- replaced with Android logging such as `Log.d`
- kept useful for future debugging, but standardized
- reduced when they are noisy or low-value

Decision made:

- `println(...)` should generally be replaced with `Log.d(...)`
- useful debug logging should not be removed completely
- logging should stay available for future troubleshooting, but follow a consistent Android logging style

### 8. The Data/Domain layer is shallow and tightly coupled

Observed example:
- `LocalListRepositoryImpl` acts primarily as a passthrough to `LocalDataSource`.
- `LocalDataSource` contains a massive `toItem` mapping function that handles every single entity type in the app (Groceries, Recipes, Albums, Guides, etc.).

This creates high coupling, makes the data source a god-object, and makes the core logic very hard to test without mocking internal database implementations.

Decision made:
- Move toward a "Ports and Adapters" architecture.
- Repositories must become "deep modules" that encapsulate their mapping logic and data source orchestration.
- Split data sources and mapping logic by feature domain rather than keeping one centralized `LocalDataSource`.
- Data layer testing should happen at the repository boundary using real local stand-ins where possible instead of mocking DAOs or internal persistence seams.

### 9. Lack of reliable background synchronization

Observed example:
- Currently, ViewModels or Data Sources handle network calls directly. If a user makes a change offline or closes the app before a network save completes, that data might not reliably reach Firestore.

Decision made on 2026-04-15:
- Introduce a dedicated "Sync Layer" (e.g., using `WorkManager` or a centralized background service).
- The UI should never be responsible for orchestrating remote data pushes or pulling background updates.

## Recommendation: What To Do With `AppSessionViewModel`

Short answer:

- do not replace it with a singleton use case

Reason:

- a use case is better for reusable business actions, not for long-lived app session state ownership
- official Android guidance treats `ViewModel` and plain state holders as UI-layer state holders, while the domain layer is for reusable business logic, often reused by multiple ViewModels
- if we move session state into a singleton use case, we will likely hide a global mutable state problem instead of solving the boundary problem

Preferred target direction:

### Split the current responsibilities into clearer owners

#### A. `SessionRepository` or `SessionManager` singleton

Owns durable app session data and shared app identity context, for example:

- auth session status
- current user info
- current family context
- maybe session refresh / session invalidation events

This should expose observable state, most likely as `StateFlow<SessionState>`.

#### B. App-level coordinator or state holder

Owns app-wide orchestration that is not screen-specific, for example:

- starting or syncing global observers
- background-ish app session reactions
- guide sync scheduling if it truly belongs to app scope

Chosen implementation:

- an activity-scoped root `ViewModel`

Decision made:

- observer coordination should live in a thin app-level coordinator implemented as an activity-scoped root `ViewModel`
- it should not live inside feature screens
- it should not be folded into the session repository by default
- the session repository owns durable shared state, while observer coordination owns orchestration

#### C. Feature ViewModels

Each feature ViewModel should depend on the smallest session input it actually needs.

Examples:

- a screen that only needs `familyId` should not receive the whole `AppSessionViewModel`
- route composables can read session state once and pass primitives or trigger setup functions
- some feature ViewModels may inject a session repository directly if that reduces parameter threading and keeps the dependency explicit

Decision made:

- Feature ViewModels must inject the `SessionRepository` directly via Hilt to read or observe session state (like `familyId` or `uid`).
- Route composables and Screen composables should no longer have any knowledge of session state orchestration.

#### D. UI composables

Stateless screen composables should accept:

- plain screen state
- callbacks
- selected IDs or small models when needed

They should not accept:

- app-scoped ViewModels
- Hilt-only dependencies
- observer managers

### What this means in practice

Best-practice target:

1. Keep global session data in a dedicated repository or manager, not a use case.
2. Keep app orchestration out of feature screens.
3. Keep screen business state in feature ViewModels.
4. Keep reusable composables free of ViewModel dependencies.

Responsibility split agreed on 2026-04-15:

- session repository or manager should own:
  - auth session status
  - current user information
  - current family context
- root coordinator `ViewModel` should own:
  - observer coordination
  - guide progress sync orchestration
  - app-start reactions driven by session changes, such as token sync triggers

Decision made:

- FCM token syncing should sit behind a dedicated service or use case boundary
- the root coordinator should trigger it, not own the full syncing logic itself
- guide progress syncing should also sit behind a dedicated sync boundary
- the root coordinator should trigger guide progress sync behavior rather than own the sync implementation details
- auth observation and current session state should be exposed directly by the session repository or manager
- the root coordinator should react to session changes rather than act as the primary source of truth for xsession state

## Data-to-UI Communication Direction

Currently, data flows up to the UI using a mix of custom listener interfaces (`ResultListener`, `ListItemsResultListener`), Kotlin `Flow`, and raw `try/catch` blocks. As we deepen the repository layer, we need a safer, more predictable way to hand data to the ViewModels.

Industry Standard Context:
Modern Kotlin/Android architecture favors "Functional Error Handling." Throwing exceptions for expected business failures (like a network timeout) creates hidden control flows and brittle `try/catch` blocks in the UI layer. Instead, expected failures should be caught in the data layer and returned as safe, explicitly typed data objects.

Decision made:
- Standardize on a functional `Result` wrapper (e.g., a sealed class like `Result<T, Error>`) for all data/domain operations.
- The Data/Domain layer must never throw raw exceptions for expected failures. It must catch them and return a clean `Result.Failure`.
- ViewModels will consume these `Result` types, ensuring that the UI explicitly handles both success and failure states safely and consistently.
- mandate a strict "Single Source of Truth" (Offline-First) pattern for data reads. The UI must only ever observe the local database (via `Flow`). When the UI requests a data refresh, the repository fetches from the network, saves to the local database, and finishes. The UI will automatically update via its ongoing observation of the local database. ViewModels must never consume network data directly.

## Native Android / Material 3 Audit Candidates

These are the first places to discuss replacement or simplification:

| Current code                                                                | Likely native-first candidate                                                                  | Why discuss it                                                                       |
|-----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| `ui/common/TopBar.kt`                                                       | `TopAppBar`, `CenterAlignedTopAppBar`, `LargeTopAppBar`                                        | Better native semantics, scrolling patterns, accessibility, and predictable slots    |
| `ui/common/dialog/CustomAlertDialog.kt`                                     | `AlertDialog` or `BasicAlertDialog`                                                            | Current component creates a custom shell and blocks dismissal by default             |
| `ui/common/dialog/ErrorAlertDialog.kt`                                      | `AlertDialog`, snackbar, inline error, or event-driven banner                                  | We should decide whether error UI should block, auto-dismiss, or remain screen-local |
| `ui/common/dialog/CustomDatePickerDialog.kt`                                | `DatePickerDialog` with remembered state, without Hilt ViewModel                               | Current version uses a ViewModel for ephemeral UI element state                      |
| `ui/common/textfield/CustomTextField.kt` and `ui/common/CustomTextField.kt` | one canonical Material 3 text field wrapper or direct use of `TextField` / `OutlinedTextField` | Reduce duplication and clarify what styling is global versus local                   |
| `ui/common/dropdown/Dropdown.kt` and `ui/common/DropDown.kt`                | one canonical exposed dropdown API                                                             | Reduce duplicate component surface and drift                                         |
| `ui/common/button/AddButton.kt`                                             | `FloatingActionButton` if the behavior matches                                                 | Better native affordance if this is primarily an add action                          |

## Top App Bar Direction

Decision made:

- `CenterAlignedTopAppBar` should be the default Material 3 replacement target for current `TopBar` usage
- exceptions are allowed when a screen genuinely needs a different top app bar pattern
- current `subText` should by default move into screen content below the app bar instead of being forced into the app bar itself
- current left and right `TopBar` icons should by default map to native `navigationIcon` and `actions` slots

## Navigation Direction

Current Compose navigation often relies on string-based routes and string concatenation for arguments, which is brittle and prone to runtime crashes if a typo occurs or an argument is missing.

Decision made on 2026-04-15:
- All refactored features must migrate to the official Compose Type-Safe Navigation using Kotlin Serialization.
- Routes must be defined as data classes or objects (e.g., `data class RecipeDetailsRoute(val recipeId: String)`).
- String-based routes are considered legacy and should not be used for new or refactored navigation graphs.

## Motion, Loading, and Transitions Direction

Without standard guidelines, Compose UI elements can either instantly "pop" into existence (which feels cheap) or rely on full-screen blocking spinners for every network call (which feels slow).

Decision made on 2026-04-15:
- **No Harsh Popping:** UI elements that appear/disappear (e.g., dropdowns, inline error messages, expanding cards) must use `AnimatedVisibility` or `animateContentSize` by default.
- **Standardized Loading:** Blocking, full-screen loading spinners are banned except for critical/unrecoverable paths. The app will default to Skeleton Loaders (shimmer effects) for fetching data, and inline progress indicators for saving data.
- **Universal Navigation Transition:** The app will use one standard, global navigation transition (e.g., a subtle slide + crossfade using Material 3 easing curves) rather than ad-hoc animations per screen. Shared Element Transitions are explicitly avoided to keep maintenance simple.

## ViewModel Cleanup Direction

We should gradually normalize ViewModels around these rules:

- expose one screen UI state object when possible
- keep transient UI events explicit
- avoid many public mutable properties
- avoid setup methods that silently launch new collectors each time unless they guard correctly
- avoid screen code mutating ViewModel fields directly
- keep Android framework types out of reusable logic unless truly needed
- move business logic into repositories or use cases only when it is genuinely reusable or complex
- delete "passthrough" Use Cases that do nothing but call a single repository method. ViewModels should call the deepened repositories directly for simple data fetching/saving. Use Cases should be strictly reserved for complex business logic, combining multiple repositories, or logic that is heavily reused across multiple ViewModels.
- separate durable screen state from one-off UI events. ViewModels should expose a `StateFlow<UiState>` for persistent data (like lists and loading spinners) and a separate `Channel<UiEvent>` (exposed as a `Flow`) for fire-and-forget actions (like navigation triggers or showing a Snackbar) to avoid state-clearing boilerplate and rotation bugs.

Decision made:

- refactored ViewModels should expose one main screen state object by default
- public mutable fields such as dialog flags, raw error strings, ad hoc IDs, and filter selections should be reduced significantly
- screens should stop mutating many ViewModel properties directly
- if extra form state is needed, it should live in a clearly paired form state model rather than scattered mutable fields
- persistent screen state should stay in `UiState`
- one-off events should use explicit event handling instead of many raw mutable booleans spread across the ViewModel
- blocking confirmations may still live in screen state when they represent part of the currently rendered UI
- refactored features should move away from ad hoc setup calls such as `setUpRecipes(familyId)` when a cleaner route-prepared input and stable initialization pattern is possible

Good migration reference points already present in the codebase:

- `ui/feature/gallery/GalleryViewModel.kt`
- `ui/feature/lists/entryDetails/ListEntryDetailsViewModel.kt`

Files that likely deserve early review:

- `ui/feature/recipes/RecipesViewModel.kt`
- `ui/feature/recipes/RecipeDetailsViewModel.kt`
- `ui/feature/family/FamilyViewModel.kt`
- `ui/feature/profile/ProfileViewModel.kt`
- `ui/feature/guides/GuidesViewModel.kt`

## Testability And Preview Direction

Desired end state:

- route composables own Android-specific wiring
- screen composables take plain state and callbacks
- previews render realistic screen states without Hilt
- ViewModel tests focus on state transitions and business interactions
- reusable UI tests do not require app session plumbing
- all composables should be previewable unless there is a strong documented reason they cannot be
- data/domain layer tests verify behavior at the public repository boundary using local stand-ins (like an in-memory database), rather than brittle unit tests that mock internal DAOs. 
  - (Note: Restructuring the code to support this boundary testing is the immediate priority; writing the actual test suites will happen in a later phase).

Current blockers to remove later:

- screens depending directly on `AppSessionViewModel`
- screens depending directly on Hilt-provided ViewModels instead of route wrappers
- duplicated UI components with inconsistent parameter shapes

## Default Error Handling Direction

Decision made:

- validation and missing-input problems should default to inline error presentation near the relevant field or form
- non-blocking operation failures should default to a snackbar or banner-style message instead of a blocking dialog
- blocking error dialogs should be reserved for rare, high-severity errors that truly need to interrupt the flow
- the current custom `ErrorAlertDialog` should be considered a migration target toward snackbar-first handling, with styling kept visually aligned where appropriate
- default snackbar positioning should remain at the native bottom position

Recommended implementation direction:

- keep one shared `SnackbarHost` at the app root or other clearly shared UI root
- keep `SnackbarHostState` in the Composition, usually remembered near the root `Scaffold`
- let feature layers emit typed UI message events instead of rendering a dialog directly
- use a reusable route-level helper or collector to forward those events to the shared snackbar host
- screen-level `Scaffold` is still a valid preferred pattern for screen structure and should not be avoided just because the default snackbar host is shared
- avoid adding extra nested `Scaffold`s only for error handling; prefer one meaningful screen scaffold plus shared snackbar handling where possible
- a screen should generally have at most one meaningful `Scaffold`, and extra scaffolds should require a clear reason

Important constraint:

- avoid replacing repeated screen code with a hidden global mutable dependency by default
- a dedicated app-wide message bus is possible, but the default preference is a shared root snackbar host plus a reusable collection helper so the wiring stays explicit

## UI Interaction Patterns Direction

While visual consistency is handled by Material 3 and shared theme tokens, we also need to ensure interaction UX is consistent. 
Common actions (such as saving, deleting, or loading) should feel and behave identically across all features so the user experience is predictable.

Decision made:
- We will standardize interaction patterns across the app.
- The specific rules for each interaction (e.g., whether to use swipe-to-delete vs. an overflow menu, or skeleton loaders vs. spinners) are intentionally deferred. We will define these step-by-step in future UX discussions.

## Shared Theme And Token Direction

Decision made:

- the plan should add a small app-specific token layer on top of `MaterialTheme`
- that token layer should be the default place for shared sizing, spacing, shape, and similar visual defaults that do not justify a custom wrapper on their own
- this token layer should reduce the need for custom wrappers whose main role is only visual styling
- the first token layer should stay intentionally small and focus on the defaults already causing wrapper duplication
- initial token candidates include spacing, corner radius or shape, common field height, and shared app-bar or content padding values
- colors should continue to come primarily from `MaterialTheme.colorScheme` unless a strong reason appears to introduce additional custom color tokens later
- adopt a strict "No Magic Numbers" policy for Compose styling. Hardcoded `.dp` values for sizing/spacing and raw `Color` objects inside feature screens are prohibited. All spacing, sizing, and colors must be explicitly pulled from the `MaterialTheme` or the shared app token layer to guarantee reusability and visual consistency.

## Proposed Workstreams

Decision made:

- first implementation phase should be session and observer boundary cleanup
- second phase should be route and screen split for previewability and testability
- third phase should be native Material 3 alignment and shared component consolidation

The named pilot targets below are the first concrete implementations inside these workstreams, not a separate parallel phase.

## Per-Screen Refactor Checklist

Use this checklist later when a specific screen is selected for refactor.

- route owns `ViewModel`, navigation, and session wiring
- screen composable accepts plain state and callbacks
- screen composable has real preview support unless a strong documented exception exists
- Android route or screen-entry layer prefers `collectAsStateWithLifecycle()`
- screen no longer takes `AppSessionViewModel` (or any other ViewModel) as a parameter.
- feature ViewModel injects `SessionRepository` directly if it needs session context.
- feature `ViewModel` exposes one main screen state object by default
- transient events are handled explicitly instead of many scattered mutable flags
- shared UI uses native Material 3 plus theme tokens first, then one canonical wrapper if needed
- custom UI is only kept, replaced, or removed with explicit user approval
- logging uses consistent Android logging style such as `Log.d(...)` instead of scattered `println(...)`
- route navigation is fully type-safe using Kotlin Serialization (no string-based navigation)
- screen uses Skeleton Loaders or inline progress indicators instead of blocking full-screen spinners.
- appearing/disappearing UI elements use `AnimatedVisibility` or `animateContentSize` (no harsh popping).

## Initial Pilot Targets

These are the pilot targets already agreed for the future implementation phase.

### Pilot 1. Root session and observer boundary

Start with:

- `ui/viewmodel/AppSessionViewModel.kt`
- `MainActivity.kt`
- `ui/navigation/NavHost.kt`
- `ui/common/observer/ObserverLifecycle.kt`

Goal:

- establish the hybrid session direction
- move observer orchestration into a thin root coordinator or state holder
- reduce `AppSessionViewModel` parameter threading

### Pilot 2. First route and screen split for previewability

Chosen screen pilot:

- `ui/feature/admin/groceryList/AdminGrocerySuggestionsScreen.kt`

Why this group:

- it is close enough to the current shared component layer to expose real friction
- it is useful for testing the route-versus-screen split
- at least one current preview is explicitly blocked by `AppSessionViewModel`

### Pilot 3. First duplicated common component cleanup

Chosen shared component pilot group:

- text fields
- dropdowns
- date picker text fields

Goal:

- choose one canonical shared component path
- decide what belongs in theme tokens versus wrappers
- test the moderate native-first policy without broad risky UI churn

### Workstream 1. Session and observer boundary cleanup

Start with:

- `MainActivity.kt`
- `ui/navigation/NavHost.kt`
- `ui/viewmodel/AppSessionViewModel.kt`
- `ui/common/observer/ObserverLifecycle.kt`

Target:

- reduce broad parameter threading
- move durable session state into a dedicated boundary
- make observer ownership explicit and easier to reason about

### Workstream 2. Native Material 3 alignment

Start with:

- top bars
- dialogs
- text fields
- dropdowns
- add buttons

Target:

- fewer custom shells
- better accessibility and consistency
- one canonical shared wrapper per component type at most

### Workstream 3. Screen API cleanup for previews

Start with:

- admin grocery screens
- list screens
- recipes screens

Target:

- route composable obtains state
- screen composable becomes previewable
- no screen should need `"Preview requires AppSessionViewModel"`

### Workstream 4. ViewModel normalization

Start with the features that still use many mutable fields and setup flags.

Target:

- stable UI state models
- reduced public mutable state
- easier unit tests

### Workstream 5. Shared component consolidation

Start with duplicate names and overlapping responsibilities in `ui/common`.

Target:

- one canonical text field path
- one canonical dropdown path
- one canonical date picker path
- clear naming and folder structure

Decision made:

- duplicated shared UI components should be merged into one canonical component or removed
- parallel shared variants should not remain unless they have a clearly different responsibility
- naming and folder placement should make the canonical choice obvious

## External References To Use During The Refactor

Primary references for the future implementation phase:

1. Android Developers, "Where to hoist state"
   - https://developer.android.com/develop/ui/compose/state-hoisting
   - key guidance: hoist state to the lowest common ancestor; keep state close to where it is consumed; use screen-level state holders when business logic is involved
2. Android Developers, "State holders and UI state"
   - https://developer.android.com/topic/architecture/ui-layer/stateholders
   - key guidance: use plain state holders for UI logic and `ViewModel` for business-logic-backed screen state
3. Android Developers, "Preview your UI with composable previews"
   - https://developer.android.com/develop/ui/compose/tooling/previews
   - key guidance: do not pass `ViewModel` instances down to child composables; preview plain-state composables instead
4. Android Developers, "Domain layer"
   - https://developer.android.com/topic/architecture/domain-layer
   - key guidance: the domain layer is optional and is for reusable business logic, often reused by multiple ViewModels
5. Android Developers, "State and Jetpack Compose"
   - https://developer.android.com/develop/ui/compose/state
   - key guidance: prefer lifecycle-aware `Flow` collection on Android UI where appropriate

## Open Questions For Later

These are important questions, but we are not locking them yet. They should stay visible so we can refine the plan over time without mixing unresolved ideas with confirmed decisions.

### 1. Session repository usage inside feature ViewModels

Still open:

- which current or future feature ViewModels truly need to observe session changes directly
- which ones should stay route-input only

### 2. Theme depth after the initial token layer

Still open:

- whether the first small token layer will be enough for the project long term
- whether the theme structure should be deepened later after the first cleanup waves

### 3. Implementation order after the agreed initial pilots

Deferred intentionally:

- no additional feature-family priority is being locked yet
- this should be decided much later, after the rules and first pilots have proved themselves

### 4. Specific UX Interaction Rules

Deferred intentionally:
- We have agreed to standardize interaction patterns (deleting, saving, loading), but the exact UX rules and flows for each action will be decided step-by-step at a later date.

## Initial Recommendation

My current recommendation is:

1. Choose the hybrid session direction.
2. Prefer native Material 3 by default, with exceptions documented explicitly.
3. Make previewability a hard rule for refactored screens.
4. Start implementation later with session/observer boundary cleanup before touching visual component consolidation.

That order is safer because it reduces architectural coupling first, which should make the later UI cleanup simpler instead of noisier.
