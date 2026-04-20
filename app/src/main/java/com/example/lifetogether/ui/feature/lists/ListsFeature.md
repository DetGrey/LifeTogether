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

### `RoutineListEntry` — an item inside a list
Stored in the `list_entries_routine` Firestore collection and Room table.

| Field             | Layer          | Purpose                                                         |
|-------------------|----------------|-----------------------------------------------------------------|
| `id`              | Both           | Firestore document ID                                           |
| `familyId`        | Both           | Inherited from the parent list                                  |
| `listId`          | Both           | FK pointing to the parent `UserList.id`                         |
| `itemName`        | Both           | What needs doing ("Water the cactus")                           |
| `recurrenceUnit`  | Both           | `DAYS` or `WEEKS`                                               |
| `interval`        | Both           | N (repeat every N days/weeks)                                   |
| `weekdays`        | Both           | List of weekday numbers (1=Mon…7=Sun), used only when `WEEKS`   |
| `nextDate`        | Both           | Next calculated due date                                        |
| `lastCompletedAt` | Both           | When it was last marked done                                    |
| `completionCount` | Both           | Running total of completions                                    |
| `dateCreated`     | Both           | Creation timestamp                                              |
| `lastUpdated`     | Both           | Last write timestamp                                            |
| `imageUrl`        | Firestore only | Download URL of the entry's image (written on upload)           |
| `imageData`       | Room only      | Image stored as `ByteArray`; downloaded from `imageUrl` on sync |

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

Entries are always synced at the family scope — the observer queries the entire family's `list_entries_routine` collection. Per-entry visibility is not needed because the parent list already controls access.

---

## Entry images

Images on entries follow the same pattern as recipe images.

### Storage layout
- **Firebase Storage** — the image file lives at `list_entries_routine/<uuid>-<timestamp>.jpeg`
- **Firestore** — the document gains an `imageUrl` string field after upload
- **Room** — `RoutineListEntryEntity.imageData: ByteArray?` holds the downloaded bytes locally

### Upload flow (existing entry only)
Image upload is only available once an entry has an ID:

```
User taps image area in edit mode
        │
        ▼
ImageUploadDialog  (ImageUploadViewModel)
        │  user picks image from gallery
        ▼
FirebaseStorageDataSource.uploadPhoto(uri, RoutineListEntryImage(familyId, entryId))
        │  ImageProcessor resizes/compresses → uploads to Storage
        ▼
FirestoreDataSource.saveImageDownloadUrl(url, RoutineListEntryImage)
        │  updates list_entries_routine/<entryId>.imageUrl
        ▼
Firestore snapshot fires → ObserveRoutineListsUseCase downloads bytes
        │
        ▼
LocalDataSource.updateRoutineListEntries(items, byteArrays)
        │  stores imageData in Room
        ▼
RoutineListsDao.getImageByteArray(entryId) → Flow<ByteArray?>
        │
        ▼
ImageViewModel.collectImageFlow / ListDetailsViewModel.updateImageJobs → Bitmap in UI
```

### Download / cache flow (on sync)
`ObserveRoutineListsUseCase` calls `getRoutineEntryIdsWithImages(familyId)` before each sync. Entries already cached in Room are skipped. Only new or changed entries with an `imageUrl` are downloaded, avoiding redundant network calls.

### ImageType
`ImageType.RoutineListEntryImage(familyId, entryId)` — used throughout the image pipeline (ImageProcessor, FirebaseStorageDataSource, FirestoreDataSource, LocalDataSource, ImageViewModel).

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
  LocalDataSource.updateUserLists / deleteFamilyUserLists
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
  ListsViewModel / ListDetailsViewModel  →  StateFlow<List<UserList>>
        │
        ▼
  ListsScreen / ListDetailsScreen  (Compose)
