package com.example.lifetogether

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val destination = intent?.getStringExtra("destination")

        // [START handle_data_extras]
//        intent.extras?.let {
//            for (key in it.keySet()) {
//                val value = intent.extras?.get(key)
//                Log.d(TAG, "Key: $key Value: $value")
//            }
//        }
        // [END handle_data_extras]

        setContent {
            LifeTogetherTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
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

                    val userInformation by firebaseViewModel.userInformation.collectAsState()
                    when (val familyId = userInformation?.familyId) {
                        is String -> {
                            firebaseViewModel.observeFirestoreFamilyData(familyId)

                            userInformation?.uid?.let { firebaseViewModel.storeFcmToken(it, familyId) }

                            if (destination != null) {
                                navController.navigate(destination)
                            }
                        }
                    }

                    val notificationService = NotificationService(this)
                    notificationService.addNotificationChannels()

                    // Makes the default Text style bodyMedium instead of bodyLarge
                    ProvideTextStyle(value = AppTypography.bodyMedium) {
                        navController = rememberNavController()
                        NavHost(navController = navController, firebaseViewModel = firebaseViewModel)
                    }
                }
            }
        }
    }

//    // Enables Automatic Initialization of FCM
//    // Ensures the app automatically initializes FCM when installed or updated.
//    fun runtimeEnableAutoInit() {
//        // [START fcm_runtime_enable_auto_init]
//        Firebase.messaging.isAutoInitEnabled = true
//        // [END fcm_runtime_enable_auto_init]
//    }
//
//    // Sends a Message to a Device Group
//    // TODO DEPRECATED find a new way to do this! EG .subscribeToTopic()??
//    // Demonstrates using FCM's device group messaging to send notifications.
//    // The to field uses a unique key identifying the group.
//    fun deviceGroupUpstream() {
//        // [START fcm_device_group_upstream]
//        val to = "a_unique_key" // the notification key
//        val msgId = AtomicInteger()
//        Firebase.messaging.send(
//            remoteMessage(to) {
//                setMessageId(msgId.get().toString())
//                addData("hello", "world")
//            },
//        )
//        // [END fcm_device_group_upstream]
//    }
//
//    // Subscribes to a Topic
//    // Devices subscribed to the same topic can receive the same notifications.
//    fun subscribeTopics() {
//        // [START subscribe_topics]
//        Firebase.messaging.subscribeToTopic("weather")
//            .addOnCompleteListener { task ->
//                var msg = "Subscribed"
//                if (!task.isSuccessful) {
//                    msg = "Subscribe failed"
//                }
//                Log.d(TAG, msg)
//                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//            }
//        // [END subscribe_topics]
//    }
}
