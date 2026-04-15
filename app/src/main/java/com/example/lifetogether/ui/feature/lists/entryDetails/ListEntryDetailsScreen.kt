package com.example.lifetogether.ui.feature.lists.entryDetails

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.TopBar
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.image.ImageUploadDialog
import com.example.lifetogether.ui.common.tagOptionRow.TagOption
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel
import com.example.lifetogether.ui.viewmodel.ImageViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListEntryDetailsScreen(
    listId: String,
    entryId: String? = null,
    appNavigator: AppNavigator? = null,
    appSessionViewModel: AppSessionViewModel,
) {
    val vm: ListEntryDetailsViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()
    val userInformation by appSessionViewModel.userInformation.collectAsState()
    val screenState by vm.screenState.collectAsState()
    val uiState = screenState.uiState
    val formState = screenState.formState
    val bitmap by imageViewModel.bitmap.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { vm.onImageSelected(it, context.contentResolver) }
    }

    LaunchedEffect(userInformation?.familyId, listId, entryId) {
        val familyId = userInformation?.familyId
        Log.d("ListEntryDetailsScreen", "familyId: $familyId, listId: $listId, entryId: $entryId")
        if (!familyId.isNullOrBlank() && listId.isNotBlank()) {
            vm.setUp(familyId, listId, entryId) { appNavigator?.navigateBack() }

            if (entryId != null) {
                imageViewModel.collectImageFlow(
                    imageType = ImageType.RoutineListEntryImage(familyId, entryId),
                    onError = { /* image load errors are non-fatal */ },
                )
            }
        }
    }

    val isExistingEntry = entryId != null
    val title = if (isExistingEntry) "Entry details" else "New entry"

    Scaffold(
        topBar = {
            TopBar(
                leftIcon = Icon(resId = R.drawable.ic_back_arrow, description = "back arrow"),
                onLeftClick = { appNavigator?.navigateBack() },
                text = title,
                // TODO: replace ic_edit_black with a dedicated close/cancel icon when in edit mode once the icon is created
                rightIcon = if (isExistingEntry) Icon(resId = R.drawable.ic_edit_black, description = "edit entry") else null,
                onRightClick = if (isExistingEntry) {
                    if ((uiState as? EntryDetailsUiState.Content)?.isEditing == true) {
                        { vm.requestCancelEdit() }
                    } else {
                        { vm.enterEditMode() }
                    }
                } else null,
            )
        }
    ) { padding ->
        when (uiState) {
            is EntryDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding()),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is EntryDetailsUiState.Content -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                        .padding(10.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            val displayBitmap = if (isExistingEntry) bitmap else formState.pendingImageBitmap
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .then(
                                        if (uiState.isEditing) {
                                            if (isExistingEntry) {
                                                Modifier.clickable { imageViewModel.showImageUploadDialog = true }
                                            } else {
                                                Modifier.clickable { imagePickerLauncher.launch("image/*") }
                                            }
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (displayBitmap != null) {
                                    Image(
                                        modifier = Modifier.fillMaxSize(),
                                        bitmap = displayBitmap.asImageBitmap(),
                                        contentDescription = "entry image",
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                if (uiState.isEditing) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                shape = MaterialTheme.shapes.small,
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = if (displayBitmap != null) "Change image" else "Add image",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            CustomTextField(
                                value = formState.name,
                                onValueChange = { vm.onNameChange(it) },
                                label = "Name",
                                modifier = Modifier.fillMaxWidth(),
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Text,
                                capitalization = true,
                                enabled = uiState.isEditing,
                            )
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextSubHeadingMedium("Recurrence")
                                TagOptionRow(
                                    options = RecurrenceUnit.entries.map { it.name.lowercase() },
                                    selectedOption = formState.recurrenceUnit.name,
                                    onSelectedOptionChange = { if (uiState.isEditing) vm.onRecurrenceUnitChange(it) },
                                    showDividers = false,
                                )
                            }
                        }

                        item {
                            CustomTextField(
                                value = formState.interval,
                                onValueChange = { vm.onIntervalChange(it) },
                                label = "Interval (N)",
                                modifier = Modifier.fillMaxWidth(),
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Number,
                                enabled = uiState.isEditing,
                            )
                        }

                        if (formState.recurrenceUnit == RecurrenceUnit.WEEKS) {
                            item {
                                TextSubHeadingMedium("Weekdays")

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    ListEntryDetailsViewModel.WEEKDAYS.forEachIndexed { index, day ->
                                        val dayNum = index + 1
                                        TagOption(
                                            tag = day,
                                            selectedTag = if (dayNum in formState.selectedWeekdays) day else "",
                                            onClick = { if (uiState.isEditing) vm.onSelectedWeekdaysChange(dayNum) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.isEditing) {
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .padding(bottom = padding.calculateBottomPadding())
                                .align(Alignment.End)
                        ) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { vm.save { appNavigator?.navigateBack() } },
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator()
                                } else {
                                    Text(if (isExistingEntry) "Save changes" else "Create")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState is EntryDetailsUiState.Content && uiState.showDiscardDialog) {
        ConfirmationDialog(
            onDismiss = { vm.dismissDiscardDialog() },
            onConfirm = { vm.confirmDiscard() },
            dialogTitle = "Discard changes?",
            dialogMessage = "Your unsaved changes will be lost.",
            dismissButtonMessage = "Keep editing",
            confirmButtonMessage = "Discard",
        )
    }

    if (uiState is EntryDetailsUiState.Content && uiState.showAlertDialog) {
        LaunchedEffect(uiState.error) {
            vm.dismissAlert()
        }
        ErrorAlertDialog(uiState.error)
    }

    val familyId = userInformation?.familyId
    if (imageViewModel.showImageUploadDialog && familyId != null && entryId != null) {
        ImageUploadDialog(
            onDismiss = { imageViewModel.showImageUploadDialog = false },
            onConfirm = { imageViewModel.showImageUploadDialog = false },
            dialogTitle = "Upload entry image",
            dialogMessage = "Select an image for this entry",
            imageType = ImageType.RoutineListEntryImage(familyId, entryId),
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Upload image",
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ListEntryDetailsScreenPreview() {
    LifeTogetherTheme {
        Scaffold(
            topBar = {
                TopBar(
                    leftIcon = Icon(resId = R.drawable.ic_back_arrow, description = "back arrow"),
                    onLeftClick = { },
                    text = "title",
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .padding(10.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No image", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    item {
                        CustomTextField(
                            value = "Name",
                            onValueChange = { },
                            label = "Name",
                            modifier = Modifier.fillMaxWidth(),
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text,
                            capitalization = true,
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextSubHeadingMedium("Recurrence")
                            TagOptionRow(
                                options = RecurrenceUnit.entries.map { it.name.lowercase() },
                                selectedOption = RecurrenceUnit.WEEKS.name.lowercase(),
                                onSelectedOptionChange = {},
                                showDividers = false,
                            )
                        }
                    }

                    item {
                        CustomTextField(
                            value = "",
                            onValueChange = { },
                            label = "Interval (N)",
                            modifier = Modifier.fillMaxWidth(),
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Number,
                        )
                    }

                    if (true) {
                        val weekdays = setOf(1, 4)
                        item {
                            TextSubHeadingMedium("Weekdays")

                            val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                dayNames.forEachIndexed { index, day ->
                                    val dayNum = index + 1
                                    TagOption(
                                        tag = day,
                                        selectedTag = if (dayNum in weekdays) day else "",
                                        onClick = {}
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(bottom = padding.calculateBottomPadding())
                        .align(Alignment.End)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {},
                    ) {
                        Text(if (false) "Save changes" else "Create")
                    }
                }
            }
        }
    }
}
