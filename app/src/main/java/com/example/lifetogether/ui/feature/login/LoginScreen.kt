package com.example.aca.ui.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.aca.ui.viewmodel.LoginViewModel
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.NavHost

@Composable
fun LoginScreen(
    appNavigator: AppNavigator,
) {
    val loginViewModel: LoginViewModel = viewModel()

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(text = loginViewModel.error)

            TextField(
                value = loginViewModel.email,
                onValueChange = { loginViewModel.email = it },
                label = {
                    Text(text = "Email")
                },
            )

            TextField(
                value = loginViewModel.password,
                onValueChange = { loginViewModel.password = it },
                label = {
                    Text(text = "Password")
                },
                visualTransformation = PasswordVisualTransformation(),
            )

            Button(onClick = { loginViewModel.onLoginClicked() }) {
                Text(text = "Login")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = { appNavigator.navigateToSignUp() }) {
                Text(text = "Create account")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    NavHost(navController = rememberNavController())
}
