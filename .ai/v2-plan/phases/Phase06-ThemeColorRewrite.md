# Phase 6 — Theme & Color System Rewrite

**Status:** Implementing _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Rewrite `Color.kt` to use private raw colors mapped to semantic Material 3 roles. Enforce the 8dp baseline grid, a strict typography scale, shape tokens, and a small app-specific token layer for spacing and sizing. Eliminate all magic numbers and direct color references from feature screens.

## Scope

- `ui/theme/Color.kt`
- `ui/theme/Theme.kt`
- `ui/theme/Type.kt`
- `ui/theme/Shape.kt`
- New: app-specific token layer (spacing, sizing, shape defaults)
- Any direct raw color or magic number references in `ui/common/` components (swept and fixed here before components are refactored in later phases)
- Icon assets used by `ui/common/` and feature screens, with new icons added as vector assets instead of PNGs

## Key Decisions Already Made

- Raw hex colors must be `private` in `Color.kt`; UI accesses colors only through `MaterialTheme.colorScheme`.
- Palette naming uses explicit theme scope (`DarkPalette`/`LightPalette`) instead of ambiguous `Raw...` names.
- Text and headings use neutral `onSurface` / `onSurfaceVariant` — never vibrant brand colors.
- **Tinted surfaces:** pure black (`#000000`) is banned for backgrounds. Foundational background must be a deep charcoal with a subtle purple tint (e.g. `#120E15`). Elevated elements use progressively lighter tinted shades.
- **10% vibrancy rule:** the saturated brand primary (`#7E1E80`) is used for at most 10% of screen real estate — primary CTAs, active toggles, primary FABs only.
- **Muted containers:** large active areas (selected cards, bottom nav) use desaturated dark lavender variants of brand purple.
- **Desaturated semantic colors:** pure red and pure green are banned for error/success states; use pastel/dusty variants that meet WCAG contrast.
- **8dp baseline grid:** all spacing, padding, and dimensions must use multiples of 8dp. Half-steps (4dp) are allowed only for tight internal component spacing. Arbitrary values like `10.dp` or `15.dp` are banned.
- **Typography:** only `MaterialTheme.typography` styles inside feature composables. No arbitrary `fontSize`, `fontWeight`, or `fontFamily`. The currently chosen explicit typography size scale is preserved in `Type.kt`.
- **Shape tokens:** no ad hoc `RoundedCornerShape(Xdp)` in feature screens. Use `MaterialTheme.shapes`. Primary action buttons use `shapes.extraLarge` or `CircleShape`; cards/containers use `shapes.medium` or `shapes.large`.
- **App-specific token layer:** foundational `Spacing` and `Sizing` tokens are exposed through theme CompositionLocals.
- **System bars:** edge-to-edge handling is owned by `MainActivity`; direct status bar color writes were removed from `LifeTogetherTheme`.
- **Icon assets:** icon replacements have already been remade. For Issue 67, treat the current files in `app/src/main/res/drawable*` as the source of truth and update call sites to use those assets.
- **Vector policy + PNG exceptions:** new icons should still be vector-first. Icons that render incorrectly when vectorized are approved PNG exceptions and should remain PNG during Issue 67 (do not force-convert them).
- **Icon renames already agreed:** `ic_recipes` is now `ic_recipe`, and `ic_groceries` is now `is_grocery`. 
  - The ic_logo has been replaced with the old ic_logo_small so those are now the same and is called ic_logo which should now be used everywhere where the old two were.
  - The profile_picture.png is now called ic_avatar and is only used as a fallback for profile picture.
- **Former black-only icon variants to replace with white assets and tint where used:** `ic_checkmark`, `ic_expand`, `ic_expanded`, `ic_heart`, `ic_overflow_menu`.

## Subphases

- [x] 6.1 Rewrite `Color.kt` — private raw colors + semantic `DarkColorScheme` mapping (light structure retained for later divergence)
- [x] 6.2 Define and apply shape tokens in `Shape.kt`
- [x] 6.3 Define and apply typography scale in `Type.kt`
- [x] 6.4 Define app-specific token layer (spacing, sizing)
- [ ] 6.5 Sweep all composables (feature screens and `ui/common/`) for magic numbers, direct color references, and icon asset cleanup; fix

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
- [x] `Color.kt` exposes zero public raw `Color` values.
- [x] `Theme.kt` maps semantic tokens for `DarkColorScheme` and keeps `LightColorScheme` wiring ready for future divergence.
- [x] Custom fonts (Montserrat Alternates, Lato) are mapped to M3 Typography roles and preserved size scale.
- [x] A foundational token layer exists for `Spacing` and `Sizing` and is theme-accessible.
- [ ] Zero raw color references (`Color(0xFF...)` or `Color.Black`) exist in `ui/feature/` or `ui/common/`.
- [ ] Zero arbitrary `.dp` or `.sp` values exist in feature screens (exceptions: 1dp borders/dividers or explicit fractional ratios).
- [ ] App compiles and visual QA passes for Dark Mode after the full sweep.

### Test cases
- [ ] **Static Analysis (Colors):** Search `ui/feature` + `ui/common` for `Color(0x` or `Color.` and verify forbidden direct color usage is eliminated.
- [ ] **Static Analysis (Spacing):** Search for ad hoc sizes like `10.dp`, `15.dp`; replace with tokenized spacing/sizing where applicable.
- [ ] **Visual QA:** Verify dark backgrounds, limited primary vibrancy, and typography mapping in key screens.

## GitHub Issues

Create milestone `Phase 6: Theme & Color System Rewrite` and the following issues assigned to it:

- `[Phase 6] Rewrite theme system — colors, shapes, typography, and token layer`
- `[Phase 6] Sweep all composables for magic numbers, raw color references, and icon asset cleanup`

## TODO

There is an extra task of changing the UI composables to use the correct colours to look nice. E.g. FeatureOverview are cards that use onBackground which is not correct.