# Phase 7 — Shared Component Consolidation

**Status:** Not started _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Merge all duplicate shared UI components into one canonical component per type. Remove the parallel implementations that currently create uncertainty about which path is authoritative.

## Scope

Current duplicates to resolve:

| Canonical target | Duplicates to remove |
|-----------------|----------------------|
| One text field | `ui/common/CustomTextField.kt` + `ui/common/textfield/CustomTextField.kt` |
| One dropdown | `ui/common/DropDown.kt` + `ui/common/dropdown/Dropdown.kt` |
| One date picker text field | `ui/common/DatePickerTextField.kt` + `ui/common/textfield/DatePickerTextField.kt` |
| Date picker dialog state | `ui/common/dialog/CustomDatePickerDialog.kt` — remove ViewModel for ephemeral UI state |

## Key Decisions Already Made

- One canonical path per component type — duplicates are deleted, not deprecated.
- Naming and folder placement must make the canonical choice immediately obvious.
- The date picker dialog should not use a `ViewModel` for ephemeral UI element state; prefer `remember`ed local state or a plain UI state holder.
- Before choosing which variant becomes canonical, check whether the desired styling can be pushed fully into theme tokens (Phase 6) rather than staying in the wrapper.
- Custom wrappers are only kept when they provide: shared business behaviour, accessibility improvements, stable app-specific tokens/layout defaults, or a product-specific interaction native components do not handle well.

## Subphases

_To be finalised during the pre-implementation grill-me session._

- [ ] 7.1 Audit both text field variants; choose canonical; migrate all usages; delete duplicate
- [ ] 7.2 Audit both dropdown variants; choose canonical; migrate all usages; delete duplicate
- [ ] 7.3 Audit both date picker text field variants; choose canonical; migrate all usages; delete duplicate
- [ ] 7.4 Refactor `CustomDatePickerDialog` to use remembered local state instead of a ViewModel

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
_To be defined during the pre-implementation grill-me session._

### Test cases
_To be defined during the pre-implementation grill-me session._

## GitHub Issues

Create milestone `Phase 7: Shared Component Consolidation` and the following issues assigned to it:

- `[Phase 7] Consolidate text field components`
- `[Phase 7] Consolidate dropdown components`
- `[Phase 7] Consolidate date picker text field components`
- `[Phase 7] Remove ViewModel from CustomDatePickerDialog`

> Pattern: one issue per component type. Additional components may be identified during grill-me — update this list before creating issues.

## Open Questions

- For each component pair, which variant is the more complete / correct starting point for the canonical version?
- Are there any other `ui/common/` duplicates not yet identified?
