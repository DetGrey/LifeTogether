# Traveller Feature Plan

## Overview

A map-based feature for tracking visited places, lived locations, and bucket-list destinations as family-shared pins. Pins are city-level. Multiple pins can reference the same album (acting as a "trip" grouping). Pins sharing an album are visually connected on the map with a polyline sorted chronologically by `dateFrom`. Accessible from the home screen as a new tile. Also reachable from album detail with a pre-applied album filter.

---

## Libraries to Add

- `com.google.maps.android:maps-compose` — Google Maps Compose SDK for rendering the map and markers
- `com.google.android.libraries.places:places` — Google Places API for city autocomplete search when adding a pin

Both use the existing Google Cloud project. An API key entry (restricted to Maps SDK + Places API) must be added to the app's manifest / local.properties.

---

## Domain Model

### `domain/model/traveller/PinType.kt`

```kotlin
enum class PinType(val displayName: String) {
    VISITED("Visited"),
    LIVED("Lived"),
    BUCKET_LIST("Bucket list"),
}
```

`displayName` is the label used in the `TagOption` filter chips on the map screen.

### `domain/model/traveller/TravellerPin.kt`

```kotlin
data class TravellerPin(
    val id: String,
    val familyId: String,
    val lastUpdated: Date = Date(),
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val type: PinType,
    val dateFrom: Date?,       // required for VISITED/LIVED, optional for BUCKET_LIST
    val dateTo: Date?,         // null only valid when type == LIVED (means "still living here")
                               // for VISITED single-day: dateTo == dateFrom
    val albumId: String?,      // optional; multiple pins can share the same albumId
)
```

**Rules enforced at save time (repository layer):**
- `VISITED`: `dateFrom` required, `dateTo` required (`dateTo == dateFrom` for single-day trips)
- `LIVED`: `dateFrom` required, `dateTo` nullable (null = currently living there)
- `BUCKET_LIST`: `dateFrom` optional, `dateTo` always null

**Naming rule:** no `is*` boolean fields — follows the architecture persisted model naming rule. If a boolean is needed later (e.g. `singleDay`), name it `singleDay`, not `isSingleDay`.

---

## Constants

### `util/Constants.kt` — add:

```kotlin
const val TRAVELLER_PINS_TABLE = "traveller_pins"
const val TRAVELLER_PINS_COLLECTION = "traveller_pins"
```

---

## Data Layer

### `data/model/TravellerPinEntity.kt`

Room entity for the `traveller_pins` table. Fields mirror `TravellerPin`. Dates stored as `Long` (epoch ms) via the existing `Converters`. `PinType` stored as `TEXT` (enum name).

```
id TEXT PK, family_id TEXT, last_updated INTEGER,
city TEXT, country TEXT, latitude REAL, longitude REAL,
type TEXT, date_from INTEGER (nullable), date_to INTEGER (nullable),
album_id TEXT (nullable)
```

### `data/local/dao/TravellerPinsDao.kt`

Follows the `AlbumsDao` shape:

- `getItems(familyId: String): Flow<List<TravellerPinEntity>>`
- `getItemById(familyId: String, id: String): TravellerPinEntity?`
- `updateItems(items: List<TravellerPinEntity>)` — `OnConflictStrategy.REPLACE`
- `deleteItems(itemIds: List<String>)`

### `data/local/AppDatabase.kt`

- Add `TravellerPinEntity::class` to `@Database` entities list
- Bump version **40 → 41**
- Add `travellerPinsDao(): TravellerPinsDao` abstract method
- Add `MIGRATION_40_41`

### `data/local/AppDatabaseMigrations.kt` — `MIGRATION_40_41`:

```sql
CREATE TABLE IF NOT EXISTS `traveller_pins` (
    `id` TEXT NOT NULL,
    `family_id` TEXT NOT NULL,
    `last_updated` INTEGER NOT NULL,
    `city` TEXT NOT NULL,
    `country` TEXT NOT NULL,
    `latitude` REAL NOT NULL,
    `longitude` REAL NOT NULL,
    `type` TEXT NOT NULL,
    `date_from` INTEGER,
    `date_to` INTEGER,
    `album_id` TEXT,
    PRIMARY KEY(`id`)
)
```

### `data/model/Entity.kt`

Add `TravellerPin` sealed case.

### `data/local/source/TravellerPinsLocalSource.kt`

Focused feature-local source (no central facade). Wraps `TravellerPinsDao`. Exposes:
- `getPins(familyId): Flow<List<TravellerPinEntity>>`
- `upsertPins(pins: List<TravellerPinEntity>)`
- `deletePins(ids: List<String>)`

---

## Remote Layer

### Firestore collection path

`families/{familyId}/traveller_pins/{pinId}`

### `data/remote/FirestoreDataSource.kt`

