package com.example.lifetogether.ui.feature.tipTracker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toAbbreviatedDateString
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.ui.common.text.TextLabel
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun TipCard(
    tip: TipItem,
    onDeleteClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(LifeTogetherTokens.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(42.dp)
                    .background(MaterialTheme.colorScheme.secondary),
            )
            Spacer(modifier = Modifier.width(LifeTogetherTokens.spacing.medium))
            Text(
                text = "${tip.amount} ${tip.currency}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextLabel(
                text = tip.date.toAbbreviatedDateString(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(LifeTogetherTokens.spacing.small))
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = "trashcan icon",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(LifeTogetherTokens.sizing.iconLarge)
                    .clickable { onDeleteClick() },
            )
        }
    }
}