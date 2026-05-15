package com.example.lifetogether.ui.feature.lists.listDetails.content

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RoutinesSection(
    entries: List<RoutineListEntry>,
    imageBitmaps: Map<String, Bitmap>,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
    onComplete: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = LifeTogetherTokens.spacing.small),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
    ) {
        items(entries) { entry ->
            val bitmap = imageBitmaps[entry.id]
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val isSelected = selectedIds.contains(entry.id)

            val next = " | Next: ${dateFormat.format(entry.nextDate)}"
            val subtitle = "Every ${entry.interval} ${entry.recurrenceUnit.value}$next"


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
                        onClick = { onClick(entry.id) },
                        onLongClick = { onLongClick(entry.id) },
                    )
                    .padding(LifeTogetherTokens.spacing.medium),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
                    ) {
                        CompletableBox(
                            isCompleted = false,
                            onCompleteToggle = { onComplete(entry.id) },
                            isEnabled = !isSelectionMode,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            tint = MaterialTheme.colorScheme.primaryContainer,
                        )
                        TextHeadingMedium(
                            text = entry.itemName,
                            maxLines = 1,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.padding(top = LifeTogetherTokens.spacing.xSmall)) {
                            if (subtitle.isNotBlank()) {
                                TextDefault(
                                    text = subtitle,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(LifeTogetherTokens.sizing.avatarMedium)
                                .background(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = MaterialTheme.shapes.medium,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "entry image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
