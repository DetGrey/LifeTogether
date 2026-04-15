# Phase 6 — Theme & Color System Rewrite

**Status:** Not started _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Rewrite `Color.kt` to use private raw colors mapped to semantic Material 3 roles. Enforce the 8dp baseline grid, a strict typography scale, shape tokens, and a small app-specific token layer for spacing and sizing. Eliminate all magic numbers and direct color references from feature screens.

## Scope

- `ui/theme/Color.kt`
- `ui/theme/Theme.kt`
- `ui/theme/Type.kt`
- `ui/theme/Shape.kt`
- New: app-specific token layer (spacing, sizing, shape defaults)
- Any direct raw color or magic number references in `ui/common/` components (swept and fixed here before components are refactored in later phases)

## Key Decisions Already Made

- Raw hex colors must be `private` in `Color.kt`; UI accesses colors only through `MaterialTheme.colorScheme`.
- Text and headings use neutral `onSurface` / `onSurfaceVariant` — never vibrant brand colors.
- **Tinted surfaces:** pure black (`#000000`) is banned for backgrounds. Foundational background must be a deep charcoal with a subtle purple tint (e.g. `#120E15`). Elevated elements use progressively lighter tinted shades.
- **10% vibrancy rule:** the saturated brand primary (`#7E1E80`) is used for at most 10% of screen real estate — primary CTAs, active toggles, primary FABs only.
- **Muted containers:** large active areas (selected cards, bottom nav) use desaturated dark lavender variants of brand purple.
- **Desaturated semantic colors:** pure red and pure green are banned for error/success states; use pastel/dusty variants that meet WCAG contrast.
- **8dp baseline grid:** all spacing, padding, and dimensions must use multiples of 8dp. Half-steps (4dp) are allowed only for tight internal component spacing. Arbitrary values like `10.dp` or `15.dp` are banned.
- **Typography:** only `MaterialTheme.typography` styles inside feature composables. No arbitrary `fontSize`, `fontWeight`, or `fontFamily`.
- **Shape tokens:** no ad hoc `RoundedCornerShape(Xdp)` in feature screens. Use `MaterialTheme.shapes`. Primary action buttons use `shapes.extraLarge` or `CircleShape`; cards/containers use `shapes.medium` or `shapes.large`.
- **App-specific token layer:** a small, intentional set of tokens for shared sizing, spacing, and shape defaults that do not justify a custom wrapper on their own.

## Subphases

_To be finalised during the pre-implementation grill-me session._

- [ ] 6.1 Rewrite `Color.kt` — private raw colors + `DarkColorScheme` semantic mapping
- [ ] 6.2 Define and apply shape tokens in `Shape.kt`
- [ ] 6.3 Define and apply typography scale in `Type.kt`
- [ ] 6.4 Define app-specific token layer (spacing, sizing)
- [ ] 6.5 Sweep all composables (feature screens and `ui/common/`) for magic numbers and direct color references; fix

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
_To be defined during the pre-implementation grill-me session._

### Test cases
_To be defined during the pre-implementation grill-me session._

## GitHub Issues

Create milestone `Phase 6: Theme & Color System Rewrite` and the following issues assigned to it:

- `[Phase 6] Rewrite theme system — colors, shapes, typography, and token layer`
- `[Phase 6] Sweep all composables for magic numbers and raw color references`

## Open Questions

- Should a light mode colour scheme also be defined in this phase, or deferred?
- How deep should the app-specific token layer go in this first pass — just spacing and corner radius, or also icon sizes and content padding defaults?
