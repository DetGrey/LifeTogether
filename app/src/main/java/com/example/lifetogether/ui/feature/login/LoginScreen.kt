package com.example.lifetogether.ui.feature.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextLabel
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onUiEvent: (LoginUiEvent) -> Unit,
    onNavigationEvent: (LoginNavigationEvent) -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = LifeTogetherTokens.spacing.xLarge)
                .padding(bottom = LifeTogetherTokens.spacing.bottomInsetLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = LifeTogetherTokens.spacing.medium,
                alignment = Alignment.CenterVertically
            ),
        ) {
            Text(
                text = "Login",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            CustomTextField(
                value = uiState.email,
                onValueChange = { value -> onUiEvent(LoginUiEvent.EmailChanged(value)) },
                label = "Email",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )
            CustomTextField(
                value = uiState.password,
                onValueChange = { value -> onUiEvent(LoginUiEvent.PasswordChanged(value)) },
                label = "Password",
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            )

            PrimaryButton(
                text = "Login",
                onClick = { onUiEvent(LoginUiEvent.LoginClicked) },
                loading = uiState.isLoading,
            )

            Column(
                modifier = Modifier
                    .padding(top = LifeTogetherTokens.spacing.small)
                    .fillMaxWidth()
                    .clickable { onNavigationEvent(LoginNavigationEvent.SignUpClicked) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextLabel(
                    text = "Do you not have an account?"
                )
                TextDefault(
                    text = "Sign up here >",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    LifeTogetherTheme {
        LoginScreen(
            uiState = LoginUiState(
                email = "alex@example.com",
                password = "password123",
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
