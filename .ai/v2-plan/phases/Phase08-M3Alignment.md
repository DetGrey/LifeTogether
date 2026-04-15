# Phase 8 — Native Material 3 Alignment

**Status:** Not started

## Goal

Replace custom component shells with native Material 3 equivalents where the native component satisfies the product need with light styling. Each replacement requires explicit decision — no component is removed without review.

## Scope

| Current component | M3 candidate | Notes |
|------------------|-------------|-------|
| `ui/common/TopBar.kt` | `CenterAlignedTopAppBar` | Default target; exceptions allowed per screen |
| `ui/common/dialog/CustomAlertDialog.kt` | `AlertDialog` / `BasicAlertDialog` | Custom shell currently blocks dismissal by default |
| `ui/common/dialog/ErrorAlertDialog.kt` | Snackbar / banner (from Phase 3) | Mostly migrated in Phase 3; any residual cleaned here |
| `ui/common/dialog/CustomDatePickerDialog.kt` | `DatePickerDialog` + remembered state | ViewModel removed in Phase 7; native shell here |
| `ui/common/button/AddButton.kt` | `FloatingActionButton` | Only if behaviour matches |

## Key Decisions Already Made

- `CenterAlignedTopAppBar` is the default replacement for `TopBar`. Exceptions must be documented per screen.
- Current `subText` content should move into screen content below the app bar — not forced into the app bar itself.
- Current left/right `TopBar` icons map to `navigationIcon` and `actions` slots natively.
- Custom UI is **never deleted or replaced without explicit user approval first**.
- Before replacing any component, check whether desired visual result can be achieved through shared theme tokens alone.
- Component policy order: (1) native M3 + theme tokens → (2) one canonical shared wrapper if truly needed → (3) feature-specific custom only if the first two are not enough.

## Subphases

_To be finalised during the pre-implementation grill-me session._

- [ ] 8.1 Replace `TopBar.kt` with `CenterAlignedTopAppBar`; handle per-screen exceptions
- [ ] 8.2 Replace `CustomAlertDialog.kt` with `AlertDialog` / `BasicAlertDialog`
- [ ] 8.3 Clean up any residual `ErrorAlertDialog` usages not migrated in Phase 3
- [ ] 8.4 Replace `CustomDatePickerDialog.kt` shell with `DatePickerDialog`
- [ ] 8.5 Review `AddButton.kt` against `FloatingActionButton`; replace or document why kept

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
_To be defined during the pre-implementation grill-me session._

### Test cases
_To be defined during the pre-implementation grill-me session._

## Open Questions

- Which screens need a non-default top app bar pattern (e.g. `LargeTopAppBar` for detail screens)?
- Are there any other custom components in `ui/common/` not listed above that should be reviewed?
