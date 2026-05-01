# Phase 9 — Motion, Loading & Interaction Patterns

**Status:** Implementing _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Standardise how UI elements appear, how loading states are communicated, how navigation transitions feel, and how common user actions (delete, save, confirm) behave consistently across all features.

## Scope

- All feature screens (loading states and UI element animations)
- Navigation setup (global transition)
- Common interaction flows: delete, save, confirm, load
- All loading-like patterns in the app, including explicit `Loading` UI states and in-content spinner/empty-loading states

## Motion Rules

- **No harsh popping:** all UI elements that appear or disappear (dropdowns, inline errors, expanding cards, etc.) must use `AnimatedVisibility` or `animateContentSize`. No instant visibility toggles.
- **One global navigation transition:** a subtle slide + crossfade using Material 3 easing curves. Applied universally - no per-screen ad hoc animations.
- **No Shared Element Transitions:** explicitly avoided to keep maintenance simple.
- The global transition applies only to full-screen route changes. Modal sheets, dialogs, and other overlays keep their native motion.

### Motion Decisions

- Navigation transitions apply as a single subtle slide + crossfade to full-screen route changes only.
- Transition definitions stay centralized in the app `NavHost` and should use shared duration/easing constants rather than repeated inline literals.
- Normalize route transition timing to one shared preset rather than letting destinations keep their own timing.
- Keep the shared route-transition preset at 350ms.
- Keep the crossfade component of the route transition very subtle.
- The global route transition applies to nested graph destinations as well as top-level app routes.
- Apply the route transition uniformly even to graph entry or landing destinations.
- Use the same slide direction logic everywhere rather than introducing reduced-motion variants for nested routes.
- Obvious `AnimatedVisibility` and `animateContentSize` gaps should be retrofitted across the app, not only on screens touched by the loading work.
- Keep native motion for sheets, dialogs, and overlays.
- Destination `Scaffold` chrome, including the `TopAppBar`, always follows the content in the route slide/fade transition.

### Navigation Transition Detail

- Keep transition definitions centralized in the app `NavHost`.
- Use the existing left/right route-direction logic as the base and layer in a small fade so the motion reads as slide + crossfade.
- Replace repeated inline durations with shared constants so the motion timing is consistent across enter, exit, pop-enter, and pop-exit at 350ms.
- Do not add per-screen or per-destination custom transitions unless a later phase explicitly creates an exception.
- Invert the slide direction on pop transitions to preserve back-stack navigation sense.

## Loading Rules

- **Skeleton loaders for data fetching:** blocking full-screen loading spinners are banned except for critical or unrecoverable paths.
- **Inline progress indicators for saving:** a spinner or progress indicator near the action, not a full-screen block.
- Any in-content loading-like pattern should be considered part of this phase, not deferred to later cleanup.
- Skeleton shimmer must only exist while the skeleton is actually visible; when loading ends, the skeleton leaves composition entirely so it stops animating and recomposing.
- When a screen is migrated to skeleton loading, the loading presentation should be done in one pass for that screen rather than split across multiple phase iterations.
- Skeleton coverage should include the main content and visible interactive chrome that is part of the loading experience, but not the scaffold shell or top app bar.
- Content-heavy routes such as list details, entry details, recipe details, guide details, and similar content edit/detail screens should migrate from spinner-based loading to skeleton-based loading.
- The app-launch auth gate remains a dedicated full-screen loading screen and is not replaced with skeleton content.

### Loading Decisions

- Use one shared reusable skeleton family for data-fetching screens rather than one-off screen-specific shimmer logic.
- Loading skeletons should use generic placeholder blocks instead of screen-specific templates.
- Phase 9 loading scope includes all loading-like patterns, not just explicit `Loading` UI states.
- Skeleton shimmer exists only while the loading placeholder is visible and is removed from composition once content is ready.
- Screen migrations should cover the full loading presentation in one pass once the screen is chosen for skeleton treatment.
- Skeleton coverage includes visible interactive chrome in addition to the main content, but excludes the scaffold shell and top app bar.
- Content-heavy detail and edit routes should be migrated to skeleton-based loading; the app-launch auth gate remains a dedicated full-screen loading screen.
- Define one shared loading shell for exceptional full-screen loading cases instead of building isolated one-off shells.
- The shared loading shell should stay minimal rather than adding branded illustration or title treatment.
- Skeleton recipes should use fixed token-based sizes rather than proportional sizing where possible; the reusable recipes should still match the average screen family they stand in for.
- Skeleton shimmer timing should be tuned independently from navigation motion timing.
- Every skeleton recipe should shimmer rather than mixing static and animated placeholder styles.
- Skeleton recipes may compress vertically on compact screens while preserving the same overall structure.
- Image- and media-heavy loading areas should get dedicated skeleton treatment rather than relying only on current image placeholders.
- Media skeleton blocks should also be standardized rather than left as ad hoc generic boxes.
- Primary actions on skeleton-loading screens should remain visible but disabled until content is ready.
- Disabled actions on loading screens should keep their labels rather than being replaced by skeleton placeholders.
- Every explicit spinner-based loading screen in the app should be migrated during Phase 9, not only the content-heavy examples identified earlier.
- Compact spinner patterns inside modals and dialogs should stay as spinners rather than being converted to skeletons.
- Loading states should remain distinct from empty states rather than using one combined empty/loading skeleton variant.
- Skeleton loaders are for initial load only and should not be reused for partial content refreshes on already-visible screens.
- Keep the skeleton shimmer subtle and soft.
- Error states should remain separate from skeletons.
- Skeleton shapes should reuse the same shape tokens as the actual cards and buttons they stand in for.

