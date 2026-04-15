# Phase 1 — Session Boundary Cleanup

**Status:** Grill-me in progress _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Extract durable app session state into a dedicated `SessionRepository`, move observer orchestration into a thin root coordinator ViewModel, and stop threading `AppSessionViewModel` through the UI tree entirely.

## Scope

- `app/src/main/java/com/example/lifetogether/ui/viewmodel/AppSessionViewModel.kt`
- `app/src/main/java/com/example/lifetogether/MainActivity.kt`
- `app/src/main/java/com/example/lifetogether/ui/navigation/NavHost.kt`
- `app/src/main/java/com/example/lifetogether/ui/common/observer/ObserverLifecycle.kt`
- New: `SessionRepository` interface + implementation (Hilt singleton)
- New: Root coordinator ViewModel (activity-scoped)

## Key Decisions Already Made

- `AppSessionViewModel` must never be passed as a parameter to screens or composables.
- `SessionRepository` (singleton) owns: auth session status, current user info, current family context — exposed as `StateFlow<SessionState>`.
- Root coordinator ViewModel (activity-scoped) owns: observer coordination, guide progress sync orchestration, app-start reactions (e.g. FCM token sync trigger).
- FCM token syncing sits behind a dedicated service/use case boundary; root coordinator triggers it, does not own the logic.
- Feature ViewModels inject `SessionRepository` directly via Hilt — they do not receive session data through the UI tree.
- `NavHost` and route composables must have no knowledge of session orchestration.

## Subphases

- [ ] 1.1 Define `SessionState` as an explicit session model and implement `SessionRepository` as a Hilt singleton
  - Recommended file placement: `SessionRepository` in `domain/repository/`, `SessionState` in `domain/model/`, `SessionRepositoryImpl` in `data/repository/`
  - Prefer `SessionState` as a sealed interface with `data object` / `data class` variants if supported cleanly by the project Kotlin version; fallback to sealed class or plain `object` variants only if necessary
  - `SessionState` shape: `Loading`, `Unauthenticated`, `Authenticated(user: UserInformation)`
  - `SessionRepository` owns auth-state observation, current user loading, durable session state exposure, and sign-out entry point
  - `SessionRepository.signOut()` absorbs the full existing logout behavior: remote logout/device-token removal, local session clearing, and transition to `SessionState.Unauthenticated`
  - `SessionRepositoryImpl` should be an eager singleton that starts session observation in `init`
  - Use an application-level coroutine scope injected through Hilt for the long-lived auth/session observation loop
  - Keep the public API minimal in phase 1: `val sessionState: StateFlow<SessionState>` and `suspend fun signOut(): ResultListener`
  - Do not expose parallel convenience flows like `uidFlow`, `familyIdFlow`, or `userInformationFlow` in phase 1
  - Internal transition rule: `Loading` while auth/user state is being resolved; `Authenticated(user)` only after full `UserInformation` load succeeds; if Firebase auth is signed in but user-info lookup fails, log it and transition to `Unauthenticated`
  - Provide `SessionRepository` and the application-level coroutine scope from a dedicated Hilt module installed in `SingletonComponent`
  - If an application-level `CoroutineScope` is added, qualify it explicitly to avoid ambiguous scope injection later
- [ ] 1.2 Define and implement the activity-scoped root coordinator ViewModel
  - Recommended file placement: root coordinator ViewModel in `ui/viewmodel/`
  - Root coordinator reacts to `SessionState`
  - Owns observer coordination, guide progress sync scheduling, and FCM token sync triggering
  - FCM sync triggers only when authenticated `uid` and `familyId` are both present, and only when identity context changes
  - Implement session reactions as explicit private handlers per concern rather than one monolithic collector
  - Track last-applied identity context separately for observers, guide sync, and FCM sync so reactions stay idempotent
  - On `Unauthenticated`, cancel guide sync, clear remembered contexts, and cancel all non-auth observers
  - On `Authenticated(user)`, sync observer context if changed, start/restart guide sync when `familyId` is valid and context changed, and trigger FCM sync only when both `uid` and `familyId` are valid and changed
- [ ] 1.3 Update `MainActivity` to use the root coordinator + `SessionRepository`
  - Remove `AppSessionViewModel` usage
  - Move app-start session reactions out of composable side effects tied directly to `AppSessionViewModel`
  - Keep `MainActivity` focused on app-root setup and root-scoped dependencies rather than session-driven navigation branching
- [ ] 1.4 Update `NavHost` and route composables to remove all `AppSessionViewModel` parameter threading
  - `NavHost` must not accept `AppSessionViewModel`
  - Route composables must not know about session orchestration
  - `LoadingScreen` may keep ownership of login-vs-home routing in phase 1, but it should depend on plain session state input rather than an app-scoped session ViewModel
  - Prefer direct plain session-state collection for startup loading flow rather than introducing a dedicated `LoadingViewModel` unless implementation friction clearly requires one
  - Graph-aware observer helpers such as the gallery graph may keep route-level navigation-condition logic, but they must only decide when to bind observer keys and must not pass session context
  - Preserve existing route files in phase 1 even when they become thinner; route/screen structural cleanup belongs to a later phase unless a route becomes impossible to keep
