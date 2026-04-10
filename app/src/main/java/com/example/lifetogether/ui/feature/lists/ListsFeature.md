# Lists Feature

## What it does

The Lists feature lets a family create named lists and fill them with recurring entries. 
The first list type is **Routine** — tasks that repeat on a schedule (e.g. "Water plants every 3 days", "Change bedsheets every Mon + Thu"). 
Each time you mark an entry done, the next due date is automatically recalculated.

---

## Two-model design

Everything is split into a **container** and its **items**.

### `UserList` — the container
Stored in the `user_lists` Firestore collection and Room table.

| Field         | Purpose                                     |
|---------------|---------------------------------------------|
| `id`          | Firestore document ID                       |
| `familyId`    | Family this list belongs to                 |
| `itemName`    | Display name ("Morning Routines")           |
| `type`        | `ListType` enum — `ROUTINE` for v1          |
| `visibility`  | `Visibility.FAMILY` or `VISIBILITY.PRIVATE` |
| `ownerUid`    | UID of the user who created it              |
| `dateCreated` | Creation timestamp                          |
| `lastUpdated` | Last write timestamp                        |
| `imageUrl`    | Optional cover image                        |

### `ListEntry` — an item inside a list
Stored in the `list_entries` Firestore collection and Room table.

| Field             | Purpose                                                       |
|-------------------|---------------------------------------------------------------|
| `id`              | Firestore document ID                                         |
| `familyId`        | Inherited from the parent list                                |
| `listId`          | FK pointing to the parent `UserList.id`                       |
| `itemName`        | What needs doing ("Water the cactus")                         |
| `recurrenceUnit`  | `DAYS` or `WEEKS`                                             |
| `interval`        | N (repeat every N days/weeks)                                 |
| `weekdays`        | List of weekday numbers (1=Mon…7=Sun), used only when `WEEKS` |
| `nextDate`        | Next calculated due date                                      |
| `lastCompletedAt` | When it was last marked done                                  |
| `completionCount` | Running total of completions                                  |
| `dateCreated`     | Creation timestamp                                            |
| `lastUpdated`     | Last write timestamp                                          |

Entries have **no** visibility, type, or ownerUid — those live on the parent `UserList`.

---

## Recurrence logic (`RecurrenceCalculator`)

Both modes are **completion-relative** — the next date is always calculated from the moment you complete the task, so completing late never shifts the entire schedule forward cumulatively.

### Every N days
```
nextDate = completedAt + N days
```
Simple addition. Completing on any day always gives you exactly N days from that moment.

### Every N weeks + weekdays
Iterates day-by-day starting from `completedAt + 1 day`, returning the first day whose weekday number is in the `weekdays` set, within a window of `interval * 7` days. Completing on a Monday with weekdays = [1] (Monday) always returns the following Monday.

### On completion (`applyCompletion`)
```
completedAt     = now
nextDate        = nextDate(entry, from = completedAt)
lastCompletedAt = completedAt
completionCount += 1
lastUpdated     = completedAt
```
The updated entry is written back to Firestore + Room via `UpdateItemUseCase`.

---

## Visibility

Controlled by the `Visibility` enum (`domain/model/enums/Visibility.kt`), backed by the generic constants `VISIBILITY_FAMILY = "family"` and `VISIBILITY_PRIVATE = "private"` from `Constants.kt`. This same enum is shared with the Guides feature.

- **FAMILY** — all family members see the list.
- **PRIVATE** — only the `ownerUid` user sees the list.

Entries are always synced at the family scope — the observer queries the entire family's `list_entries` collection. Per-entry visibility is not needed because the parent list already controls access.

---

## Data flow

### Firestore → Room → UI

```
Firestore (user_lists)
  ├── familySharedUserListsSnapshotListener  (visibility = "family")
  └── privateUserListsSnapshotListener       (visibility = "private", ownerUid = uid)
        │
        ▼
  ObserveUserListsUseCase  (merges both streams, deduplicates by id)
        │
        ▼
  LocalDataSource.upsertUserLists / updateUserLists / deleteFamilyUserLists
        │
        ▼
  Room  user_lists  (UserListEntity)
        │
        ▼
  userListsDao.getItems(familyId)  →  Flow<List<UserListEntity>>
        │
        ▼
  LocalListRepositoryImpl.fetchListItems  →  Flow<ListItemsResultListener<UserList>>
        │
        ▼
  ListsViewModel / ListDetailViewModel  →  StateFlow<List<UserList>>
        │
        ▼
  ListsScreen / ListDetailScreen  (Compose)
```

