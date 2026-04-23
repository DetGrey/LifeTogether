# Phase 4 — Navigation Type-Safety

**Status:** Implementing _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Migrate all navigation routes from string-based concatenation to Kotlin Serialization typed route objects, eliminating runtime crash risk from typos or missing arguments. This includes simplifying the `Navigator` interface from 28 named functions down to `navigate(AppRoute)` + `navigateBack()`, so that typed route objects are the public contract at every level — not just inside `AppNavigator`.

## Scope

- `ui/navigation/NavRoutes.kt` (renamed from `AppRoutes.kt`)
- `ui/navigation/NavHost.kt`
- `ui/navigation/Navigator.kt`
- `ui/navigation/AppNavigator.kt`
- `ui/feature/guides/details/GuideGraphRoutes.kt`
- `ui/feature/gallery/GalleryRoutes.kt`
- `MainActivity.kt` (FCM notification entry point)
- All ViewModel and composable call sites that currently call Navigator functions

## Key Decisions

### Navigator interface: full migration (not internals-only)

The `Navigator` interface is simplified from 28 named functions to two:

```kotlin
interface Navigator {
    fun navigate(route: AppRoute)
    fun navigateBack()
}
```

Rationale: keeping 28 functions would hide typed routes behind a wrapper that adds no value once typed route objects exist. The typed routes become the public API at every level. ViewModels call `navigator.navigate(RecipeDetailsNavRoute(id))` directly.

### Route naming: `NavRoute` suffix for destinations, `Graph` suffix for nested graph markers

- Screen destinations: `HomeNavRoute`, `RecipeDetailsNavRoute`, `GuideDetailsNavRoute`, etc.
- Nested graph markers: `GuideGraph`, `GalleryGraph`, `TipTrackerGraph` (not navigable directly; used only as `navigation<T>()` keys in NavHost and for ViewModel scoping)

### `sealed interface AppRoute`

All navigable screen destinations implement `sealed interface AppRoute`. Nested graph markers (`GuideGraph`, `GalleryGraph`, `TipTrackerGraph`) are plain `@Serializable object`s — they do **not** implement `AppRoute` because they are never passed to `navigator.navigate()`.

### Nested graphs: args per destination, not on the graph

Nested graph objects carry no args. Each destination carries its own:
- `@Serializable data class GuideDetailsNavRoute(val guideId: String) : AppRoute`
- `@Serializable data class GuideStepPlayerNavRoute(val guideId: String) : AppRoute`

Navigation goes directly to the start destination (e.g. `navigate(GuideDetailsNavRoute(guideId))`), not to the graph entry point. Nested graphs exist solely for ViewModel scoping and lifecycle binding.

### `NavOptions`: `launchSingleTop` universal default

`AppNavigator.navigate()` applies `launchSingleTop = true` to every navigation call. Additionally, `GalleryNavRoute` gets `popUpTo<GalleryNavRoute> { inclusive = false }` to clear album/media screens when returning to the gallery. The `popUpTo` previously on `navigateToAlbumMedia` is dropped — it was unreachable (always called from the gallery list screen).

### `navigateBack()` fallback

`navigateBack()` calls `popBackStack()`; if it returns `false` (empty stack), it falls back to `navigate(HomeNavRoute)` to prevent the app from closing.

### FCM notification entry point

`MainActivity` reads `intent?.getStringExtra("destination")` and currently calls `navController.navigate(string)`. With typed routes, a mapper function `fun routeFromDestinationString(destination: String): AppRoute?` is added in `ui/navigation/`. `MainActivity` calls the mapper and routes the result through `Navigator`. Unknown strings fall back to `HomeNavRoute`.

### `AppRoutes.kt` → `NavRoutes.kt`

The file is renamed to `NavRoutes.kt`. All string route constants and all arg name constants (`RECIPE_ID_ARG`, `GUIDE_ID_ARG`, etc.) are deleted. The file contains only `sealed interface AppRoute`, all `@Serializable` route objects/data classes, and the nested graph marker objects.

### No deep links

There are no deep link declarations in `AndroidManifest.xml`. The only external navigation entry point is the FCM string-based intent extra handled above.

## Subphases

- [ ] **4.1 Verify dependencies** — Navigation 2.9.6 and the Kotlin Serialization plugin (`kotlin("plugin.serialization")`) and `kotlinx-serialization-json` are already present. Confirm no version changes are needed.

