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

## Before Starting Any Phase

1. Open the phase file in `.ai/v2-plan/phases/`
2. Run `/grill-me` with the file to finalise subphases, acceptance criteria, and test cases
3. Answer and remove all Open Questions
4. Create the GitHub milestone and issues per the phase file's `## GitHub Issues` section
5. Only then write code
