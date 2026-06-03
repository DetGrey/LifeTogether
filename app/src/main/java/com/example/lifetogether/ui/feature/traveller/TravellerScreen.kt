package com.example.lifetogether.ui.feature.traveller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.traveller.PinType
import com.example.lifetogether.domain.model.traveller.TravellerPin
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.button.AddButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.model.AlbumUiModel
import com.example.lifetogether.ui.model.ColorPair
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.text.TextLabel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.text.style.TextAlign
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun rememberDateFormat() = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravellerScreen(
    uiState: TravellerUiState,
    onUiEvent: (TravellerUiEvent) -> Unit,
    onNavigationEvent: (TravellerNavigationEvent) -> Unit,
) {
    when (uiState) {
        is TravellerUiState.Loading -> TravellerLoadingContent(onNavigationEvent)
        is TravellerUiState.Error -> TravellerErrorContent(uiState.message, onNavigationEvent)
        is TravellerUiState.Loaded -> TravellerLoadedContent(
            content = uiState.content,
            onUiEvent = onUiEvent,
            onNavigationEvent = onNavigationEvent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravellerLoadingContent(onNavigationEvent: (TravellerNavigationEvent) -> Unit) {
    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(R.drawable.ic_back, "back arrow icon"),
                onLeftClick = { onNavigationEvent(TravellerNavigationEvent.NavigateBack) },
                text = "Traveller",
            )
        },
    ) { padding ->
        Skeletons.ListDetail(modifier = Modifier.fillMaxSize().padding(padding))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravellerErrorContent(
    message: String,
    onNavigationEvent: (TravellerNavigationEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(R.drawable.ic_back, "back arrow icon"),
                onLeftClick = { onNavigationEvent(TravellerNavigationEvent.NavigateBack) },
                text = "Traveller",
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(LifeTogetherTokens.spacing.medium),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextDefault(message)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravellerLoadedContent(
    content: TravellerContent,
    onUiEvent: (TravellerUiEvent) -> Unit,
    onNavigationEvent: (TravellerNavigationEvent) -> Unit,
) {
    val pinTypeColors = mapOf(
        PinType.VISITED to ColorPair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary),
        PinType.LIVED to ColorPair(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary),
        PinType.BUCKET_LIST to ColorPair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary),
    )

    val visiblePins = remember(content.pins, content.activeFilters, content.filterAlbumId) {
        if (content.filterAlbumId != null) {
            content.pins.filter { it.albumId == content.filterAlbumId }
        } else {
            content.pins.filter { it.type in content.activeFilters }
        }
    }

    val polylineGroups = remember(visiblePins) {
        visiblePins
            .filter { it.albumId != null }
            .groupBy { it.albumId }
            .values
            .filter { it.size > 1 }
            .map { group -> group.sortedBy { it.dateFrom?.time ?: 0L } }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(56.0, 10.0), 5f)
    }

    val initialPin = remember(content.pins, content.filterAlbumId) {
        if (content.filterAlbumId != null) {
            content.pins
                .filter { it.albumId == content.filterAlbumId }
                .minByOrNull { it.dateFrom?.time ?: Long.MAX_VALUE }
        } else {
            content.pins.filter { it.type == PinType.LIVED }
                .let { lived ->
                    lived.firstOrNull { it.dateTo == null }
                        ?: lived.maxByOrNull { it.dateTo?.time ?: 0L }
                }
                ?: content.pins.maxByOrNull { it.lastUpdated.time }
        }
    }
    var hasAnimatedCamera by remember(content.filterAlbumId) { mutableStateOf(false) }
    LaunchedEffect(initialPin) {
        if (initialPin != null && !hasAnimatedCamera) {
            hasAnimatedCamera = true
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(initialPin.latitude, initialPin.longitude),
                    if (content.filterAlbumId != null) 8f else 6f,
                ),
            )
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(R.drawable.ic_back, "back arrow icon"),
                onLeftClick = { onNavigationEvent(TravellerNavigationEvent.NavigateBack) },
                text = "Traveller",
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false,
                    indoorLevelPickerEnabled = false,
                ),
            ) {
                visiblePins.forEach { pin ->
                    key(pin.id) {
                        val pinColor = pinTypeColors[pin.type]?.containerColor ?: MaterialTheme.colorScheme.primary
                        MarkerComposable(
                            state = rememberUpdatedMarkerState(position = LatLng(pin.latitude, pin.longitude)),
                            title = pin.city,
                            onClick = {
                                onUiEvent(TravellerUiEvent.PinTapped(pin))
                                false
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_map_pin),
                                contentDescription = null,
                                tint = pinColor,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }

                polylineGroups.forEach { group ->
                    Polyline(
                        points = group.map { LatLng(it.latitude, it.longitude) },
                        color = Color.Gray.copy(alpha = 0.5f),
                        width = 4f,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = LifeTogetherTokens.spacing.medium),
            ) {
                if (visiblePins.isNotEmpty()) {
                    val countryCount = remember(visiblePins) {
                        visiblePins.map { it.country }.toSet().size
                    }
                    TextLabel(
                        text = "${visiblePins.size} ${if (visiblePins.size == 1) "pin" else "pins"} · $countryCount ${if (countryCount == 1) "country" else "countries"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = LifeTogetherTokens.spacing.medium),
                        textAlign = TextAlign.Center
                    )
                }
                AnimatedVisibility(visible = content.filterAlbumId != null) {
                    val albumName = content.albums.firstOrNull { it.id == content.filterAlbumId }?.name
                    AlbumFilterChip(
                        albumName = albumName,
                        modifier = Modifier.padding(horizontal = LifeTogetherTokens.spacing.small),
                        onDismiss = { onUiEvent(TravellerUiEvent.DismissAlbumFilter) },
                    )
                }
                AnimatedVisibility(visible = content.filterAlbumId == null) {
                    TravellerFilterRow(
                        activeFilters = content.activeFilters,
                        onToggle = { onUiEvent(TravellerUiEvent.FilterToggled(it)) },
                        pinTypeColors = pinTypeColors,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(LifeTogetherTokens.spacing.medium),
            ) {
                AddButton(onClick = { onUiEvent(TravellerUiEvent.AddPinClicked) })
            }

            if (content.sheetMode != PinSheetMode.Hidden) {
                TravellerBottomSheet(
                    content = content,
                    onUiEvent = onUiEvent,
                )
            }

            if (content.showDeleteConfirmation) {
                ConfirmationDialog(
                    onDismiss = { onUiEvent(TravellerUiEvent.DismissSheet) },
                    onConfirm = { onUiEvent(TravellerUiEvent.ConfirmDeletePin) },
                    dialogTitle = "Delete pin?",
                    dialogMessage = "This will remove the pin permanently.",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Delete",
                )
            }
        }
    }
}

@Composable
private fun AlbumFilterChip(
    albumName: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onDismiss() }
            .padding(horizontal = LifeTogetherTokens.spacing.small, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
    ) {
        Text(
            text = if (albumName != null) "📍 $albumName" else "Trip filter",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = "dismiss filter",
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravellerBottomSheet(
    content: TravellerContent,
    onUiEvent: (TravellerUiEvent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onUiEvent(TravellerUiEvent.DismissSheet) },
        sheetState = sheetState,
    ) {
        when (content.sheetMode) {
            PinSheetMode.Searching -> SearchSheetContent(content, onUiEvent)
            PinSheetMode.ViewingGroup -> GroupViewSheetContent(content, onUiEvent)
            PinSheetMode.Viewing -> ViewPinSheetContent(content, onUiEvent)
            PinSheetMode.Creating, PinSheetMode.Editing -> EditPinSheetContent(content, onUiEvent)
            PinSheetMode.Hidden -> Unit
        }
    }
}

@Composable
private fun SearchSheetContent(
    content: TravellerContent,
    onUiEvent: (TravellerUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LifeTogetherTokens.spacing.medium),
    ) {
        TextSubHeadingMedium("Search for a city")
        Spacer(Modifier.height(LifeTogetherTokens.spacing.small))
        OutlinedTextField(
            value = content.addSearchQuery,
            onValueChange = { onUiEvent(TravellerUiEvent.SearchQueryChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("City name…") },
            singleLine = true,
        )
        Spacer(Modifier.height(LifeTogetherTokens.spacing.small))
        LazyColumn {
            items(content.addSearchResults) { result ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUiEvent(TravellerUiEvent.PlaceSelected(result)) }
                        .padding(vertical = LifeTogetherTokens.spacing.small),
                ) {
                    Text(result.city, fontWeight = FontWeight.Medium)
                    Text(result.country, style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
            }
        }
        Spacer(Modifier.height(LifeTogetherTokens.spacing.large))
    }
}

@Composable
private fun ViewPinSheetContent(
    content: TravellerContent,
    onUiEvent: (TravellerUiEvent) -> Unit,
) {
    val pin = content.selectedPin ?: return
    val dateFormat = rememberDateFormat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LifeTogetherTokens.spacing.medium)
            .padding(bottom = LifeTogetherTokens.spacing.medium),
    ) {
        if (content.groupedPins.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(bottom = LifeTogetherTokens.spacing.small)
                    .clickable { onUiEvent(TravellerUiEvent.BackToGroup) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "back to group",
                    modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    pin.city,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${pin.city}, ${pin.country}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    pin.type.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                IconButton(onClick = { onUiEvent(TravellerUiEvent.EditPinClicked) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "edit pin",
                        modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge)
                    )
                }
                IconButton(onClick = { onUiEvent(TravellerUiEvent.DeletePinClicked) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "delete pin",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge)
                    )
                }
            }
        }

        Spacer(Modifier.height(LifeTogetherTokens.spacing.small))

        if (pin.dateFrom != null) {
            val dateText = when {
                pin.dateTo == null && pin.type == PinType.LIVED ->
                    "${dateFormat.format(pin.dateFrom)} – present"
                pin.dateTo == pin.dateFrom ->
                    dateFormat.format(pin.dateFrom)
                pin.dateTo != null ->
                    "${dateFormat.format(pin.dateFrom)} – ${dateFormat.format(pin.dateTo)}"
                else -> dateFormat.format(pin.dateFrom)
            }
            TextDefault(dateText)
        } else if (pin.type == PinType.BUCKET_LIST) {
            TextDefault("No planned date")
        }

        if (pin.albumId != null) {
            val albumName = content.albums.firstOrNull { it.id == pin.albumId }?.name
            Spacer(Modifier.height(LifeTogetherTokens.spacing.small))
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUiEvent(TravellerUiEvent.AlbumLinkClicked(pin.albumId)) }
                    .padding(vertical = LifeTogetherTokens.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextLabel(if (albumName != null) "View $albumName album" else "View album")
                Icon(
                    painter = painterResource(R.drawable.ic_expand),
                    contentDescription = "open album",
                    modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge),
                )
            }
        }

        Spacer(Modifier.height(LifeTogetherTokens.spacing.large))
    }
}

