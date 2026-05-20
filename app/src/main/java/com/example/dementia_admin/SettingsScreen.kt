package com.example.dementia_admin

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
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
import kotlinx.coroutines.launch
import android.print.PrintAttributes
import android.print.PrintManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

            // Medizinischer Bericht
            Text("Für den Arztbesuch", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ModernBrandColor)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val db = FirebaseDatabase.getInstance("https://dementia-b4ac2-default-rtdb.europe-west1.firebasedatabase.app/").reference

                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

                        db.child("patients").child(currentUserId).child("medications").get().addOnSuccessListener { snapshot ->
                            val medicationList = mutableListOf<Medication>()

                            for (child in snapshot.children) {
                                val med = child.getValue(Medication::class.java)
                                if (med != null) {
                                    medicationList.add(med)
                                }
                            }

                            if (medicationList.isNotEmpty()) {
                                exportDataToDoctor(context, medicationList)
                            } else {
                                android.widget.Toast.makeText(context, "Keine Daten zum Exportieren gefunden.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = ModernBrandColor, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Medikamenten Einnahmebericht exportieren", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text("Letzte 14 Tage", fontSize = 14.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "App-Version 2.0.0 (Prototyp)",
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }
    }
}

fun exportDataToDoctor(context: Context, medicationList: List<Medication>) {
    val erledigtCount = medicationList.count { it.status.lowercase() == "erledigt" }
    val verpasstCount = medicationList.count { it.status.lowercase() == "verpasst" }

    val currentTimestamp = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date())

    // HTML/Css styled PDF
    val htmlBuilder = StringBuilder()
    htmlBuilder.append("""
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <style>
            body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #2c3e50; line-height: 1.4; padding: 10px; }
            .header { border-bottom: 2px solid #2c3e50; padding-bottom: 8px; margin-bottom: 20px; }
            .header h1 { font-size: 22px; margin: 0; color: #2c3e50; }
            .header .subtitle { font-size: 13px; color: #7f8c8d; margin-top: 4px; }
            
            .meta-box { background-color: #f8f9fa; border: 1px solid #e2e8f0; border-radius: 6px; padding: 12px; margin-bottom: 20px; }
            .meta-table { width: 100%; border-collapse: collapse; font-size: 12px; }
            .meta-table td { padding: 4px; vertical-align: top; }
            .meta-table td.label { font-weight: bold; color: #34495e; width: 18%; }
            
            .metrics { width: 100%; margin-bottom: 25px; border-collapse: collapse; }
            .metric-card { background-color: #f1f5f9; border-left: 4px solid #0284c7; padding: 10px; font-size: 12px; }
            .metric-card.success { border-left-color: #16a34a; background-color: #f0fdf4; }
            .metric-card.danger { border-left-color: #dc2626; background-color: #fef2f2; }
            .metric-value { font-size: 18px; font-weight: bold; color: #0f172a; margin-top: 2px; }
            
            h2 { font-size: 15px; border-bottom: 1px solid #cbd5e1; padding-bottom: 5px; margin-top: 20px; }
            .log-table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 12px; }
            .log-table th { background-color: #f8fafc; color: #475569; font-weight: bold; text-align: left; padding: 8px; border-bottom: 2px solid #cbd5e1; }
            .log-table td { padding: 8px; border-bottom: 1px solid #e2e8f0; }
            
            .badge { display: inline-block; padding: 3px 6px; font-size: 11px; font-weight: bold; border-radius: 4px; }
            .status-erledigt { background-color: #dcfce7; color: #15803d; }
            .status-verpasst { background-color: #fee2e2; color: #b91c1c; }
            .notes { font-style: italic; color: #64748b; }
        </style>
        </head>
        <body>
            <div class="header">
                <h1>Medikamenten-Einnahmebericht</h1>
                <div class="subtitle">Medizinisches Protokoll zur Vorlage beim behandelnden Arzt</div>
            </div>
            
            <div class="meta-box">
                <table class="meta-table">
                    <tr>
                        <td class="label">Patient:</td><td>Anton Meier</td>
                        <td class="label">Zeitraum:</td><td>Letzte 14 Tage</td>
                    </tr>
                    <tr>
                        <td class="label">Geburtsdatum:</td><td>14.03.1940 (86 J.)</td>
                        <td class="label">Exportdatum:</td><td>$currentTimestamp Uhr</td>
                    </tr>
                </table>
            </div>
            
            <table class="metrics">
                <tr>
                    <td style="width: 33%;">
                        <div class="metric-card danger">
                            <div class="metric-value">$verpasstCount</div>
                            <div style="color: #64748b; font-size: 10px; font-weight: bold; text-transform: uppercase;">Verpasste Einnahmen</div>
                        </div>
                    </td>
                </tr>
            </table>
            
            <h2>Chronologisches Einnahmeprotokoll</h2>
            <table class="log-table">
                <thead>
                    <tr>
                        <th>Datum/Zeit</th>
                        <th>Medikament</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
    """.trimIndent())

    // fill with data
    medicationList.forEach { med ->
        val isErledigt = med.status.lowercase() == "erledigt"
        val badgeClass = if (isErledigt) "status-erledigt" else "status-verpasst"
        val badgeLabel = if (isErledigt) "Erledigt" else "Verpasst"
        val notesText = med.notes ?: "-"

        htmlBuilder.append("""
            <tr>
                <td>${med.date} &ndash; ${med.time} Uhr</td>
                <td style="font-weight: 500;">${med.name}</td>
                <td><span class="badge $badgeClass">$badgeLabel</span></td>
            </tr>
        """.trimIndent())
    }

    htmlBuilder.append("</tbody></table></body></html>")

    // printManager
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = webView.createPrintDocumentAdapter("Medikamentenbericht_\$currentTimestamp")

            val jobName = "Medikamentenbericht_Patient"
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }

    // render PDF
    webView.loadDataWithBaseURL(null, htmlBuilder.toString(), "text/html", "utf-8", null)
}