package com.example.lifetogether

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.lifetogether.ui.feature.notification.NotificationService
import com.example.lifetogether.ui.navigation.NavHost
import com.example.lifetogether.ui.theme.AppTypography
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val destination = intent?.getStringExtra("destination")

        setContent {
            LifeTogetherTheme {
                // A surface container using the 'background' color from the theme
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background), // background for status/nav bars
                )
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .imePadding(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val postNotificationPermission =
                        rememberPermissionState(permission = "android.permission.POST_NOTIFICATIONS")

                    LaunchedEffect(key1 = true) {
                        if (!postNotificationPermission.status.isGranted) {
                            postNotificationPermission.launchPermissionRequest()
                        }
                    }

                    val firebaseViewModel: FirebaseViewModel = hiltViewModel()
                    val navControllerState = rememberNavController()
                    navController = navControllerState

                    LaunchedEffect(destination, navControllerState) {
                        if (destination != null) {
                            navControllerState.navigate(destination)
                        }
                    }

                    val userInformation by firebaseViewModel.userInformation.collectAsState()
                    when (val familyId = userInformation?.familyId) {
                        is String -> {
                            firebaseViewModel.observeFirestoreFamilyData(familyId)

                            userInformation?.uid?.let { firebaseViewModel.storeFcmToken(it, familyId) }
                        }
                    }

                    val notificationService = remember { NotificationService(this) }
                    LaunchedEffect(Unit) {
                        notificationService.addNotificationChannels()
                    }

                    // Makes the default Text style bodyMedium instead of bodyLarge
                    ProvideTextStyle(value = AppTypography.bodyMedium) {
                        NavHost(navController = navControllerState, firebaseViewModel = firebaseViewModel)
                    }
                }
            }
        }
    }
}
