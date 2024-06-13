package com.example.lifetogether.util

import com.example.lifetogether.domain.model.LoginInfo

// Ane
object UserValidationUtil {
    // ------------------------------------------- VALIDATE LOGIN
    fun validateLogin(
        loginInfo: LoginInfo,
    ): ValidationResult {
        // ------------------------------------- EMAIL
        val emailValidationResult: ValidationResult = emailValidation(loginInfo.email)

        if (emailValidationResult is ValidationResult.Error) {
            return ValidationResult.Error(emailValidationResult.message)
        }

        // ------------------------------------- PASSWORD
        val passwordValidationResult: ValidationResult = passwordValidation(loginInfo.password)

        if (passwordValidationResult is ValidationResult.Error) {
            return ValidationResult.Error(passwordValidationResult.message)
        }

        return ValidationResult.Success
    }

    // ------------------------------------------- VALIDATE SIGNUP
    fun validateSignUp(
        loginInfo: LoginInfo,
    ): ValidationResult {
        // ------------------------------------- USERNAME
        if (loginInfo.username.isEmpty()) {
            return ValidationResult.Error("Username cannot be empty")
        }

        // ------------------------------------- EMAIL
        val emailValidationResult: ValidationResult = emailValidation(loginInfo.email)

        if (emailValidationResult is ValidationResult.Error) {
            return ValidationResult.Error(emailValidationResult.message)
        }

        // ------------------------------------- PASSWORD
        if (loginInfo.password != loginInfo.confirmPassword) {
            return ValidationResult.Error("Confirmed password does not match")
        }

        val passwordValidationResult: ValidationResult = passwordValidation(loginInfo.password)

        if (passwordValidationResult is ValidationResult.Error) {
            return ValidationResult.Error(passwordValidationResult.message)
        }

        return ValidationResult.Success
    }

    private fun emailValidation(email: String): ValidationResult {
        if (email.isEmpty()) {
            return ValidationResult.Error("Email cannot be empty")
        }

        val emailRegex = "^([A-Za-z0-9._%+-])+@[a-zA-Z0-9-]+\\.[a-zA-Z.]{2,}\$"
        if (!email.matches(emailRegex.toRegex())) {
            return ValidationResult.Error("Wrong email format")
        }

        return ValidationResult.Success
    }

    private fun passwordValidation(password: String): ValidationResult {
        if (password.isEmpty()) {
            return ValidationResult.Error("Password cannot be empty")
        }

        if (password.length < 6) {
            return ValidationResult.Error("Password must have at least 6 characters")
        }

        return ValidationResult.Success
    }
}
