## Lists Hub v1 (Routines First) — Implementation Plan

### Summary
- Build one **Lists hub overview** (no separate routines overview), following current project patterns:
    - Firestore observer -> Room sync -> use case -> ViewModel -> Compose screen.
- Show all list entries in one hub; each item includes a visible type label (`Routine` in v1).
- Standardize visibility across app with shared constants and enums so `family/private` is reusable for guides, lists, and future features.
- Keep local + remote persistence, optional image URL, and completion-driven recurrence updates.

### Core Model & Constant Changes
- Introduce generic visibility constants in `Constants.kt`:
    - `VISIBILITY_FAMILY = "family"`
    - `VISIBILITY_PRIVATE = "private"`
- Replace guide-specific constants usage:
    - migrate all `GUIDE_VISIBILITY_FAMILY` / `GUIDE_VISIBILITY_PRIVATE` references to generic visibility constants.
- Keep `GuideVisibility` enum but rename it to `Visibility`, move it out of guide package and back it with generic constants.
- New generic list entry model (`ListEntry`) with:
    - base fields: `id`, `familyId`, `itemName`, `lastUpdated`, `dateCreated`, `type`, `nextDate`, `visibility`, `ownerUid`, `imageUrl`, `lastCompletedAt`, `completionCount`
    - routine fields: `recurrenceUnit`, `interval`, `weekdays`, `anchorDate`
- `ListType` enum includes `ROUTINE` for v1.

### Implementation Changes (Pattern-Aligned)
- Data layer:
    - add generic lists table/entity/DAO (Room) + explicit migration.
    - add Firestore shared/private listeners for list entries; merge streams like guides flow.
    - add lists observer use case and wire through `ObserverCoordinator`.
    - extend local mapping pipeline (`Entity`, `LocalDataSource`, local repository mapping).
- Domain logic:
    - recurrence calculator for `every N days` and `every N weeks + weekdays`.
    - on completion: recalc `nextDate`, set `lastCompletedAt`, increment `completionCount`.
- UI:
    - Lists hub screen with unified cards/rows and type badge.
    - Details/create screen for entry name, recurrence config, visibility, optional image.
    - keep existing UI patterns/components and architecture style used in this project.
- Navigation/Home:
    - single “Lists” entry point in home.
    - hub + details routes only.
- Visibility standardization sweep:
    - update guide Firestore queries and parser defaults to shared visibility constants.
    - ensure future features can reuse same visibility source without new feature-specific constants.

### Unit Test Plan
- Visibility constants and parsing:
    - guide and list visibility parse/serialize uses shared constants.
    - defaults/fallback behavior for missing/invalid visibility values.
- Recurrence calculator:
    - `N days` cases, `N weeks + weekday(s)` cases, completion-driven `nextDate`.
    - date boundary/timezone edge cases.
- Data mapping/converters:
    - `ListEntry` domain <-> entity roundtrip.
    - Firestore map normalization and unknown-key safety.
- Observer/use-case:
    - shared + private merge behavior and owner filtering.
    - empty snapshot/local cleanup behavior.
- ViewModel:
    - hub state contains mixed entries with type badge value.
    - create/edit validation and save/update flow.
    - completion action mutation and state update.
- Migration test:
    - DB upgrade keeps existing data and creates new list table.
    - guide visibility still works after switching to generic constants.

### Assumptions / Defaults
- Visibility domain is globally standardized to `family/private`.
- Guide feature remains behaviorally unchanged; only constant source becomes generic.
- Hub is the only overview for lists; routines are represented by type label.
- Implementation follows existing project architecture and coding patterns already used in this repo.

## Implementation notes

Here's what was implemented. The architecture uses two models: `UserList` (named container) and `ListEntry` (item inside a list). Entries inherit visibility from their parent list.

### Core model & constants

- Constants.kt — Added VISIBILITY_FAMILY/VISIBILITY_PRIVATE generic constants; removed GUIDE_VISIBILITY_* aliases (guides reference generic constants directly); added USER_LISTS_TABLE = "user_lists", ROUTINE_LIST_ENTRIES_TABLE = "list_entries_routine"
- domain/model/guides/GuideVisibility.kt — Backed by Constants.VISIBILITY_FAMILY/VISIBILITY_PRIVATE directly
- domain/model/lists/ListType.kt — ROUTINE enum with .value and fromValue()
- domain/model/lists/RecurrenceUnit.kt — DAYS/WEEKS enum
- domain/model/lists/UserList.kt — Container model (id, familyId, itemName, lastUpdated, dateCreated, type, visibility, ownerUid, imageUrl)
- domain/model/lists/ListEntry.kt — Entry model (id, familyId, listId FK, itemName, lastUpdated, dateCreated, nextDate, lastCompletedAt, completionCount, recurrenceUnit, interval, weekdays, anchorDate) — no type/visibility/ownerUid, those live on UserList
- domain/logic/RecurrenceCalculator.kt — nextDate() for N-days and N-weeks+weekdays, applyCompletion()

### Data layer

