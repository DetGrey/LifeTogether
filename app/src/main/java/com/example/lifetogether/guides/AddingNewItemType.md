# Guide to adding a new item type

1. /util/Constants
   - Add table name
2. /domain/model
    - Create new data class file for item
3. /data
   1. /model
      - Create new Entity file
   2. /local/dao
      - Create new dao file
   3. /model/Entity
      - Add data class for new entity
   4. /local/LocalListRepositoryImpl
      - Update fun Entity.toItem() to include new entity
   5. /local/LocalDataSource
      - Update getListItems() with table + dao reference
      - Add update function (e.g. updateAlbums) with entity + dao
   6. /local/AppDatabase
      - Update entities list
      - Add abstract fun for new dao
4. /di/DatabaseModule
   - Add dao reference
5. /data/remote/FirestoreDataSource
   - Add snapshot listener for new item
6. /domain/usecase/observers
   - Create new observe use case with snapshotlistener and update function
7. /ui/viewmodel/FirebaseViewModel
   - update observeFirestoreFamilyData to include observe use case
8. /ui/viewmodel
   - Create a new viewmodel
     - Inject fetchListItemsUseCase
     - Use fetchListItemsUseCase.collect

