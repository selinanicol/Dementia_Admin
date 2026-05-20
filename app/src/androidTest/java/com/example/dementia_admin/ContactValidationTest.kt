package com.example.dementia_admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactValidationTest {

    // Wir lagern die Logik aus dem Screen kurz in eine Hilfsfunktion aus, um sie zu testen
    private fun isSaveButtonEnabled(name: String, phone: String): Boolean {
        return name.isNotBlank() && phone.isNotBlank()
    }

    @Test
    fun testContactFormValidation() {
        // Test 1: Alles korrekt ausgefüllt -> Button sollte aktiv (true) sein
        assertTrue(isSaveButtonEnabled("Maria Tochter", "01512345678"))

        // Test 2: Telefonnummer fehlt -> Button muss inaktiv (false) sein
        assertFalse(isSaveButtonEnabled("Maria Tochter", ""))

        // Test 3: Name besteht nur aus Leerzeichen -> Button muss inaktiv (false) sein
        assertFalse(isSaveButtonEnabled("   ", "01512345678"))

        // Test 4: Beides leer -> Button muss inaktiv (false) sein
        assertFalse(isSaveButtonEnabled("", ""))
    }
}