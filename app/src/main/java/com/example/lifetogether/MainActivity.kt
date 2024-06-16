package com.example.lifetogether

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.lifetogether.ui.navigation.NavHost
import com.example.lifetogether.ui.theme.AppTypography
import com.example.lifetogether.ui.theme.LifeTogetherTheme

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeTogetherTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // Makes the default Text style bodyMedium instead of bodyLarge
                    ProvideTextStyle(value = AppTypography.bodyMedium) {
                        navController = rememberNavController()
                        NavHost(navController = navController)
                    }
                }
            }
        }
    }
}
