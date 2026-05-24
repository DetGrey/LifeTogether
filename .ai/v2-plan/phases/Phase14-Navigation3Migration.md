# Phase 14 — Navigation 3 Migration

**Status:** Planned

## Goal

Migrate the app from Navigation 2 to Navigation 3 in a single atomic cutover while preserving intentional behavior.

This phase is not a navigation redesign. It is a behavior-preserving port to Navigation 3 with a small amount of simplification where the current Navigation 2 graph machinery is only being used as an implementation detail.

The migration must preserve:
- the current Home-anchored single-stack behavior
- the existing auth/loading hard-reset flow
- the special Profile/Settings back behavior
- shared ViewModels for guide details/step player and tip tracker overview/statistics
- the current custom route transitions
- notification-driven external entry
- the meal planner focus-date return behavior
- the recipe -> meal-plan preselected recipe flow

The migration may simplify:
- graph observer routes for sync activation
- `NavController` ownership
- `SavedStateHandle.toRoute(...)` as a route-argument source
- `previousBackStackEntry.savedStateHandle` as a result channel

The migration must not add new product behavior:
- no adaptive multi-pane layouts yet
- no new destination types
- no phased Nav2/Nav3 coexistence
- no route redesign beyond what Navigation 3 requires

---

## Architectural Shape

Navigation 3 in this app should be modeled as:
- a single app-level back stack driven by `SnapshotStateList<AppRoute>`
- a `NavDisplay` at the root of the app
- typed route keys as the public navigation contract
- a small `AppNavigator` abstraction that mutates Navigation 3 state instead of wrapping `NavController`
- typed `NavigationResult` events for back results
- route arguments passed through official Hilt assisted injection

The migration should keep route and screen responsibilities aligned with the existing architecture:
- Routes collect state and wire side effects.
- Screens remain stateless and UI-focused.
- ViewModels own business state, loading, persistence, and validation.
- The navigation layer owns back-stack state and route dispatch.

---

## Non-Negotiable Behavior To Preserve

### 1. Home-anchored stack

The app currently behaves like a single stack rooted at `Home`, not like independent persistent stacks per bottom-navigation section.

Preserve this behavior:
- top-level navigation still returns to or rebuilds from `Home`
- feature screens do not retain hidden stacks when switching top-level areas
- `navigateTopLevel(...)` should continue to enforce the current home-based behavior

### 2. Auth flow hard reset

`Loading`, `Login`, and `Signup` should keep hard-reset semantics.

Preserve:
- loading decides whether to show login or home
- successful login clears the old stack and lands on home
- signup/login transitions do not preserve old stacks

### 3. Profile / Settings special behavior

Keep the existing pairwise behavior between `Profile` and `Settings`.

Preserve:
- switching between `Profile` and `Settings` may pop instead of pushing in the current paired case
- top-level navigation to those routes should keep the existing logic, just implemented over Nav3 state

### 4. Shared ViewModels

Two shared ViewModel scopes are intentional and must remain shared:
- `GuideDetailsViewModel` shared by guide details and guide step player
- `TipTrackerViewModel` shared by tip tracker overview and statistics

This is not a candidate for destination-scoped splitting during the migration.

### 5. Existing transitions

Preserve the current route transition feel:
- slide and fade motion remain the default route motion
- no transition simplification for the first cutover
- no new adaptive motion system yet

### 6. Notifications

Keep notification taps as the one external raw entry path.

The notification payload may still arrive as a string, but it must be mapped immediately to a typed route before it is handed to navigation state.

### 7. Meal planner result behavior

Preserve the current “return the selected date back to meal planner” flow.

This should become a typed Navigation 3 result event, not a shared ViewModel trick and not a loose callback-only special case.

### 8. Recipe -> meal plan preselection

The recipe details screen can still navigate to meal plan creation with a preselected recipe.

That is a forward route argument, not a result.

---

## Navigation Model

### Back stack

Replace `NavController` as the source of truth with a root-level `SnapshotStateList<AppRoute>` / `NavDisplay` model.

The app should start from a typed key such as:
- `LoadingNavRoute`

The back stack should be serializable and restore after process death.

### AppNavigator

Keep an `AppNavigator` class, but make it a Navigation 3 state reducer instead of a `NavController` wrapper.

Expected responsibilities:
- `navigate(route: AppRoute)`
- `navigateBack()`
- `navigateTopLevel(route: AppRoute)`
- `clearAndNavigate(route: AppRoute)`
- `navigateBack(result: NavigationResult?)`
- preserve special Profile/Settings behavior
- preserve gallery stack-clearing behavior

