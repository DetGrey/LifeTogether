package com.example.lifetogether.ui.feature.guides

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.sync.SyncUpdatingText
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun GuidesScreen(
    uiState: GuidesUiState,
    onUiEvent: (GuidesUiEvent) -> Unit,
    onNavigationEvent: (GuidesNavigationEvent) -> Unit,
) {
    val context = LocalContext.current
    var guideTemplate by remember { mutableStateOf("") }
    var guideProgressTemplate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        guideTemplate = loadTemplateAsset(context, "guide_template.json")
        guideProgressTemplate = loadTemplateAsset(context, "guide_progress_template.json")
    }

    val createGuideTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            writeTemplateToUri(context, uri, guideTemplate)
        }
    }

    val createGuideProgressTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            writeTemplateToUri(context, uri, guideProgressTemplate)
        }
    }

    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val content = uri?.let { readTextFromUri(context, it) }
        if (content.isNullOrBlank()) {
            onUiEvent(GuidesUiEvent.CloseImportDialog)
            onUiEvent(GuidesUiEvent.ImportGuidesFromJson(""))
            return@rememberLauncherForActivityResult
        }
        onUiEvent(GuidesUiEvent.ImportGuidesFromJson(content))
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
                        onLeftClick = {
                            onNavigationEvent(GuidesNavigationEvent.NavigateBack)
                        },
                        text = "Guides",
                    )

                    SyncUpdatingText(keys = setOf(SyncKey.GUIDES))
                }
            }

            if (uiState.guides.isEmpty()) {
                item {
                    Text(text = "No guides yet. Tap + to create or import one.")
                }
            } else {
                items(uiState.guides) { guide ->
                    GuideOverviewCard(
                        guide = guide,
                        onClick = {
                            guide.id?.let {
                                onNavigationEvent(GuidesNavigationEvent.NavigateToGuideDetails(it))
                            }
                        },
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
            AddButton(onClick = { onUiEvent(GuidesUiEvent.OpenAddOptionsDialog) })
        }
    }

    if (uiState.showAddOptionsDialog) {
        AlertDialog(
            onDismissRequest = { onUiEvent(GuidesUiEvent.CloseAddOptionsDialog) },
            title = { Text("Add guide") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onUiEvent(GuidesUiEvent.CloseAddOptionsDialog)
                            onNavigationEvent(GuidesNavigationEvent.NavigateToGuideCreate)
                        },
                    ) {
                        Text("Create guide manually")
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onUiEvent(GuidesUiEvent.OpenImportDialog)
                        },
                    ) {
                        Text("Upload JSON file")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(onClick = { onUiEvent(GuidesUiEvent.CloseAddOptionsDialog) }) {
                    Text("Close")
                }
            },
        )
    }

    if (uiState.showImportDialog) {
        AlertDialog(
            onDismissRequest = { onUiEvent(GuidesUiEvent.CloseImportDialog) },
            title = { Text("Import guides from JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Upload a JSON object or array using the guide schema. " +
                            "Guide IDs are assigned by Firestore automatically, while section/step IDs are regenerated.",
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                    ) {
                        Text(
                            text = guideTemplate.take(500),
                            maxLines = 12,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            createGuideTemplateLauncher.launch("guide_template.json")
                        },
                    ) {
                        Text("Download guide template")
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            createGuideProgressTemplateLauncher.launch("guide_progress_template.json")
                        },
                    ) {
                        Text("Download guide progress template")
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            openJsonLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/plain",
                                    "application/octet-stream",
                                ),
                            )
                        },
                    ) {
                        Text("Choose JSON file")
                    }

                    if (uiState.isImporting) {
                        RowWithCenteredLoader()
                    }

                    if (uiState.importSummary.isNotEmpty()) {
                        Text(
                            text = uiState.importSummary,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onUiEvent(GuidesUiEvent.CloseImportDialog) }) {
                    Text("Done")
                }
            },
            dismissButton = {
                Button(onClick = { onUiEvent(GuidesUiEvent.CloseImportDialog) }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GuidesScreenPreview() {
    LifeTogetherTheme {
        GuidesScreen(
            uiState = GuidesUiState(
                guides = listOf(
                    Guide(
                        itemName = "Family reset",
                        description = "Set up a weekly reset plan.",
                        visibility = Visibility.FAMILY,
                        started = true,
                        sections = emptyList(),
                    ),
                ),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Composable
private fun GuideOverviewCard(
    guide: Guide,
    onClick: () -> Unit,
) {
    val completedSections = guide.sections.count { it.completed }
    val sectionProgress = guide.sections.sumOf { GuideProgress.sectionProgress(it).first }
    val sectionTotal = guide.sections.sumOf { GuideProgress.sectionProgress(it).second }
    val progressPercent = if (sectionTotal == 0) 0f else sectionProgress.toFloat() / sectionTotal.toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large)
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (guide.visibility == Visibility.FAMILY) "Family shared" else "Private",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = if (guide.started) "In progress" else "Not started",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            TextHeadingMedium(
                text = guide.itemName,
            )
            if (guide.description.isNotBlank()) {
                Text(
                    text = guide.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { progressPercent },
                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
            )

            Text(
                text = "Sections: $completedSections/${guide.sections.size}  •  Steps: $sectionProgress/$sectionTotal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun RowWithCenteredLoader() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
}

private fun loadTemplateAsset(
    context: Context,
    assetName: String,
): String {
    return runCatching {
        context.assets.open(assetName).bufferedReader().use { it.readText() }
    }.getOrDefault("")
}

private fun writeTemplateToUri(
    context: Context,
    uri: Uri,
    template: String,
) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(template.toByteArray())
            output.flush()
        }
    }
}

private fun readTextFromUri(
    context: Context,
    uri: Uri,
): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()
}
