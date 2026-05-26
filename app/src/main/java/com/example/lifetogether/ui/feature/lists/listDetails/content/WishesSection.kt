package com.example.lifetogether.ui.feature.lists.listDetails.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.domain.model.lists.WishListPriority
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun WishesSection(
    entries: List<WishListEntry>,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
    onComplete: (String) -> Unit,
) {
    var completedExpanded by rememberSaveable { mutableStateOf(false) }
    val activeEntries = entries.filterNot { it.purchased }
    val completedEntries = entries.filter { it.purchased }
    val activeEntriesByPriority = activeEntries.groupBy { it.priority }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = LifeTogetherTokens.spacing.small),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
    ) {
        WishListPriority.entries.forEach { priority ->
            val priorityEntries = activeEntriesByPriority[priority].orEmpty()
            if (priorityEntries.isNotEmpty()) {
                item {
                    WishPriorityHeader(priority = priority)
                }
                items(
                    items = priorityEntries,
                    key = { it.id },
                ) { entry ->
                    WishCard(
                        entry = entry,
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedIds.contains(entry.id),
                        onClick = { onClick(entry.id) },
                        onLongClick = { onLongClick(entry.id) },
                        onComplete = { onComplete(entry.id) },
                    )
                }
            }
        }

        if (completedEntries.isNotEmpty()) {
            item {
                CompletedWishHeader(
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
                            WishCard(
                                entry = entry,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedIds.contains(entry.id),
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
private fun WishPriorityHeader(priority: WishListPriority) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextHeadingMedium(
            text = priority.value.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            maxLines = 1,
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = LifeTogetherTokens.spacing.xSmall),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun CompletedWishHeader(
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
            TextDefault(text = "Purchased ($count)")
            Icon(
                painter = painterResource(id = if (expanded) R.drawable.ic_expanded else R.drawable.ic_expand),
                contentDescription = if (expanded) "collapse purchased" else "expand purchased",
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
private fun WishCard(
    entry: WishListEntry,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onComplete: () -> Unit,
) {
    val priceText = entry.price?.let { price ->
        val normalizedPrice = if (price == price.toInt().toDouble()) {
            price.toInt().toString()
        } else {
            price.toString()
        }
        if (entry.currencyCode.isNullOrBlank()) normalizedPrice else "$normalizedPrice ${entry.currencyCode}"
    } ?: "No price"
    val wishNotesSnippet = entry.notes?.takeIf { it.isNotBlank() }?.take(80)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                shape = MaterialTheme.shapes.large,
            )
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(LifeTogetherTokens.spacing.medium),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
            ) {
                CompletableBox(
                    isCompleted = entry.purchased,
                    onCompleteToggle = onComplete,
                    isEnabled = !isSelectionMode,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    tint = MaterialTheme.colorScheme.primaryContainer,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = entry.itemName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    textDecoration = if (entry.purchased) TextDecoration.LineThrough else TextDecoration.None,
                )
                TextDefault(
                    text = priceText,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                )
            }
            if (!wishNotesSnippet.isNullOrBlank()) {
                TextDefault(
                    text = wishNotesSnippet,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                )
            }
        }
    }
}