- [ ] **4.2 Define typed route objects in `NavRoutes.kt`**
  - Rename `AppRoutes.kt` → `NavRoutes.kt`
  - Declare `sealed interface AppRoute`
  - For every no-arg screen: `@Serializable object XxxNavRoute : AppRoute`
  - For every screen with args: `@Serializable data class XxxNavRoute(val arg: Type) : AppRoute`
  - For each nested graph: `@Serializable object GuideGraph`, `@Serializable object GalleryGraph`, `@Serializable object TipTrackerGraph` (no `AppRoute`)
  - Delete all string constants and all arg name constants

- [ ] **4.3 Migrate `NavHost.kt` and destination wrapper composables**
  - Replace every `composable("route_string") { }` with `composable<XxxNavRoute> { backStackEntry -> }`
  - Replace nested graph definitions with `navigation<GuideGraph>(startDestination = GuideDetailsNavRoute::class)` etc.
  - Remove all `navArgument()` declarations
  - Remove all `Uri.decode()` / `Uri.encode()` arg handling — args are now properties on the route object
  - In `GuideGraphRoutes.kt` and `GalleryRoutes.kt`: replace `backStackEntry.arguments?.getString(ARG)` with `backStackEntry.toRoute<XxxNavRoute>().argName`
  - `GalleryGraphObserverRoute` requires no changes (lifecycle binding, not a navigation destination)

- [ ] **4.4 Simplify `Navigator`, update `AppNavigator`, update all call sites, add FCM mapper**
  - Replace 28-function `Navigator` interface with `navigate(AppRoute)` + `navigateBack()`
  - Replace `AppNavigator` implementation: single `navigate(route: AppRoute)` applies `launchSingleTop = true` universally; `when(route)` check adds `popUpTo<GalleryNavRoute>` for `GalleryNavRoute`
  - `navigateBack()`: `popBackStack()` with fallback to `navController.navigate(HomeNavRoute)`
  - Update every ViewModel and composable that calls a Navigator function to call `navigator.navigate(XxxNavRoute(...))` instead
  - Add `routeFromDestinationString(destination: String): AppRoute?` in `ui/navigation/`; update `MainActivity` to use it

- [ ] **4.5 Remove all legacy string route references**
  - Confirm `AppRoutes.kt` is fully deleted (renamed to `NavRoutes.kt` in 4.2)
  - Grep for any remaining string route patterns (`navigate("`, `AppRoutes.`, `navArgument`) and resolve
  - Build must pass cleanly with zero navigation-related warnings

## Acceptance Criteria

- [ ] `NavRoutes.kt` exists with `sealed interface AppRoute` and all `@Serializable` route objects/data classes; `AppRoutes.kt` is deleted
- [ ] `Navigator` interface has exactly two methods: `navigate(route: AppRoute)` and `navigateBack()`
- [ ] `NavHost.kt` uses only typed `composable<T>` and `navigation<T>` destinations; no `navArgument()` calls remain
- [ ] `AppNavigator` builds no route strings; all `navigate()` calls use typed route instances
- [ ] `launchSingleTop = true` applied to all navigate calls; `popUpTo<GalleryNavRoute>` applied only for `GalleryNavRoute`
- [ ] Destination wrapper composables use `backStackEntry.toRoute<T>()` instead of string arg extraction
- [ ] FCM mapper function handles all known destination strings; unknown strings fall back to `HomeNavRoute`
- [ ] No string route constants remain anywhere in the codebase
- [ ] Project compiles with zero navigation-related warnings

## Test Cases

- [ ] Navigate to every top-level screen and verify it loads
- [ ] Navigate to guide details with a valid `guideId` → guide content loads correctly
- [ ] From guide step player, press back → returns to guide details (not guide list)
- [ ] From gallery, tap album → album loads; tap media → media loads; press back → returns to album
- [ ] Navigate to gallery from home when already deep in gallery → stack clears to gallery, no duplicate instance
- [ ] `navigateBack()` with empty back stack → lands on Home, app does not close
- [ ] FCM intent with a known destination string → navigates to the correct screen
- [ ] FCM intent with an unknown destination string → navigates to Home without crashing

## GitHub Issues

Create milestone `Phase 4: Navigation Type-Safety` and the following issues assigned to it:

- `[Phase 4] Define typed route objects and migrate NavHost`
  - Covers subphases 4.1, 4.2, 4.3
- `[Phase 4] Migrate all navigate() call sites and remove string routes`
  - Covers subphases 4.4, 4.5