```
Firestore (list_entries)
  └── familyListEntriesSnapshotListener  (familyId = familyId, no visibility split)
        │
        ▼
  ObserveListsUseCase  (upsert / delete in Room)
        │
        ▼
  Room  list_entries  (ListEntryEntity)
        │
        ▼
  listsDao.getItemsByListId(familyId, listId)  →  Flow<List<ListEntryEntity>>
        │
        ▼
  LocalListRepositoryImpl.fetchListItems (uid param = listId)  →  Flow<ListItemsResultListener<ListEntry>>
        │
        ▼
  ListDetailViewModel.entries  →  StateFlow<List<RoutineListEntry>>
        │
        ▼
  ListDetailScreen  (Compose)
```

### UI → Firestore (write path)

```
User taps Create / Save
        │
        ▼
  ViewModel.createList() / save()
        │
        ▼
  SaveItemUseCase / UpdateItemUseCase
        │
        ▼
  FirestoreDataSource.saveItem / updateItem
     (UserList → userListToFirestoreMap)
     (ListEntry → listEntryToFirestoreMap)
        │
        ▼
  Firestore  (triggers snapshot listener → Room sync → UI update)
```

---

## Observer lifecycle

The observers are started by `ObserverCoordinator` and ref-counted, so they run only while a screen that needs them is visible.

- `ObserverKey.USER_LISTS` → `ObserveUserListsUseCase` — started when `ListsRoute` or `ListDetailRoute` is on screen.
- `ObserverKey.ROUTINE_LIST_ENTRIES` → `ObserveRoutineListsUseCase` — started at the same time.

Both keys are acquired in `ListsRoute` and `ListDetailRoute` via `FeatureObserverLifecycleBinding`.

---

## Screens and navigation

```
HomeScreen
    └── "Lists" card  →  ListsRoute
                              │
                    ListsScreen  (hub — shows all UserLists)
                              │
                    Tap a list  →  ListDetailRoute
                                        │
                              ListDetailScreen  (entries for one list)
                                        │
                              Tap entry  →  ListEntryDetailsRoute
                                                  │
                                        ListEntryDetailsScreen  (edit entry)
                              Tap +     →  ListEntryDetailsRoute
                                                  │
                                        ListEntryDetailsScreen  (create entry)
                    Tap +  →  CreateListDialog  (inline in ListsScreen)
```

### ListsScreen
- Shows one card per `UserList` with name, type badge, and family/private label.
- FAB opens `CreateListDialog` (name, type, visibility). On successful save, navigates straight to the new list's `ListDetailScreen`.

### ListDetailScreen
- TopBar title = `UserList.itemName` (resolved from Room).
- Entries sorted by `nextDate` ascending (nulls last).
- Each entry card: name, recurrence description ("Every 3 days"), next due date, completion count, and a "Mark done" tap target.
- Tapping the card body navigates to `ListEntryDetailsScreen` for editing.
- FAB navigates to `ListEntryDetailsScreen` for creating a new entry in this list.

### ListEntryDetailsScreen
- Create or edit a single `ListEntry`.
- Fields: name, recurrence unit (DAYS / WEEKS), interval (N), weekday picker (shown only in WEEKS mode).
- On save, `nextDate` is calculated immediately from the anchor (creation time).
- Navigates back to `ListDetailScreen` on success.

---

## Key files

| Layer             | File                                                                                       |
|-------------------|--------------------------------------------------------------------------------------------|
| Domain models     | `domain/model/lists/UserList.kt`, `ListEntry.kt`, `ListType.kt`, `RecurrenceUnit.kt`       |
| Visibility        | `domain/model/enums/Visibility.kt`                                                         |
| Recurrence logic  | `domain/logic/RecurrenceCalculator.kt`                                                     |
| Room entities     | `data/model/UserListEntity.kt`, `RoutineListEntryEntity.kt`                                        |
| DAOs              | `data/local/dao/UserListsDao.kt`, `RoutineListsDao.kt`                                             |
| DB migration      | `data/local/AppDatabase.kt` — `MIGRATION_23_24`                                                    |
| Firestore parsing | `data/remote/FirestoreDataSource.kt` — `parseFirestoreUserList`, `parseFirestoreRoutineListEntry`  |
| Observers         | `domain/usecase/observers/ObserveUserListsUseCase.kt`, `ObserveRoutineListsUseCase.kt`             |
| ViewModels        | `ListsViewModel.kt`, `ListDetailViewModel.kt`, `ListEntryDetailsViewModel.kt`              |
| Screens           | `ListsScreen.kt`, `ListDetailScreen.kt`, `ListEntryDetailsScreen.kt`                       |
| Routes            | `ListsRoute.kt`, `ListDetailRoute.kt`, `ListEntryDetailsRoute.kt`                          |
