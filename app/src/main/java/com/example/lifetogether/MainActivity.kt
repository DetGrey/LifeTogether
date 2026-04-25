package com.example.lifetogether

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.lifetogether.ui.common.ErrorStyledSnackbar
import com.example.lifetogether.ui.feature.notification.NotificationService
import com.example.lifetogether.ui.common.event.LocalRootSnackbarHostState
import com.example.lifetogether.ui.navigation.NavHost
import com.example.lifetogether.ui.navigation.routeFromDestinationString
import com.example.lifetogether.ui.theme.AppTypography
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.RootCoordinatorViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController

    @Inject
    lateinit var notificationService: NotificationService

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

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

                    // Eagerly create activity-scoped root coordinator.
                    // Session observation, observer orchestration, guide sync, and FCM sync
                    // are all owned internally by the root coordinator.
                    hiltViewModel<RootCoordinatorViewModel>()

                    val navControllerState = rememberNavController()
                    navController = navControllerState
                    val rootSnackbarHostState = remember { SnackbarHostState() }

                    LaunchedEffect(destination, navControllerState) {
                        if (destination != null) {
                            navControllerState.navigate(routeFromDestinationString(destination))
                        }
                    }

                    LaunchedEffect(Unit) {
                        notificationService.addNotificationChannels()
                    }

                    // Makes the default Text style bodyMedium instead of bodyLarge
                    ProvideTextStyle(value = AppTypography.bodyMedium) {
                        CompositionLocalProvider(LocalRootSnackbarHostState provides rootSnackbarHostState) {
                            Scaffold(
                                snackbarHost = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.TopCenter,
                                    ) {
                                        SnackbarHost(
                                            hostState = rootSnackbarHostState,
                                            modifier = Modifier.fillMaxWidth(),
                                            snackbar = { data ->
                                                ErrorStyledSnackbar(data)
                                            },
                                        )
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.background,
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                ) {
                                    NavHost(
                                        navController = navControllerState,
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
