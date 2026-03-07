# Implementation Plan: Guides (List/Details/Step Player) + Manual/JSON Upload + Family/Private Visibility

## Summary
Build a full Guides feature aligned with your current architecture:
- Firestore `guides` collection synced to Room via observer use case.
- Overview screen with `AddButton` and upload options.
- Guide details with sections/steps completion, Start/Continue CTA, and visibility toggle (Family shared vs Private).
- Dedicated step-player screen with current step focus, next-step preview, and section progress percentage bar.
- Support manual guide creation and JSON import (with template preview + downloadable example).

## Important Public API / Interface / Type Changes
1. Add constants in [Constants.kt](/Users/anenovruplarsen/Documents/Private/LifeTogether/app/src/main/java/com/example/lifetogether/util/Constants.kt):
- `GUIDES_TABLE = "guides"`
- `GUIDE_VISIBILITY_FAMILY = "family"`
- `GUIDE_VISIBILITY_PRIVATE = "private"`

2. Add domain guide model package `domain/model/guides`:
- `Guide : Item`
- `GuideSection`
- `GuideStep`
- `GuideResume`
- `enum class GuideVisibility { FAMILY, PRIVATE }`

3. `Guide` fields (canonical):
- `id`, `familyId`, `itemName`, `description`, `lastUpdated`
- `visibility`, `ownerUid`
- `started`
- `sections: List<GuideSection>`
- `resume: GuideResume?`

4. Add Room entity and wrappers:
- `GuideEntity` in `data/model`
- `Entity.Guide` in [Entity.kt](/Users/anenovruplarsen/Documents/Private/LifeTogether/app/src/main/java/com/example/lifetogether/data/model/Entity.kt)
- `GuidesDao`

5. Navigation additions:
- `GUIDES_SCREEN`
- `GUIDE_DETAILS_SCREEN/{guideId}`
- `GUIDE_STEP_PLAYER_SCREEN/{guideId}`
- `GUIDE_CREATE_SCREEN` (manual editor)

6. Navigator interface additions in [Navigator.kt](/Users/anenovruplarsen/Documents/Private/LifeTogether/app/src/main/java/com/example/lifetogether/ui/navigation/Navigator.kt) and [AppNavigator.kt](/Users/anenovruplarsen/Documents/Private/LifeTogether/app/src/main/java/com/example/lifetogether/ui/navigation/AppNavigator.kt):
- `navigateToGuides()`
- `navigateToGuideDetails(guideId: String)`
- `navigateToGuideStepPlayer(guideId: String)`
- `navigateToGuideCreate()`

## Data Model and Schema Rules

### Firestore document shape (canonical)
- `familyId: String`
- `ownerUid: String`
- `visibility: "family" | "private"`
- `name` (mapped to `itemName` in app)
- `description`
- `started`
- `resume`
- `sections`

### Compatibility read rules (from existing template variants)
- Accept `completed` and `isCompleted` for sections.
- Accept subsection `steps` as object or list; normalize to `subSteps: List<GuideStep>`.
- Default missing booleans to `false`, missing lists to empty.
- Preserve content fields but normalize to one internal shape before saving to Room.

### Import ID policy
- On JSON import, regenerate IDs for guide/sections/steps/substeps (UUID).
- Keep imported completion/start flags if present; default to false if missing.

## Architecture Changes by Layer

### 1) Local storage (Room)
1. Add `GuideEntity` to [AppDatabase.kt](/Users/anenovruplarsen/Documents/Private/LifeTogether/app/src/main/java/com/example/lifetogether/data/local/AppDatabase.kt), add `guidesDao()`, bump DB version.
2. Add converters in [Converters.kt](/Users/anenovruplarsen/Documents/Private/LifeTogether/app/src/main/java/com/example/lifetogether/data/local/Converters.kt) for:
- `List<GuideSection>`
- `GuideResume`
3. Add `GuidesDao`:
- `getItems(familyId: String): Flow<List<GuideEntity>>`
- `getItemById(familyId: String, id: String): GuideEntity?`
- `updateItems(items: List<GuideEntity>)`
- `deleteItems(ids: List<String>)`
4. Add DAO provider in [DatabaseModule.kt](/Users/anenovruplarsen/Documents/Private/LifeTogether/app/src/main/java/com/example/lifetogether/di/DatabaseModule.kt).

### 2) LocalDataSource + local repository
1. In [LocalDataSource.kt](/Users/anenovruplarsen/Documents/Private/LifeTogether/app/src/main/java/com/example/lifetogether/data/local/LocalDataSource.kt):
- Add guides DAO dependency.
- Extend `getListItems()` and `getItemById()` for `GUIDES_TABLE`.
- Add `updateGuides(items: List<Guide>)` with same diff/upsert/delete strategy used by recipes/albums/tips.
- Add `deleteFamilyGuides(familyId: String)`.
2. In [LocalListRepositoryImpl.kt](/Users/anenovruplarsen/Documents/Private/LifeTogether/app/src/main/java/com/example/lifetogether/data/repository/LocalListRepositoryImpl.kt):
- Add `Entity.Guide -> Guide` mapping in `toItem()`.

