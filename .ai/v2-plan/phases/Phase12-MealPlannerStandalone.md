# Phase 12 - Meal Planner Standalone

Meal planner will become its own top-level feature and will no longer be part of user lists in any way.

## Agreed target shape

- One planner per family.
- No meal-planner list overview.
- The overview is the week pager itself, similar to tip tracker.
- Meal plans can be recipe-backed or custom.
- The feature has its own home card.
- The feature has its own graph, routes, storage, sync, and UI files.
- Existing meal-planner code can be copied into new files where needed.
- The old `ListType.MEAL_PLANNER` path will be removed completely.
- No compatibility migration is needed because the old meal planner only exists on a test device.
- Meal planner is a standalone top-level feature, not a `UserList` subfeature.

## Checklist

### Data layer

- [ ] Create standalone meal planner domain models for planner and meal plan entries.
- [ ] Create dedicated Room entities and DAO for meal plan entries.
- [ ] Create a dedicated local data source for meal planner.
- [ ] Create a dedicated Firestore data source for meal planner.
- [ ] Create a dedicated repository for meal planner.
- [ ] Move meal planner sync into its own sync use case and sync key.
- [ ] Remove all meal planner references from `UserListRepository`.
- [ ] Remove all meal planner references from `UserListLocalDataSource`.
- [ ] Remove all meal planner references from `UserListFirestoreDataSource`.

### Domain and model cleanup

- [ ] Remove `ListType.MEAL_PLANNER` from the shared list model.
- [ ] Remove meal-plan specific fields and branches from shared list entry models.
- [ ] Remove meal planner branches from shared list save/load/delete flows.
- [ ] Remove meal planner branches from shared list sync flows.

### UI and navigation

- [ ] Create a standalone `MealPlannerGraph`.
- [ ] Add a `MealPlannerGraphObserverRoute` for sync lifecycle.
- [ ] Add a home screen feature card for meal planner.
- [ ] Create a standalone meal planner overview route with the week pager.
- [ ] Create standalone meal-plan entry details route(s) for create/edit/view.
- [ ] Copy the existing meal-plan UI into standalone meal-planner files where needed.
- [ ] Keep the existing user-facing behavior for recipe-backed and custom meals.
- [ ] Keep the recipe search, prep time, and recipe navigation behavior.
- [ ] Reuse the shared recipe card in read-only planner entry views.

### Meal planner behavior

- [ ] Preserve the current week pager behavior in the standalone planner.
- [ ] Preserve the focus-date jump after creating a new meal plan.
- [ ] Keep list-to-entry and entry-to-recipe navigation behavior equivalent to today.
- [ ] Keep edit mode, discard, and delete behavior equivalent to today.
- [ ] Ensure meal entries load from the standalone planner repository only.

### Cleanup

- [ ] Remove the old meal planner screens from the user-lists flow.
- [ ] Remove meal planner handling from `ListDetailsScreen`.
- [ ] Remove meal planner handling from `ListDetailsViewModel`.
- [ ] Remove meal planner handling from `ListEntryDetailsLoader`.
- [ ] Remove meal planner handling from `ListEntryDetailsSaver`.
- [ ] Remove meal planner handling from `ListEntryDetailsModels`.
- [ ] Remove meal planner handling from `SyncCoordinator` user-list sync.
- [ ] Remove any dead imports, routes, and helper functions left behind after the split.

### Home and discovery

- [ ] Add the meal planner feature card to the home screen.
- [ ] Route the card directly into the standalone meal planner screen.
- [ ] Remove any old home references that point at the user-list meal planner path.

### Final verification

- [ ] Confirm the standalone planner works without any `UserList` coupling.
- [ ] Confirm creating, editing, deleting, and syncing meal plans still behave the same for the user.
- [ ] Confirm the old meal planner code paths are gone.
- [ ] Confirm the project compiles after the split.
