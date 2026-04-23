package com.example.lifetogether.ui.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.BuildConfig
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.model.session.authenticatedUserOrNull
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.event.LocalRootSnackbarHostState
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.ImageViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    appNavigator: AppNavigator,
) {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()
    val bitmap by imageViewModel.bitmap.collectAsStateWithLifecycle()
    val sessionState by homeViewModel.sessionState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalRootSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    val userInformation = sessionState.authenticatedUserOrNull
    val isAdmin = userInformation?.uid in BuildConfig.ADMIN_LIST.split(",")
    val displayBitmap = if (userInformation?.familyId != null) bitmap else null
    val sections = buildHomeSections(isAdmin = isAdmin)
    val currentSessionState = sessionState

    val content = HomeContent(
        statusCard = when (currentSessionState) {
            SessionState.Loading -> HomeStatusCard.None
            SessionState.Unauthenticated -> HomeStatusCard.Message("Please login to use the app")
            is SessionState.Authenticated -> {
                if (userInformation?.familyId == null) {
                    HomeStatusCard.Message("Please create or join a family to save your data")
                } else {
                    HomeStatusCard.None
                }
            }
        },
        bitmap = displayBitmap,
        sections = sections,
    )

    val uiState = when (currentSessionState) {
        SessionState.Loading -> HomeUiState.Loading
        SessionState.Unauthenticated -> HomeUiState.Unauthenticated(content)
        is SessionState.Authenticated -> HomeUiState.Authenticated(
            userInformation = currentSessionState.user,
            content = content,
        )
    }

    LaunchedEffect(userInformation?.familyId) {
        userInformation?.familyId?.let { familyId ->
            imageViewModel.collectImageFlow(
                imageType = ImageType.FamilyImage(familyId),
                onError = { },
            )
        }
    }

    HomeScreen(
        uiState = uiState,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                HomeNavigationEvent.ProfileClicked -> {
                    if (userInformation != null) {
                        appNavigator.navigateToProfile()
                    } else {
                        appNavigator.navigateToLogin()
                    }
                }

                HomeNavigationEvent.SettingsClicked -> appNavigator.navigateToSettings()

                HomeNavigationEvent.StatusCardClicked -> {
                    when (sessionState) {
                        SessionState.Loading -> Unit
                        SessionState.Unauthenticated -> appNavigator.navigateToLogin()
                        is SessionState.Authenticated -> {
                            if (userInformation?.familyId == null) {
                                appNavigator.navigateToSettings()
                            }
                        }
                    }
                }

                is HomeNavigationEvent.TileClicked -> {
                    handleTileClick(
                        tile = navigationEvent.tile,
                        isAdmin = isAdmin,
                        familyId = userInformation?.familyId,
                        appNavigator = appNavigator,
                        snackbarHostState = snackbarHostState,
                        coroutineScope = coroutineScope,
                    )
                }
            }
        },
    )
}

private fun buildHomeSections(isAdmin: Boolean): List<HomeSection> {
    val familySection = HomeSection(
        maxItemsInEachRow = 3,
        items = listOf(
            HomeSectionItem.Tile(HomeTile.GroceryList),
            HomeSectionItem.Tile(HomeTile.Recipes),
            HomeSectionItem.Break,
            HomeSectionItem.Tile(HomeTile.Guides),
            HomeSectionItem.Tile(HomeTile.Gallery),
            HomeSectionItem.Tile(HomeTile.TipTracker),
            HomeSectionItem.Tile(HomeTile.Lists),
        ),
    )

    val adminSection = if (isAdmin) {
        HomeSection(
            title = "Admin features",
            maxItemsInEachRow = 2,
            items = listOf(
                HomeSectionItem.Tile(HomeTile.AdminGroceryCategories),
                HomeSectionItem.Tile(HomeTile.AdminGrocerySuggestions),
            ),
        )
    } else {
        null
    }

    return buildList {
        add(familySection)
        if (adminSection != null) {
            add(adminSection)
        }
    }
}

private fun handleTileClick(
    tile: HomeTile,
    isAdmin: Boolean,
    familyId: String?,
    appNavigator: AppNavigator,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
) {
    if (tile.requiresFamilyAccess && familyId == null) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Please join a family to use this feature")
        }
        return
    }

    if (tile.requiresAdminAccess && !isAdmin) {
        return
    }

    when (tile) {
        HomeTile.GroceryList -> appNavigator.navigateToGroceryList()
        HomeTile.Recipes -> appNavigator.navigateToRecipes()
        HomeTile.Guides -> appNavigator.navigateToGuides()
        HomeTile.Gallery -> appNavigator.navigateToGallery()
        HomeTile.TipTracker -> appNavigator.navigateToTipTracker()
        HomeTile.Lists -> appNavigator.navigateToLists()
        HomeTile.AdminGroceryCategories -> appNavigator.navigateToAdminGroceryCategories()
        HomeTile.AdminGrocerySuggestions -> appNavigator.navigateToAdminGrocerySuggestions()
    }
}
