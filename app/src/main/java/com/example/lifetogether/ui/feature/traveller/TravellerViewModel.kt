package com.example.lifetogether.ui.feature.traveller

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.model.traveller.PinType
import com.example.lifetogether.domain.model.traveller.TravellerPin
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.repository.TravellerRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.usecase.gallery.GetAlbumDisplayModelsUseCase
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import com.example.lifetogether.ui.common.snackbar.SnackbarSeverity
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "TravellerViewModel"
private const val SEARCH_DEBOUNCE_MS = 300L

@HiltViewModel(assistedFactory = TravellerViewModel.Factory::class)
class TravellerViewModel @AssistedInject constructor(
    @Assisted val filterAlbumId: String?,
    private val sessionRepository: SessionRepository,
    private val travellerRepository: TravellerRepository,
    private val placesClient: PlacesClient,
    private val getAlbumDisplayModelsUseCase: GetAlbumDisplayModelsUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(filterAlbumId: String?): TravellerViewModel
    }

    private val _uiState = MutableStateFlow<TravellerUiState>(TravellerUiState.Loading)
    val uiState: StateFlow<TravellerUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<TravellerCommand>(Channel.BUFFERED)
    val commands: Flow<TravellerCommand> = _commands.receiveAsFlow()

    private var familyId: String? = null
    private var observePinsJob: Job? = null
    private var observeAlbumsJob: Job? = null
    private var searchJob: Job? = null
    private var searchToken = AutocompleteSessionToken.newInstance()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                if (newFamilyId != null && newFamilyId != familyId) {
                    familyId = newFamilyId
                    observePins(newFamilyId)
                    observeAlbums(newFamilyId)
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                    observePinsJob?.cancel()
                    observeAlbumsJob?.cancel()
                    _uiState.value = TravellerUiState.Error("Not authenticated")
                }
            }
        }
    }

    private fun observePins(familyId: String) {
        observePinsJob?.cancel()
        observePinsJob = viewModelScope.launch {
            travellerRepository.observePins(familyId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val current = (_uiState.value as? TravellerUiState.Loaded)?.content
                            ?: TravellerContent(filterAlbumId = filterAlbumId)
                        _uiState.value = TravellerUiState.Loaded(
                            current.copy(pins = result.data),
                        )
                    }
                    is Result.Failure -> {
                        _uiState.value = TravellerUiState.Error(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun observeAlbums(familyId: String) {
        observeAlbumsJob?.cancel()
        observeAlbumsJob = viewModelScope.launch {
            getAlbumDisplayModelsUseCase(familyId).collect { result ->
                if (result is Result.Success) {
                    updateContent { it.copy(albums = result.data) }
                }
            }
        }
    }

    fun onEvent(event: TravellerUiEvent) {
        when (event) {
            is TravellerUiEvent.FilterToggled -> updateContent { content ->
                val updated = if (event.type in content.activeFilters) {
                    content.activeFilters - event.type
                } else {
                    content.activeFilters + event.type
                }
                content.copy(activeFilters = updated)
            }

            is TravellerUiEvent.PinTapped -> updateContent { content ->
                val visiblePins = if (content.filterAlbumId != null) {
                    content.pins.filter { it.albumId == content.filterAlbumId }
                } else {
                    content.pins.filter { it.type in content.activeFilters }
                }
                val sameCityPins = visiblePins.filter {
                    it.city == event.pin.city && it.country == event.pin.country
                }.sortedBy { it.dateFrom?.time ?: 0L }

                if (sameCityPins.size > 1) {
                    content.copy(groupedPins = sameCityPins, sheetMode = PinSheetMode.ViewingGroup)
                } else {
                    content.copy(selectedPin = event.pin, sheetMode = PinSheetMode.Viewing)
                }
            }

            is TravellerUiEvent.AddPinClicked -> {
                searchToken = AutocompleteSessionToken.newInstance()
                updateContent { it.copy(sheetMode = PinSheetMode.Searching, addSearchQuery = "", addSearchResults = emptyList()) }
            }

            is TravellerUiEvent.SearchQueryChanged -> {
                updateContent { it.copy(addSearchQuery = event.query) }
                searchJob?.cancel()
                if (event.query.isBlank()) {
                    updateContent { it.copy(addSearchResults = emptyList()) }
                    return
                }
                searchJob = viewModelScope.launch {
                    delay(SEARCH_DEBOUNCE_MS.milliseconds)
                    searchPlaces(event.query)
                }
            }

            is TravellerUiEvent.PlaceSelected -> {
                viewModelScope.launch {
                    fetchPlaceDetails(event.result)
                }
            }

            is TravellerUiEvent.DraftTypeChanged -> updateContent { content ->
                val draft = content.draftPin ?: return@updateContent content
                content.copy(
                    draftPin = draft.copy(
                        type = event.type,
                        singleDay = if (event.type == PinType.LIVED) false else draft.singleDay,
                        stillLivingHere = if (event.type != PinType.LIVED) false else draft.stillLivingHere,
                        dateTo = if (event.type == PinType.BUCKET_LIST) null else draft.dateTo,
                    ),
                )
            }

            is TravellerUiEvent.DraftDateFromChanged -> updateContent { content ->
                val draft = content.draftPin ?: return@updateContent content
                val newDraft = if (draft.singleDay) {
                    draft.copy(dateFrom = event.date, dateTo = event.date)
                } else {
                    draft.copy(dateFrom = event.date)
                }
                content.copy(draftPin = newDraft)
            }

            is TravellerUiEvent.DraftDateToChanged -> updateContent { content ->
                val draft = content.draftPin ?: return@updateContent content
                content.copy(draftPin = draft.copy(dateTo = event.date))
            }

            is TravellerUiEvent.DraftSingleDayToggled -> updateContent { content ->
                val draft = content.draftPin ?: return@updateContent content
                val newDraft = if (event.checked) {
                    draft.copy(singleDay = true, dateTo = draft.dateFrom)
                } else {
                    draft.copy(singleDay = false, dateTo = null)
                }
                content.copy(draftPin = newDraft)
            }

            is TravellerUiEvent.DraftStillLivingHereToggled -> updateContent { content ->
                val draft = content.draftPin ?: return@updateContent content
                content.copy(draftPin = draft.copy(stillLivingHere = event.checked, dateTo = null))
            }

            is TravellerUiEvent.DraftAlbumChanged -> updateContent { content ->
                val draft = content.draftPin ?: return@updateContent content
                content.copy(draftPin = draft.copy(albumId = event.albumId))
            }

            is TravellerUiEvent.SaveDraftPin -> savePin()

            is TravellerUiEvent.EditPinClicked -> updateContent { content ->
                val pin = content.selectedPin ?: return@updateContent content
                content.copy(
                    sheetMode = PinSheetMode.Editing,
                    draftPin = DraftPin(
                        id = pin.id,
                        city = pin.city,
                        country = pin.country,
                        latitude = pin.latitude,
                        longitude = pin.longitude,
                        type = pin.type,
                        dateFrom = pin.dateFrom,
                        dateTo = pin.dateTo,
                        singleDay = pin.dateFrom != null && pin.dateTo == pin.dateFrom,
                        stillLivingHere = pin.type == PinType.LIVED && pin.dateTo == null,
                        albumId = pin.albumId,
                    ),
                )
            }

            is TravellerUiEvent.DeletePinClicked -> updateContent { it.copy(showDeleteConfirmation = true) }

            is TravellerUiEvent.ConfirmDeletePin -> deletePin()

            is TravellerUiEvent.SelectPinFromGroup -> updateContent { content ->
                content.copy(selectedPin = event.pin, sheetMode = PinSheetMode.Viewing)
            }

            is TravellerUiEvent.BackToGroup -> updateContent { content ->
                content.copy(selectedPin = null, sheetMode = PinSheetMode.ViewingGroup)
            }

            is TravellerUiEvent.DismissSheet -> updateContent { content ->
                content.copy(
                    sheetMode = PinSheetMode.Hidden,
                    selectedPin = null,
                    groupedPins = emptyList(),
                    draftPin = null,
                    addSearchQuery = "",
                    addSearchResults = emptyList(),
                    showDeleteConfirmation = false,
                )
            }

            is TravellerUiEvent.DismissAlbumFilter -> updateContent { it.copy(filterAlbumId = null) }

            is TravellerUiEvent.RequestAlbumPicker -> updateContent { it.copy(showAlbumPicker = true) }

            is TravellerUiEvent.DismissAlbumPicker -> updateContent { it.copy(showAlbumPicker = false) }

            is TravellerUiEvent.AlbumLinkClicked -> {
                _commands.trySend(TravellerCommand.NavigateToAlbum(event.albumId))
            }
        }
    }

    private fun savePin() {
        val content = (_uiState.value as? TravellerUiState.Loaded)?.content ?: return
        val draft = content.draftPin ?: return
        val fid = familyId ?: return

        if (draft.city.isBlank()) {
            _uiCommands.trySend(UiCommand.ShowSnackbar(message = "City is required", severity = SnackbarSeverity.Error))
            return
        }
        if (draft.type == PinType.LIVED && draft.dateFrom == null) {
            _uiCommands.trySend(UiCommand.ShowSnackbar(message = "Start date is required for a lived-in location", severity = SnackbarSeverity.Error))
            return
        }

        val pin = TravellerPin(
            id = draft.id ?: UUID.randomUUID().toString(),
            familyId = fid,
            city = draft.city,
            country = draft.country,
            latitude = draft.latitude,
            longitude = draft.longitude,
            type = draft.type,
            dateFrom = draft.dateFrom,
            dateTo = when {
                draft.type == PinType.LIVED && draft.stillLivingHere -> null
                draft.singleDay -> draft.dateFrom
                else -> draft.dateTo
            },
            albumId = draft.albumId,
        )

        updateContent { it.copy(sheetMode = PinSheetMode.Hidden, draftPin = null, selectedPin = null) }

        viewModelScope.launch {
            val result = travellerRepository.savePin(pin)
            if (result is Result.Failure) {
                _uiCommands.trySend(
                    UiCommand.ShowSnackbar(message = result.error.toUserMessage(), severity = SnackbarSeverity.Error),
                )
            }
        }
    }

    private fun deletePin() {
        val content = (_uiState.value as? TravellerUiState.Loaded)?.content ?: return
        val pin = content.selectedPin ?: return
        val fid = familyId ?: return

        viewModelScope.launch {
            when (val result = travellerRepository.deletePin(fid, pin.id)) {
                is Result.Success -> {
                    updateContent {
                        it.copy(
                            sheetMode = PinSheetMode.Hidden,
                            selectedPin = null,
                            showDeleteConfirmation = false,
                        )
                    }
                }
                is Result.Failure -> {
                    updateContent { it.copy(showDeleteConfirmation = false) }
                    _uiCommands.trySend(
                        UiCommand.ShowSnackbar(message = result.error.toUserMessage(), severity = SnackbarSeverity.Error),
                    )
                }
            }
        }
    }

    private suspend fun searchPlaces(query: String) {
        try {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(searchToken)
                .setTypesFilter(listOf("(cities)"))
                .build()
            val response = placesClient.findAutocompletePredictions(request).await()
            val results = response.autocompletePredictions.map { prediction ->
                val parts = prediction.getFullText(null).toString().split(", ")
                PlaceResult(
                    placeId = prediction.placeId,
                    city = parts.firstOrNull() ?: prediction.getPrimaryText(null).toString(),
                    country = parts.lastOrNull() ?: "",
                    latitude = 0.0,
                    longitude = 0.0,
                )
            }
            updateContent { it.copy(addSearchResults = results) }
        } catch (e: Exception) {
            Log.e(TAG, "Places autocomplete failed: ${e.message}")
        }
    }

    private fun fetchPlaceDetails(result: PlaceResult) {
        viewModelScope.launch {
            try {
                val fields = listOf(Place.Field.LOCATION, Place.Field.ADDRESS_COMPONENTS)
                val request = FetchPlaceRequest.newInstance(result.placeId, fields)
                val response = placesClient.fetchPlace(request).await()
                val place = response.place
                val latLng = place.location ?: return@launch

                val components = place.addressComponents?.asList() ?: emptyList()
                val city = components.firstOrNull { c ->
                    c.types.contains("locality") || c.types.contains("administrative_area_level_1")
                }?.name ?: result.city
                val country = components.firstOrNull { c ->
                    c.types.contains("country")
                }?.name ?: result.country

                val fullResult = result.copy(
                    city = city,
                    country = country,
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                )
                updateContent { content ->
                    content.copy(
                        sheetMode = PinSheetMode.Creating,
                        draftPin = DraftPin(
                            city = fullResult.city,
                            country = fullResult.country,
                            latitude = fullResult.latitude,
                            longitude = fullResult.longitude,
                        ),
                        addSearchResults = emptyList(),
                        addSearchQuery = "",
                    )
                }
                searchToken = AutocompleteSessionToken.newInstance()
            } catch (e: Exception) {
                Log.e(TAG, "Place details fetch failed: ${e.message}")
            }
        }
    }

    private fun updateContent(transform: (TravellerContent) -> TravellerContent) {
        _uiState.update { state ->
            when (state) {
                is TravellerUiState.Loaded -> TravellerUiState.Loaded(transform(state.content))
                else -> state
            }
        }
    }
}
