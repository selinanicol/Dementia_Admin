package com.example.dementia_admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun checkLoginUIElementsAreDisplayed() {
        // 1. Arrange: Wir starten den LoginScreen
        composeTestRule.setContent {
            val navController = rememberNavController()
            LoginScreen(navController = navController)
        }

        // 2. Assert: Wir prüfen, ob alle wichtigen Texte und Felder sichtbar sind
        composeTestRule.onNodeWithText("Admin Login").assertIsDisplayed()
        composeTestRule.onNodeWithText("E-Mail Adresse").assertIsDisplayed()
        composeTestRule.onNodeWithText("Passwort").assertIsDisplayed()
        composeTestRule.onNodeWithText("EINLOGGEN").assertIsDisplayed()
    }
}