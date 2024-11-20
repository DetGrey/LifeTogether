package com.example.lifetogether.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.BuildConfig
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.LoveButton
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
) {
    val userInformationState by firebaseViewModel?.userInformation!!.collectAsState()

    if (userInformationState?.familyId == BuildConfig.ADMIN) {
        appNavigator?.navigateToAdmin()
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
                    text = "Life Together",
                    rightIcon = Icon(
                        resId = R.drawable.ic_settings,
                        description = "settings icon",
                    ),
                    onRightClick = {
                        appNavigator?.navigateToSettings()
                    },
                    subText = "x days together", // TODO
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(shape = RoundedCornerShape(20))
                        .background(color = MaterialTheme.colorScheme.onBackground),
                ) {
                    // TODO add image
                }
            }

            item {
                when (userInformationState) {
                    is UserInformation -> {
                        if (userInformationState?.familyId == null) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 15.dp)
                                    .fillMaxWidth()
                                    .height(75.dp)
                                    .clip(shape = RoundedCornerShape(20))
                                    .background(MaterialTheme.colorScheme.tertiary)
                                    .padding(horizontal = 20.dp)
                                    .clickable {
                                        appNavigator?.navigateToSettings()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text =
                                    "Please create or join a family to save your data",
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

//                        Text(text = "Important dates:")
//                        // TODO update events and make clickable
//                        CountdownRow(event = "Wedding anniversary", daysLeft = "20")
//                        CountdownRow(event = "Andrés' birthday", daysLeft = "35")
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(75.dp)
                                .clip(shape = RoundedCornerShape(20))
                                .background(MaterialTheme.colorScheme.tertiary)
                                .clickable {
                                    appNavigator?.navigateToLogin()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "Please login to use the app")
                        }
                    }
                }
            }

            item {
                FlowRow(
                    maxItemsInEachRow = 2,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) { // TODO add real items
                    FeatureOverview(
                        "Grocery list",
                        10,
                        "Recipe",
                        onClick = {
                            if (userInformationState?.familyId == null) {
                                // TODO add popup asking to join a family
                            } else {
                                appNavigator?.navigateToGroceryList()
                            }
                        },
                        icon = Icon(R.drawable.ic_groceries, "groceries basket icon"),
                    )
                    FeatureOverview(
                        "Recipes",
                        1,
                        "Recipe",
                        onClick = {
                            if (userInformationState?.familyId == null) {
                                // TODO add popup asking to join a family
                            } else {
                                appNavigator?.navigateToRecipes()
                            }
                        },
                        icon = Icon(R.drawable.ic_recipes, "recipes chef hat icon"),
                    )
//                    FeatureOverview(
//                        "Memory lane",
//                        4,
//                        "Recipe",
//                        fullWidth = true,
//                        onClick = {
//                            if (userInformation?.value?.familyId == null) {
//                                // TODO add popup asking to join a family
//                            } else {
//                                // TODO
//                            }
//                        },
//                    )
//                    FeatureOverview(
//                        "Gallery",
//                        10,
//                        "Recipe",
//                        onClick = {
//                            if (userInformation?.value?.familyId == null) {
//                                // TODO add popup asking to join a family
//                            } else {
//                                // TODO
//                            }
//                        },
//                    )
//                    FeatureOverview(
//                        "Note Corner",
//                        43,
//                        "Recipe",
//                        onClick = {
//                            if (userInformation?.value?.familyId == null) {
//                                // TODO add popup asking to join a family
//                            } else {
//                                // TODO
//                            }
//                        },
//                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    LoveButton()
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    LifeTogetherTheme {
        HomeScreen()
    }
}
