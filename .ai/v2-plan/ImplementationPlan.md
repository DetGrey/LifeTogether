# LifeTogether v2 Implementation Plan

This is the sequenced implementation roadmap for the v2 modernisation arc.
All decisions and principles are captured in [ProjectImprovementPlan.md](ProjectImprovementPlan.md).

Each phase has its own detail file linked in the table below.

**Rule: complete each phase fully before starting the next. Before starting any phase, follow the mandatory [PhaseExecutionFlow.md](PhaseExecutionFlow.md), read the phase file, and [run `/grill-me`](../skills/grill-me/grill-me.md) to finalise subphases, acceptance criteria, test cases, milestone setup, and issue breakdown — no code before that flow is complete.**

---

## Phase Overview

> Estimates are rough and assume solo development with no other priorities. Treat them as order-of-magnitude guides, not commitments.
> Size: **S** = days · **M** = ~1 week · **L** = 2–3 weeks · **XL** = 4+ weeks

| #                                               | Phase                                  | Key outcome                                                                              | Size | Estimate   |
|-------------------------------------------------|----------------------------------------|------------------------------------------------------------------------------------------|------|------------|
| [1](phases/Phase01-SessionBoundary.md)          | Session Boundary Cleanup               | `SessionRepository` established; `AppSessionViewModel` no longer threaded through the UI | L    | ~2 weeks   |
| [2](phases/Phase02-DataLayer.md)                | Data Layer Deepening                   | Deep repositories, `Result` wrapper, Offline-First/SSOT, data/domain logging cleanup     | XL   | ~4 weeks   |
| [3](phases/Phase03-RouteScreenSplit.md)         | Route/Screen Split                     | All screens previewable; shared `SnackbarHost`; snackbar-first error handling            | XL   | ~4–6 weeks |
| [4](phases/Phase04-NavigationTypeSafety.md)     | Navigation Type-Safety                 | All routes migrated to Kotlin Serialization typed routes                                 | M    | ~1 week    |
| [5](phases/Phase05-ViewModelNormalization.md)   | ViewModel Normalization                | All ViewModels on `UiState` + `UiCommand` pattern; ViewModel/screen logging cleanup     | L    | ~2–3 weeks |
| [6](phases/Phase06-ThemeColorRewrite.md)        | Theme & Color System Rewrite           | Semantic `Color.kt`; 8dp grid; typography and shape tokens enforced                      | M    | ~1–2 weeks |
| [7](phases/Phase07-ComponentConsolidation.md)   | Shared Component Consolidation         | One canonical text field, dropdown, and date picker; all duplicates removed              | S    | ~3–5 days  |
| [8](phases/Phase08-M3Alignment.md)              | Native Material 3 Alignment            | Custom component shells replaced with native M3 where justified                          | M    | ~1–2 weeks |
| [9](phases/Phase09-MotionLoadingInteraction.md) | Motion, Loading & Interaction Patterns | Skeleton loaders; `AnimatedVisibility`; global nav transition; UX interaction standards  | L    | ~2–3 weeks |
| [10](phases/Phase10-BackgroundSync.md)          | Background Sync Layer                  | WorkManager-based reliable sync; UI never orchestrates remote pushes                     | L    | ~2–3 weeks |
| [11](phases/Phase11-TestSuites.md)              | Test Suite Implementation              | Repository boundary tests; ViewModel state tests; composable preview tests               | XL   | ~3–5 weeks |

**Rough total: 6–10 months of focused solo work.**

---

## Dependency Chain

Phases must be completed in order. Key hard dependencies:

- **Phase 2** depends on Phase 1 — `SessionRepository` must exist before feature ViewModels can inject it during the data layer refactor.
- **Phase 3** depends on Phase 2 — clean repositories make route/screen split and ViewModel wiring straightforward.
- **Phase 4** depends on Phase 3 — type-safe routes are built on top of the route/screen split structure.
- **Phase 5** depends on Phase 4 — ViewModel normalization is cleaner once navigation inputs are typed and stable.
- **Phases 6–9** follow Phase 5 — UI visual and interaction work begins after the architectural layers are clean.
- **Phase 10** depends on Phase 2 — background sync sits on top of the Offline-First repositories.
- **Phase 11** depends on all prior phases — tests are written against the fully restructured codebase.
