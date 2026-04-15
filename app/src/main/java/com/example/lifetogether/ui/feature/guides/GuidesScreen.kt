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
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.observer.ObserverUpdatingText
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel
import com.example.lifetogether.domain.observer.ObserverKey

@Composable
fun GuidesScreen(
    appNavigator: AppNavigator? = null,
    appSessionViewModel: AppSessionViewModel,
) {
    val guidesViewModel: GuidesViewModel = hiltViewModel()
    val userInformation by appSessionViewModel.userInformation.collectAsState()
    val guides by guidesViewModel.guides.collectAsState()

    val context = LocalContext.current
    var guideTemplate by remember { mutableStateOf("") }
    var guideProgressTemplate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        guideTemplate = loadTemplateAsset(context, "guide_template.json")
        guideProgressTemplate = loadTemplateAsset(context, "guide_progress_template.json")
    }

    LaunchedEffect(userInformation?.familyId, userInformation?.uid) {
        val familyId = userInformation?.familyId
        val uid = userInformation?.uid
        if (!familyId.isNullOrBlank() && !uid.isNullOrBlank()) {
            guidesViewModel.setUpGuides(familyId, uid)
        }
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
            guidesViewModel.error = "Could not read the selected JSON file"
            guidesViewModel.showAlertDialog = true
            return@rememberLauncherForActivityResult
        }
        guidesViewModel.importGuidesFromJson(content)
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
                            appNavigator?.navigateBack()
                        },
                        text = "Guides",
                    )

                    ObserverUpdatingText(
                        keys = setOf(ObserverKey.GUIDES),
                    )
                }
            }

            if (guides.isEmpty()) {
                item {
                    Text(text = "No guides yet. Tap + to create or import one.")
                }
            } else {
                items(guides) { guide ->
                    GuideOverviewCard(
                        guide = guide,
                        onClick = {
                            guide.id?.let { appNavigator?.navigateToGuideDetails(it) }
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
            AddButton(onClick = { guidesViewModel.openAddOptionsDialog() })
        }
    }

    if (guidesViewModel.showAddOptionsDialog) {
        AlertDialog(
            onDismissRequest = { guidesViewModel.closeAddOptionsDialog() },
            title = { Text("Add guide") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            guidesViewModel.closeAddOptionsDialog()
                            appNavigator?.navigateToGuideCreate()
                        },
                    ) {
                        Text("Create guide manually")
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            guidesViewModel.openImportDialog()
                        },
                    ) {
                        Text("Upload JSON file")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(onClick = { guidesViewModel.closeAddOptionsDialog() }) {
                    Text("Close")
                }
            },
        )
    }

    if (guidesViewModel.showImportDialog) {
        AlertDialog(
            onDismissRequest = { guidesViewModel.closeImportDialog() },
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

                    if (guidesViewModel.isImporting) {
                        RowWithCenteredLoader()
                    }

                    if (guidesViewModel.importSummary.isNotEmpty()) {
                        Text(
                            text = guidesViewModel.importSummary,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { guidesViewModel.closeImportDialog() }) {
                    Text("Done")
                }
            },
            dismissButton = {
                Button(onClick = { guidesViewModel.closeImportDialog() }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (guidesViewModel.showAlertDialog) {
        LaunchedEffect(guidesViewModel.error) {
            guidesViewModel.dismissAlert()
        }
        ErrorAlertDialog(guidesViewModel.error)
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
            .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(20.dp))
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
                    color = MaterialTheme.colorScheme.background,
                )
                Text(
                    text = if (guide.started) "In progress" else "Not started",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.background,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = guide.itemName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.Bold,
            )
            if (guide.description.isNotBlank()) {
                Text(
                    text = guide.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.background,
                )
            }

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { progressPercent },
                trackColor = MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
            )

            Text(
                text = "Sections: $completedSections/${guide.sections.size}  •  Steps: $sectionProgress/$sectionTotal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.background,
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
