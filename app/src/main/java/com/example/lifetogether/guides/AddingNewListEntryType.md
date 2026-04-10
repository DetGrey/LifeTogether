# Guide to adding a new list entry type

Example: adding `SimpleListEntry` (a plain bullet-point entry with no recurrence).

Each entry type gets its own Room table and Firestore collection — no shared table with nullable columns.

1. /domain/model/lists/ListType
   - Add new enum value (e.g. `SIMPLE`)

2. /domain/model/lists
   - Create new data class implementing `ListEntry` interface
   - Include only the fields relevant to this type (no completion/recurrence if not needed)

3. /data/model
   - Create `SimpleListEntryEntity.kt` — Room entity with `tableName = Constants.SIMPLE_LIST_ENTRIES_TABLE`
   - Add only the columns needed for this type

4. /util/Constants
   - Add `const val SIMPLE_LIST_ENTRIES_TABLE = "list_entries_simple"`

5. /data/local/dao
   - Create `SimpleListsDao.kt` — DAO targeting the new table

6. /data/local/AppDatabase
   - Bump db version
   - Add `SimpleListEntryEntity::class` to the `@Database` entities list
   - Add abstract `fun simpleListsDao(): SimpleListsDao`
   - Add `ALTER TABLE` / `CREATE TABLE` migration for the new table

7. /di/DatabaseModule
   - Add `provideSimpleListsDao(db: AppDatabase): SimpleListsDao`

8. /data/model/Entity
   - Add `data class SimpleListEntry(val entity: SimpleListEntryEntity) : Entity()`

9. /data/local/LocalDataSource
   - Inject `SimpleListsDao`
   - Add `private fun SimpleListEntry.toEntity()` extension
   - Add `Constants.SIMPLE_LIST_ENTRIES_TABLE` branch in `getListItems()`
   - Add `updateSimpleListEntries()` and `deleteFamilySimpleListEntries()` helpers

10. /data/repository/LocalListRepositoryImpl
    - Add `Entity.SimpleListEntry` branch in `toItem()`

11. /data/remote/FirestoreDataSource
    - Add `familySimpleListEntriesSnapshotListener()` querying `SIMPLE_LIST_ENTRIES_TABLE`
    - Add `parseFirestoreSimpleListEntry()` parser
    - Add `simpleListEntryToFirestoreMap()` serializer
    - Handle `SimpleListEntry` in `saveItem` / `updateItem` dispatch

12. /domain/observer/ObserverTypes
    - Add `SIMPLE_LIST_ENTRIES` to the `ObserverKey` enum

13. /domain/usecase/observers
    - Create `ObserveSimpleListsUseCase.kt` — listens to `familySimpleListEntriesSnapshotListener`

14. /domain/observer/ObserverCoordinator
    - Inject `ObserveSimpleListsUseCase`
    - Add `SIMPLE_LIST_ENTRIES` to `featureObserverKeys`
    - Handle `ObserverKey.SIMPLE_LIST_ENTRIES` in `createObserverHandle()`

15. /ui/feature/lists
    - Create `SimpleListEntryDetailsViewModel` and `SimpleListEntryDetailsScreen`
    - Update `ListDetailScreen` to dispatch on entry type when rendering cards
      and when tapping to navigate to the details screen
    - Update `ListsRoute` / `ListDetailRoute` to acquire the new observer key
