package com.example.lifetogether.ui.feature.lists.listDetails.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun CheckItemsSection(
    entries: List<ChecklistEntry>,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
    onComplete: (String) -> Unit,
) {
    var completedExpanded by rememberSaveable { mutableStateOf(false) }
    val activeEntries = entries.filterNot { it.isChecked }
    val completedEntries = entries.filter { it.isChecked }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = LifeTogetherTokens.spacing.small),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
    ) {
        items(activeEntries) { entry ->
            ChecklistCard(
                entry = entry,
                isCompleted = false,
                isSelected = selectedIds.contains(entry.id),
                isSelectionMode = isSelectionMode,
                onClick = { onClick(entry.id) },
                onLongClick = { onLongClick(entry.id) },
                onComplete = { onComplete(entry.id) },
            )
        }

        if (completedEntries.isNotEmpty()) {
            item {
                CompletedSectionHeader(
                    count = completedEntries.size,
                    expanded = completedExpanded,
                    onToggle = { completedExpanded = !completedExpanded },
                )
            }
            item {
                AnimatedVisibility(
                    visible = completedExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                    ) {
                        completedEntries.forEach { entry ->
                            ChecklistCard(
                                entry = entry,
                                isCompleted = true,
                                isSelected = selectedIds.contains(entry.id),
                                isSelectionMode = isSelectionMode,
                                onClick = { onClick(entry.id) },
                                onLongClick = { onLongClick(entry.id) },
                                onComplete = { onComplete(entry.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onToggle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextDefault(text = "Completed ($count)")
            Icon(
                painter = painterResource(id = if (expanded) R.drawable.ic_expanded else R.drawable.ic_expand),
                contentDescription = if (expanded) "collapse completed" else "expand completed",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = LifeTogetherTokens.spacing.xSmall),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ChecklistCard(
    entry: ChecklistEntry,
    isCompleted: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onComplete: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                shape = MaterialTheme.shapes.large,
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        leadingContent = {
            CompletableBox(
                isCompleted = isCompleted,
                onCompleteToggle = onComplete,
                isEnabled = !isSelectionMode,
            )
        },
        headlineContent = {
            Text(
                text = entry.itemName,
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
    )
}
