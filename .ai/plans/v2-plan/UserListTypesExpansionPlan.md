# User List Types Expansion Plan

## Status

Agreed through grill-me interview. Planning only, no implementation included.

## Goal

Expand the current user lists system beyond routine entries by introducing four new list types with type-specific data models and UX:

1. Wish List
2. Notes
3. Checklist
4. Meal Planner

Existing Routine behavior remains intact.

## Release Strategy

1. Ship all four new list types together in one coordinated feature release.
2. Keep current ROUTINE lists and data untouched.
3. Additive rollout only in this scope (no conversion tooling from routine data).

## Canonical List Types

User-facing labels:

1. Routine (existing)
2. Wish List
3. Notes
4. Checklist
5. Meal Planner

Suggested internal enum keys:

1. ROUTINE
2. WISH_LIST
3. NOTES
4. CHECKLIST
5. MEAL_PLANNER

## Core Architecture Decisions

1. Keep UserList as parent entity for all list types.
2. Use separate child entry models per list type (no unified polymorphic child schema). All child entry models must implement the `ListEntry` interface (include `itemName`, `listId`, `dateCreated`).
3. Use separate Room tables and separate Firestore collections per child entry type.
4. Child entries inherit access from parent UserList only (no per-entry visibility fields).
5. Shared List Details route stays in place, but renders type-specific content/components.
6. Shared Add action remains; it launches type-specific create flows based on parent list type.
7. Parent list delete is hard-delete with cascade delete of child entries in both Room and Firestore.

## Data Model Decisions

### WishListEntry

Required fields:

1. id
2. familyId
3. listId
4. itemName
5. lastUpdated
6. dateCreated
7. isPurchased (Boolean)
8. url (String, single stored field as typed by user)
9. estimatedPriceMinor (Long?)
10. currencyCode (String?)
11. priority (enum)
12. notes (String?)

Priority labels:

1. Urgent
2. Planned
3. Someday

Sort behavior in details screen:

1. Unpurchased first, then purchased.
2. Within each group: priority first.
3. Tie-breaker: lastUpdated descending.

### NoteEntry

Required fields:

1. id
2. familyId
3. listId
4. itemName (String)
5. markdownBody (String)
5. lastUpdated
6. dateCreated

Rules:

1. Notes uses child entries (NoteEntry) linked by listId.
2. Notes lists may contain multiple NoteEntry items (each is a separate list entry).

### ChecklistEntry

Required fields:

1. id
2. familyId
3. listId
4. itemName
5. isChecked (Boolean)
6. lastUpdated
7. dateCreated

Sort behavior in details screen:

1. Unchecked first, then checked.
2. Within each group: lastUpdated descending.

### MealPlanEntry

Required fields:

1. id
2. familyId
3. listId
4. date (ISO string: YYYY-MM-DD)
5. recipeId (String?)
6. customMealName (String?)
7. lastUpdated
8. dateCreated

Rules:

1. One entry per day.
2. recipeId XOR customMealName (exactly one must be set).
3. Time is not stored for date; date semantics are calendar-day only.

### RoutineEntry

1. Existing routine data model and behavior remain unchanged in this scope.

## UI and UX Decisions

### Lists Screen

1. Create List dialog includes all four new list types plus routine.
2. Navigation from list card remains unchanged: one shared List Details destination with type-based rendering.

### Wish List Details

1. Shows active (unpurchased) entries first.
2. Completed (purchased) entries are shown in a separate collapsible group, following the grocery list interaction pattern.
3. Price and URL are visible on entry cards.
4. URL is clickable/openable from the entry.
5. Item notes are displayed as a short snippet on the card and are editable only in the Entry Details screen (keeps card compact).

### Notes Details

1. Edit and Preview toggle mode.
2. Stored text remains markdown source (no hidden mutation of source text during typing).
3. Preview must render selectable text so users can copy partial content.
4. Markdown bullets based on input patterns are a rendering concern, not a storage rewrite.

### Checklist Details

1. Active (unchecked) items first.
2. Checked items in a collapsible completed group (same interaction pattern as grocery).

### Meal Planner Details

1. Primary view is a 7-day week view (Mon-Sun).
2. Each day row/card shows weekday plus day-of-month.
3. Horizontal swipe pagination moves week-by-week left/right.
4. Includes a quick action to jump back to current week.
5. Includes bottom Add button consistent with other screens.

## Validation and Integrity Rules

1. Parent list type controls allowed child entry type.
2. Notes lists allow multiple NoteEntry items.
3. MealPlanEntry enforces exactly one of recipeId or customMealName.
4. WishListEntry URL is stored as provided in a single url field.
5. Sorting behavior per type is deterministic as defined above.

## Implementation Work Breakdown

1. Extend list type enum and create-flow options.
2. Add new domain models for WishListEntry, NoteEntry, ChecklistEntry, and MealPlanEntry.
3. Add Room entities/DAO/local data source paths for each new entry type.
4. Add Firestore parser/mapper/listener/save/update/delete paths for each new entry type.
5. Extend UserList repository contracts for observing, saving, updating, and deleting per new type.
6. Refactor shared List Details and Entry Details screens to dispatch by list type.
7. Implement type-specific form validation and save/update paths.
8. Implement list-type-specific sorting/grouping behaviors.
9. Implement meal planner week pagination and jump-to-current-week interaction.
10. Add cascade delete handling for each new child entry table/collection.

## Acceptance Criteria

1. All four new list types can be created from the Create List dialog.
2. Each list type persists and syncs via Room and Firestore using its own entry schema.
3. Existing routine lists continue to work without regression.
4. Wish List supports url, price minor+currency, purchased state, and priority sorting/grouping.
5. Notes supports multiple NoteEntry items; each entry includes itemName and markdown body. Notes supports markdown edit/preview and selectable preview text.
6. Checklist supports checked state and collapsible completed group.
7. Meal Planner supports one entry per day, week pagination, current-week jump, and XOR recipeId/customMealName.
8. Parent deletion cascades to all child entries for that list across local and remote storage.

## Non-Goals

1. Migrating or converting existing routine data.
2. Introducing manual drag-and-drop ordering in this scope.
3. Building grocery-sync coupling from Meal Planner in this scope.
4. Adding per-entry visibility/permissions.

## Notes

This document reflects explicit agreements reached in the grill-me interview sequence.
