package com.example.lifetogether.ui.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.BuildConfig
import com.example.lifetogether.R
import com.example.lifetogether.domain.converter.daysSinceDate
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.feature.home.FeatureOverview
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
) {
    val userInformationState by firebaseViewModel?.userInformation!!.collectAsState()

    if (userInformationState?.familyId != BuildConfig.ADMIN) {
        appNavigator?.navigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                TopBar(
                    leftIcon = Icon(
                        resId = R.drawable.ic_profile_picture,
                        description = "profile picture icon",
                    ),
                    onLeftClick = {
                        if (firebaseViewModel?.userInformation?.value != null) {
                            appNavigator?.navigateToProfile()
                        } else {
                            appNavigator?.navigateToLogin()
                        }
                    },
                    text = "A Life Together",
                    rightIcon = Icon(
                        resId = R.drawable.ic_settings,
                        description = "settings icon",
                    ),
                    onRightClick = {
                        appNavigator?.navigateToSettings()
                    },
                    subText = userInformationState?.birthday?.let {
                        "${daysSinceDate(it)} days together"
                    },
                )
            }

            item {
                FlowRow(
                    maxItemsInEachRow = 2,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // TODO use a when statement with enum class to show main features
                    // TODO E.g. when clicking grocery list, it shows the features grocery categories and suggestions

                    FeatureOverview(
                        "Grocery categories",
                        0,
                        "Recipe",
                        onClick = {
                            if (userInformationState?.familyId == null) {
                                // TODO add popup asking to join a family
                            } else {
                                appNavigator?.navigateToAdminGroceryCategories()
                            }
                        },
                        icon = Icon(R.drawable.ic_groceries, "groceries basket icon"),
                        fullWidth = true,
                    )
                    FeatureOverview(
                        "Grocery suggestions",
                        0,
                        "Recipe",
                        onClick = {
                            if (userInformationState?.familyId == null) {
                                // TODO add popup asking to join a family
                            } else {
                                appNavigator?.navigateToAdminGrocerySuggestions()
                            }
                        },
                        icon = Icon(R.drawable.ic_groceries, "groceries basket icon"),
                        fullWidth = true,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    LifeTogetherTheme {
        AdminScreen()
    }
}
