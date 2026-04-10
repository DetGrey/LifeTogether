package com.example.lifetogether.ui.feature.lists.listDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.observer.ObserverUpdatingText
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ListDetailsScreen(
    listId: String,
    appNavigator: AppNavigator? = null,
    appSessionViewModel: AppSessionViewModel,
) {
    val vm: ListDetailsViewModel = hiltViewModel()
    val userInformation by appSessionViewModel.userInformation.collectAsState()
    val entries by vm.entries.collectAsState()

    LaunchedEffect(userInformation?.familyId, userInformation?.uid, listId) {
        val familyId = userInformation?.familyId
        val uid = userInformation?.uid
        if (!familyId.isNullOrBlank() && !uid.isNullOrBlank() && listId.isNotBlank()) {
            vm.setUp(familyId, uid, listId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TopBar(
                        leftIcon = Icon(
                            resId = R.drawable.ic_back_arrow,
                            description = "back arrow icon",
                        ),
                        onLeftClick = { appNavigator?.navigateBack() },
                        text = vm.listName.ifBlank { "List" },
                    )

                    ObserverUpdatingText(
                        appSessionViewModel = appSessionViewModel,
                        keys = setOf(ObserverKey.ROUTINE_LIST_ENTRIES),
                    )
                }
            }

            if (entries.isEmpty()) {
                item {
                    Text(text = "No entries yet. Tap + to add one.")
                }
            } else {
                items(entries) { entry ->
                    ListEntryCard(
                        entry = entry,
                        onClick = {
                            entry.id?.let { appNavigator?.navigateToListEntryDetails(listId, it) }
                        },
                        onComplete = { vm.completeEntry(entry) },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 30.dp, end = 30.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            AddButton(onClick = { appNavigator?.navigateToListEntryDetails(listId) })
        }
    }

    if (vm.showAlertDialog) {
        LaunchedEffect(vm.error) {
            vm.dismissAlert()
        }
        ErrorAlertDialog(vm.error)
    }
}

@Composable
private fun ListEntryCard(
    entry: RoutineListEntry,
    onClick: () -> Unit,
    onComplete: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = entry.itemName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.Bold,
            )

            val recurrenceLabel = "Every ${entry.interval} ${entry.recurrenceUnit.value}"
            Text(
                text = recurrenceLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.background,
            )

            entry.nextDate?.let { next ->
                Text(
                    text = "Next: ${dateFormat.format(next)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.background,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Completions: ${entry.completionCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.background,
                )
                Text(
                    text = "Mark done",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onComplete() }
                        .padding(4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListDetailScreenPreview() {
    LifeTogetherTheme {
        Text("Preview requires AppSessionViewModel")
    }
}

@Preview(showBackground = true)
@Composable
fun ListEntryCardDailyPreview() {
    LifeTogetherTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ListEntryCard(
                entry = RoutineListEntry(
                    itemName = "Water the plants",
                    recurrenceUnit = RecurrenceUnit.DAYS,
                    interval = 3,
                    nextDate = Date(),
                    completionCount = 7,
                ),
                onClick = {},
                onComplete = {},
            )
            ListEntryCard(
                entry = RoutineListEntry(
                    itemName = "Change bedsheets",
                    recurrenceUnit = RecurrenceUnit.WEEKS,
                    interval = 2,
                    weekdays = listOf(1, 4),
                    nextDate = null,
                    completionCount = 0,
                ),
                onClick = {},
                onComplete = {},
            )
        }
    }
}
