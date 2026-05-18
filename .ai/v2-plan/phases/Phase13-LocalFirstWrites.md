# Phase 13 — Local-First Writes & DAO Correctness

**Status:** Not started _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Close two systemic data layer bugs that cause stale UI and silent failures:

1. **Non-suspend DAO deletes** — all `@Query DELETE` methods in all DAOs lack `suspend`, so when called from a coroutine on the main thread Room throws `IllegalStateException`, which is swallowed by `appResultOf`/`appResultOfSuspend` catch blocks and logged only. Deletions silently fail; the UI shows data that no longer exists on the server.

2. **Write-then-wait pattern** — all user-triggered writes (create/update/delete) go to Firestore first and rely on the snapshot listener to propagate the change back through Room before the UI updates. On any network latency, the user navigates back and sees stale state for 1–2+ seconds.

Also fixes a missing error surface in `MealPlannerViewModel` / `MealPlannerRoute`, which currently silently drops all `Result.Failure` values with no snackbar shown.

---

## Architectural change

**Before (every write):**
```
ViewModel → Repository → FirestoreDS.await() → [network] → snapshot listener → LocalDS → Room → Flow → ViewModel
```

**After (every write):**
```
ViewModel → Repository → LocalDS (write Room immediately) → Flow → ViewModel  ← instant UI update
                       ↓
                    FirestoreDS.await()
                    on failure: LocalDS (explicit rollback) + Result.Failure → ViewModel → snackbar
```

The Firestore snapshot listener stays active. Its role changes from *source of UI truth* to *server-correction mechanism* — it reconciles any divergence caused by another device or a conflict.

---

## Subphase 1 — DAO Suspend Fix

### What to change

Add `suspend` to every `@Query DELETE` DAO method across all 12 DAOs:

| DAO                   | Methods                                               |
|-----------------------|-------------------------------------------------------|
| `MealPlanDao`         | `deleteItems`, `deleteFamilyItems`                    |
| `GroceryListDao`      | `deleteTable`, `deleteItems`                          |
| `RecipesDao`          | `deleteTable`, `deleteItems`                          |
| `GuidesDao`           | `deleteTable`, `deleteItems`                          |
| `TipTrackerDao`       | `deleteTable`, `deleteItems`                          |
| `GalleryMediaDao`     | `deleteTable`, `deleteItems`                          |
| `AlbumsDao`           | `deleteTable`, `deleteItems`                          |
| `UserListsDao`        | `deleteItems`, `deleteFamilyItems`                    |
| `RoutineListsDao`     | `deleteItems`, `deleteByListIds`, `deleteFamilyItems` |
| `ChecklistEntriesDao` | `deleteItems`, `deleteByListIds`, `deleteFamilyItems` |
| `WishListsDao`        | `deleteItems`, `deleteByListIds`, `deleteFamilyItems` |
| `NoteEntriesDao`      | `deleteItems`, `deleteByListIds`, `deleteFamilyItems` |

### Cascade to LocalDataSources

Any `LocalDataSource` method that calls a now-suspend DAO method must also become `suspend`. Key ones:

- `MealPlanLocalDataSource.deleteFamilyMealPlans`
- `UserListLocalDataSource.deleteFamilyUserLists`, `deleteChildEntriesForList`, `deleteFamilyRoutineListEntries`, `deleteFamilyWishListEntries`, `deleteFamilyNoteEntries`, `deleteFamilyChecklistEntries`
- `UserListLocalDataSource.deleteRoutineListEntries`, `deleteWishListEntries`, `deleteNoteEntries`, `deleteChecklistEntries` — these already wrap in `appResultOf`; change wrapper to `appResultOfSuspend` and add `suspend` modifier
- `GroceryLocalDataSource.deleteItems` — same pattern

### Fix appResultOf/appResultOfSuspend mismatch in sync repos

Several sync `Flow.map` pipelines use `appResultOf` (non-suspend) while calling suspend `LocalDataSource` methods inside. This compiles due to Kotlin inline semantics but is misleading. Change to `appResultOfSuspend` everywhere suspend DataSource methods are called from a sync pipeline.

