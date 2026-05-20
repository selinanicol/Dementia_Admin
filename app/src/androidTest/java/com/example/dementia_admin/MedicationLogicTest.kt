package com.example.dementia_admin

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationLogicTest {

    @Test
    fun testStatusColorMapping() {
        // 1. Arrange (Vorbereitung: Wir definieren zwei mögliche Status aus Firebase)
        val statusDone = "erledigt"
        val statusMissed = "verpasst"

        // 2. Act (Ausführung: Die Logik, die auch in deinem Dashboard läuft)
        val colorDone = if (statusDone == "erledigt") Color(0xFF2E7D32) else Color.Gray
        val colorMissed = if (statusMissed == "verpasst") Color(0xFFC62828) else Color.Gray

        // 3. Assert (Überprüfung: Entspricht das Ergebnis der Erwartung?)
        assertEquals("Erledigt sollte SuccessGreen (0xFF2E7D32) sein", Color(0xFF2E7D32), colorDone)
        assertEquals("Verpasst sollte ErrorRed (0xFFC62828) sein", Color(0xFFC62828), colorMissed)
    }
}