Add `travellerPinsSnapshotListener(familyId)` — single family-scoped collection listener (no visibility split; all pins are family-shared). Follows the same observer pattern as other family-scoped collections.

Add `saveTravellerPin(pin: TravellerPin)` and `deleteTravellerPin(familyId, pinId)` for writes.

---

## Repository

### `domain/repository/TravellerRepository.kt`

```kotlin
interface TravellerRepository {
    fun observePins(familyId: String): Flow<Result<List<TravellerPin>, AppError>>
    suspend fun savePin(pin: TravellerPin): Result<Unit, AppError>
    suspend fun deletePin(familyId: String, pinId: String): Result<Unit, AppError>
}
```

### `data/repository/TravellerRepositoryImpl.kt`

- `observePins` — returns `Flow` from `TravellerPinsLocalSource`; Firestore snapshot listener reconciles divergence via `SyncCoordinator`
- `savePin` — local-first: stamps `lastUpdated` via `stampNow()`, writes to Room first, then writes to Firestore. On remote failure, rolls back the local write
- `deletePin` — local-first: deletes from Room, then Firestore

---

## Sync

### `domain/sync/SyncTypes.kt`

Add `TRAVELLER_PINS` to `SyncKey` enum.

### `domain/sync/SyncCoordinator.kt`

Add `SyncKey.TRAVELLER_PINS` to `featureSyncKeys`. Add the observer case: starts `travellerPinsSnapshotListener`, reconciles Room via upsert/delete.

### `ui/common/sync/RouteSyncMapper.kt`

Add `TravellerNavRoute` case to `activeSyncKeys()`:

```kotlin
is TravellerNavRoute -> setOf(SyncKey.TRAVELLER_PINS)
```

---

## DI

### `di/DatabaseModule.kt`

- Add `MIGRATION_40_41` to `addMigrations(...)`
- Add `provideTravellerPinsDao(db: AppDatabase): TravellerPinsDao`

### `di/RepositoryModule.kt`

Bind `TravellerRepositoryImpl` to `TravellerRepository`.

---

## Navigation

### `ui/navigation/NavRoutes.kt`

```kotlin
@Serializable data class TravellerNavRoute(val filterAlbumId: String? = null) : AppRoute
```

When `filterAlbumId` is non-null, the map opens pre-filtered to that album's pins only with a dismissible filter chip.

### `ui/navigation/NavHost.kt`

```kotlin
entry<TravellerNavRoute> { key ->
    TravellerRoute(filterAlbumId = key.filterAlbumId, appNavigator = appNavigator)
}
```

### `ui/navigation/AppNavigator.kt`

Add `fun navigateToTraveller(filterAlbumId: String? = null)`.

---

## Home

### `ui/feature/home/HomeModels.kt`

Add to `HomeTile`:

```kotlin
data object Traveller : HomeTile {
    override val title: String = "Traveller"
    override val appIcon: AppIcon = AppIcon(R.drawable.ic_traveller, "map pin traveller icon")
    override val requiresFamilyAccess: Boolean = true
    override val requiresAdminAccess: Boolean = false
}
```

### `ui/feature/home/HomeRoute.kt` / `HomeViewModel.kt`

Add `HomeTile.Traveller` to the home sections list. Wire `TileClicked(HomeTile.Traveller)` → `appNavigator.navigateToTraveller()` in `HomeRoute`.

---

## UI Feature

All files live under `ui/feature/traveller/`.

### `TravellerModels.kt`

