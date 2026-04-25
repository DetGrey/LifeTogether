# Phase 5 — ViewModel Normalization

**Status:** Implementing _(Not started → Grill-me in progress → Implementing → Complete)_

## Goal

Normalise all feature ViewModels to expose a single `UiState` object for persistent screen state and a `Channel<UiCommand>` for fire-and-forget commands. Remove scattered public mutable fields, ad hoc setup flags, and ad hoc setup call patterns.

## Scope

- All ViewModels under `ui/`
- Priority targets: `RecipesViewModel`, `RecipeDetailsViewModel`, `FamilyViewModel`, `ProfileViewModel`, `GuidesViewModel`
- Good reference models already in codebase: `GalleryViewModel`, `ListEntryDetailsViewModel`
- Any remaining `println(...)` calls in ViewModels and screens (replace with `Log.d`)

## Key Decisions Already Made

- Each ViewModel exposes **one main `UiState` data class** as a `StateFlow`.
- One-off commands (navigation triggers, snackbar messages) use a **`Channel<UiCommand>` exposed as a `Flow`** — avoids rotation bugs and state-clearing boilerplate.
- No public mutable fields: dialog flags, raw error strings, ad hoc IDs, and filter selections must move into `UiState` or `UiCommand`.
- No ad hoc setup calls like `setUpRecipes(familyId)` — prefer clean route-prepared input and stable `init`-based initialisation.
- If extra form state is needed it lives in a clearly paired form state model, not scattered mutable fields.
- Blocking confirmation state (e.g. delete confirmation dialogs currently rendered) may stay in `UiState` — it is part of the rendered UI.
- Delete any remaining passthrough use cases encountered during this phase.
- When making subphases and discussing the details for each subphase, we have to go through every ViewModel one for one to make sure we don't forget to refactor and normalise something
- Later phases should grill ViewModels one at a time where needed so each one can be normalized to the smallest contract it actually needs, instead of forcing `UiCommand` or other structure where it is not useful
- If a class named `ViewModel` does not actually need to remain a ViewModel, Phase 5 may convert it into a pure helper, merge it into another ViewModel, or replace it with another class/function shape that better fits the responsibility
- Temporary fixes are avoided by default; keep them only when there is truly no better solution and they are clearly marked as such during the phase
- Minor Phase 3 carry-over fixes that move overlooked render state into `UiState` are allowed during this phase when they are part of the same ViewModel normalization work
- Empty-shell ViewModels should be treated as removal candidates first, not automatically normalized just because they exist
- Feature-specific route navigation commands may stay separate from shared `UiCommand` when they are only intended for a single route to handle
- Remaining feature ViewModels that still use `setUp(...)` should prefer route-prepared initialization or `SavedStateHandle`; keep `setUp(...)` only when there is no cleaner alternative
- Phase 5 should not reopen or rename the already-normalized Phase 3 screen/route contracts unless a concrete ViewModel normalization bug requires it
- During the ViewModel grill-me for this phase, do not advance to the next ViewModel until the user explicitly says to continue

## Subphase Notes

### 5.0 Shared Utility ViewModels

