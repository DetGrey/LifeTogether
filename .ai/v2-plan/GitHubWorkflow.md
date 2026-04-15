# GitHub Project & Issue Tracking Workflow

This document describes how to track v2 implementation work in [DetGrey/LifeTogether](https://github.com/DetGrey/LifeTogether) using GitHub Milestones, Issues, and the **LifeTogether Board** project.

The `.ai/v2-plan/` markdown files are the source of truth for *what* to build and *why*. GitHub tracks *execution progress*.

Before using the workflow below for any active phase, follow the required startup order in [PhaseExecutionFlow.md](PhaseExecutionFlow.md).

---

## The Tracking Hierarchy

1. **Source of truth (`.ai/v2-plan/` files):** All architectural decisions, phase scope, and subphase checklists live here. If anything changes, update these files first.

2. **Milestones (one per phase):** Each phase file maps directly to a GitHub Milestone.
   - Naming: `V2 Phase N: <Phase Name>` — e.g. `V2 Phase 1: Session Boundary Cleanup`
   - Full phase list: see [ImplementationPlan.md](ImplementationPlan.md)

3. **Issues (fit the granularity to the nature of the phase):** Each milestone contains multiple issues. The right number and granularity varies by phase — some phases call for one issue per feature domain, others for one per screen, others for one per component. A single pattern forced across all phases creates either too many micro-issues or too few meaningful ones.
   - The specific issue list for each phase is defined in that phase's `## GitHub Issues` section, confirmed and updated after the pre-implementation grill-me session.
   - Naming convention: `[Phase N] [N.X–N.Y] <Title>` — e.g. `[Phase 1] [1.1] Create SessionRepository and root coordinator` or `[Phase 1] [1.2–1.5] Create root coordinator and observer lifecycle migration`. Include the subphase ID(s) so the intended implementation order is visible directly from the issue title.
   - Issue body: paste the relevant subphase checklist from the phase file as Markdown checkboxes (`- [ ]`)

4. **GitHub Project Board:** Execution tracking must be connected to the **LifeTogether Board** project (`DetGrey/projects/2`). This provides the kanban view across all active work.
   - Project number: **2**
   - Add each issue to the board after creating it (see terminal commands below)
   - Milestones themselves cannot be added with `gh project item-add`; if you want the phase visible on the board before issues exist, create a draft item representing the phase milestone
   - Requires the `project` OAuth scope — run `gh auth refresh -s project` once to enable it, then verify with `gh auth status`

---

## Branch Strategy

All v2 work lives on the `architecture-improvement` branch:

- Issue branches are created **from `architecture-improvement`**, not from `master`
- PRs target **`architecture-improvement`**, not `master`
- When all issues in a milestone are merged into `architecture-improvement`, that milestone is complete
- `master` is only touched once the entire v2 arc (all 11 phases) is complete — at that point `architecture-improvement` is merged into `master`

---

## Commit Message Convention

```
[Phase N] Short description of what this commit does
```

Examples:
- `[Phase 1] Add SessionRepository interface and Hilt singleton`
- `[Phase 3] Split AdminGrocerySuggestionsScreen into route and screen`
- `[Phase 6] Rewrite Color.kt with private raw colors and semantic mapping`

---

## Definition of Complete (for a phase)

A phase is complete when **all** of the following are true — none can happen without explicit user approval:

- [ ] All subphase checkboxes in the phase file are ticked
- [ ] All acceptance criteria are met
- [ ] `Architecture.md` or another current-state explainer is updated when the phase changed the project's actual architecture or implementation reality
- [ ] All PRs for this phase are merged into `architecture-improvement`
- [ ] All issues in the milestone are closed
- [ ] The milestone is closed
- [ ] The phase file `Status` is updated to `Complete`

---

## Daily Execution Workflow

1. **Pick up an issue:** Go to the LifeTogether Board, pick an issue from the current milestone, and move it to **In Progress**.
2. **Branch + issue connection — MUST happen before any code is written.**
   - Preferred: create the branch from the issue so GitHub tracks it in Development (`gh issue develop <issue-number> --base architecture-improvement --name <branch-name>`)
   - If the issue depends on a previous branch that is not yet merged into `architecture-improvement`, use that branch as the base instead and note the dependency explicitly in the issue body.
   - If the branch already exists or linking cannot be created, add explicit branch and PR references in the issue body and keep them updated.
   - **Do not write a single line of implementation code until the issue branch exists and is checked out.**
3. **Micro-commits:** Write code and commit using the convention `[Phase N] Short description`.
4. **Pull Request:** Open a PR targeting `architecture-improvement`.
   - Do not write `Closes #N` unless the PR completes the *entire* issue.
   - Instead write: `Relates to #N` or `Completes SessionRepository setup for #N`.
   - **Never merge a PR without explicit user approval first.**
5. **Tick checkboxes:** When an issue is completed, edit the issue body so completed checklist items are checked (`- [x]`) before closing.
6. **Close:** Once all relevant checkboxes are ticked, close the issue.
   - **Never close an issue or milestone without explicit user approval first.**
   - A phase is only complete when all issues are closed, all PRs are merged, and the milestone is closed — see Definition of Complete above.

---

## Phase Startup Workflow

For every phase, use this order before writing code:

1. Finish the pre-implementation `grill-me` session using the phase file.
2. Create the milestone for that phase.
3. Represent the phase on the project board:
   - either by creating the phase issues immediately and adding them to the board
   - or, if issues are not created yet, by creating a draft project item for the phase milestone
4. Grill-me the final issue breakdown until the issue count and granularity are agreed.
5. Create the issues from the final phase-file content and connect them to the milestone and project.
6. Only then create the first issue branch from `architecture-improvement` and start coding.

---

## Terminal Commands

### 1. Create a Milestone

The `gh-milestone` extension is already installed:

```bash
gh milestone create --title "V2 Phase 1: Session Boundary Cleanup"
```

Create milestones one at a time as you are ready to start each phase — no need to create all upfront.

### 2. Create an Issue and Add It to the Board

```bash
# Create the issue
gh issue create --title "[Phase 1] Create SessionRepository and root coordinator" --milestone "V2 Phase 1: Session Boundary Cleanup"

# Add it to the LifeTogether Board (project number 2)
gh project item-add 2 --owner DetGrey --url "https://github.com/DetGrey/LifeTogether/issues/<issue-number>"
```

The CLI will prompt for a body when creating the issue — paste the relevant subphase checklist from the phase file.

### 2b. Create a Draft Project Item for the Phase Milestone

Use this when the milestone must be visible on the project board before the real issues exist:

```bash
gh project item-create 2 --owner DetGrey --title "V2 Phase 1: Session Boundary Cleanup" --body "Milestone placeholder for Phase 1. Replace draft visibility with real milestone issues as they are created."
```

### 3. Branch and Commit

```bash
# See what is open
gh issue list

# Preferred: create and link the issue branch from GitHub issue development
gh issue develop 15 --base architecture-improvement --name V2/15-session-setup
git checkout V2/15-session-setup

# Fallback: if needed, create branch manually and then record the branch in issue body
git checkout -b V2/15-session-setup

# Commit using the convention
git commit -m "[Phase 1] Add SessionRepository interface and Hilt singleton"
```

### 4. Update Issue Checkboxes

```bash
# Opens the issue in your terminal editor (mark completed checklist items as - [x])
gh issue edit 15
```

### 5. Open a Pull Request

```bash
git push origin 15-session-setup

# Target architecture-improvement, not master
gh pr create --base architecture-improvement --body "Implements SessionRepository and Hilt wiring. Relates to #15"
```

> **Do not merge the PR until the user has explicitly reviewed and approved it.**

### 6. Close an Issue

```bash
gh issue close 15
```

> **Do not close an issue or milestone until the user has explicitly asked or approved it.**