The goal is to reduce churn in route files while removing `NavController` from the implementation.

### Results

Use typed results for back navigation.

Recommended shape:
- `sealed interface NavigationResult`
- `data class MealPlannerFocusDate(val date: String) : NavigationResult`

Use cases:
- meal plan save returns focus date to meal planner
- future similar one-shot return flows can use the same channel

### Route arguments

Use official Hilt assisted injection for route arguments.

Reason:
- route keys are explicit typed input in Navigation 3
- `SavedStateHandle.toRoute(...)` is a Navigation 2 mechanism
- the migration should not fake a Navigation 2 argument source

The affected route-argument ViewModels are:
- `RecipeDetailsViewModel`
- `GuideDetailsViewModel`
- `GuideStepPlayerViewModel`
- `MealPlanDetailsViewModel`
- `ListDetailsViewModel`
- `ListEntryDetailsViewModel`
- `AlbumDetailsViewModel`
- `MediaDetailsViewModel`

`MealPlannerViewModel` is different:
- it does not use route args
- it handles a returned focus-date result

---

## Simplification Targets

### Sync activation

Replace the graph observer route layer with a direct route-to-sync mapping.

Current graph observer routes to remove:
- `RecipeGraphObserverRoute`
- `GalleryGraphObserverRoute`
- `MealPlannerGraphObserverRoute`
- `UserListGraphObserverRoute`
- `GuideGraphObserverRoute`
- `TipTrackerGraphObserverRoute`

Replace them with a small route mapping that answers:
- which `SyncKey`s should be active for the current top route or route family

This should still preserve the current feature-area semantics:
- recipes and recipe details keep `RECIPES`
- gallery screens keep `GALLERY_ALBUMS` and `GALLERY_MEDIA`
- meal planner screens keep `MEAL_PLANNER`
- user lists keep all list-entry sync keys
- guides keep `GUIDES`
- tip tracker screens keep `TIP_TRACKER`

### Navigation host simplification

Replace `NavHost` with `NavDisplay`.

Remove graph markers that only existed for Navigation 2 structure unless they are still needed for shared scope semantics.

The Nav3 host should:
- hold the back stack
- map keys to entries
- provide shared decorators
- provide current top-level semantics
- consume typed results

### Result flow simplification

Replace `previousBackStackEntry.savedStateHandle` with a typed result path.

The meal planner case should become:
- create meal plan with `preselectedRecipeId` as a forward argument
- save new meal plan
- return `MealPlannerFocusDate(date)` as a typed result
- meal planner consumes the result and sets its focus date
- existing `ClearFocusDate` still clears after the UI handles it

---

## Implementation Order

### Step 1 — Add Navigation 3 dependencies

Add the current Navigation 3 runtime and UI artifacts.

Also add the lifecycle ViewModel add-on required for Hilt ViewModel integration with Nav3 entries.

Verify:
- the versions align with the project’s Kotlin / Compose toolchain
- the app still compiles with the new dependencies in place

### Step 2 — Convert route types to NavKey

Make every navigable route implement `NavKey`.

Keep the current `@Serializable` typing.

Expected outcomes:
- `AppRoute` remains the typed route contract
- route objects/data classes can be used directly as Nav3 keys
- no string route building remains in app navigation

### Step 3 — Build the Nav3 root state

Create the Navigation 3 state holder at the same scope as the old `NavHostController`.

It should provide:
- a serializable back stack
- current top-level route tracking
- restore behavior after process death
- a `NavDisplay` input list of entries

This state holder should be the source of truth for:
- current active route
- back-stack changes
- top-level switching
- hard resets
- result routing

### Step 4 — Replace `NavHost` with `NavDisplay`

Move all destinations from the current `NavHost` graph DSL into an `entryProvider`.

Preserve:
- route grouping by feature
- route-specific transitions
- shared ViewModel scoping
- any entry metadata needed later

The `NavDisplay` should be configured with:
- the composed entries from the Nav3 back stack
- the current `onBack` handler
- decorators for saveable state and ViewModel store scope
- the current transition behavior

### Step 5 — Replace `AppNavigator`

Reimplement `AppNavigator` over the Nav3 state holder.

Keep the API shape, but change the internals:
- `navigate(route)`
- `navigateBack()`
- `navigateTopLevel(route)`
- `clearAndNavigate(route)`
- result-aware back navigation

The goal is to keep route files simple during migration.

### Step 6 — Migrate route-argument ViewModels

Move the route-argument ViewModels to official Hilt assisted injection.

