# Phase 10 — Background Sync Layer

**Status:** Not started _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Introduce a dedicated background sync layer so that data changes made offline or when the app is in the background reliably reach Firestore, without the UI being responsible for orchestrating remote pushes.

## Scope

- New sync infrastructure (WorkManager workers or centralized sync service)
- Integration points with the Offline-First repositories from Phase 2
- Firestore push / pull orchestration

## Key Decisions Already Made

- The UI must never be responsible for orchestrating remote data pushes or pulling background updates.
- The sync layer sits on top of the Phase 2 Offline-First repositories — it is not a replacement for them.
- `WorkManager` is the default candidate for reliable background sync; a centralized sync service is an alternative to evaluate during grill-me.
- The root coordinator ViewModel (Phase 1) may trigger sync behaviour in response to session changes, but the sync implementation detail lives in the sync layer itself.

## Subphases

_To be finalised during the pre-implementation grill-me session._

- [ ] 10.1 Define sync architecture: WorkManager vs. centralized service; conflict resolution strategy
- [ ] 10.2 Implement sync infrastructure and Hilt wiring
- [ ] 10.3 Implement per-feature-domain sync workers
- [ ] 10.4 Integrate sync triggers with root coordinator and app lifecycle
- [ ] 10.5 Verify offline → sync → Firestore flow end to end

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
_To be defined during the pre-implementation grill-me session._

### Test cases
_To be defined during the pre-implementation grill-me session._

## GitHub Issues

Create milestone `Phase 10: Background Sync Layer` and the following issues assigned to it:

- `[Phase 10] Implement sync infrastructure and Hilt wiring`
- `[Phase 10] Sync workers — <domain>` _(one per feature domain, same domains as Phase 2 — confirm during grill-me)_
- `[Phase 10] Integrate sync triggers and verify end-to-end`

> Pattern: one issue per feature domain for the sync worker implementation. Update the domain list after the pre-implementation grill-me session.

## Open Questions

- How should sync conflicts be resolved — last-write-wins, server-wins, or per-entity policy?
- Should sync be triggered eagerly (on every change) or batched (on reconnect / periodic schedule)?
- Which feature domains need sync most urgently vs. are low-risk with Firestore real-time listeners?