```

```
Firestore (list_entries_routine)
  └── familyRoutineListEntriesSnapshotListener  (familyId = familyId)
        │
        ▼
  ObserveRoutineListsUseCase
        │  for each entry with imageUrl not yet cached:
        │      StorageRepository.fetchImageByteArray(url) → ByteArray
        ▼
  LocalDataSource.updateRoutineListEntries(items, byteArrays)
        │  preserves existing imageData for already-cached entries
        ▼
  Room  list_entries_routine  (RoutineListEntryEntity)
        │
        ├── routineListsDao.getItemsByListId(familyId, listId)  →  Flow<List<RoutineListEntryEntity>>
        │         │
        │         ▼
        │   LocalListRepositoryImpl.fetchListItems  →  Flow<ListItemsResultListener<RoutineListEntry>>
        │         │
        │         ▼
        │   ListDetailsViewModel.entries + imageBitmaps  →  StateFlow
        │         │
        │         ▼
        │   ListDetailsScreen / ListEntryCard  (shows bitmap thumbnail)
        │
        └── routineListsDao.getImageByteArray(entryId)  →  Flow<ByteArray?>
                  │
                  ▼
            ImageViewModel.collectImageFlow  →  Bitmap
                  │
                  ▼
            ListEntryDetailsScreen  (shows full image)
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
     (RoutineListEntry → listEntryToFirestoreMap — includes imageUrl)
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
                              ListDetailsScreen  (entries for one list)
                                        │
                              Tap entry  →  ListEntryDetailsRoute
                                                  │
                                        ListEntryDetailsScreen  (view/edit entry)
                              Tap +     →  ListEntryDetailsRoute
                                                  │
                                        ListEntryDetailsScreen  (create entry)
                    Tap +  →  CreateListDialog  (inline in ListsScreen)
```

### ListsScreen
- Shows one card per `UserList` with name, type badge, and family/private label.
- FAB opens `CreateListDialog` (name, type, visibility). On successful save, navigates straight to the new list's `ListDetailsScreen`.

### ListDetailsScreen
- TopBar title = `UserList.itemName` (resolved from Room).
- Entries sorted by `nextDate` ascending (nulls last).
- Each entry card: name, recurrence description ("Every 3 days"), next due date, and a 60×60dp image thumbnail (shows entry image if available, tertiary background otherwise).
- Tapping the card body navigates to `ListEntryDetailsScreen` for editing.
- FAB navigates to `ListEntryDetailsScreen` for creating a new entry in this list.
- `ListDetailsViewModel` maintains `imageBitmaps: Map<String, Bitmap>` — one Room-observing coroutine per entry, started/cancelled as entries come and go.

### ListEntryDetailsScreen
- Create or edit a single `RoutineListEntry`.
- Fields: image area (180dp), name, recurrence unit (DAYS / WEEKS), interval (N), weekday picker (shown only in WEEKS mode).
- **Image area**: shows the entry's bitmap if available, or a tinted placeholder. In edit mode on an existing entry, tapping it opens `ImageUploadDialog`. Image upload is not available on new entries (no ID yet).
- On save, `nextDate` is calculated immediately from the anchor (creation time).
- Navigates back on success.

---

## Key files

| Layer             | File                                                                                                                                                                             |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Domain models     | `domain/model/lists/UserList.kt`, `RoutineListEntry.kt`, `ListType.kt`, `RecurrenceUnit.kt`                                                                                      |
| Image type        | `domain/model/sealed/ImageType.kt` — `RoutineListEntryImage(familyId, entryId)`                                                                                                  |
| Visibility        | `domain/model/enums/Visibility.kt`                                                                                                                                               |
| Recurrence logic  | `domain/logic/RecurrenceCalculator.kt`                                                                                                                                           |
| Room entities     | `data/model/UserListEntity.kt`, `RoutineListEntryEntity.kt` (has `imageData: ByteArray?`)                                                                                        |
| DAOs              | `data/local/dao/UserListsDao.kt`, `RoutineListsDao.kt` (has `getImageByteArray`, `getEntryIdsWithImages`)                                                                        |
| DB migrations     | `data/local/AppDatabase.kt` — v26; `MIGRATION_23_24` creates tables, `MIGRATION_24_25` adds `image_data`, `MIGRATION_25_26` drops `image_url` from `user_lists`                  |
| Image processing  | `data/logic/ImageProcessor.kt` — `RoutineListEntryImage` config                                                                                                                  |
| Firestore parsing | `data/remote/FirestoreDataSource.kt` — `parseFirestoreRoutineListEntry` (reads `imageUrl`), `listEntryToFirestoreMap` (writes `imageUrl`), `getImageUrl`, `saveImageDownloadUrl` |
| Observers         | `domain/usecase/observers/ObserveUserListsUseCase.kt`, `ObserveRoutineListsUseCase.kt` (downloads images on sync)                                                                |
| ViewModels        | `ListsViewModel.kt`, `ListDetailsViewModel.kt` (has `imageBitmaps`), `ListEntryDetailsViewModel.kt`                                                                              |
| Screens           | `ListsScreen.kt`, `ListDetailsScreen.kt`, `ListEntryDetailsScreen.kt`                                                                                                            |
| Routes            | `ListsRoute.kt`, `ListDetailRoute.kt`, `ListEntryDetailsRoute.kt`                                                                                                                |