```kotlin
sealed interface TravellerUiState {
    data object Loading : TravellerUiState
    data class Loaded(val content: TravellerContent) : TravellerUiState
    data class Error(val message: String) : TravellerUiState
}

data class TravellerContent(
    val pins: List<TravellerPin> = emptyList(),
    val activeFilters: Set<PinType> = setOf(PinType.VISITED, PinType.LIVED),
    val filterAlbumId: String? = null,    // non-null = locked to album filter
    val selectedPin: TravellerPin? = null,
    val sheetMode: PinSheetMode = PinSheetMode.Hidden,
    val addSearchQuery: String = "",
    val addSearchResults: List<PlaceResult> = emptyList(),
    val draftPin: DraftPin? = null,       // in-progress pin being created or edited
)

sealed interface PinSheetMode {
    data object Hidden : PinSheetMode
    data object Searching : PinSheetMode   // Places search sheet
    data object Viewing : PinSheetMode     // tap existing pin
    data object Creating : PinSheetMode    // filling in details for new pin
    data object Editing : PinSheetMode     // editing existing pin inline
}

data class DraftPin(
    val id: String? = null,              // null = new pin
    val city: String = "",
    val country: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val type: PinType = PinType.VISITED,
    val dateFrom: Date? = null,
    val dateTo: Date? = null,
    val singleDay: Boolean = false,      // true = dateTo == dateFrom
    val stillLivingHere: Boolean = false,// true = dateTo null, only valid for LIVED
    val albumId: String? = null,
)

data class PlaceResult(
    val placeId: String,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
)

sealed interface TravellerUiEvent {
    data class FilterToggled(val type: PinType) : TravellerUiEvent
    data class PinTapped(val pin: TravellerPin) : TravellerUiEvent
    data object AddPinClicked : TravellerUiEvent
    data class SearchQueryChanged(val query: String) : TravellerUiEvent
    data class PlaceSelected(val result: PlaceResult) : TravellerUiEvent
    data class DraftTypeChanged(val type: PinType) : TravellerUiEvent
    data class DraftDateFromChanged(val date: Date) : TravellerUiEvent
    data class DraftDateToChanged(val date: Date) : TravellerUiEvent
    data class DraftSingleDayToggled(val checked: Boolean) : TravellerUiEvent
    data class DraftStillLivingHereToggled(val checked: Boolean) : TravellerUiEvent
    data class DraftAlbumChanged(val albumId: String?) : TravellerUiEvent
    data object SaveDraftPin : TravellerUiEvent
    data object EditPinClicked : TravellerUiEvent
    data object DeletePinClicked : TravellerUiEvent
    data object ConfirmDeletePin : TravellerUiEvent
    data object DismissSheet : TravellerUiEvent
    data object DismissAlbumFilter : TravellerUiEvent
}

sealed interface TravellerCommand {
    data class NavigateToAlbum(val albumId: String) : TravellerCommand
}
```

### `TravellerViewModel.kt`

- Assisted injection receives `filterAlbumId: String?` from the route key
- Observes `TravellerRepository.observePins(familyId)` → exposes `uiState: StateFlow<TravellerUiState>`
- `onEvent(TravellerUiEvent)` handles all UI events
- Places API search debounced on `SearchQueryChanged` (300ms), results populate `addSearchResults`
- `SaveDraftPin` validates, stamps `lastUpdated`, calls `TravellerRepository.savePin()`
- `ConfirmDeletePin` calls `TravellerRepository.deletePin()`
- Emits `UiCommand.ShowSnackbar` on errors
- Emits `TravellerCommand.NavigateToAlbum` when user taps the album link in the view sheet

**Filtered pin list logic (derived in ViewModel):**
```
visiblePins = if (filterAlbumId != null)
    pins.filter { it.albumId == filterAlbumId }
else
    pins.filter { it.type in activeFilters }
```

**Polyline groups (derived in ViewModel):**
```
polylineGroups = visiblePins
    .filter { it.albumId != null }
    .groupBy { it.albumId }
    .mapValues { (_, pins) -> pins.sortedBy { it.dateFrom } }
    .values
    .filter { it.size > 1 }
```

### `TravellerRoute.kt`

- Takes `filterAlbumId: String?` from route key
- Passes to `hiltViewModel` via assisted factory
- `CollectUiCommands(viewModel.uiCommands)`
- Collects `viewModel.commands` for `TravellerCommand.NavigateToAlbum` → `appNavigator.navigate(AlbumMediaNavRoute(albumId))`
- Navigation events: back

### `TravellerScreen.kt`

Top-level layout:
1. `GoogleMap` composable filling the screen
2. `TravellerFilterRow` overlaid at top (inside a `Box`)
3. Album filter chip overlaid below filter row when `filterAlbumId != null` (dismissible)
4. FAB (add pin) at bottom right
5. Pin detail / search / create bottom sheet driven by `sheetMode`

**Map content:**
- One `Marker` per `visiblePin`, tinted with the pin's type colour:
  - `VISITED` → `MaterialTheme.colorScheme.primary`
  - `LIVED` → `MaterialTheme.colorScheme.secondary`
  - `BUCKET_LIST` → `MaterialTheme.colorScheme.tertiary`