@Composable
private fun GroupViewSheetContent(
    content: TravellerContent,
    onUiEvent: (TravellerUiEvent) -> Unit,
) {
    val pins = content.groupedPins
    if (pins.isEmpty()) return
    val city = pins.first().city
    val country = pins.first().country
    val dateFormat = rememberDateFormat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LifeTogetherTokens.spacing.medium),
    ) {
        TextSubHeadingMedium("$city, $country")
        Text(
            "${pins.size} pins",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(LifeTogetherTokens.spacing.small))
        HorizontalDivider()

        pins.forEach { pin ->
            val albumName = content.albums.firstOrNull { it.id == pin.albumId }?.name
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUiEvent(TravellerUiEvent.SelectPinFromGroup(pin)) }
                    .padding(vertical = LifeTogetherTokens.spacing.small),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pin.type.displayName, fontWeight = FontWeight.Medium)
                        if (pin.dateFrom != null) {
                            val dateText = when {
                                pin.dateTo == null && pin.type == PinType.LIVED ->
                                    "${dateFormat.format(pin.dateFrom)} – present"
                                pin.dateTo == pin.dateFrom ->
                                    dateFormat.format(pin.dateFrom)
                                pin.dateTo != null ->
                                    "${dateFormat.format(pin.dateFrom)} – ${dateFormat.format(pin.dateTo)}"
                                else -> dateFormat.format(pin.dateFrom)
                            }
                            Text(
                                dateText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (albumName != null) {
                            TextLabel(
                                text = "Album: $albumName",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_expand),
                        contentDescription = "view pin",
                        modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge),
                    )
                }
            }
            HorizontalDivider()
        }

        val linkedAlbums = pins
            .mapNotNull { pin -> pin.albumId?.let { id -> content.albums.firstOrNull { it.id == id } } }
            .distinctBy { it.id }

        if (linkedAlbums.isNotEmpty()) {
            Spacer(Modifier.height(LifeTogetherTokens.spacing.medium))
            TextLabel(
                text = "Albums",
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(LifeTogetherTokens.spacing.xSmall))
            linkedAlbums.forEach { album ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUiEvent(TravellerUiEvent.AlbumLinkClicked(album.id)) }
                        .padding(vertical = LifeTogetherTokens.spacing.small),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("View ${album.name} album")
                    Icon(
                        painter = painterResource(R.drawable.ic_expand),
                        contentDescription = "open album",
                        modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge),
                    )
                }
                HorizontalDivider()
            }
        }

        Spacer(Modifier.height(LifeTogetherTokens.spacing.large))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPinSheetContent(
    content: TravellerContent,
    onUiEvent: (TravellerUiEvent) -> Unit,
) {
    val draft = content.draftPin ?: return
    val isEditing = content.sheetMode == PinSheetMode.Editing
    val dateFormat = rememberDateFormat()

    var showDateFromPicker by remember { mutableStateOf(false) }
    var showDateToPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LifeTogetherTokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
    ) {
        TextSubHeadingMedium(if (isEditing) "Edit pin" else "Add pin")

        Text(
            "${draft.city}, ${draft.country}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.height(LifeTogetherTokens.spacing.xSmall))

        Text("Type", style = MaterialTheme.typography.labelMedium)
        TagOptionRow(
            options = PinType.entries.map { it.displayName },
            selectedOption = draft.type.displayName,
            onSelectedOptionChange = { name ->
                PinType.entries.firstOrNull { it.displayName == name }?.let {
                    onUiEvent(TravellerUiEvent.DraftTypeChanged(it))
                }
            },
            center = true,
            showDividers = false,
        )

        if (draft.type != PinType.BUCKET_LIST) {
            HorizontalDivider()
            Text("From", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showDateFromPicker = true }
                    .padding(LifeTogetherTokens.spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(draft.dateFrom?.let { dateFormat.format(it) } ?: "Select date")
                Icon(
                        painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge),
                    )
            }
        } else {
            HorizontalDivider()
            Text("Planned date (optional)", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showDateFromPicker = true }
                    .padding(LifeTogetherTokens.spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(draft.dateFrom?.let { dateFormat.format(it) } ?: "No date set")
                Icon(
                        painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge),
                    )
            }
        }

        when (draft.type) {
            PinType.VISITED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Single day")
                    Switch(
                        checked = draft.singleDay,
                        onCheckedChange = { onUiEvent(TravellerUiEvent.DraftSingleDayToggled(it)) },
                    )
                }
                if (!draft.singleDay) {
                    Text("To", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showDateToPicker = true }
                            .padding(LifeTogetherTokens.spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(draft.dateTo?.let { dateFormat.format(it) } ?: "Select date")
                        Icon(
                        painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge),
                    )
                    }
                }
            }
            PinType.LIVED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Still living here")
                    Switch(
                        checked = draft.stillLivingHere,
                        onCheckedChange = { onUiEvent(TravellerUiEvent.DraftStillLivingHereToggled(it)) },
                    )
                }
                if (!draft.stillLivingHere) {
                    Text("To", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showDateToPicker = true }
                            .padding(LifeTogetherTokens.spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(draft.dateTo?.let { dateFormat.format(it) } ?: "Select date")
                        Icon(
                        painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge),
                    )
                    }
                }
            }
            PinType.BUCKET_LIST -> Unit
        }

        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onUiEvent(TravellerUiEvent.RequestAlbumPicker) }
                .padding(vertical = LifeTogetherTokens.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val linkedAlbumName = content.albums.firstOrNull { it.id == draft.albumId }?.name
            Text(linkedAlbumName ?: "Link album (optional)")
            Icon(
                painterResource(R.drawable.ic_expand),
                contentDescription = "link album",
                modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge)
            )
        }

        Spacer(Modifier.height(LifeTogetherTokens.spacing.xSmall))

        PrimaryButton(
            text = if (isEditing) "Save" else "Add pin",
            onClick = { onUiEvent(TravellerUiEvent.SaveDraftPin) },
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(Modifier.height(LifeTogetherTokens.spacing.large))
    }

    if (content.showAlbumPicker) {
        AlbumPickerDialog(
            albums = content.albums,
            selectedAlbumId = content.draftPin.albumId,
            onSelect = { onUiEvent(TravellerUiEvent.DraftAlbumChanged(it)) },
            onDismiss = { onUiEvent(TravellerUiEvent.DismissAlbumPicker) },
        )
    }

    if (showDateFromPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = draft.dateFrom?.time)
        DatePickerDialog(
            onDismissRequest = { showDateFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onUiEvent(TravellerUiEvent.DraftDateFromChanged(Date(it)))
                    }
                    showDateFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateFromPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showDateToPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = draft.dateTo?.time ?: draft.dateFrom?.time)
        DatePickerDialog(
            onDismissRequest = { showDateToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onUiEvent(TravellerUiEvent.DraftDateToChanged(Date(it)))
                    }
                    showDateToPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateToPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun AlbumPickerDialog(
    albums: List<AlbumUiModel>,
    selectedAlbumId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        onDismiss = onDismiss,
        onConfirm = onDismiss,
        dialogTitle = "Link album",
        dialogMessage = "Choose an album to link to this pin.",
        dismissButtonMessage = "Cancel",
        confirmButtonMessage = "Done",
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
            ) {
                if (selectedAlbumId != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable { onSelect(null) }
                            .padding(LifeTogetherTokens.spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Remove link", color = MaterialTheme.colorScheme.onErrorContainer)
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge)
                        )
                    }
                }
                albums.forEach { album ->
                    val isSelected = album.id == selectedAlbumId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { onSelect(album.id); onDismiss() }
                            .padding(LifeTogetherTokens.spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = album.name,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (isSelected) {
                            Icon(
                                painterResource(R.drawable.ic_checkmark),
                                contentDescription = "album selected",
                                modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge)
                            )
                        }
                    }
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun TravellerScreenPreview() {
    LifeTogetherTheme {
        TravellerScreen(
            uiState = TravellerUiState.Loaded(
                TravellerContent(
                    pins = listOf(
                        TravellerPin(
                            id = "1",
                            familyId = "fam",
                            city = "Edinburgh",
                            country = "United Kingdom",
                            latitude = 55.9533,
                            longitude = -3.1883,
                            type = PinType.VISITED,
                            dateFrom = Date(),
                            dateTo = Date(),
                            albumId = null,
                        ),
                    ),
                ),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
