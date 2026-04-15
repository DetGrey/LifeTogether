# Phase 4 — Navigation Type-Safety

**Status:** Not started _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Migrate all navigation routes from string-based concatenation to Kotlin Serialization typed route objects, eliminating runtime crash risk from typos or missing arguments.

## Scope

- `ui/navigation/NavHost.kt`
- All route definitions across the app
- All `navigate(...)` call sites in route composables and ViewModels

## Key Decisions Already Made

- Routes must be defined as `@Serializable` data classes or objects (e.g. `data class RecipeDetailsRoute(val recipeId: String)`).
- String-based routes are considered legacy and must not be used for any refactored navigation.
- Official Compose Type-Safe Navigation with Kotlin Serialization is the target API.

## Subphases

_To be finalised during the pre-implementation grill-me session._

- [ ] 4.1 Add / verify Kotlin Serialization dependency and navigation type-safe plugin
- [ ] 4.2 Define typed route objects for all destinations
- [ ] 4.3 Migrate `NavHost` composable destinations to typed routes
- [ ] 4.4 Update all `navigate(...)` call sites to use typed route instances
- [ ] 4.5 Remove all legacy string route constants

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
_To be defined during the pre-implementation grill-me session._

### Test cases
_To be defined during the pre-implementation grill-me session._

## GitHub Issues

Create milestone `Phase 4: Navigation Type-Safety` and the following issues assigned to it:

- `[Phase 4] Define typed route objects and migrate NavHost`
- `[Phase 4] Migrate all navigate() call sites and remove string routes`

## Open Questions

- Are there any deep-link or external navigation entry points that need special handling with typed routes?
