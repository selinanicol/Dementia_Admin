package com.example.dementia_admin

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// --- DataStore Initialisierung (Einmalig für die ganze App) ---
val Context.dataStore by preferencesDataStore(name = "settings")
val PUSH_ENABLED = booleanPreferencesKey("push_enabled")
val SOUND_ENABLED = booleanPreferencesKey("sound_enabled") // Zweiter Key für die Töne

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val preferences by context.dataStore.data.collectAsState(initial = emptyPreferences())

    val pushEnabled = preferences[PUSH_ENABLED] ?: true
    val soundEnabled = preferences[SOUND_ENABLED] ?: true

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Einstellungen", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ModernBrandColor, // Zieht die Farbe automatisch aus der MainActivity
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- BENACHRICHTIGUNGEN ---
            Text("Alarme & Benachrichtigungen", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ModernBrandColor)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Push-Benachrichtigungen", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("Warnungen bei verpassten Medis", fontSize = 14.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = pushEnabled,
                            onCheckedChange = { isChecked ->
                                // Neuen Wert im DataStore speichern
                                coroutineScope.launch {
                                    context.dataStore.edit { settings ->
                                        settings[PUSH_ENABLED] = isChecked
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = ModernBrandColor)
                        )
                    }
                    HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Laute Töne abspielen", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("Alarmton bei Notfällen", fontSize = 14.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { isChecked ->
                                // Neuen Wert im DataStore speichern
                                coroutineScope.launch {
                                    context.dataStore.edit { settings ->
                                        settings[SOUND_ENABLED] = isChecked
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = ModernBrandColor),
                            enabled = pushEnabled // Ton-Schalter nur aktiv, wenn Push aktiv ist
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- MEDIZINISCHE BERICHTE ---
            Text("Für den Arztbesuch", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ModernBrandColor)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        // 1. Firebase-Datenbank aufrufen
                        val db = FirebaseDatabase.getInstance("https://dementia-b4ac2-default-rtdb.europe-west1.firebasedatabase.app/").reference

                        // 2. ID des verknüpften Patienten abfragen
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

                        // 3. Medikamente einmalig abrufen (.get())
                        db.child("patients").child(currentUserId).child("medications").get().addOnSuccessListener { snapshot ->
                            val medicationList = mutableListOf<Medication>()

                            // Alle Medikamente aus Firebase in eine Liste packen
                            for (child in snapshot.children) {
                                val med = child.getValue(Medication::class.java)
                                if (med != null) {
                                    medicationList.add(med)
                                }
                            }

                            // 4. Unsere Export-Funktion mit der fertigen Liste aufrufen!
                            if (medicationList.isNotEmpty()) {
                                exportDataToDoctor(context, medicationList)
                            } else {
                                // Optional: Kurze Meldung, wenn es keine Daten gibt
                                android.widget.Toast.makeText(context, "Keine Daten zum Exportieren gefunden.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = ModernBrandColor, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Aktivitäten-Bericht exportieren", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text("Letzte 14 Tage als Text teilen", fontSize = 14.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- APP INFO ---
            Text(
                text = "App-Version 1.0.0 (Prototyp)",
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }
    }
}

fun exportDataToDoctor(context: Context, medicationList: List<Medication>) {
    val exportBuilder = StringBuilder()
    exportBuilder.append("Medikamenten-Report:\n\n")

    medicationList.forEach { med ->
        exportBuilder.append("- ${med.date} ${med.time}: ${med.name} -> Status: ${med.status}\n")
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Medikamentenbericht Patient")
        putExtra(Intent.EXTRA_TEXT, exportBuilder.toString())
    }

    context.startActivity(Intent.createChooser(shareIntent, "Bericht senden via..."))
}