### Sync Lifecycle Decisions

- `SyncUpdatingText` will be removed in Phase 9.
- Remove the sync status state from `SyncCoordinator` entirely.
- Remove the first-success tracking state from the sync coordinator entirely.
- Remove the `activeSyncKeys` bookkeeping from `SyncCoordinator` entirely.
- Do not replace sync status text with a spinner, skeleton, or other persistent busy indicator.
- `FeatureSyncLifecycleBinding` stays in place as the sync startup and shutdown hook.
- Slots previously occupied by `SyncUpdatingText` should collapse away entirely rather than being replaced by fixed spacers.
- Update all `SyncUpdatingText` call sites in the same pass without a compatibility shim.

### Refresh Decisions

- Manual pull-to-refresh should keep the native pull-to-refresh spinner.
- Pull-to-refresh should remain separate from sync lifecycle status language.
- Background refreshes should stay silent unless they fail.
- Background refresh failures should surface as snackbar errors.
- Refresh failures should use the existing general error snackbar pattern.
- Refresh failure snackbars should be informational only and should not include a retry action.
- Sync status should be suppressed while a screen is still showing its skeleton loading state.

### Skeleton Layout Decisions

- The shared skeleton family should include a few reusable layout recipes on top of low-level blocks rather than only raw primitives.
- The shared skeleton family should expose a fixed set of recipe variants rather than being open-ended composition.
- The fixed skeleton recipe variants should live under one `Skeletons` API surface.
- Screen-specific skeletons are still allowed only when a screen has a genuinely unusual shape that cannot be expressed cleanly with the shared recipes.
- The fixed recipe set now includes a simple feed/list layout, a section/detail layout, a form/edit layout, a gallery/grid layout, and a tile-based grid/collection layout.
- The feed/list recipe should fit screens that mostly render repeated cards in a vertical list.
- The section/detail recipe should fit screens that have a header or hero area plus stacked content sections.
- The form/edit recipe should cover edit screens with a header, fields, and action row.
- The gallery/grid recipe should cover album/media surfaces with a 2-column image grid and lightweight chrome.
- The grid/collection recipe should cover home/dashboard tile surfaces.
- The reusable recipes should mirror the real screen density closely rather than using a loose generic density.
- The shimmer animation should be consistent across all skeleton recipes.
- Text-line placeholders should vary in length rather than using one uniform width.
- Each skeleton recipe should use a fixed placeholder line count.
- The fixed skeleton recipes should be reusable for both full-load states and in-place section placeholders.
- In-place section placeholders should blend into the surrounding content with the same background.

## Interaction Rules

- **Standardised interaction patterns:** delete, save, and confirm actions must feel and behave identically across all features.
- Delete confirmations stay dialog-based for destructive or irreversible actions.
- Save actions default to conservative updates: the visible state changes after the save succeeds.
- Optimistic updates are not allowed in this phase.
- No undo snackbars will be added in this phase.
- Save failures show a snackbar error only. No retry action in this phase.

### Interaction Decisions

- Delete confirmations stay dialog-based for destructive or irreversible actions.
- Save actions default to conservative updates: the visible state changes after the save succeeds.
- Optimistic updates are not allowed in this phase.
- No undo snackbars will be added in this phase.
- Save failures show a snackbar error only, with no retry action in this phase.
- Download flows keep snackbar-style progress messages and should not be converted into inline save spinners.

## Subphases

_To be finalised during the pre-implementation grill-me session._

- [ ] 9.1 Remove `SyncUpdatingText` and simplify the sync lifecycle
- [ ] 9.2 Define and apply the global navigation transition
- [ ] 9.3 Design and implement the skeleton loader component; migrate all loading screens
- [ ] 9.4 Audit and fix all harsh `AnimatedVisibility` / `animateContentSize` gaps
- [ ] 9.5 Define specific UX rules for delete, save, and confirm interactions
- [ ] 9.6 Implement standardised interaction patterns across all features

