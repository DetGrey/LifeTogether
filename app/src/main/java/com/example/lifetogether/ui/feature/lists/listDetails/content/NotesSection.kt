package com.example.lifetogether.ui.feature.lists.listDetails.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.common.text.TextLabel
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun NotesSection(
    entries: List<NoteEntry>,
    selectedIds: Set<String>,
    onClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = LifeTogetherTokens.spacing.small),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
    ) {
        items(entries) { entry ->
            val isSelected = selectedIds.contains(entry.id)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                        shape = MaterialTheme.shapes.large,
                    )
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.shapes.large
                    )
                    .clickable { onClick(entry.id) }
                    .padding(LifeTogetherTokens.spacing.medium),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
                    TextHeadingMedium(
                        text = entry.itemName,
                    )
                    TextLabel(
                        text = entry.body.take(60) + "..."
                    )
                }
            }
        }
    }
}