- We have some shared ViewModels like observer, notification and session which we should see if can be done in a better way
- `RootCoordinatorViewModel`, `NotificationViewModel`, and `ImageViewModel` are utility/orchestration ViewModels and should be normalized only to the extent their responsibilities actually need
- `RootCoordinatorViewModel` should be kept as a coordinator, but its public surface should stay minimal and should not be forced into a screen-style contract
- `NotificationViewModel` should be removed and replaced with a more appropriate helper or use case because it does not own UI state
- All routes should move off `ImageViewModel` and use the shared image use case / image-state contract instead; `ImageViewModel` is only a temporary compatibility layer until that migration is complete
- Shared image observation should treat missing image input (`null` / blank / no URL) as a valid empty state rather than a fetch error where possible
- Shared image observation should return a richer state than nullable bitmap alone so callers can distinguish loading, empty, loaded, and error states
- Shared image data should stay on raw bytes at the helper/domain boundary; routes can convert to `Bitmap` for display
- The shared image use case should return `ByteArray`-based state, not `Bitmap`
- The shared image use case should return a sealed `ImageState` contract with explicit loading, empty, loaded, and error states
- Missing or blank image input should map to `ImageState.Empty`, but a configured image that fails to load should surface as `ImageState.Error`
- `RootCoordinatorViewModel` should not own observer acquire/release pass-throughs if `SyncCoordinator` can expose that API directly.
- Keep the root coordinator as the shared sync/session orchestration surface until the sync lifecycle abstraction is finalized; do not spread sync-state plumbing into routes.
- `RootCoordinatorViewModel` should keep session observation, but sync scheduling work such as guide progress polling and FCM token writes should move into `SyncCoordinator` so the root coordinator becomes a thin session-driven orchestrator.
- `RootCoordinatorViewModel.sessionState` is only consumed by `GalleryGraphObserverRoute`, so the proxy should be removed and that route should read `SessionRepository.sessionState` directly instead.
- `SyncLifecycle.kt` should read sync state directly from `SyncCoordinator`, not through `RootCoordinatorViewModel`, so the root coordinator stops proxying shared sync state.
- `NotificationViewModel` should be replaced with a shared `SendNotificationUseCase` that fetches tokens and delegates Android notification plumbing to an injected `NotificationService` with `@ApplicationContext`, so callers never pass `Context`.

### 5.1 Recipe ViewModels

- `RecipesViewModel` is already on the target `UiState` + `UiEvent` + `UiCommand` shape.
- `RecipesViewModel` may keep a private `allRecipes` cache if that remains the cleanest way to derive the visible list.
- `RecipeDetailsViewModel` should remove `setUp(recipeId)` and move `recipeId` into `SavedStateHandle` if the route can supply it cleanly.
- `RecipeDetailsViewModel` should keep ownership of save/delete/edit state.
- `RecipeDetailsViewModel` should keep `RecipeDetailsCommand.NavigateBack` as route-only command handling.
- `RecipeDetailsViewModel` should drop `onImageError(...)` once image loading moves out of the ViewModel.
- `RecipeDetailsViewModel` should keep the snapshot-based edit reset approach unless a concrete bug or duplication forces a different model.
- `RecipeDetailsViewModel` should keep `saveRecipe()` as a single readable method unless a concrete need for splitting appears later.
- `RecipeDetailsViewModel` should keep `createContentState()` as the canonical content builder.
- Recipe image handling should move to the shared image use case directly from the route, not via `ImageViewModel`.

### 5.2 Family/Auth ViewModels

- `FamilyViewModel` already has the right general state/command split; focus on cleanup rather than redesign.
- `FamilyRoute` should move off `ImageViewModel` and use the shared image use case / image-state contract instead.
- `FamilyViewModel` should keep confirmation dialogs in `UiState` because they are rendered state.
- `FamilyViewModel` should keep the current cancellable family-information job pattern for now.
- `FamilyCommand.CopyFamilyId` and `FamilyCommand.NavigateBack` may stay feature-specific route commands if they remain single-route concerns.
- `HomeViewModel` and `LoadingViewModel` should be deleted once routes read `SessionRepository.sessionState` directly
- TODO for `SignUpViewModel`: validate `password` and `confirmPassword` locally before `SignUpUseCase`, but do not change that logic in this phase so only add it as a comment.
- `LoginViewModel` stays as-is for Phase 5 unless a concrete bug shows up later.
- `SettingsViewModel` should keep the current state ownership and confirmation-dialog flow in the ViewModel.
- `ProfileViewModel.newName` should stay as a simple top-level field in `ProfileUiState` for now.

### 5.3 Guides, Grocery, and Lists ViewModels