- [ ] 1.5 Update `ObserverLifecycle` to align with root coordinator ownership
  - `FeatureObserverLifecycleBinding` takes observer keys only
  - No `uid` / `familyId` / `AppSessionViewModel` parameters
  - `FeatureObserverLifecycleBinding` and `ObserverUpdatingText` should resolve the activity-scoped root coordinator internally instead of receiving it through route/screen parameter threading
  - Route composables may still decide which observer keys to bind and under what route-specific conditions, but they must not pass session context into observer plumbing
  - UI-facing observer ownership should be fully cleaned up in phase 1, but internal `ObserverCoordinator` API changes should stay modest unless they are required to unblock the migration
  - It is acceptable for the root coordinator to remain the layer that translates `SessionState` into calls on `ObserverCoordinator`
- [ ] 1.6 Migrate remaining feature code off `AppSessionViewModel`
  - Feature ViewModels inject `SessionRepository` directly when they need reactive session access
  - Screens stop reading `uid` / `familyId` from an app-scoped ViewModel
  - Even screens with heavy current-user rendering requirements should get session-backed data from their own feature ViewModel rather than reading session directly in the composable
  - Route-level temporary session extraction should not be kept as a compatibility shortcut unless there is a blocker that clearly belongs to a later phase
  - Feature ViewModels should map only the session-backed fields each screen actually needs into their own UI-facing state rather than exposing raw `SessionState` directly to the composable
  - Profile/Family/Settings-style screens should delegate sign-out and other session-backed actions through their own ViewModel APIs
  - A very small shared helper/extension for common `SessionState` extraction is allowed, such as authenticated user or familyId lookup, but phase 1 must not create a second broad session abstraction
  - In Android UI files touched during this migration, prefer `collectAsStateWithLifecycle()` for new or replaced session-related collection points, but do not widen phase 1 into a mass collection-API sweep
  - Expected migration targets in this phase include startup/auth gating (`LoadingScreen`), user-data-heavy screens (`ProfileScreen`, `FamilyScreen`, `SettingsScreen`, `HomeScreen`), setup-driven feature screens (`GroceryListScreen`, `RecipesScreen`, `RecipeDetailsScreen`, `GuidesScreen`, `GuideCreateScreen`, `GuideDetailsScreen`, `GuideStepPlayerScreen`, `ListsScreen`, `ListDetailsScreen`, `ListEntryDetailsScreen`, `GalleryScreen`, `AlbumDetailsScreen`, `MediaDetailsScreen`, `TipTrackerScreen`, `TipStatisticsScreen`), and observer-only routes (grocery, recipes, guides, lists, tip tracker, gallery graph, admin grocery routes)
  - Feature ViewModels that depend on session context must react coherently to context changes: start/restart work when required context becomes valid or changes, and clear/reset state when required context becomes invalid
  - Existing `setUp(...)` / `setUpX(...)` APIs may remain temporarily in phase 1 if removing them would widen the phase, but they must stop requiring session-derived parameters from routes/screens and should only keep true screen-specific inputs like IDs or nav arguments
  - ViewModels with jobs or collectors keyed by session context must cancel old jobs before starting new ones, clear stale state when context becomes invalid, and replace one-time setup booleans with real context-keyed restart logic where needed
  - ViewModels that always need session context may observe `SessionRepository` in `init`; ViewModels that also require true screen-specific input should combine that input with session context internally rather than pushing session composition back to routes
  - ID-based screens should pass only true navigation input from routes; the ViewModel must own composition of screen-specific IDs plus session context and must reset or restart correctly when either side of that keyed context changes
  - When required session context becomes invalid, migrated feature ViewModels should reset or clear state without inventing new feature-specific error UI; app/session routing should handle the broader transition
- [ ] 1.7 Delete `AppSessionViewModel` and remove `itemCount` completely
  - No compatibility wrapper kept after migration
  - Remove all `itemCount` state and update calls, even if partially wired to local or remote persistence

### Recommended Implementation Order

- [ ] Step A: Add singleton DI + application coroutine scope + `SessionState` + `SessionRepository`
- [ ] Verify compilation after Step A / the first issue slice
- [ ] Step B: Add the activity-scoped root coordinator and wire its session reaction loop
- [ ] Verify compilation after Step B / the second issue slice
- [ ] Step C: Switch `MainActivity` to the root coordinator and remove app-start `AppSessionViewModel` side effects
- [ ] Step D: Migrate observer lifecycle infrastructure to root coordinator ownership
- [ ] Step E: Remove `NavHost` parameter threading and clean route observer bindings
- [ ] Step F: Migrate feature/session consumers screen-by-screen through their own ViewModels
- [ ] Step G: Delete `AppSessionViewModel`, delete `itemCount`, and sweep remaining references
- [ ] Verify final compilation after Step G / the final issue slice
- [ ] Step H: Update `Architecture.md` or another current-state explainer to reflect the finished phase

