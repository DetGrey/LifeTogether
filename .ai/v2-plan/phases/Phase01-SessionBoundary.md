# Phase 1 — Session Boundary Cleanup

**Status:** Not started

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

_To be finalised during the pre-implementation grill-me session._

- [ ] 1.1 Define and implement `SessionRepository` interface and Hilt-provided singleton
- [ ] 1.2 Define and implement root coordinator ViewModel (activity-scoped)
- [ ] 1.3 Update `MainActivity` to use root coordinator instead of `AppSessionViewModel`
- [ ] 1.4 Update `NavHost` to remove `AppSessionViewModel` parameter threading
- [ ] 1.5 Update `ObserverLifecycle` to align with new coordinator ownership
- [ ] 1.6 Migrate any remaining direct `AppSessionViewModel` references

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
_To be defined during the pre-implementation grill-me session._

### Test cases
_To be defined during the pre-implementation grill-me session._

## Open Questions

- Which current or future feature ViewModels truly need to observe session changes reactively vs. receive session input once at route level?
