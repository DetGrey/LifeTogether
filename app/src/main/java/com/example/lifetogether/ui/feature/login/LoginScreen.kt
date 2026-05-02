package com.example.lifetogether.ui.feature.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onUiEvent: (LoginUiEvent) -> Unit,
    onNavigationEvent: (LoginNavigationEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(LoginNavigationEvent.NavigateBack)
                },
                text = "Login",
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(LifeTogetherTokens.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xLarge),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = LifeTogetherTokens.spacing.xLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                ) {
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

                    TextDefault(
                        modifier = Modifier
                            .padding(top = LifeTogetherTokens.spacing.small)
                            .fillMaxWidth()
                            .clickable { onNavigationEvent(LoginNavigationEvent.SignUpClicked) },
                        text = "Do you not have an account?\nSign up here",
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
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
