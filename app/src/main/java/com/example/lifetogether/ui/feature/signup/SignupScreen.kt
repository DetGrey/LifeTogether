package com.example.lifetogether.ui.feature.signup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import com.example.lifetogether.ui.common.dialog.CustomDatePickerDialog
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.common.textfield.DatePickerTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun SignupScreen(
    uiState: SignupUiState,
    onUiEvent: (SignupUiEvent) -> Unit,
    onNavigationEvent: (SignupNavigationEvent) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.padding(LifeTogetherTokens.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xLarge),
        ) {
            item {
                TopBar(
                    leftIcon = Icon(
                        resId = R.drawable.ic_back_arrow,
                        description = "back arrow icon",
                    ),
                    onLeftClick = {
                        onNavigationEvent(SignupNavigationEvent.NavigateBack)
                    },
                    text = "Sign up",
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = LifeTogetherTokens.spacing.xLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                ) {
                    CustomTextField(
                        value = uiState.name,
                        onValueChange = { value -> onUiEvent(SignupUiEvent.NameChanged(value)) },
                        label = "Name",
                        capitalization = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    )
                    CustomTextField(
                        value = uiState.email,
                        onValueChange = { value -> onUiEvent(SignupUiEvent.EmailChanged(value)) },
                        label = "Email",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    )
                    DatePickerTextField(
                        label = "Birthday",
                        date = uiState.birthday,
                        onClick = {
                            onUiEvent(SignupUiEvent.BirthdayClicked)
                        },
                    )
                    CustomTextField(
                        value = uiState.password,
                        onValueChange = { value -> onUiEvent(SignupUiEvent.PasswordChanged(value)) },
                        label = "Password",
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    )
                    CustomTextField(
                        value = uiState.confirmPassword,
                        onValueChange = { value -> onUiEvent(SignupUiEvent.ConfirmPasswordChanged(value)) },
                        label = "Confirm password",
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    )

                    Button(
                        onClick = {
                            onUiEvent(SignupUiEvent.SignUpClicked)
                        },
                    ) {
                        Text(text = "Sign up")
                    }

                    TextDefault(
                        modifier = Modifier
                            .padding(top = LifeTogetherTokens.spacing.small)
                            .fillMaxWidth()
                            .clickable { onNavigationEvent(SignupNavigationEvent.LoginClicked) },
                        text = "Do you already have an account?\nLogin here",
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (uiState.showBirthdayPicker) {
            CustomDatePickerDialog(
                selectedDate = uiState.birthday,
                onDateSelected = { date ->
                    onUiEvent(SignupUiEvent.BirthdaySelected(date))
                },
                onDismiss = {
                    onUiEvent(SignupUiEvent.BirthdayDismissed)
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignupScreenPreview() {
    LifeTogetherTheme {
        SignupScreen(
            uiState = SignupUiState(
                name = "Alex",
                email = "alex@example.com",
                birthday = null,
                password = "password123",
                confirmPassword = "password123",
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
