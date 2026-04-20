# LifeTogether Architecture (Current State)

This document describes the current implementation reality of the app.
Historical phase decisions remain in `.ai/v2-plan/` and are not duplicated here.

## Layering

- `data/`: local/remote data sources and repository implementations
- `domain/`: domain models, repository interfaces, and use cases
- `ui/`: routes/screens/viewmodels/navigation/theme/common components
- `di/`: Hilt modules and qualifiers

## Session Boundary (Phase 1)

- Durable app session state is owned by `SessionRepository` (`domain/repository/SessionRepository.kt`).
- Session contract is `SessionState` with:
  - `Loading`
  - `Unauthenticated`
  - `Authenticated(user: UserInformation)`
- `SessionRepositoryImpl` is a singleton and the source of truth for session state and sign-out orchestration.
- `RootCoordinatorViewModel` is activity-scoped and reacts to session transitions for root-level coordination concerns.

## UI Session Consumption

- Legacy app-scoped session viewmodel threading is removed.
- Feature viewmodels consume `SessionRepository` directly where session context is needed.
- Startup/auth gating uses feature-owned loading state handling and no app-scoped session viewmodel threading through routes.

## Removed Legacy State

- Legacy count state and related persistence artifacts were removed as part of this cleanup.

## Local Data Sources (Phase 2)

- `data/local/LocalDataSource.kt` has been removed.
- Local persistence logic is split into focused data sources under `data/local/source/`:
  - `ListLocalDataSource`
  - `UserLocalDataSource`
  - `MediaLocalDataSource`
  - `GuideProgressLocalDataSource`
- Repositories and observer/sync use cases now depend on these focused sources directly.

## Current Focus

- Phase 2 implementation and review closure.
