# Phase Execution Flow

This file defines the required startup flow for **every** v2 phase.

The phase markdown file remains the source of truth for that phase's scope, subphases, acceptance criteria, test cases, and GitHub issue content.

No implementation work starts until this flow is completed for the active phase.

This flow also defines the minimum level of detail required in the phase file before implementation starts. The grill-me session must not stop at high-level agreement. It must drive the phase file to concrete implementation detail for each subphase, capture important design decisions made during the conversation, and keep the phase markdown aligned with those decisions as they are made.

---

## Required Order For Each Phase

### 1. Grill-me the phase file to completion

Use the `grill-me` skill with the active phase file and finish the pre-implementation discussion before writing code.

This step must:

- finalise the subphases
- add implementation detail for how each subphase is expected to be implemented
- finalise the acceptance criteria as Markdown checkboxes (`- [ ]`)
- finalise the test cases as Markdown checkboxes (`- [ ]`)
- answer all open questions in the phase file
- update the phase file with the agreed decisions before proceeding

Required detail level:

- each subphase should say not only what will be done, but how it should be implemented where that detail is already known
- important boundary decisions, ownership decisions, lifecycle decisions, migration strategy decisions, and deletion decisions must be written into the phase file during grill-me rather than left implicit in chat history
- if the conversation creates or changes a standing rule for future phase prep, update the relevant source-of-truth markdown file in `.ai/v2-plan/` during the same session
- if a phase changes the current architecture or current implementation reality of the project, the implementation work for that phase must also update `Architecture.md` or another current-state project explainer so it reflects the new reality
- historical implementation/phase/plan files must keep their historical references; do not remove old decisions or references from phase files just to make the current-state docs cleaner
- do not add catch-all sections such as `Resolved during grill-me` that dump unrelated decisions together; decisions must be incorporated into the relevant existing sections, or into a new well-named section when they do not fit anywhere else

The phase file should be moved from `Not started` to `Grill-me in progress` during this process when appropriate.

### 2. Create the milestone before implementation begins

Once the phase file is ready, create the GitHub milestone for that phase using the naming rule from [GitHubWorkflow.md](GitHubWorkflow.md).

Required outcome:

- the milestone exists before any issue implementation starts

Important GitHub project note:

- GitHub Projects do **not** accept milestones directly as project items through `gh project item-add`
- to make the phase visible on the **LifeTogether Board** before issues exist, create a **draft item** for the phase milestone in the project
- once issues are created, the real execution tracking happens through those issues on the board

### 3. Grill-me the final issue breakdown before creating issues

After the phase file is complete and the milestone exists, use `grill-me` again if needed to agree on the **final number and granularity of issues** for that specific phase.

Do not create issues until this is agreed.

Required outcome:

- the issue list for the phase is confirmed
- the phase file's `## GitHub Issues` section matches the agreed breakdown
- issue granularity fits the actual nature of the phase rather than forcing a fixed pattern
- the issue bodies can be created directly from the phase file without losing implementation detail agreed during grill-me

### 4. Create the issues from the phase file and connect them properly

After the issue list is agreed:

- create the GitHub issues
- assign them to the phase milestone
- add them to the **LifeTogether Board** project

Required issue rule:

- each issue body must contain the information already written in the phase file
- do not create thinner GitHub issues that lose the agreed implementation details
- paste the relevant subphase checklist and any already-agreed acceptance or testing context from the phase file

### 5. Start implementation on the first issue

Only after the phase file, milestone, and issues are ready:

- **Move the issue to `In Progress` on the LifeTogether Board before touching any code.** An issue in `Backlog` or `Ready` while code is being written means this step was skipped.
- connect the issue to its implementation branch (prefer GitHub-linked issue branch flow before coding; otherwise add explicit branch + PR references in the issue body)
- create a branch from `architecture-improvement` using the branch format from [GitHubWorkflow.md](GitHubWorkflow.md)
  - if the issue depends on a previous issue branch not yet merged into `architecture-improvement`, use that branch as the base and note the dependency in the issue body
- **create the branch and check it out BEFORE writing any implementation code** — not after

Required outcome:

- implementation never starts on an issue that is still in `Backlog`/`Ready` on the board — move it to `In Progress` first
- the active issue has an explicit branch connection for traceability
- coding starts from an issue branch, not directly from `architecture-improvement`
- **no implementation code is written before the issue branch exists** — this rule has no exceptions
- the issue already contains the implementation context agreed during the grill-me phase

### 5b. Keep the phase file current during implementation

The phase file is not just a planning artefact — it must stay accurate throughout implementation.

- if a design decision is made during implementation that differs from what the phase file says, **update the phase file in the same commit or the next one**
- if the agreed approach for a subphase changes, rewrite that subphase section rather than leaving the old text in place
- if an out-of-scope decision is recognised during implementation (something that should be deferred), add it to the `## Out of Scope` section of the phase file so it is tracked
- the phase file's `Status` field must reflect the current real state (e.g. `In Progress` once coding starts)

Required outcome:

- the phase file always matches what was actually built, not just what was planned
- a future reader can rely on the phase file as an accurate record of decisions made

### 6. Complete the issue after the last commit

When implementation is done and all commits are made, the following steps are **mandatory** — do not wait to be asked:

1. **Tick completed checkboxes** in the issue body: mark every completed item in the Subphase Checklist and Acceptance Criteria sections as `- [x]`. Never tick Test / Verification items — those are for the user.
2. **Move the issue to `In Review`** on the LifeTogether Board.
3. **Push the branch** to the remote.
4. **Open a PR** targeting `architecture-improvement` with `Relates to #N` (not `Closes #N` unless the PR completes the whole issue).

See [GitHubWorkflow.md](GitHubWorkflow.md) for exact terminal commands for each step.

Required outcome:

- the issue is in `In Review` on the board — not still `In Progress` — by the time the PR is open
- the issue body reflects what was actually implemented, with accurate checkboxes
- the PR is open and awaiting user review before any further action
- nothing is merged or closed without explicit user approval

---

## Short Version

For each phase, the flow is:

1. grill-me the phase file to completion
2. create the milestone and represent the phase on the project board
3. grill-me the final issue breakdown
4. create the issues from the phase file and connect them to the milestone and project
5. move the active issue to `In Progress`, create the issue branch, and start coding
6. build and verify before every commit — never commit uncompiled code
7. when implementation is done: tick all completed non-test checkboxes in the issue body, move the issue to `In Review` on the board, push the branch, and open a PR targeting `architecture-improvement`
8. wait for explicit user approval before merging the PR or closing the issue

Steps 7 and 8 are mandatory after every issue — do not wait to be asked. See [GitHubWorkflow.md](GitHubWorkflow.md) for exact commands.

---

## What This Prevents

This flow exists to prevent:

- coding before the phase design is settled
- milestones being skipped
- issue lists being decided too early or too loosely
- GitHub issues losing the detailed information already agreed in the phase file
- implementation starting without project tracking in place — issue left in Backlog while code is written
- grill-me sessions ending with only high-level intent instead of implementation-ready phase detail
- phase files going stale during implementation while the actual code diverges from them
- implementation finishing without a PR being opened, checkboxes being ticked, or the board being updated
