package com.example.lifetogether.ui.common.tagOptionRow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TagOptionRow(
    options: List<String>,
    selectedOption: String,
    onSelectedOptionChange: (String) -> Unit,
    center: Boolean = false,
) {
    Column {
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(10.dp))

        if (center) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (tag in options) {
                        TagOption(
                            tag = tag,
                            selectedTag = selectedOption,
                            onClick = { onSelectedOptionChange(it) },
                        )
                    }
                }
            }
        } else {
            LazyRow {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (tag in options) {
                            TagOption(
                                tag = tag,
                                selectedTag = selectedOption,
                                onClick = { onSelectedOptionChange(it) },
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
    }
}
