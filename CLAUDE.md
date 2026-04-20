# LifeTogether

An Android app (Kotlin + Jetpack Compose) for couples and families to manage shared routines, grocery lists, recipes, guides, and a photo gallery.

## Active Work

This project is in the **v2 modernisation arc** — a structured 11-phase refactor to improve architecture, testability, and visual consistency.

The full plan lives in `.ai/v2-plan/`:

- [ImplementationPlan.md](.ai/v2-plan/ImplementationPlan.md) — sequenced 11-phase roadmap with size estimates. **Start here.**
- [ProjectImprovementPlan.md](.ai/v2-plan/ProjectImprovementPlan.md) — all architectural decisions and guiding principles
- [GitHubWorkflow.md](.ai/v2-plan/GitHubWorkflow.md) — branch, issue, and PR conventions
- `.ai/v2-plan/phases/` — one detail file per phase

## Branch Strategy

All v2 work lives on the `architecture-improvement` branch. **Do not target `master` for any v2 PRs.** Phase issue branches are created from `architecture-improvement` and merged back into it. `master` is only touched when the entire v2 arc is complete.

## Mandatory Checklist — Before Writing Any Code For an Issue

**Every item below is required before the first line of implementation code is written. No exceptions.**

- [ ] The issue exists on GitHub and is assigned to the correct milestone
- [ ] The issue is moved to **In Progress** on the LifeTogether Board
- [ ] The issue branch is created and checked out locally (not just mentally planned)
- [ ] The phase file in `.ai/v2-plan/phases/` has been read and the relevant subphase is understood

## Mandatory Checklist — After the Last Commit on an Issue

**Every item below is required after implementation is complete. Do not wait to be asked.**

- [ ] Build passes — never commit uncompiled code
- [ ] Phase file updated if any implementation decisions diverged from what was written there
- [ ] All completed **Subphase Checklist** and **Acceptance Criteria** items are checked in the issue body (`- [x]`). Test / Verification items are left unchecked — those are for the user.
- [ ] Issue moved to **In Review** on the LifeTogether Board
- [ ] Branch pushed to remote
- [ ] PR opened targeting `architecture-improvement` with `Relates to #N`

See [GitHubWorkflow.md](.ai/v2-plan/GitHubWorkflow.md) and [PhaseExecutionFlow.md](.ai/v2-plan/PhaseExecutionFlow.md) for full detail and exact commands.

## Before Starting Any Phase

1. Open the phase file in `.ai/v2-plan/phases/`
2. Run `/grill-me` with the file to finalise subphases, acceptance criteria, and test cases
3. Answer and remove all Open Questions
4. Create the GitHub milestone and issues per the phase file's `## GitHub Issues` section
5. Only then write code
