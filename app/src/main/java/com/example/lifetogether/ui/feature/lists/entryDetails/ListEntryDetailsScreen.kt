package com.example.lifetogether.ui.feature.lists.entryDetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.ui.common.TopBar
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListEntryDetailsScreen(
    listId: String,
    entryId: String? = null,
    appNavigator: AppNavigator? = null,
    appSessionViewModel: AppSessionViewModel,
) {
    val vm: ListEntryDetailsViewModel = hiltViewModel()
    val userInformation by appSessionViewModel.userInformation.collectAsState()

    LaunchedEffect(userInformation?.familyId, listId) {
        val familyId = userInformation?.familyId
        if (!familyId.isNullOrBlank() && listId.isNotBlank()) {
            vm.setContext(familyId, listId)
        }
    }

    LaunchedEffect(entryId) {
        vm.entryId = entryId
    }

    val isEditing = entryId != null
    val title = if (isEditing) "Edit entry" else "New entry"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TopBar(
                leftIcon = Icon(resId = R.drawable.ic_back_arrow, description = "back arrow"),
                onLeftClick = { appNavigator?.navigateBack() },
                text = title,
            )
        }

        item {
            TextField(
                value = vm.name,
                onValueChange = { vm.name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Recurrence", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurrenceUnit.entries.forEach { unit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .toggleable(
                                    value = vm.recurrenceUnit == unit,
                                    onValueChange = { if (it) vm.recurrenceUnit = unit },
                                    role = Role.RadioButton,
                                )
                                .padding(4.dp),
                        ) {
                            Checkbox(
                                checked = vm.recurrenceUnit == unit,
                                onCheckedChange = null,
                            )
                            Text("Every N ${unit.value}")
                        }
                    }
                }
            }
        }

        item {
            TextField(
                value = if (vm.interval <= 0) "" else vm.interval.toString(),
                onValueChange = { vm.interval = it.toIntOrNull() ?: 1 },
                label = { Text("Interval (N)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        if (vm.recurrenceUnit == RecurrenceUnit.WEEKS) {
            item {
                val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Weekdays", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dayNames.forEachIndexed { index, day ->
                            val dayNum = index + 1
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .toggleable(
                                        value = dayNum in vm.weekdays,
                                        onValueChange = { checked ->
                                            vm.weekdays = if (checked) {
                                                vm.weekdays + dayNum
                                            } else {
                                                vm.weekdays - dayNum
                                            }
                                        },
                                        role = Role.Checkbox,
                                    )
                                    .padding(4.dp),
                            ) {
                                Checkbox(
                                    checked = dayNum in vm.weekdays,
                                    onCheckedChange = null,
                                )
                                Text(day)
                            }
                        }
                    }
                }
            }
        }

        item {
            if (vm.isSaving) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { vm.save { appNavigator?.navigateBack() } },
                ) {
                    Text(if (isEditing) "Save changes" else "Create")
                }
            }
        }
    }

    if (vm.showAlertDialog) {
        LaunchedEffect(vm.error) {
            vm.dismissAlert()
        }
        ErrorAlertDialog(vm.error)
    }
}

@Preview(showBackground = true)
@Composable
fun ListEntryDetailsScreenPreview() {
    LifeTogetherTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TopBar(
                    leftIcon = Icon(resId = R.drawable.ic_back_arrow, description = "back arrow"),
                    onLeftClick = { },
                    text = "title",
                )
            }

            item {
                TextField(
                    value = "name",
                    onValueChange = { },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Recurrence", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecurrenceUnit.entries.forEach { unit ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .toggleable(
                                        value = unit == RecurrenceUnit.WEEKS,
                                        onValueChange = { },
                                        role = Role.RadioButton,
                                    )
                                    .padding(4.dp),
                            ) {
                                Checkbox(
                                    checked = RecurrenceUnit.WEEKS == unit,
                                    onCheckedChange = null,
                                )
                                Text("Every N ${unit.value}")
                            }
                        }
                    }
                }
            }

            item {
                TextField(
                    value = "",
                    onValueChange = {  },
                    label = { Text("Interval (N)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            if (true) {
                val weekdays = setOf(1, 4)
                item {
                    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Weekdays", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            dayNames.forEachIndexed { index, day ->
                                val dayNum = index + 1
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .toggleable(
                                            value = dayNum in weekdays,
                                            onValueChange = { },
                                            role = Role.Checkbox,
                                        )
                                        .padding(4.dp),
                                ) {
                                    Checkbox(
                                        checked = dayNum in weekdays,
                                        onCheckedChange = null,
                                    )
                                    Text(day)
                                }
                            }
                        }
                    }
                }
            }

            item {
                if (false == true) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {  },
                    ) {
                        Text(if (true) "Save changes" else "Create")
                    }
                }
            }
        }
    }
}
