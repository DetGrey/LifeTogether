package com.example.lifetogether

import com.example.lifetogether.domain.model.LoginInfo
import com.example.lifetogether.util.UserValidationUtil
import com.example.lifetogether.util.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Ane
class UserValidationUtilTest {
    // ------------------------------------- EMAIL
    @Test
    fun `empty username returns error`() {
        val result = UserValidationUtil.validateLogin(
            LoginInfo(
                email = "",
                password = "password",
            ),
        )
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `wrong email format returns error`() {
        val result = UserValidationUtil.validateLogin(
            LoginInfo(
                email = "@email.dk",
                password = "password",
            ),
        )
        assertEquals("Wrong email format", (result as ValidationResult.Error).message)
    }

    @Test
    fun `wrong email format returns error 2`() {
        val result = UserValidationUtil.validateLogin(
            LoginInfo(
                email = "hi@.dk",
                password = "password",
            ),
        )
        assertEquals("Wrong email format", (result as ValidationResult.Error).message)
    }

    @Test
    fun `wrong email format returns error 3`() {
        val result = UserValidationUtil.validateLogin(
            LoginInfo(
                email = "hi@email",
                password = "password",
            ),
        )
        assertEquals("Wrong email format", (result as ValidationResult.Error).message)
    }

    @Test
    fun `wrong email format returns error 4`() {
        val result = UserValidationUtil.validateLogin(
            LoginInfo(
                email = "hi@email.2com",
                password = "password",
            ),
        )
        assertEquals("Wrong email format", (result as ValidationResult.Error).message)
    }

    @Test
    fun `valid email format returns success`() {
        val result = UserValidationUtil.validateLogin(
            LoginInfo(
                email = "email@email.com",
                password = "password",
            ),
        )
        assertTrue(result is ValidationResult.Success)
    }

    // ------------------------------------- PASSWORD
    @Test
    fun `empty password returns error`() {
        val result = UserValidationUtil.validateLogin(
            LoginInfo(
                email = "email@email.com",
                password = "",
            ),
        )
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `too short password returns error`() {
        val result = UserValidationUtil.validateLogin(
            LoginInfo(
                email = "email@email.com",
                password = "pass1",
            ),
        )
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `valid email and password returns success`() {
        val result = UserValidationUtil.validateLogin(
            LoginInfo(
                email = "email@email.com",
                password = "password",
            ),
        )
        assertTrue(result is ValidationResult.Success)
    }
}
