# GitHub Project & Issue Tracking Workflow

This document describes how to track v2 implementation work in [DetGrey/LifeTogether](https://github.com/DetGrey/LifeTogether) using GitHub Milestones, Issues, and the **LifeTogether Board** project.

The `.ai/v2-plan/` markdown files are the source of truth for *what* to build and *why*. GitHub tracks *execution progress*.

---

## The Tracking Hierarchy

1. **Source of truth (`.ai/v2-plan/` files):** All architectural decisions, phase scope, and subphase checklists live here. If anything changes, update these files first.

2. **Milestones (one per phase):** Each phase file maps directly to a GitHub Milestone.
   - Naming: `Phase N: <Phase Name>` — e.g. `Phase 1: Session Boundary Cleanup`
   - Full phase list: see [ImplementationPlan.md](ImplementationPlan.md)

3. **Issues (fit the granularity to the nature of the phase):** Each milestone contains multiple issues. The right number and granularity varies by phase — some phases call for one issue per feature domain, others for one per screen, others for one per component. A single pattern forced across all phases creates either too many micro-issues or too few meaningful ones.
   - The specific issue list for each phase is defined in that phase's `## GitHub Issues` section, confirmed and updated after the pre-implementation grill-me session.
   - Naming convention: `[Phase N] <Title>` — e.g. `[Phase 1] Create SessionRepository and root coordinator`
   - Issue body: paste the relevant subphase checklist from the phase file as Markdown checkboxes (`- [ ]`)

4. **GitHub Project Board:** All milestones and issues must be connected to the **LifeTogether Board** project (`DetGrey/projects/2`). This provides the kanban view across all active work.
   - Project number: **2**
   - Add each issue to the board after creating it (see terminal commands below)
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
- [ ] All PRs for this phase are merged into `architecture-improvement`
- [ ] All issues in the milestone are closed
- [ ] The milestone is closed
- [ ] The phase file `Status` is updated to `Complete`

---

## Daily Execution Workflow

1. **Pick up an issue:** Go to the LifeTogether Board, pick an issue from the current milestone, and move it to **In Progress**.
2. **Branch:** Create a branch from `architecture-improvement` using the format `<issue-id>-<short-description>` — e.g. `git checkout -b 15-session-setup`
3. **Micro-commits:** Write code and commit using the convention `[Phase N] Short description`.
4. **Pull Request:** Open a PR targeting `architecture-improvement`.
   - Do not write `Closes #N` unless the PR completes the *entire* issue.
   - Instead write: `Relates to #N` or `Completes SessionRepository setup for #N`.
   - **Never merge a PR without explicit user approval first.**
5. **Tick checkboxes:** As PRs are merged, manually tick the checkboxes in the issue body.
6. **Close:** Once all checkboxes are ticked, close the issue.
   - **Never close an issue or milestone without explicit user approval first.**
   - A phase is only complete when all issues are closed, all PRs are merged, and the milestone is closed — see Definition of Complete above.

---

## Terminal Commands

### 1. Create a Milestone

The `gh-milestone` extension is already installed:

```bash
gh milestone create --title "Phase 1: Session Boundary Cleanup"
```

Create milestones one at a time as you are ready to start each phase — no need to create all upfront.

### 2. Create an Issue and Add It to the Board

```bash
# Create the issue
gh issue create --title "[Phase 1] Create SessionRepository and root coordinator" --milestone "Phase 1: Session Boundary Cleanup"

# Add it to the LifeTogether Board (project number 2)
gh project item-add 2 --owner DetGrey --url "https://github.com/DetGrey/LifeTogether/issues/<issue-number>"
```

The CLI will prompt for a body when creating the issue — paste the relevant subphase checklist from the phase file.

### 3. Branch and Commit

```bash
# See what is open
gh issue list

# Create your branch from architecture-improvement — use the issue ID (e.g. issue #15)
git checkout -b 15-session-setup

# Commit using the convention
git commit -m "[Phase 1] Add SessionRepository interface and Hilt singleton"
```

### 4. Update Issue Checkboxes

```bash
# Opens the issue in your terminal editor
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