### 9.1 Remove `SyncUpdatingText` and simplify the sync lifecycle

- Remove `SyncUpdatingText` from `ui/common/sync` and delete the call sites on every feature screen in the same pass.
- Remove `syncStates`, `activeSyncKeys`, and `hasSyncedOnce` from `SyncCoordinator`.
- Keep `FeatureSyncLifecycleBinding` so screens still acquire and release syncs on lifecycle start/stop.
- Collapse the layout slots that only existed for the sync text instead of replacing them with placeholder spacing.

### 9.2 Define and apply the global navigation transition

- Centralize the route transition in `NavHost` with shared timing/easing constants.
- Apply the same subtle slide + very small crossfade to all app routes and nested graph destinations.
- Preserve back-stack sense by inverting the slide direction on pop transitions.
- Keep the transition scoped to route content, not top app bars or overlay surfaces.

### 9.3 Design and implement the skeleton loader component; migrate all loading screens

- Build a shared `Skeletons` API surface with fixed variants for feed/list, section/detail, form/edit, gallery/grid, and grid/collection layouts.
- Reuse the same skeleton system for initial load states and in-place section placeholders.
- Migrate every explicit spinner-based loading screen in the app to the new skeleton system where the screen is a data-loading surface.
- Keep empty states, error states, dialog spinners, pull-to-refresh spinners, save spinners, and download progress feedback in their current separate patterns.

### 9.4 Audit and fix all harsh `AnimatedVisibility` / `animateContentSize` gaps

- Scan the app for obvious instant visibility toggles and abrupt size changes in UI that appears or disappears.
- Replace those gaps with `AnimatedVisibility` or `animateContentSize` where the change is part of the user-facing UI motion.
- Leave native modal, dialog, and sheet motion alone unless a gap is inside the content itself.

### 9.5 Define specific UX rules for delete, save, and confirm interactions

- Keep delete confirmations dialog-based.
- Keep saves conservative with visible state changing only after success.
- Keep failures as snackbar errors only, without undo or retry behavior.

### 9.6 Implement standardised interaction patterns across all features

- Apply the delete/save/confirm rules across all screen implementations that currently vary.
- Keep download feedback as snackbar progress and save feedback as inline spinners.
- Ensure loading states, empty states, and refresh states remain distinct from each other across the app.

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
- [ ] `SyncUpdatingText` and its coordinator-side support state are removed from the app.
- [ ] The global route transition is centralized, applied to top-level and nested routes, and uses the same subtle slide + crossfade everywhere.
- [ ] The shared `Skeletons` API exists with fixed variants for feed/list, section/detail, form/edit, gallery/grid, and grid/collection, and it is used for both full-load and in-place placeholders.
- [ ] All explicit spinner-based loading screens are migrated away from blocking full-screen spinners where the screen is a data-loading surface.
- [ ] Error states, empty states, dialog spinners, pull-to-refresh spinners, save spinners, and download progress snackbars remain distinct from skeleton loading.
- [ ] Harsh `AnimatedVisibility` and `animateContentSize` gaps are reduced across the app.
- [ ] Delete, save, and confirm behaviors follow the agreed dialog/conservative/snackbar rules consistently.

### Test cases
- [ ] `:app:compileDebugKotlin` passes after the changes.
- [ ] No `SyncUpdatingText` references remain in the codebase.
- [ ] A representative top-level route and nested route both show the shared navigation transition in push and pop directions.
- [ ] A representative feed/list screen, section/detail screen, form/edit screen, gallery/grid screen, and grid/collection screen all show the new skeleton treatment during initial load.
- [ ] A representative in-place placeholder blends into its surrounding content rather than looking like a separate loading panel.
- [ ] Pull-to-refresh still shows the native pull-to-refresh spinner.
- [ ] Save actions still show inline progress near the action and download flows still show progress snackbars.
- [ ] Dialog-based confirmations and snackbar-only failure handling remain intact.

## GitHub Issues

Create milestone `V2 Phase 9: Motion, Loading & Interaction Patterns` and the following issues assigned to it:

- `[Phase 9] [9.1] Remove SyncUpdatingText and simplify the sync lifecycle`
- `[Phase 9] [9.2] Implement global navigation transition`
- `[Phase 9] [9.3] Build skeleton loader component`
- `[Phase 9] [9.3] Migrate all loading screens to skeleton loaders`
- `[Phase 9] [9.4] Fix AnimatedVisibility and animateContentSize gaps`
- `[Phase 9] [9.5-9.6] Define and implement standardised UX interaction patterns`
