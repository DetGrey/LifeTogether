package com.example.lifetogether.ui.feature.traveller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.lifetogether.domain.model.traveller.PinType
import com.example.lifetogether.ui.common.tagOptionRow.TagOption
import com.example.lifetogether.ui.model.ColorPair
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun TravellerFilterRow(
    activeFilters: Set<PinType>,
    onToggle: (PinType) -> Unit,
    pinTypeColors: Map<PinType, ColorPair>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LifeTogetherTokens.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(
            space = LifeTogetherTokens.spacing.small,
            alignment = Alignment.CenterHorizontally
        ),
    ) {
        PinType.entries.forEach { type ->
            TagOption(
                tag = type.displayName,
                selectedTag = if (type in activeFilters) type.displayName else "",
                onClick = { onToggle(type) },
                selectedColors = pinTypeColors[type],
            )
        }
    }
}