- `GuidesViewModel` should keep the import UI state, but the JSON parsing and per-guide persistence should move into a dedicated `ImportGuidesUseCase`.
- `ImportGuidesUseCase` should return a structured result with success and failure counts.
- `GuideCreateViewModel` should split `saveGuide()` into smaller helpers.
- `GuideCreateViewModel.isSaving` should be removed because it is unused.
- `GuideDetailsViewModel` should remove `Initialize` / `setUp(guideId)` and move `guideId` to `SavedStateHandle`, while keeping the ownership-check helpers separate for now.
- `GuideDetailsViewModel.canToggleAmountState` should remain in `UiState` for now.
- `GuideDetailsRoute` should stop calling `Initialize(guideId)` and let `GuideDetailsViewModel` receive the id through `SavedStateHandle`.
- `GuideDetailsViewModel` should keep the guide observation job, but simplify it into a single `observeGuide()` path keyed off the saved guide id.
- `GuideStepPlayerViewModel` should also drop `Initialize` / `setUp(guideId)` and move `guideId` to `SavedStateHandle`.
- `GroceryListViewModel` should collapse the add-bar fields into the shared nested `AddNewListItemUiState`.
- `GroceryListViewModel` should keep the grocery-specific state together in one ViewModel for now.
- `GroceryListUiState.isLoading` should be removed because it is unused.
- `GroceryListUiState.expectedTotalPrice` should remain precomputed in `UiState`.
- `GroceryListUiState.currentGrocerySuggestions` should remain precomputed in `UiState`.
- `AdminGrocerySuggestionsViewModel` should collapse its add/edit draft fields into the shared `AddNewListItemUiState`.
- `AdminGrocerySuggestionsViewModel.isEditMode` should be removed because it duplicates `editingSuggestionId` state.
- `AdminGrocerySuggestionsViewModel` should keep `showDeleteCategoryConfirmationDialog` separate from `selectedSuggestion` for now.
- `AdminGroceryCategoriesViewModel` should keep its current separate delete-dialog flag and selected category state for now.
- `AdminGroceryCategoriesViewModel` should keep the current free-form string add-category flow for now.
- `GroceryListViewModel` should refactor `setUpGroceryList()` into a single cancellable setup path instead of launching repeated collectors on each new family id.
- `GalleryViewModel` should refactor `observeAlbums()` into a single cancellable setup path instead of launching repeated collectors on each new family id.
- `ListsViewModel` should group the create-dialog fields into a nested create form model.
- `ListsViewModel` should explicitly cancel `listsJob` and clear `userLists` on logout/session loss.
- `ListsViewModel` should keep `ListsCommand.NavigateToListDetails` for create-success navigation.
- `ListDetailsViewModel` should move `listId` to `SavedStateHandle` and remove `setUp(...)`.
- `ListDetailsViewModel` should keep the bitmap cache/job map in the ViewModel for now.
- `ListEntryDetailsViewModel` should move `listId` and `entryId` to `SavedStateHandle` and remove `setUp(..., onLoadFailed)`.
- `ListEntryDetailsViewModel` should keep `pendingImageUri` and `pendingImageBitmap` inside `EntryFormState` for now.
- `ListEntryDetailsViewModel` should emit a feature-specific command on successful create instead of taking an `onDone` callback.
- `ListEntryDetailsViewModel` should keep the snapshot-based discard/reset approach for now.

### 5.4 Gallery and Tip Tracker ViewModels

- The media-upload cleanup for `MediaUploadViewModel` should also include a cleanup pass on `UploadGalleryMediaItemsUseCase`, because its current `invoke` is too long to absorb the moved prep logic without refactoring
- `UploadGalleryMediaItemsUseCase` should keep its public API but split the long `invoke(...)` into small private helpers for validation, per-item upload, metadata save, and failure aggregation
- `MediaUploadViewModel` should be removed as soon as the surrounding files stop using it; do not keep it around after the upload dialog and parent screen state refactor is complete
- `MediaUploadMultipleDialog` should keep its own confirm button and emit the selected URIs upward on confirm.
- `AlbumDetailsViewModel` should use the existing shared `UploadState` as-is for now.
- `AlbumDetailsViewModel` should move `albumId` to `SavedStateHandle` and remove `setUp(albumId)`.
- `MediaDetailsViewModel` should move `albumId` and `initialIndex` to `SavedStateHandle` and remove `setUp(...)`.
- `TipTrackerViewModel` should refactor `observeTips()` into a single cancellable setup path instead of launching repeated collectors on each new family id.
- `VideoPlayerViewModel` should be removed and `DisplayVideoFromUri` should own `ExoPlayer` directly as a composable-managed resource instead.

