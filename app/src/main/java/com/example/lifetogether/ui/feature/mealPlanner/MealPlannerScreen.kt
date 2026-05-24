package com.example.lifetogether.ui.feature.mealPlanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.minToHourMinString
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.domain.model.mealplanner.MealPlan
import com.example.lifetogether.ui.common.ActionSheet
import com.example.lifetogether.ui.common.ActionSheetItem
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.common.text.TextLabel
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MealPlannerScreen(
    uiState: MealPlannerUiState,
    onUiEvent: (MealPlannerUiEvent) -> Unit,
    onNavigationEvent: (MealPlannerNavigationEvent) -> Unit,
) {
    val isLoading = uiState is MealPlannerUiState.Loading
    val contentState = uiState as? MealPlannerUiState.Content
    val context = LocalContext.current

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(resId = R.drawable.ic_back, description = "back arrow icon"),
                onLeftClick = { onNavigationEvent(MealPlannerNavigationEvent.NavigateBack) },
                text = "Meal Planner",
                rightAppIcon = if (contentState != null) AppIcon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ) else null,
                onRightClick = { onUiEvent(MealPlannerUiEvent.ToggleActionSheet) },
            )
        },
        floatingActionButton = {
            if (!isLoading) {
                AddButton(
                    onClick = { onNavigationEvent(MealPlannerNavigationEvent.NavigateToCreateMealPlan()) },
                )
            }
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = isLoading,
            label = "meal_planner_loading_content",
            loadingContent = {
                Skeletons.ListDetail(modifier = Modifier.fillMaxSize())
            },
        ) {
            val content = contentState ?: return@AnimatedLoadingContent

            MealPlannerWeekPager(
                mealPlans = content.mealPlans,
                recipePrepTimes = content.recipePrepTimes,
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = LifeTogetherTokens.spacing.small),
                focusDate = content.focusDate,
                onFocusDateHandled = { onUiEvent(MealPlannerUiEvent.ClearFocusDate) },
                onMealPlanClick = { mealPlan ->
                    onNavigationEvent(MealPlannerNavigationEvent.NavigateToMealPlanDetails(mealPlan.id))
                },
                onEmptyDayClick = { date ->
                    onNavigationEvent(MealPlannerNavigationEvent.NavigateToCreateMealPlan(defaultDate = date.toString()))
                },
            )

            if (content.showActionSheet) {
                ActionSheet(
                    onDismiss = { onUiEvent(MealPlannerUiEvent.ToggleActionSheet) },
                    actionsList = listOf(
                        ActionSheetItem(
                            label = "Notification settings",
                            onClick = {
                                onUiEvent(MealPlannerUiEvent.ToggleActionSheet)
                                onNavigationEvent(MealPlannerNavigationEvent.NavigateToNotifications)
                            },
                        ),
                    ),
                )
            }

            if (content.showOnboarding) {
                OnboardingBottomSheet(
                    onDismiss = { onUiEvent(MealPlannerUiEvent.DismissOnboarding) },
                    onEnable = { onUiEvent(MealPlannerUiEvent.EnableNotifications) },
                    context = context,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun OnboardingBottomSheet(
    onDismiss: () -> Unit,
    onEnable: () -> Unit,
    context: Context,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val notificationPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LifeTogetherTokens.spacing.medium)
                .padding(bottom = LifeTogetherTokens.spacing.large),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextHeadingMedium(text = "Meal reminders")
            TextDefault(
                text = "Get notified before each meal — breakfast at 9:00, lunch at 12:00, dinner at 18:00. You can customise times in notification settings.",
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
            ) {
                SecondaryButton(
                    text = "Not now",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Enable",
                    onClick = {
                        onEnable()
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!notificationPermission.status.isGranted) {
                                notificationPermission.launchPermissionRequest()
                            } else {
                                openExactAlarmSettings(context)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun openExactAlarmSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = "package:${context.packageName}".toUri()
    }
    context.startActivity(intent)
}

@Composable
private fun MealPlannerWeekPager(
    mealPlans: List<MealPlan>,
    recipePrepTimes: Map<String, Int>,
    modifier: Modifier = Modifier,
    focusDate: String?,
    onFocusDateHandled: () -> Unit,
    onMealPlanClick: (MealPlan) -> Unit,
    onEmptyDayClick: (LocalDate) -> Unit,
) {
    val parsedMealPlans = remember(mealPlans) {
        mealPlans.mapNotNull { mealPlan ->
            parseMealDate(mealPlan.date)?.let { it to mealPlan }
        }
    }
    val mealPlansByDate = remember(parsedMealPlans) {
        parsedMealPlans.groupBy({ it.first }, { it.second })
    }
    val today = remember { LocalDate.now() }
    val currentWeekStart = remember(today) { startOfWeek(today) }
    val pageCount = 4001
    val currentWeekPage = pageCount / 2
    val pagerState = rememberPagerState(initialPage = currentWeekPage) { pageCount }
    val coroutineScope = rememberCoroutineScope()
    val focusWeekStart = remember(focusDate) { focusDate?.let(::parseMealDate)?.let(::startOfWeek) }

    LaunchedEffect(focusWeekStart, currentWeekPage) {
        val weekStart = focusWeekStart ?: return@LaunchedEffect
        val targetPage = (
            currentWeekPage + ChronoUnit.WEEKS.between(currentWeekStart, weekStart).toInt()
            ).coerceIn(0, pageCount - 1)
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
        onFocusDateHandled()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextDefault(text = "Week view")
            SecondaryButton(
                text = "Current week",
                onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(currentWeekPage) }
                },
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val weekStart = currentWeekStart.plusWeeks((page - currentWeekPage).toLong())
            val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                contentPadding = PaddingValues(bottom = LifeTogetherTokens.spacing.bottomInsetMedium),
            ) {
                items(weekDays) { day ->
                    val dayMealPlans = mealPlansByDate[day]
                        .orEmpty()
                        .sortedWith(
                            compareBy<MealPlan> { it.mealType.mealOrder }
                                .thenBy { it.dateCreated }
                                .thenBy { it.itemName.lowercase() },
                        )
                    val label = "${day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${day.dayOfMonth}"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.large,
                            )
                            .clickable(
                                enabled = dayMealPlans.isEmpty()
                            ) { onEmptyDayClick(day) }
                            .padding(LifeTogetherTokens.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                    ) {
                        if (dayMealPlans.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextDefault(text = label)
                                TextLabel(text = "No meal planned")
                            }
                        } else {
                            TextDefault(text = label)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                            ) {
                                dayMealPlans.forEach { mealPlan ->
                                    MealPlanCard(
                                        mealPlan = mealPlan,
                                        prepTimeMin = mealPlan.recipeId?.let { recipePrepTimes[it] },
                                        onClick = { onMealPlanClick(mealPlan) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseMealDate(value: String): LocalDate? {
    return try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun startOfWeek(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

@Composable
private fun MealPlanCard(
    mealPlan: MealPlan,
    prepTimeMin: Int?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors().copy(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        val showPrepTime = prepTimeMin != null && prepTimeMin > 0
        Column(
            modifier = Modifier.padding(LifeTogetherTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(if (showPrepTime) LifeTogetherTokens.spacing.small else 0.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
            ) {
                if (showPrepTime) {
                    TextDefault(
                        text = "Prep time: ${minToHourMinString(prepTimeMin)}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                TextLabel(
                    text = mealPlan.mealType.displayName,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
            }
            TextHeadingMedium(text = mealPlan.itemName)
        }
    }
}

private val MealType.mealOrder: Int
    get() = when (this) {
        MealType.BREAKFAST -> 0
        MealType.LUNCH -> 1
        MealType.DINNER -> 2
        MealType.SNACK -> 3
        MealType.OTHER -> 4
    }

@Preview(showBackground = true)
@Composable
private fun MealPlannerScreenPreview() {
    LifeTogetherTheme {
        MealPlannerScreen(
            uiState = MealPlannerUiState.Content(
                familyId = "family-1",
                mealPlans = listOf(
                    MealPlan(
                        id = "1",
                        familyId = "family-1",
                        itemName = "Pasta Bolognese",
                        date = LocalDate.now().toString(),
                        mealType = MealType.DINNER,
                        dateCreated = Date(),
                    ),
                ),
                recipePrepTimes = emptyMap(),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
