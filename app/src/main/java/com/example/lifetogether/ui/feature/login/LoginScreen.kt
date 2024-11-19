package com.example.lifetogether.ui.feature.login

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel
import com.example.lifetogether.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
) {
    val loginViewModel: LoginViewModel = hiltViewModel()

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
                    text = "Login",
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
                        value = loginViewModel.email,
                        onValueChange = { value -> loginViewModel.email = value },
                        label = "Email",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    )
                    CustomTextField(
                        value = loginViewModel.password,
                        onValueChange = { value -> loginViewModel.password = value },
                        label = "Password",
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    )

                    Button(onClick = {
                        loginViewModel.onLoginClicked(
                            onSuccess = { userInformation ->
                                appNavigator?.navigateBack()
                            },
                        )
                    }) {
                        Text(text = "Login")
                    }

                    Text(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                            .clickable { appNavigator?.navigateToSignUp() },
                        text = "Do you not have an account?\nSign up here",
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth(),
                        text = loginViewModel.error,
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
        LoginScreen()
    }
}
