# Phase 9 — Motion, Loading & Interaction Patterns

**Status:** Not started _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Standardise how UI elements appear, how loading states are communicated, how navigation transitions feel, and how common user actions (delete, save, confirm) behave consistently across all features.

## Scope

- All feature screens (loading states and UI element animations)
- Navigation setup (global transition)
- Common interaction flows: delete, save, confirm, load

## Key Decisions Already Made

- **No harsh popping:** all UI elements that appear or disappear (dropdowns, inline errors, expanding cards, etc.) must use `AnimatedVisibility` or `animateContentSize`. No instant visibility toggles.
- **Skeleton loaders for data fetching:** blocking full-screen loading spinners are banned except for critical/unrecoverable paths.
- **Inline progress indicators for saving:** a spinner or progress indicator near the action, not a full-screen block.
- **One global navigation transition:** a subtle slide + crossfade using Material 3 easing curves. Applied universally — no per-screen ad hoc animations.
- **No Shared Element Transitions** — explicitly avoided to keep maintenance simple.
- **Standardised interaction patterns:** delete, save, and confirm actions must feel and behave identically across all features. Specific UX rules for each action will be defined during the pre-implementation grill-me session.

## Subphases

_To be finalised during the pre-implementation grill-me session._

- [ ] 9.1 Define and apply the global navigation transition
- [ ] 9.2 Design and implement the skeleton loader component; migrate all loading screens
- [ ] 9.3 Audit and fix all harsh `AnimatedVisibility` / `animateContentSize` gaps
- [ ] 9.4 Define specific UX rules for delete, save, and confirm interactions
- [ ] 9.5 Implement standardised interaction patterns across all features

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
_To be defined during the pre-implementation grill-me session._

### Test cases
_To be defined during the pre-implementation grill-me session._

## GitHub Issues

Create milestone `Phase 9: Motion, Loading & Interaction Patterns` and the following issues assigned to it:

- `[Phase 9] Implement global navigation transition`
- `[Phase 9] Build skeleton loader component`
- `[Phase 9] Migrate all loading screens to skeleton loaders`
- `[Phase 9] Fix AnimatedVisibility and animateContentSize gaps`
- `[Phase 9] Define and implement standardised UX interaction patterns`

## Open Questions

- What exactly should the delete confirmation flow look like — bottom sheet, inline undo snackbar, or dialog?
- Should save failures show a snackbar with a retry action, or inline near the save button?
- How should optimistic updates (show success immediately, revert on failure) be handled?
