# Phase 3 — Route/Screen Split

**Status:** Not started

## Goal

Separate every feature screen into a route composable (Android wiring) and a screen composable (plain, previewable). Set up the shared `SnackbarHost` at the app root. Standardise lifecycle-aware state collection. Migrate error handling to a snackbar-first approach.

## Scope

- All feature screens under `ui/feature/`
- `ui/navigation/NavHost.kt`
- App root / `MainActivity` scaffold
- `ui/common/observer/` (aligned with root coordinator from Phase 1)

## Key Decisions Already Made

- **Route composable** responsibilities: resolves nav args, obtains `ViewModel` via `hiltViewModel()`, reads shared state if needed, passes plain state + callbacks down.
- **Screen composable** responsibilities: receives plain state and callbacks only — no `ViewModel`, no Hilt, fully previewable.
- All route/screen collection layers use `collectAsStateWithLifecycle()` (not `collectAsState()`).
- Plain reusable composables stay free of Android-only collection APIs.
- **All screens must have real `@Preview` support** — `"Preview requires AppSessionViewModel"` is banned.
- **Shared `SnackbarHost`** wired at the app root with a reusable route-level collector.
- Feature layers emit typed `UiEvent`s (via `Channel`) — they do not render error dialogs directly.
- **Snackbar-first error handling:** validation → inline field errors; operation failures → snackbar/banner; blocking dialogs reserved for rare high-severity interruptions only.
- Each screen has at most one meaningful `Scaffold`; no extra scaffolds just for error handling.
- Pilot screen: `ui/feature/admin/groceryList/AdminGrocerySuggestionsScreen.kt`.

## Subphases

_To be finalised during the pre-implementation grill-me session._

- [ ] 3.1 Wire shared `SnackbarHost` at app root; define typed `UiEvent` base / collection helper
- [ ] 3.2 Pilot split: `AdminGrocerySuggestionsScreen` — route + screen + preview
- [ ] 3.3 Split remaining feature screens (one by one or grouped by feature)
- [ ] 3.4 Enforce `collectAsStateWithLifecycle()` across all route layers
- [ ] 3.5 Migrate `ErrorAlertDialog` usages to snackbar/event pattern

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
_To be defined during the pre-implementation grill-me session._

### Test cases
_To be defined during the pre-implementation grill-me session._

## Open Questions

- Should the typed `UiEvent` base type be a project-wide sealed interface, or per-feature sealed classes?
- Which screens have the most complex session wiring and should be tackled last?
