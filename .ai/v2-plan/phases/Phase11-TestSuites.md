# Phase 11 — Test Suite Implementation

**Status:** Not started _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Write the test suites that the fully restructured codebase now makes possible. Focus on boundary tests at the repository layer, state-transition tests for ViewModels, and preview-based tests for composables.

## Scope

- Data/domain layer: repository boundary tests
- UI layer: ViewModel unit tests, composable preview and interaction tests
- Test infrastructure setup if not already in place (in-memory database, fake repositories, etc.)

## Key Decisions Already Made

- **Repository boundary tests** use real local stand-ins (e.g. in-memory Room database) — not mocked DAOs or internal persistence seams. This is the primary test layer for correctness of data logic.
- **ViewModel tests** focus on state transitions and business interactions — given an action, assert the resulting `UiState` and emitted `UiEvent`.
- **Composable tests** are `@Preview`-only — no automated UI interaction tests. Screens are plain composables by this point; previews allow quick manual visual inspection without test infrastructure overhead.
- All composables must be previewable (enforced since Phase 3); this phase adds the actual `@Preview` declarations.
- Writing test suites was intentionally deferred to this phase so the restructured seams are stable before investing in tests.

## Subphases

_To be finalised during the pre-implementation grill-me session._

- [ ] 11.1 Set up test infrastructure: in-memory database, fake `SessionRepository`, fake repositories
- [ ] 11.2 Write repository boundary tests (data/domain layer)
- [ ] 11.3 Write ViewModel unit tests (state transition and event coverage)
- [ ] 11.4 Write composable `@Preview` tests for manual visual inspection
- [ ] 11.5 Audit coverage gaps; add missing tests for critical paths

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
_To be defined during the pre-implementation grill-me session._

### Test cases
_To be defined during the pre-implementation grill-me session._

## GitHub Issues

Create milestone `Phase 11: Test Suite Implementation` and the following issues assigned to it:

- `[Phase 11] Set up test infrastructure`
- `[Phase 11] Repository boundary tests`
- `[Phase 11] ViewModel unit tests`
- `[Phase 11] Composable @Preview tests`
- `[Phase 11] Coverage audit and gap filling`

## Open Questions

- Which feature domains carry the highest risk and should be prioritised for test coverage first?
- Should UI interaction tests use Compose testing APIs, screenshot testing, or both?
- What coverage threshold, if any, should be set as an acceptance criterion for this phase?
