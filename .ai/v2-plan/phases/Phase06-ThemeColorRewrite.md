# Phase 6 — Theme & Color System Rewrite

**Status:** Not started _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Rewrite `Color.kt` to use private raw colors mapped to semantic Material 3 roles. Enforce the 8dp baseline grid, a strict typography scale, shape tokens, and a small app-specific token layer for foundational spacing and sizing. Eliminate all magic numbers and direct color references from feature screens.

## Scope

- `ui/theme/Color.kt`
- `ui/theme/Theme.kt`
- `ui/theme/Type.kt`
- `ui/theme/Shape.kt`
- New: app-specific token layer (spacing, sizing, shape defaults)
- Any direct raw color or magic number references in `ui/common/` components (swept and fixed here before components are refactored in later phases)

## Key Decisions Already Made

- **Private Raw Colors:** Raw hex colors must be `private` in `Color.kt`; UI accesses colors only through `MaterialTheme.colorScheme`.
- **Light Mode Readiness:** Light mode is deferred to a future phase, but the architecture (semantic mapping via `colorScheme`) must be set up so that slotting in a `LightColorScheme` later is trivial.
- **Neutral Typography:** Text and headings use neutral `onSurface` / `onSurfaceVariant` — never vibrant brand colors.
- **Tinted Surfaces:** Pure black (`#000000`) is banned for backgrounds. Foundational background must be a deep charcoal with a subtle purple tint (e.g. `#120E15`). Elevated elements use progressively lighter tinted shades.
- **10% Vibrancy Rule:** The saturated brand primary (`#7E1E80`) is used for at most 10% of screen real estate — primary CTAs, active toggles, primary FABs only.
- **Muted Containers:** Large active areas (selected cards, bottom nav) use desaturated dark lavender variants of brand purple.
- **Desaturated Semantic Colors:** Pure red and pure green are banned for error/success states; use pastel/dusty variants that meet WCAG contrast.
- **8dp Baseline Grid:** All spacing, padding, and dimensions must use multiples of 8dp. Half-steps (4dp) are allowed only for tight internal component spacing. Arbitrary values like `10.dp` or `15.dp` are banned.
- **Typography:** Only `MaterialTheme.typography` styles inside feature composables. No arbitrary `fontSize`, `fontWeight`, or `fontFamily`.
- **Shape Tokens:** No ad hoc `RoundedCornerShape(Xdp)` in feature screens. Use `MaterialTheme.shapes`. Primary action buttons use `shapes.extraLarge` or `CircleShape`; cards/containers use `shapes.medium` or `shapes.large`.
- **App-specific Token Layer:** Strictly foundational. Limited to generic `Spacing` (multiples of 8dp + 4dp half-step) and basic `Sizing` (e.g., 24dp for icons, 48dp for minimum touch targets). Component-specific paddings (like card internal padding) are deferred to Phase 7.

## Subphases

- [ ] 6.1 Rewrite `Color.kt` — private raw colors + `DarkColorScheme` semantic mapping (structured to easily support Light Mode later)
- [ ] 6.2 Define and apply shape tokens in `Shape.kt`
- [ ] 6.3 Define and apply typography scale in `Type.kt`
- [ ] 6.4 Define app-specific foundational token layer (Spacing & Sizing only)
- [ ] 6.5 Sweep all composables (feature screens and `ui/common/`) for magic numbers and direct color references; replace with theme tokens

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.

### Acceptance criteria
- `Color.kt` exposes zero public raw `Color` values.
- `Theme.kt` fully maps semantic tokens for `DarkColorScheme`.
- A foundational token layer exists and is accessible for standardized `Spacing` and `Sizing`.
- Zero raw color references (`Color(0xFF...)` or `Color.Black`) exist in `ui/feature/` or `ui/common/`.
- Zero arbitrary `.dp` or `.sp` values exist in feature screens (exceptions allowed for standard 1dp borders/dividers or explicit fractional aspect ratios).
- The app compiles successfully and exhibits no glaring visual contrast or layout regressions in Dark Mode.

### Test cases
- **Static Analysis (Colors):** Search the codebase for `Color(0x` or `Color.` inside `ui/feature` and `ui/common`. The search must return zero results.
- **Static Analysis (Spacing):** Search the codebase for ad-hoc sizing like `10.dp`, `15.dp`, etc. All padding and spacing should reference the new token layer (e.g., `Spacing.Medium`).
- **Visual QA:** Boot the app in Dark Mode. Verify that backgrounds use the tinted charcoal, vibrant primary colors are limited to CTAs, and semantic error/success states use desaturated colors.

## GitHub Issues

Create milestone `Phase 6: Theme & Color System Rewrite` and the following issues assigned to it:

- `[Phase 6] Rewrite theme system — colors, shapes, typography, and foundational token layer`
- `[Phase 6] Sweep all composables for magic numbers and raw color references`