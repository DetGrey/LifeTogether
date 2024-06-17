package com.example.lifetogether.domain.usecase.user

class UserValidationUseCase {

    fun emailValidation(email: String): Boolean {
//        if (email.isEmpty()) {
//            return false
//        }
//
//        if (email.length < 8) { // minimum: aa@aa.aa
//            return false
//        }
//
//        if (!email.contains("@")) {
//            return false
//        }
//
//        if (!email.contains(".")) {
//            return false
//        }
//
//        return true

        // ANOTHER WAY
        val emailRegex = "^([A-Za-z0-9._%+-])+@[a-zA-Z0-9-]+\\.[a-zA-Z.]{2,}\$"
        return email.matches(emailRegex.toRegex())
    }

    fun passwordValidation(password: String): Boolean {
        if (password.isEmpty()) {
            return false
        }

        if (password.length < 6) {
            return false
        }

        if (password.count { it.isDigit() } < 2) {
            return false
        }

        if (password.count { it.isUpperCase() } < 1) {
            return false
        }

        if (password.count { it.isLowerCase() } < 1) {
            return false
        }

        return true
    }
}