## Before Starting This Phase

> Follow the mandatory [PhaseExecutionFlow](../PhaseExecutionFlow.md) for this phase.
>
> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria

- [ ] `SessionRepository` exists as the single durable session boundary and exposes `StateFlow<SessionState>`
- [ ] `SessionState` is explicit and no screen depends on the old `loading + nullable userInformation` contract
- [ ] The root coordinator ViewModel is activity-scoped and owns observer coordination, guide sync orchestration, and FCM token sync triggering
- [ ] `MainActivity` no longer creates or depends on `AppSessionViewModel`
- [ ] `NavHost` and all route/screen composables no longer accept or thread `AppSessionViewModel`
- [ ] `FeatureObserverLifecycleBinding` no longer accepts `uid`, `familyId`, or `AppSessionViewModel`
- [ ] Feature ViewModels that need session data depend on `SessionRepository` directly via Hilt
- [ ] Sign-out flows go through `SessionRepository.signOut()` and result in clean transition to `SessionState.Unauthenticated`
- [ ] Remote logout/device-token removal behavior remains intact after the sign-out refactor
- [ ] FCM token sync triggers only when authenticated `uid` and `familyId` are both available, and does not retrigger unnecessarily when identity context is unchanged
- [ ] `AppSessionViewModel` is deleted by the end of the phase
- [ ] `itemCount` is fully removed from the codebase and not relocated into `SessionRepository` or the root coordinator
- [ ] Preview-blocking `"Preview requires AppSessionViewModel"` placeholders touched by this migration are removed
- [ ] `Architecture.md` or another current-state explainer is updated to reflect the new session boundary and root coordinator ownership without removing historical references from the v2 planning files

### Test cases

- [ ] App startup while auth state is unresolved exposes `SessionState.Loading`, then resolves to either `Authenticated` or `Unauthenticated`
- [ ] Authenticated startup with valid `uid` and `familyId` triggers root coordinator setup, including observer context sync and one FCM sync attempt for that identity context
- [ ] Authenticated startup with missing `familyId` does not start family-scoped observers or guide sync and does not trigger FCM token sync
- [ ] Sign-out clears session state to `Unauthenticated`, preserves remote logout/device-token removal behavior, and tears down non-auth observers plus guide sync orchestration
- [ ] Re-entering the same authenticated identity context does not duplicate FCM token sync work
- [ ] `FeatureObserverLifecycleBinding` acquires and releases observer keys without requiring route-level session arguments
- [ ] Key feature ViewModels compile and run with `SessionRepository` injection instead of `AppSessionViewModel` threading
- [ ] Loading/navigation gating still routes authenticated users to home and unauthenticated users to login
- [ ] Codebase compiles with no remaining `AppSessionViewModel` references
- [ ] Codebase compiles with no remaining `itemCount` references
- [ ] Compilation is verified after each major issue slice, not only at the end of the phase

## GitHub Issues

Create milestone `V2 Phase 1: Session Boundary Cleanup` and the following issues assigned to it:

- `[Phase 1] Create SessionRepository and SessionState`
- `[Phase 1] Create root coordinator and observer lifecycle migration`
- `[Phase 1] Remove AppSessionViewModel and itemCount references`

Issue ownership expectations:

- `[Phase 1] Create SessionRepository and SessionState`
  - Owns singleton DI additions, application coroutine scope, `SessionState`, `SessionRepository`, `SessionRepositoryImpl`, sign-out boundary migration, and any small `SessionState` helper needed by feature ViewModels
- `[Phase 1] Create root coordinator and observer lifecycle migration`
  - Owns root coordinator ViewModel, `MainActivity` root wiring, observer lifecycle helpers, graph observer migration, and route observer cleanup that removes session/context plumbing from UI-facing observer code
- `[Phase 1] Remove AppSessionViewModel and itemCount references`
  - Owns feature ViewModel/session-consumer migration, removal of direct session reads from screens/routes, deletion of `AppSessionViewModel`, deletion of `itemCount`, final reference sweep, and `Architecture.md` or equivalent current-state documentation update

Issue body expectations:

- Each issue body should include:
  - issue-specific scope/ownership
  - the relevant subphase checklist items copied from this phase file
  - the relevant acceptance criteria copied from this phase file
  - the relevant test/verification checklist items copied from this phase file
  - a short `Out of scope` section that names which work belongs to the other Phase 1 issues or to later phases
