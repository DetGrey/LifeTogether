package com.example.lifetogether.ui.feature.traveller

import com.example.lifetogether.domain.model.traveller.PinType
import com.example.lifetogether.domain.model.traveller.TravellerPin
import com.example.lifetogether.ui.model.AlbumUiModel
import java.util.Date

sealed interface TravellerUiState {
    data object Loading : TravellerUiState
    data class Loaded(val content: TravellerContent) : TravellerUiState
    data class Error(val message: String) : TravellerUiState
}

data class TravellerContent(
    val pins: List<TravellerPin> = emptyList(),
    val activeFilters: Set<PinType> = PinType.entries.toSet(),
    val filterAlbumId: String? = null,
    val selectedPin: TravellerPin? = null,
    val groupedPins: List<TravellerPin> = emptyList(),
    val sheetMode: PinSheetMode = PinSheetMode.Hidden,
    val addSearchQuery: String = "",
    val addSearchResults: List<PlaceResult> = emptyList(),
    val draftPin: DraftPin? = null,
    val showDeleteConfirmation: Boolean = false,
    val showAlbumPicker: Boolean = false,
    val albums: List<AlbumUiModel> = emptyList(),
)

sealed interface PinSheetMode {
    data object Hidden : PinSheetMode
    data object Searching : PinSheetMode
    data object ViewingGroup : PinSheetMode
    data object Viewing : PinSheetMode
    data object Creating : PinSheetMode
    data object Editing : PinSheetMode
}

data class DraftPin(
    val id: String? = null,
    val city: String = "",
    val country: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val type: PinType = PinType.VISITED,
    val dateFrom: Date? = null,
    val dateTo: Date? = null,
    val singleDay: Boolean = false,
    val stillLivingHere: Boolean = false,
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
    data class AlbumLinkClicked(val albumId: String) : TravellerUiEvent
    data class SelectPinFromGroup(val pin: TravellerPin) : TravellerUiEvent
    data object BackToGroup : TravellerUiEvent
    data object RequestAlbumPicker : TravellerUiEvent
    data object DismissAlbumPicker : TravellerUiEvent
}

sealed interface TravellerNavigationEvent {
    data object NavigateBack : TravellerNavigationEvent
}

sealed interface TravellerCommand {
    data class NavigateToAlbum(val albumId: String) : TravellerCommand
}