- data/model/UserListEntity.kt — Room entity for user_lists table
- data/model/RoutineListEntryEntity.kt — Room entity for list_entries_routine table (includes list_id FK column); each entry type gets its own table
- data/model/Entity.kt — Added UserList and RoutineListEntry sealed cases
- data/local/Converters.kt — Added List<Int> ↔ JSON converters for weekdays
- data/local/dao/UserListsDao.kt — DAO: getItems(familyId), updateItems, deleteItems, deleteFamilyItems
- data/local/dao/RoutineListsDao.kt — DAO: getItems(familyId), getItemsByListId(familyId, listId), updateItems, deleteItems, deleteByListIds, deleteFamilyItems
- data/local/AppDatabase.kt — Version bumped 23→24; both entities added; MIGRATION_23_24 creates both user_lists and list_entries_routine tables; userListsDao() and routineListsDao() abstract methods
- data/local/LocalDataSource.kt — Injected UserListsDao + RoutineListsDao; USER_LISTS_TABLE and ROUTINE_LIST_ENTRIES_TABLE branches in getListItems(); ROUTINE_LIST_ENTRIES_TABLE uses getItemsByListId when uid param is provided (uid reused as listId filter); updateRoutineListEntries()/deleteFamilyRoutineListEntries() helpers
- data/repository/LocalListRepositoryImpl.kt — Entity.UserList → UserList and Entity.RoutineListEntry → RoutineListEntry mappings in toItem()
- data/remote/FirestoreDataSource.kt — Two-stream merge for user_lists (familySharedUserListsSnapshotListener + privateUserListsSnapshotListener); single family-scoped query for list_entries_routine (familyRoutineListEntriesSnapshotListener, no visibility split since entries inherit from list); saveItem/updateItem handle both models

### Domain/observer

- domain/observer/ObserverTypes.kt — USER_LISTS and ROUTINE_LIST_ENTRIES keys (one key per entry-type table; no single LISTS key)
- domain/usecase/observers/ObserveUserListsUseCase.kt — Combines shared + private user_lists streams, merges, calls update/upsert/delete
- domain/usecase/observers/ObserveRoutineListsUseCase.kt — Single family-scoped list_entries_routine listener, calls updateRoutineListEntries()/deleteFamilyRoutineListEntries()
- domain/observer/ObserverCoordinator.kt — Added observeUserListsUseCase + observeRoutineListsUseCase; both keys in featureObserverKeys; USER_LISTS and ROUTINE_LIST_ENTRIES cases in createObserverHandle

### DI

- di/DatabaseModule.kt — Added addMigrations(MIGRATION_23_24), provideUserListsDao(), provideRoutineListsDao()

### Navigation

- AppRoutes.kt — LISTS_SCREEN, LIST_DETAIL_SCREEN, LIST_ID_ARG, LIST_ENTRY_DETAILS_SCREEN, LIST_ENTRY_ID_ARG
- Navigator.kt — navigateToLists(), navigateToListDetail(listId), navigateToListEntryDetails(listId, entryId?)
- AppNavigator.kt — All three methods implemented
- NavHost.kt — Routes for lists hub, list detail, entry details (create: listId only; edit: listId + entryId)

### UI

- HomeScreen.kt — "Lists" FeatureOverview card added
- ListsRoute.kt — Acquires ObserverKey.USER_LISTS + ObserverKey.ROUTINE_LIST_ENTRIES via FeatureObserverLifecycleBinding
- ListsScreen.kt — Hub showing UserList cards (type badge, family/private label); FAB opens CreateListDialog (name, type, visibility); on create navigates to ListDetailScreen
- ListsViewModel.kt — setUp(familyId, uid) fetches USER_LISTS_TABLE; openCreateDialog(); createList(onCreated) saves UserList and returns new ID
- ListDetailRoute.kt — Acquires USER_LISTS + ROUTINE_LIST_ENTRIES observers; wraps ListDetailScreen
- ListDetailScreen.kt — Shows entries for one list sorted by nextDate; entry card has name, recurrence label, nextDate, completionCount, "Mark done" tap; FAB navigates to create entry
- ListDetailViewModel.kt — setUp(familyId, uid, listId) fetches USER_LISTS_TABLE to resolve listName, fetches ROUTINE_LIST_ENTRIES_TABLE filtered by listId (via uid param); completeEntry() calls RecurrenceCalculator.applyCompletion + updateItemUseCase
- ListEntryDetailsRoute.kt — Accepts listId + entryId, wraps ListEntryDetailsScreen
- ListEntryDetailsScreen.kt — Create/edit form: name, recurrence unit, interval, weekdays (WEEKS only); no type/visibility/imageUrl (those are on UserList)
- ListEntryDetailsViewModel.kt — setContext(familyId, listId); save() builds ListEntry with listId FK; validation, save (new) / update (existing), recurrence pre-calculation on save

### Tests

- RecurrenceCalculatorTest.kt — N-days, N-weeks+weekdays, completion, boundary cases, visibility constant assertions