### 5.5 Local Component and Helper ViewModels

- `AddNewListItemViewModel` and `AddNewIngredientViewModel` should be converted into plain state/data holders or moved into their parent composables if they do not need lifecycle ownership
- Local helper ViewModels may stay as state holders if they truly need local interactive state, but they should not be forced to expose `UiCommand` unless they actually emit one-shot UI effects
- Upload dialogs should prefer local dialog state plus parent screen ViewModel upload-state ownership instead of a dedicated shared `ImageUploadViewModel` wrapper when the wrapper only forwards a single upload use case
- `AddNewListItemViewModel` should become a single `UiState` model or be moved into the parent screen state instead of exposing many separate mutable fields
- `ConfirmationDialogWithDropdown` should be generic over the option type and format labels through lambdas so `AddNewListItemUiState` can keep a real `Category` selection instead of string labels
- `AddNewListItemUiState` should stay mode-free and hold the shared form fields only; the caller decides whether the action is Add or Save
- `AddNewListItemUiState.selectedCategory` should default to `UNCATEGORIZED_CATEGORY`; that category should also be the first item in the list when present
- The shared add-item form state should live in the `ui/common/add/` model file instead of being duplicated in feature packages
- The legacy helper-based `AddNewListItem` implementation should be replaced outright now
- `ImageUploadViewModel` and `MediaUploadViewModel` may stay as lightweight lifecycle-owned helpers only if they truly need that ownership; otherwise they are candidates for collapse into smaller abstractions

### 5.6 Logging Cleanup

- Logging cleanup: all `println(...)` in ViewModels and screens should be replaced with `Log.d(...)`

## Subphases

_To be finalised during the pre-implementation grill-me session._

- [ ] 5.0 Shared utility foundation:
  - `RootCoordinatorViewModel`
  - `NotificationViewModel`
  - `ImageViewModel`
  - shared sync/image/notification cleanup
  - keep this as its own cross-cutting issue, separate from the feature ViewModel subphases below
- [ ] 5.1 Review and trim recipe ViewModels:
  - `RecipesViewModel`
  - `RecipeDetailsViewModel`
  - verify the pair already follows the target `UiState` / `UiEvent` / `UiCommand` shape
  - remove any remaining setup/state ownership that is still ad hoc
  - prefer `SavedStateHandle` for `RecipeDetailsViewModel` route arguments and remove `setUp(recipeId)` if the route can supply the argument cleanly
- [ ] 5.2 Normalise family/auth ViewModels:
  - `FamilyViewModel`
  - `ProfileViewModel`
  - `LoginViewModel`
  - `SignUpViewModel`
  - `SettingsViewModel`
  - `LoadingViewModel`
- [ ] 5.3 Normalise guides/grocery/list ViewModels:
  - `GuidesViewModel`
  - `GuideCreateViewModel`
  - `GuideDetailsViewModel`
  - `GuideStepPlayerViewModel`
  - `GroceryListViewModel`
  - `AdminGroceryCategoriesViewModel`
  - `AdminGrocerySuggestionsViewModel`
  - `ListsViewModel`
  - `ListDetailsViewModel`
  - `ListEntryDetailsViewModel`
- [ ] 5.4 Normalise gallery/tip tracker helper ViewModels:
  - `GalleryViewModel`
  - `AlbumDetailsViewModel`
  - `MediaDetailsViewModel`
  - `TipTrackerViewModel`
