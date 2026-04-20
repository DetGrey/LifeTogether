# Phase 5 — ViewModel Normalization

**Status:** Not started _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Normalise all feature ViewModels to expose a single `UiState` object for persistent screen state and a `Channel<UiEvent>` for fire-and-forget events. Remove scattered public mutable fields, ad hoc setup flags, and ad hoc setup call patterns.

## Scope

- All feature ViewModels under `ui/feature/`
- Priority targets: `RecipesViewModel`, `RecipeDetailsViewModel`, `FamilyViewModel`, `ProfileViewModel`, `GuidesViewModel`
- Good reference models already in codebase: `GalleryViewModel`, `ListEntryDetailsViewModel`
- Any remaining `println(...)` calls in ViewModels and screens (replace with `Log.d`)

## Key Decisions Already Made

- Each ViewModel exposes **one main `UiState` data class** as a `StateFlow`.
- One-off events (navigation triggers, snackbar messages) use a **`Channel<UiEvent>` exposed as a `Flow`** — avoids rotation bugs and state-clearing boilerplate.
- No public mutable fields: dialog flags, raw error strings, ad hoc IDs, and filter selections must move into `UiState` or `UiEvent`.
- No ad hoc setup calls like `setUpRecipes(familyId)` — prefer clean route-prepared input and stable `init`-based initialisation.
- If extra form state is needed it lives in a clearly paired form state model, not scattered mutable fields.
- Blocking confirmation state (e.g. delete confirmation dialogs currently rendered) may stay in `UiState` — it is part of the rendered UI.
- Delete any remaining passthrough use cases encountered during this phase.
- Logging cleanup: all `println(...)` in ViewModels and screens replaced with `Log.d(...)`.
- We have some shared ViewModels like observer, notification and session which we should see if can be done in a better way
- When making subphases and discussing the details for each subphase, we have to go through every ViewModel one for one to make sure we don't forget to refactor and normalise something

## Subphases

_To be finalised during the pre-implementation grill-me session._

- [ ] 5.1 Normalise `RecipesViewModel` and `RecipeDetailsViewModel`
- [ ] 5.2 Normalise `FamilyViewModel`
- [ ] 5.3 Normalise `ProfileViewModel`
- [ ] 5.4 Normalise `GuidesViewModel`
- [ ] 5.5 Audit and normalise remaining feature ViewModels
- [ ] 5.6 Logging cleanup across ViewModels and screens

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
_To be defined during the pre-implementation grill-me session._

### Test cases
_To be defined during the pre-implementation grill-me session._

## GitHub Issues

Create milestone `Phase 5: ViewModel Normalization` and the following issues assigned to it:

- `[Phase 5] Normalise RecipesViewModel and RecipeDetailsViewModel`
- `[Phase 5] Normalise FamilyViewModel`
- `[Phase 5] Normalise ProfileViewModel`
- `[Phase 5] Normalise GuidesViewModel`
- `[Phase 5] Normalise remaining feature ViewModels`
- `[Phase 5] Logging cleanup — ViewModels and screens`

## Open Questions

- None locked yet — open questions will surface during the grill-me session.
