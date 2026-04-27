# Phase 6 — Theme & Color System Rewrite

**Status:** Complete _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Rewrite `Color.kt` to use private raw colors mapped to semantic Material 3 roles. Enforce the 8dp baseline grid, a strict typography scale, shape tokens, and a small app-specific token layer for foundational spacing and sizing. Eliminate all magic numbers and direct color references from feature screens.

## Scope

- `ui/theme/Color.kt`
- `ui/theme/Theme.kt`
- `ui/theme/Type.kt`
- `ui/theme/Shape.kt`
- New: app-specific token layer (spacing, sizing, shape defaults)
- Any direct raw color or magic number references in `ui/common/` components (swept and fixed here before components are refactored in later phases)
- Icon assets used by `ui/common/` and feature screens, with new icons added as vector assets instead of PNGs

## Key Decisions Already Made

- **Private Raw Colors:** Raw hex colors must be `private` in `Color.kt`; UI accesses colors only through `MaterialTheme.colorScheme`.
- **Light Mode Readiness:** Light mode is deferred to a future phase, but the architecture (semantic mapping via `colorScheme`) must be set up so that slotting in a `LightColorScheme` later is trivial.
- **Color Mapping (Dark Mode Base):**
    - *Backgrounds:* `background` & `surface` = `#120E15` (Deep tinted charcoal); `surfaceVariant` = `#1E1822`.
    - *Neutral Text:* `onSurface` & `onBackground` = `#E6E1E5`; `onSurfaceVariant` = `#CAC4D0`.
    - *Primary (10% Vibrancy Rule):* `primary` = `#7E1E80` (Saturated brand purple); `onPrimary` = `#FFFFFF`.
    - *Primary Containers (Muted Active Areas):* `primaryContainer` = `#3A233D`; `onPrimaryContainer` = `#E2DCE6` (Crisp silver-lilac).
    - *Secondary (Accent):* `secondary` = `#4DB6AC` (Soft dusty teal); `secondaryContainer` = `#004F4F`; `onSecondaryContainer` = `#A6F4EA`.
    - *Semantic States:* `error` = `#F2B8B5` (Pastel red); `onError` = `#601410`.
- **Typography & Custom Fonts:**
    - Expressive/Header text (`Display`, `Headline`, `Title`) uses **Montserrat Alternates**.
    - Functional text (`Body`, `Label`) uses **Lato**.
    - Only `MaterialTheme.typography` styles inside feature composables. No arbitrary `fontSize`, `fontWeight`, or `fontFamily`.
- **8dp Baseline Grid:** All spacing, padding, and dimensions must use multiples of 8dp. Half-steps (4dp) are allowed only for tight internal component spacing. Arbitrary values like `10.dp` or `15.dp` are banned.
- **Shape Tokens:** No ad hoc `RoundedCornerShape(Xdp)` in feature screens. Use `MaterialTheme.shapes`. Primary action buttons use `shapes.extraLarge` or `CircleShape`; cards/containers use `shapes.medium` or `shapes.large`.
- **App-specific Token Layer:** Strictly foundational. Limited to generic `Spacing` (multiples of 8dp + 4dp half-step) and basic `Sizing` (e.g., 24dp for icons, 48dp for minimum touch targets). Component-specific paddings (like card internal padding) are deferred to Phase 7.
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
- **Allowed raw color values:** `Color.Transparent` and `Color.Unspecified` are permitted anywhere — they are not semantic colors. All other raw `Color.*` and `Color(0xFF...)` references are banned from `ui/feature/` and `ui/common/`.
- **Dp value scope:** Hardcoded `.dp` values are allowed for object sizes, heights, widths, padding, and border thickness. Only `Spacer` `height`/`width` modifiers must use `LifeTogetherTokens.spacing` tokens instead of raw dp values.
- **Font size exceptions:** Hardcoded `fontSize` in `TipsCalendar.kt` (9.sp day label, 18.sp total) are approved exceptions — the calendar requires non-standard sizes that do not map to the typography scale.

## Subphases

- [x] 6.1 Rewrite `Color.kt` — private raw colors + `DarkColorScheme` semantic mapping (structured to easily support Light Mode later)
- [x] 6.2 Define and apply shape tokens in `Shape.kt`
- [x] 6.3 Define and apply typography scale in `Type.kt`
- [x] 6.4 Define app-specific foundational token layer (Spacing & Sizing only)
- [x] 6.5 Sweep all composables (feature screens and `ui/common/`) for magic numbers and direct color references; replace with theme tokens

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.

### Acceptance criteria
- [x] `Color.kt` exposes zero public raw `Color` values.
- [x] `Theme.kt` maps semantic tokens for `DarkColorScheme` and keeps `LightColorScheme` wiring ready for future divergence.
- [x] Custom fonts (Montserrat Alternates, Lato) are mapped to M3 Typography roles and preserved size scale.
- [x] A foundational token layer exists for `Spacing` and `Sizing` and is theme-accessible.
- [x] Zero raw color references (`Color(0xFF...)` or `Color.Black/White/Gray`) exist in `ui/feature/` or `ui/common/` (exceptions: `Color.Transparent` and `Color.Unspecified`).
- [x] No hardcoded `RoundedCornerShape(...)` in feature screens; all replaced with `MaterialTheme.shapes.*` or `CircleShape`.
- [x] All `Spacer` `height`/`width` modifiers use `LifeTogetherTokens.spacing` tokens instead of raw dp.
- [ ] App compiles and visual QA passes for Dark Mode after the full sweep.

### Test cases
- [ ] **Static Analysis (Colors):** Search `ui/feature` + `ui/common` for `Color(0x` or `Color.` and verify only `Color.Transparent` / `Color.Unspecified` remain.
- [ ] **Static Analysis (Spacers):** Search for `Spacer.*height\|Spacer.*width` with raw dp values and verify all use `LifeTogetherTokens.spacing`.
- [ ] **Visual QA:** Verify dark backgrounds, limited primary vibrancy, and typography mapping in key screens.

## GitHub Issues

Create milestone `Phase 6: Theme & Color System Rewrite` and the following issues assigned to it:

- `[Phase 6] Rewrite theme system — colors, shapes, typography, and token layer`
- `[Phase 6] Sweep all composables for magic numbers, raw color references, and icon asset cleanup`
