# Phase 2 — Data Layer Deepening

**Status:** Implementing _(Not started → Grill-me in progress → Implementing → Complete)_

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
- Error model for Phase 2: one shared `AppError` hierarchy across domains.
- Streaming contract default: `Flow<T>` for local observation + explicit `refresh()/write` commands returning `Result`.
- `LocalDataSource` split strategy: full split in Phase 2; no new production callsites may depend on the god-object.
- Passthrough use case policy: remove passthrough use cases in Phase 2 domains; keep only orchestration/business-rule use cases.
- Rollout strategy: vertical domain slices.
- Contract strictness: no temporary adapter layer for repository contracts in Phase 2; migrated slices must be fully strict on `Result`.
- Logging strictness: full sweep of `println(...)` in `data/` and `domain/` during this phase.

## Subphases

- [ ] 2.1 Define phase-wide `Result<T, E>` and shared `AppError` hierarchy
  - Create functional `Result` sealed class (`Success`, `Failure`) and shared `AppError`.
  - Map expected Firebase/Room/IO failures into `AppError` instead of throwing.
  - Define contract rules for one-shot commands vs streaming observations.
- [ ] 2.2 Split `LocalDataSource` into feature-focused local data sources
  - Decompose the current god-object local source into per-domain local sources.
  - Rewire DI so repositories depend on focused local data sources.
  - Do not add new production callsites to the old god-object local API.
- [ ] 2.3 Deepen repositories with Offline-First / SSOT per vertical domain slice
  - Reads exposed to UI observe local DB (`Flow<T>`).
  - Refresh/write operations perform remote fetch + local persistence inside repositories and return `Result`.
  - Mapping/orchestration logic moves from higher layers into repositories.
  - Domain slices:
    - lists/grocery
    - recipes/guides
    - gallery/tip-tracker/user-family
- [ ] 2.4 Migrate repository contracts in migrated slices to strict `Result`
  - Replace legacy listener-style repository contracts in migrated slices.
  - No temporary adapter shims for repository boundaries in this phase.
- [ ] 2.5 Remove passthrough use cases in migrated slices
  - Remove use cases that only forward single repository calls.
  - Rewire ViewModels to inject repositories directly where appropriate.
  - Keep use cases that add orchestration/business rules.
- [ ] 2.6 Logging cleanup in `data/` and `domain/`
  - Replace `println(...)` with `Log.d/w/e` as appropriate.
  - Keep tags consistent and avoid sensitive data in logs.
- [ ] 2.7 Verification discipline for each issue slice
  - Compile after each issue slice before commit.
  - Run grep checks for `println(...)` and legacy contract leftovers.
  - Keep issue/phase checkboxes aligned with implemented scope.

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> Decision rule: important design decisions for this phase are only final after explicit user agreement in the grill-me session. Proposed options can be drafted, but they must stay marked as proposed until approved.
>
> Open questions for this phase have been resolved in grill-me. New decision branches discovered during implementation must be added here and explicitly approved before being marked final.

### Acceptance criteria

- [ ] Shared `Result<T, AppError>` contract is implemented and used in all Phase 2 migrated repository slices.
- [ ] Shared `AppError` hierarchy is used for mapped expected failures in migrated slices.
- [ ] `LocalDataSource` is split by feature domain with DI updated accordingly.
- [ ] Migrated repository reads follow Offline-First/SSOT by observing local DB flows.
- [ ] Refresh/write orchestration (remote + local persistence) is owned by repositories, not ViewModels/use cases.
- [ ] Passthrough use cases in migrated slices are removed and call sites updated.
- [ ] Repository boundaries in migrated slices no longer expose legacy listener wrappers.
- [ ] `println(...)` is removed from `data/` and `domain/`.

### Test cases

- [ ] For at least one flow per migrated domain slice, local observation reflects remote refresh after repository persistence.
- [ ] Expected repository failure paths return `Result.Failure(AppError)` without raw exception leaks.
- [ ] ViewModel call sites compile and work after passthrough use case removals.
- [ ] Repository contracts in migrated slices compile with strict `Result` signatures.
- [ ] `rg "println\\(" app/src/main/java/com/example/lifetogether/data app/src/main/java/com/example/lifetogether/domain` returns no matches.
- [ ] Compilation passes after each issue slice and at phase integration checkpoints.

## GitHub Issues

Milestone: `V2 Phase 2: Data Layer Deepening` (created)

Issue breakdown (agreed):

- `[Phase 2] [2.1] Define Result wrapper and AppError hierarchy` — issue #9
- `[Phase 2] [2.2] Split LocalDataSource by feature domain and rewire DI` — issue #10
- `[Phase 2] [2.3] Deepen lists/grocery repositories with Offline-First` — issue #11
- `[Phase 2] [2.3] Deepen recipes/guides repositories with Offline-First` — issue #12
- `[Phase 2] [2.3] Deepen gallery/tip-tracker/user-family repositories with Offline-First` — issue #13
- `[Phase 2] [2.4-2.5] Migrate repository contracts to strict Result and remove passthrough use cases` — issue #14
- `[Phase 2] [2.6-2.7] Logging cleanup and verification sweep` — issue #15

Issue body requirements:

- include scope/ownership
- include issue-specific subphase checklist items from this file
- include issue-specific acceptance criteria and test checklist items as checkboxes
- include out-of-scope boundaries against other Phase 2 issues

## Questions

- Is there any of the use cases that handles logic that might as well be inside the repo instead?
- Find all old patterns like runCatching and such and give me a list of things that should be updated to follow current best practices
- ImageRepository should be removed and move the functions into the domain repos instead