Affected: `TipTrackerRepositoryImpl.syncTipsFromRemote` and any other repo where `syncXxx` uses `appResultOf` with suspend calls inside.

### Subphase 1 Checklist
- [ ] All 12 DAOs: every `@Query DELETE` method has `suspend`
- [ ] Cascading `LocalDataSource` delete methods have `suspend`
- [ ] All sync pipeline `appResultOf` wrappers calling suspend methods changed to `appResultOfSuspend`
- [ ] Build passes

---

## Subphase 2 — Optimistic Writes + Error Handling

### 2a — MealPlannerViewModel error surface

`MealPlannerViewModel` is the only ViewModel without a `uiCommands` error channel. Its `successData()` extension currently silently drops every `Result.Failure`.

Changes:
- Add `private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)` and `val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()`
- Add `private fun showError(message: String)` (same pattern as all other ViewModels)
- Change `successData()` so that `Result.Failure` emits a snackbar via `_uiCommands` instead of being silently dropped
- Add `CollectUiCommands(viewModel.uiCommands)` to `MealPlannerRoute`

### 2b — Optimistic write pattern

Every repository with user-triggered writes follows this contract:

**Create — client-generated UUID replaces Firestore auto-ID:**
```kotlin
suspend fun saveFoo(foo: Foo): Result<String, AppError> {
    val id = UUID.randomUUID().toString()
    val fooWithId = foo.copy(id = id)
    fooLocalDataSource.insertFoo(fooWithId)                       // 1. write to Room
    return when (val result = fooFirestoreDS.saveFoo(fooWithId)) { // 2. document(id).set(...)
        is Result.Success -> Result.Success(id)
        is Result.Failure -> {
            fooLocalDataSource.deleteFoo(id)                       // 3. rollback
            Result.Failure(result.error)
        }
    }
}
```

**Update — snapshot old state for rollback:**
```kotlin
suspend fun updateFoo(foo: Foo): Result<Unit, AppError> {
    val old = fooLocalDataSource.getFooOnce(foo.id)  // 1. snapshot
    fooLocalDataSource.upsertFoo(foo)                 // 2. write to Room
    return when (val result = fooFirestoreDS.updateFoo(foo)) {
        is Result.Success -> Result.Success(Unit)
        is Result.Failure -> {
            old?.let { fooLocalDataSource.upsertFoo(it) }  // 3. rollback
            Result.Failure(result.error)
        }
    }
}
```

**Delete — snapshot old state for rollback:**
```kotlin
suspend fun deleteFoo(fooId: String): Result<Unit, AppError> {
    val old = fooLocalDataSource.getFooOnce(fooId)  // 1. snapshot
    fooLocalDataSource.deleteFoo(fooId)              // 2. delete from Room
    return when (val result = fooFirestoreDS.deleteFoo(fooId)) {
        is Result.Success -> Result.Success(Unit)
        is Result.Failure -> {
            old?.let { fooLocalDataSource.upsertFoo(it) }  // 3. rollback
            Result.Failure(result.error)
        }
    }
}
```

### 2c — Required LocalDataSource additions

Each DataSource needs methods the optimistic path calls directly (separate from the existing bulk-diff `updateXxx` methods, which remain for the sync path):

- `insertFoo(entity)` — upsert a single entity (can reuse `@Insert(onConflict = REPLACE)`)
- `deleteFoo(id)` — delete a single entity by ID
- `getFooOnce(id): EntityType?` — non-Flow, one-shot query for snapshotting before update/delete

Add these as `suspend` DAO methods and expose them from the DataSource.

### 2d — Firestore DataSource changes

All `saveFoo()` methods that currently use `collection.add(dto)` must change to `collection.document(id).set(dto)` to honour the client-generated ID passed in. The `id` field on the DTO must be populated before the call.

### 2e — Feature scope

