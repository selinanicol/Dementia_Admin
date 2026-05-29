package com.example.dementia_admin

import androidx.compose.ui.graphics.Color

// Farb-Konstanten
val AdminBlue = Color(0xFF0D47A1)
val ModernBrandColor = Color(0xFF22385A)
val SuccessGreen = Color(0xFF2E7D32)
val ErrorRed = Color(0xFFC62828)
val BackgroundGray = Color(0xFFF8F9FA)

// Datenmodelle
data class Patient(val id: String = "", val name: String = "")

data class CareContact(val id: String = "", val name: String = "", val role: String = "", val phone: String = "")

data class Medication(
    val id: String = "",
    val name: String = "",
    val time: String = "",
    val instructions: String = "",
    val date: String = "",
    val recurrence: String = "",
    val status: String = "ausstehend",
    val imageUrl: String = "",
    val notes: String? = null
)

data class MedicationTask(
    val id: String,
    val name: String,
    val time: String,
    val status: String,
    val color: Color
)