### 3) Firestore + observer sync
1. In [FirestoreDataSource.kt](/Users/anenovruplarsen/Documents/Private/LifeTogether/app/src/main/java/com/example/lifetogether/data/remote/FirestoreDataSource.kt), add two listeners:
- `familySharedGuidesSnapshotListener(familyId)` with filters `familyId == X` and `visibility == family`
- `privateGuidesSnapshotListener(familyId, uid)` with filters `familyId == X`, `visibility == private`, `ownerUid == uid`
2. Parse each document with compatibility parser (not plain `toObjects`).
3. Add `ObserveGuidesUseCase`:
- Combine the two flows, merge both guide sets, then update Room.
- If merged set is empty, call `deleteFamilyGuides(familyId)`.
4. Wire it in [FirebaseViewModel.kt](/Users/anenovruplarsen/Documents/Private/LifeTogether/app/src/main/java/com/example/lifetogether/ui/viewmodel/FirebaseViewModel.kt) from `observeFirestoreFamilyData`, passing both `uid` and `familyId`.

## UI / UX Implementation

### A) Guides overview screen
1. Create `GuidesScreen` + `GuidesViewModel` (same setup style as Recipes/Grocery):
- Fetch from Room with `FetchListItemsUseCase(familyId, GUIDES_TABLE, Guide::class)`.
- Render list of guide cards.
2. Add floating [AddButton.kt](/Users/anenovruplarsen/Documents/Private/LifeTogether/app/src/main/java/com/example/lifetogether/ui/common/button/AddButton.kt) bottom-right.
3. On add click, show option box (`AlertDialog`) with:
- `Create guide manually`
- `Upload JSON file`

### B) Manual creation flow
1. New `GuideCreateScreen` + `GuideCreateViewModel`.
2. Editable fields:
- title, description, visibility, optional section/step builder.
3. Save via `SaveItemUseCase` to `guides` collection.
4. After save: navigate to GuideDetails.

### C) JSON upload flow (from add option)
1. Show import dialog/screen with:
- short format instructions
- example snippet preview
- `Download example JSON`
- `Choose JSON file`
2. Example source:
- move template to `app/src/main/assets/guide_template.json` (single source of truth).
3. Download example:
- use `ActivityResultContracts.CreateDocument("application/json")`, write template bytes to selected URI.
4. Select file:
- use `ActivityResultContracts.OpenDocument()` for `application/json` (and fallback text MIME).
5. Parse + validate:
- object or array support
- normalize schema
- regenerate all IDs
- assign `familyId` current family and `ownerUid` current user
- if visibility missing -> default `private`
6. Persist imported guides with `SaveItemUseCase` per guide; show result summary (success/fail count).

### D) Guide details screen
1. `GuideDetailsScreen` + `GuideDetailsViewModel`.
2. Show:
- guide metadata
- visibility toggle control (`Family shared` / `Private`)
- Start/Continue CTA section above sections list
- section list with step completion status
3. Visibility toggle behavior:
- update `visibility` and `lastUpdated` via `UpdateItemUseCase`
- if switched to private, force `ownerUid = current uid`
- if switched to family, keep `ownerUid` unchanged
- disable toggle if user is not owner (best-practice ownership control)
4. Start/Continue button:
- `Start` if not started
- `Continue where you left off` if started
- both navigate to step player at computed resume step.

### E) Step player screen
1. `GuideStepPlayerScreen` + `GuideStepPlayerViewModel`.
2. Show:
- current step prominently
- next-step preview below
- section progress bar + percentage (`LinearProgressIndicator`)
3. Actions:
- mark step complete/incomplete
- prev/next step
- auto-update section completed state, guide started flag, and resume pointer
4. Save every change through `UpdateItemUseCase`.

## Progress and Resume Logic
1. `started = true` on first Start tap or first completion change.
2. `resume` points to first incomplete leaf step in order; if none, section complete.
3. Section progress:
- `completedLeafSteps / totalLeafSteps` for active section.
4. CTA routing:
- Start -> first step of first section.
- Continue -> `resume` target, fallback to first incomplete, else final step.

## Firestore Rules / Indexes
1. Security rules:
- Family-shared guides readable/writable by family members.
- Private guides readable/writable only by `ownerUid`.
2. Composite indexes (required):
- `(familyId, visibility)`
- `(familyId, visibility, ownerUid)`

## Tests and Validation Scenarios
1. Unit tests:
- schema normalization (`completed` vs `isCompleted`, subsection object/list)
- resume calculation
- section progress calculation
- visibility toggle state transitions
2. DAO/converter tests:
- `Guide <-> GuideEntity` roundtrip with nested steps/substeps.
3. ViewModel tests:
- overview list load
- add option dialog state
- manual save path
- JSON import success/error handling
- details toggle and step completion persistence
4. UI tests:
- Add button opens two upload options
- JSON import screen shows format example + download action
- Start/Continue text behavior
- step player progress bar updates
5. Sync tests:
- merged shared + private listener results in correct Room state
- deletion sync when guides removed remotely

## Assumptions and Defaults
1. Private guides are private within the same family context (`familyId` still present), not global across families.
2. Only the guide owner can change visibility in details.
3. Import supports single guide JSON object or array of guides.
4. Imported guides regenerate IDs to avoid collisions.
5. Existing style is preserved: observer use case for Firestore->Room sync, feature VMs reading Room via existing fetch use cases, and mutations via save/update use cases.
