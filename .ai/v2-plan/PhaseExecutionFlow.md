# Phase Execution Flow

This file defines the required startup flow for **every** v2 phase.

The phase markdown file remains the source of truth for that phase's scope, subphases, acceptance criteria, test cases, and GitHub issue content.

No implementation work starts until this flow is completed for the active phase.

---

## Required Order For Each Phase

### 1. Grill-me the phase file to completion

Use the `grill-me` skill with the active phase file and finish the pre-implementation discussion before writing code.

This step must:

- finalise the subphases
- finalise the acceptance criteria
- finalise the test cases
- answer all open questions in the phase file
- update the phase file with the agreed decisions before proceeding

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

- pick the first issue
- create a branch from `architecture-improvement` using the branch format from [GitHubWorkflow.md](GitHubWorkflow.md)
- begin coding from the issue content that was already agreed and written down

Required outcome:

- coding starts from an issue branch, not directly from `architecture-improvement`
- the issue already contains the implementation context agreed during the grill-me phase

---

## Short Version

For each phase, the flow is:

1. grill-me the phase file to completion
2. create the milestone and represent the phase on the project board
3. grill-me the final issue breakdown
4. create the issues from the phase file and connect them to the milestone and project
5. create the first issue branch and start coding

---

## What This Prevents

This flow exists to prevent:

- coding before the phase design is settled
- milestones being skipped
- issue lists being decided too early or too loosely
- GitHub issues losing the detailed information already agreed in the phase file
- implementation starting without project tracking in place
