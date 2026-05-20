package com.example.dementia_admin

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class EscalationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun checkEmergencyButtonsExist() {
        // 1. Arrange (Wir starten den Screen isoliert im Test-Modus)
        composeTestRule.setContent {
            // Wir brauchen einen Dummy-NavController für den Test
            val navController = rememberNavController()
            // Wir rufen deinen roten Alarm-Screen auf
            EscalationScreen(navController = navController, patientId = "dummy_1", medId = "med_1")
        }

        // 2. Assert (Wir suchen die Buttons auf dem Bildschirm und prüfen, ob man sie klicken kann)
        composeTestRule.onNodeWithText("MEDIKAMENT VERPASST!").assertIsDisplayed()
        composeTestRule.onNodeWithText("ANRUFEN").assertIsDisplayed().assertHasClickAction()
        composeTestRule.onNodeWithText("KÜMMERE MICH").assertIsDisplayed().assertHasClickAction()
    }
}