- One `Polyline` per polyline group (light weight, matching the album's primary pin colour or a neutral colour)
- Tapping a marker emits `PinTapped`

**Filter row (`TravellerFilterRow`):**
Uses `TagOption` (with the new `selectedContainerColor` parameter) for each `PinType`. Multi-select state is `Set<PinType>`. Each chip uses its type colour as `selectedContainerColor`. Hidden when `filterAlbumId` is active (album filter takes precedence).

**Bottom sheet behaviour:**

| `sheetMode` | Content |
|---|---|
| `Hidden` | No sheet |
| `Searching` | Search text field + `LazyColumn` of `PlaceResult` rows |
| `Viewing` | Pin detail: city+country header, type badge, date range, album link row. Edit pencil icon + delete icon (error tint) in header |
| `Creating` | Place confirmed (city+country shown read-only). Type selector (`TagOptionRow`), dateFrom picker, singleDay/stillLivingHere toggle, dateTo picker (hidden when toggle active), album picker. Save button |
| `Editing` | Same fields as Creating, pre-populated. Save/cancel buttons |

**Delete confirmation:** `ConfirmationDialog` shown on top of the sheet when `DeletePinClicked` is emitted. Confirm emits `ConfirmDeletePin`.

**Album link row (in Viewing mode):**
Shown only when `pin.albumId != null`. Tapping navigates to the album via `TravellerCommand.NavigateToAlbum`.

---

## `ui/common/tagOptionRow/TagOption.kt` — change

Add optional `selectedContainerColor` parameter defaulting to the current hardcoded value:

```kotlin
fun TagOption(
    tag: String,
    selectedTag: String,
    onClick: ((String) -> Unit)? = null,
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondary,  // new param
)
```

Update `FilterChipDefaults.filterChipColors(selectedContainerColor = selectedContainerColor)`. No change to existing callers.

---

## Album Detail Changes

### `ui/feature/gallery/AlbumDetailsScreen.kt`

Add a single tappable row below the album title when `connectedCities` is non-empty:

```
📍 Edinburgh · Glasgow · Inverness  >
```

Tapping emits a navigation event handled in `AlbumDetailsRoute` which calls `appNavigator.navigateToTraveller(filterAlbumId = albumId)`.

### `ui/feature/gallery/AlbumDetailsViewModel.kt`

Inject `TravellerRepository`. Observe `pins.filter { it.albumId == albumId }`. Expose `connectedCities: List<String>` (city names of matching pins, sorted by `dateFrom`).

### `ui/feature/gallery/AlbumDetailsModels.kt`

Add `connectedCities: List<String> = emptyList()` to the loaded state. Add `NavigateToTraveller` to the navigation event sealed interface.

---

## Date UX Detail

In the create/edit sheet:

- **`VISITED` or `BUCKET_LIST`:** show `dateFrom` date picker, then a `"Single day"` checkbox. When checked, hide `dateTo` picker and set `dateTo = dateFrom` on save. When unchecked, show `dateTo` picker.
- **`LIVED`:** show `dateFrom` date picker, then a `"Still living here"` checkbox. When checked, hide `dateTo` picker and leave `dateTo = null` on save. When unchecked, show `dateTo` picker.
- **`BUCKET_LIST`:** `dateFrom` is optional (labelled "Planned date (optional)"). `dateTo` always null.

---

## File Checklist

### New files
- `domain/model/traveller/PinType.kt`
- `domain/model/traveller/TravellerPin.kt`
- `domain/repository/TravellerRepository.kt`
- `data/model/TravellerPinEntity.kt`
- `data/local/dao/TravellerPinsDao.kt`
- `data/local/source/TravellerPinsLocalSource.kt`
- `data/repository/TravellerRepositoryImpl.kt`
- `ui/feature/traveller/TravellerModels.kt`
- `ui/feature/traveller/TravellerViewModel.kt`
- `ui/feature/traveller/TravellerRoute.kt`
- `ui/feature/traveller/TravellerScreen.kt`
- `ui/feature/traveller/TravellerFilterRow.kt`

### Modified files
- `util/Constants.kt` — add table/collection constants
- `data/model/Entity.kt` — add TravellerPin sealed case
- `data/local/AppDatabase.kt` — bump version 40→41, add entity + DAO + MIGRATION_40_41
- `data/local/AppDatabaseMigrations.kt` — add MIGRATION_40_41
- `domain/sync/SyncTypes.kt` — add TRAVELLER_PINS to SyncKey
- `domain/sync/SyncCoordinator.kt` — add TRAVELLER_PINS to featureSyncKeys + observer case
- `ui/common/sync/RouteSyncMapper.kt` — add TravellerNavRoute → TRAVELLER_PINS
- `ui/navigation/NavRoutes.kt` — add TravellerNavRoute
- `ui/navigation/NavHost.kt` — add TravellerNavRoute entry
- `ui/navigation/AppNavigator.kt` — add navigateToTraveller()
- `ui/feature/home/HomeModels.kt` — add HomeTile.Traveller
- `ui/feature/home/HomeRoute.kt` — wire Traveller tile navigation
- `ui/feature/home/HomeViewModel.kt` — add Traveller to sections
- `ui/common/tagOptionRow/TagOption.kt` — add selectedContainerColor param
- `ui/feature/gallery/AlbumDetailsModels.kt` — add connectedCities + navigation event
- `ui/feature/gallery/AlbumDetailsViewModel.kt` — inject TravellerRepository, observe connected pins
- `ui/feature/gallery/AlbumDetailsScreen.kt` — add connected cities row
- `di/DatabaseModule.kt` — add migration + DAO provider
- `di/RepositoryModule.kt` — bind TravellerRepository
