# Phase 2 — Data Layer Deepening

**Status:** Not started _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Deepen the repository layer using a Ports & Adapters approach, introduce a functional `Result` wrapper for all data operations, split the god-object `LocalDataSource` by feature domain, and enforce the Offline-First/Single Source of Truth pattern throughout.

## Scope

- All repositories under `data/repository/`
- `LocalDataSource` and all DAOs
- Domain use cases
- Any `println(...)` calls in data and domain layers (replace with `Log.d`)

## Key Decisions Already Made

- Standardise on a functional `Result<T, E>` sealed class for all data/domain operations.
- The data/domain layer must never throw raw exceptions for expected failures — catch and return `Result.Failure`.
- Repositories become deep modules: they own mapping logic and data source orchestration internally.
- Split `LocalDataSource` by feature domain (Groceries, Recipes, Albums, Guides, etc.) — no more god-object.
- **Offline-First / SSOT mandate:** UI observes only the local database via `Flow`. When a refresh is requested, the repository fetches from network, saves to local DB, and finishes. ViewModels must never consume network data directly.
- Delete passthrough use cases that do nothing but call a single repository method. ViewModels call repositories directly for simple operations. Use cases are reserved for complex/reused business logic only.
- Logging cleanup: all `println(...)` in data and domain layers replaced with `Log.d(...)`.

## Subphases

_To be finalised during the pre-implementation grill-me session._

- [ ] 2.1 Define `Result<T, E>` sealed class and error hierarchy
- [ ] 2.2 Split `LocalDataSource` into per-feature data sources
- [ ] 2.3 Deepen repositories — move mapping logic in, enforce Offline-First pattern
- [ ] 2.4 Update all repository return types to use `Result`
- [ ] 2.5 Remove passthrough use cases; update ViewModel call sites
- [ ] 2.6 Logging cleanup across data and domain layers

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
_To be defined during the pre-implementation grill-me session._

### Test cases
_To be defined during the pre-implementation grill-me session._

## GitHub Issues

Create milestone `Phase 2: Data Layer Deepening` and the following issues assigned to it:

- `[Phase 2] Define Result wrapper and error hierarchy`
- `[Phase 2] Data layer — <domain>` _(one per feature domain — confirm full list during grill-me)_
- `[Phase 2] Remove passthrough use cases`
- `[Phase 2] Logging cleanup — data and domain layers`

> Pattern: one issue per feature domain for the repository deepening work. Update the domain list after the pre-implementation grill-me session.

## Open Questions

- What should the error hierarchy look like — one sealed `AppError`, per-domain errors, or something else?
- Should repositories expose `Flow<Result<T>>` for stream operations or only `Result<T>` for one-shot operations?