- [ ] 5.5 Normalise local component/helper ViewModels:
  - `AddNewIngredientViewModel`
  - `AddNewListItemViewModel`
  - `ImageUploadViewModel`
  - `MediaUploadViewModel`
  - `VideoPlayerViewModel`
  - `ImageViewModel`
  - `NotificationViewModel`
  - `RootCoordinatorViewModel`
  - `HomeViewModel`
  - `LoadingViewModel`
- [ ] 5.6 Logging cleanup across ViewModels and screens

## Before Starting This Phase

> **[Run `/grill-me`](../../skills/grill-me/grill-me.md)** with this file to stress-test the plan, finalise the subphases above, and fill in the sections below before writing any code.
>
> All **Open Questions** at the bottom of this file must be answered and the section removed before implementation begins.

### Acceptance criteria
- [ ] Every in-scope ViewModel exposes one clear `UiState` owner or is intentionally removed/merged into a better abstraction.
- [ ] Feature ViewModels that need route arguments use `SavedStateHandle` or clean route-prepared initialization instead of ad hoc `setUp(...)` calls.
- [ ] No in-scope ViewModel exposes public mutable fields for dialogs, IDs, filters, or other screen state.
- [ ] One-off UI effects use `UiCommand` or a feature-specific command only where that is actually needed.
- [ ] Shared utility concerns use the shared foundation layer decided in `5.0` instead of per-screen ad hoc wiring.
- [ ] `NotificationViewModel`, `VideoPlayerViewModel`, `AddNewListItemViewModel`, and `AddNewIngredientViewModel` are gone if their responsibilities can be moved to better abstractions.
- [ ] `ImageViewModel` and `MediaUploadViewModel` are either removed or clearly reduced to the smallest temporary compatibility layer needed for the phase.
- [ ] `println(...)` calls are removed from ViewModels and screens.
- [ ] The refactors preserve the existing visible behavior of the screens that remain in scope.
- [ ] The phase file contains one clear note block per subphase instead of one long mixed decision list.

### Test cases
- [ ] Open the shared utility screens and verify the coordinator/notification/image flows still work after the `5.0` cleanup.
- [ ] Open `RecipeDetails`, edit a recipe, and verify route-argument handling, save, discard, and navigation still behave correctly.
- [ ] Open `Family`, `Profile`, `Login`, `SignUp`, `Settings`, and `Loading` and verify the session-driven flows still match the current app behavior.
- [ ] Open `Guides`, create a guide, edit guide details, and step through the player to verify the saved-state route arguments and commands work as expected.
- [ ] Open `GroceryList`, `AdminGroceryCategories`, and `AdminGrocerySuggestions` and verify create/edit/delete flows still behave the same after the shared form-state cleanup.
- [ ] Open `Lists`, `ListDetails`, and `ListEntryDetails` and verify list creation, list details, selection state, image handling, and entry creation/edit flows still work.
- [ ] Open `Gallery`, `AlbumDetails`, `MediaDetails`, and `TipTracker` and verify the session-gated setup paths, cached presentation state, and upload/delete flows still behave correctly.
- [ ] Verify the helper components still render correctly after removing `AddNewListItemViewModel`, `AddNewIngredientViewModel`, `ImageUploadViewModel`, `MediaUploadViewModel`, and `VideoPlayerViewModel`.
- [ ] Run the app build and confirm the relevant modules still compile after the ViewModel normalization pass.

## GitHub Issues

Create milestone `Phase 5: ViewModel Normalization` and the following issues assigned to it:

- `[Phase 5] Shared utility foundation — RootCoordinator, Notification, Image`
- `[Phase 5] Normalise RecipesViewModel and RecipeDetailsViewModel`
- `[Phase 5] Normalise FamilyViewModel`
- `[Phase 5] Normalise ProfileViewModel`
- `[Phase 5] Normalise GuidesViewModel`
- `[Phase 5] Normalise remaining feature ViewModels`
- `[Phase 5] Logging cleanup — ViewModels and screens`