| Feature                                                      | Create | Update | Delete | Notes                                                                              |
|--------------------------------------------------------------|--------|--------|--------|------------------------------------------------------------------------------------|
| MealPlanner                                                  | ✅      | ✅      | ✅      | Primary reported bug                                                               |
| Grocery items                                                | ✅      | ✅      | ✅      | Includes toggle-bought as update                                                   |
| Recipes                                                      | ✅      | ✅      | ✅      | Image upload step is unchanged — runs after Firestore write, before navigation     |
| TipTracker                                                   | ✅      | ✅      | ✅      |                                                                                    |
| Guides                                                       | ✅      | ✅      | ✅      |                                                                                    |
| UserLists — all 4 sub-types (Routine, Wish, Note, Checklist) | ✅      | ✅      | ✅      |                                                                                    |
| Albums                                                       | ✅      | ✅      | ✅      | No image upload involved                                                           |
| Gallery media — creates                                      | ❌      | —      | —      | Image upload is the primary operation; entity cannot exist without the storage URL |
| Gallery media — update/delete                                | —      | ✅      | ✅      | Safe to do optimistically                                                          |

**Recipe image upload note:** the image upload happens after `saveRecipe`/`updateRecipe` succeeds in Firestore (it needs the document ID and the storage path). With optimistic writes, `saveRecipe` still writes to Room first, then Firestore, then triggers the image upload — the sequence is unchanged except the recipe appears in the list immediately.

### Subphase 2 Checklist
- [ ] `MealPlannerViewModel` has `_uiCommands`, `showError()`, `successData()` emits errors
- [ ] `MealPlannerRoute` calls `CollectUiCommands(viewModel.uiCommands)`
- [ ] All in-scope repositories implement optimistic create/update/delete
- [ ] All in-scope Firestore DataSources use `document(id).set()` for creates
- [ ] All in-scope LocalDataSources have `insertFoo`, `deleteFoo(id)`, `getFooOnce(id)` methods
- [ ] Build passes

---

## Acceptance Criteria

- [ ] No `@Query DELETE` DAO method is missing `suspend`
- [ ] Deleting a meal plan and navigating back removes it from the list immediately (no stale ghost entry)
- [ ] Creating a meal plan and navigating back shows it in the list immediately
- [ ] Editing a meal plan and navigating back shows the updated values immediately
- [ ] Same instant-feedback behaviour verified for: grocery item delete, recipe delete, tip delete, list entry delete
- [ ] `MealPlannerScreen` shows a snackbar when a sync error occurs (no longer silently dropped)
- [ ] Gallery media creates are unaffected (still upload-first)
- [ ] Recipe image upload still works (save recipe → upload image → navigate back)

### Verification (manual — not automated in this phase)
- [ ] Simulate Firestore failure (e.g. disable network mid-save) and confirm: the optimistically-written item is rolled back from Room, and a snackbar appears
- [ ] Confirm snapshot listener still corrects local state after a multi-device change

---

## GitHub Issues

### Issue 1 — Fix non-suspend DAO delete operations
**Milestone:** Phase 12
- Add `suspend` to all `@Query DELETE` methods across all 12 DAOs
- Cascade `suspend` to calling `LocalDataSource` delete methods
- Replace `appResultOf` with `appResultOfSuspend` in any sync `Flow.map` pipeline that calls suspend DataSource methods
- Build must pass before merging

### Issue 2 — Optimistic local writes + MealPlannerViewModel error handling
**Milestone:** Phase 12
**Depends on:** Issue 1 (DAO suspend fix must be merged first)
- Add `uiCommands` error channel to `MealPlannerViewModel`; wire `CollectUiCommands` in `MealPlannerRoute`
- Add `insertFoo`, `deleteFoo(id)`, `getFooOnce(id)` to all in-scope DAOs and DataSources
- Change all `collection.add()` to `collection.document(id).set()` in in-scope Firestore DataSources
- Implement optimistic create/update/delete in all in-scope repositories
- Build must pass before merging

---

## Sequencing notes

- **Subphase 1 before Subphase 2** — the sync path and the new optimistic path both call the same DAO delete methods; they must be `suspend` before either path is safe to call them from a coroutine.
- **Independent of Phases 1–11** — this work touches only the data and ViewModel layers and does not depend on the Session Boundary, Route/Screen Split, or any other phase.
- **Compatible with Phase 10 (Background Sync)** — optimistic local writes and WorkManager-based background sync coexist naturally. The snapshot listener (and eventually WorkManager) act as the server-correction layer on top of the optimistic local state.