Rules:
- the route key comes from the Nav3 entry lambda
- the ViewModel receives the typed route object via assisted injection
- the ViewModel no longer reads route arguments from `SavedStateHandle.toRoute(...)`
- `SavedStateHandle` remains available for local transient saved state only

This should be done for every destination ViewModel that currently depends on typed route args.

### Step 7 — Migrate meal planner result flow

Replace the `previousBackStackEntry.savedStateHandle` focus-date return with a typed result event.

Expected behavior:
- when a new meal plan is created from recipe details, `preselectedRecipeId` still enters the form as a forward arg
- when the user saves and returns, the new date is emitted as `NavigationResult.MealPlannerFocusDate`
- `MealPlannerViewModel` consumes the result and sets `focusDate`
- `ClearFocusDate` still clears it after display

### Step 8 — Replace graph observer routes with direct sync mapping

Delete the graph observer layer once the direct route-to-sync mapping is in place.

Do not lose behavior:
- sync should still activate only for the relevant feature area
- sync should still deactivate when the feature area leaves the screen

### Step 9 — Remove Navigation 2 leftovers

After the Nav3 host is stable, remove:
- `NavController` ownership
- `NavHostController`-specific code
- `navigation<...>` graph blocks used only for hierarchy
- `hasRoute(...)` hierarchy checks
- `previousBackStackEntry.savedStateHandle` result logic
- `SavedStateHandle.toRoute(...)` in route-argument ViewModels
- graph observer routes

---

## Files Likely To Change

These are the files and areas expected to move:
- `app/src/main/java/com/example/lifetogether/MainActivity.kt`
- `app/src/main/java/com/example/lifetogether/ui/navigation/NavHost.kt`
- `app/src/main/java/com/example/lifetogether/ui/navigation/AppNavigator.kt`
- `app/src/main/java/com/example/lifetogether/ui/navigation/NavRoutes.kt`
- `app/src/main/java/com/example/lifetogether/ui/navigation/Navigator.kt`
- `app/src/main/java/com/example/lifetogether/ui/navigation/NotificationDestination.kt`
- `app/src/main/java/com/example/lifetogether/ui/navigation/DestinationMapper.kt`
- all route wrappers in `ui/feature/*/*Route.kt`
- all graph observer routes
- route-argument ViewModels listed above
- `MealPlannerViewModel`
- `MealPlanDetailsRoute`
- `MealPlanDetailsViewModel`

---

## Acceptance Criteria

- [ ] The app compiles with Navigation 3 dependencies
- [ ] `AppRoute` is used as `NavKey`
- [ ] `NavHost` is replaced by `NavDisplay`
- [ ] There is no `NavController` in app navigation code
- [ ] The back stack is owned by Navigation 3 state
- [ ] `AppNavigator` remains, but only as a thin state reducer
- [ ] Auth/loading still hard reset
- [ ] Profile/Settings special behavior is preserved
- [ ] Shared guide and tip tracker ViewModels still work
- [ ] Custom route transitions are preserved
- [ ] Graph observer routes are removed
- [ ] Sync activation is driven by route mapping, not graph hierarchy
- [ ] Route-argument ViewModels use official Hilt assisted injection
- [ ] Meal planner focus-date return uses typed `NavigationResult`
- [ ] Recipe -> meal plan preselection still works
- [ ] Notification taps still work as the external entry path
- [ ] Process death restoration works for the Nav3 back stack

---

## Risks

- Assisted injection introduces boilerplate across several ViewModels.
- Shared ViewModel scoping for guides and tip tracker must be handled carefully or behavior will drift.
- Result routing for meal planner is easy to get subtly wrong if the result is treated like durable state instead of a one-shot event.
- The sync mapping must preserve feature visibility semantics exactly or background sync may stop starting/stopping at the right time.
- The first cutover will touch many files at once; partial migration would be harder to reason about than the atomic migration already agreed upon.

---

## Out Of Scope

- Adaptive multi-pane layouts
- New bottom-sheet or dialog patterns
- Changing the current product flow or information architecture
- Reworking Profile/Settings behavior beyond the current special-case logic
- Replacing notifications with a different external entry system
- Introducing phased Navigation 2 / Navigation 3 coexistence

---

## Notes

This phase intentionally keeps the app behavior conservative while moving the implementation to Navigation 3 primitives.

The simplification we are taking is only the part that removes Navigation 2 graph hierarchy as an implementation detail:
- sync activation becomes direct route mapping
- the navigation host becomes `NavDisplay`
- route arguments become typed keys passed through Hilt assisted injection
- back results become typed events

Everything else should behave the same on the surface.
