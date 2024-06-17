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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.CustomTextField
import com.example.lifetogether.ui.common.DatePickerTextField
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.CustomDatePickerDialog
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AuthViewModel
import com.example.lifetogether.ui.viewmodel.SignUpViewModel

@Composable
fun SignupScreen(
    appNavigator: AppNavigator? = null,
    authViewModel: AuthViewModel? = null,
) {
    val signupViewModel: SignUpViewModel = viewModel()

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            item {
                TopBar(
                    leftIcon = Icon(
                        resId = R.drawable.ic_back_arrow,
                        description = "back arrow icon",
                    ),
                    onLeftClick = {
                        appNavigator?.navigateBack()
                    },
                    text = "Sign up",
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    CustomTextField(
                        value = signupViewModel.name,
                        onValueChange = { value -> signupViewModel.name = value },
                        label = "Name",
                        capitalization = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    )
                    CustomTextField(
                        value = signupViewModel.email,
                        onValueChange = { value -> signupViewModel.email = value },
                        label = "Email",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    )
                    DatePickerTextField(
                        label = "Birthday",
                        date = signupViewModel.birthday,
                        onClick = { signupViewModel.birthdayExpanded = true }, // TODO
                    )
                    CustomTextField(
                        value = signupViewModel.password,
                        onValueChange = { value -> signupViewModel.password = value },
                        label = "Password",
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    )
                    CustomTextField(
                        value = signupViewModel.confirmPassword,
                        onValueChange = { value -> signupViewModel.confirmPassword = value },
                        label = "Confirm password",
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    )

                    Button(onClick = {
                        signupViewModel.onSignUpClicked(
                            onSuccess = { userInformation ->
                                authViewModel?.updateUserInformation(userInformation)
                                appNavigator?.navigateToProfile()
                            },
                        )
                    }) {
                        Text(text = "Sign up")
                    }

                    Text(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                            .clickable { appNavigator?.navigateToLogin() },
                        text = "Do you already have an account?\nLogin here",
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (signupViewModel.birthdayExpanded) {
            CustomDatePickerDialog(
                selectedDate = signupViewModel.birthday,
                onDateSelected = { date ->
                    signupViewModel.birthday = date
                    signupViewModel.birthdayExpanded = false
                },
                onDismiss = {
                    signupViewModel.birthdayExpanded = false
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignupScreenPreview() {
    LifeTogetherTheme {
        SignupScreen()
    }
}
