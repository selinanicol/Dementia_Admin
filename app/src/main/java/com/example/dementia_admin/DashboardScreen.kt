package com.example.dementia_admin

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth // WICHTIG: NEUER IMPORT
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(navController: NavController, selectedPatientId: String) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance().time) }
    val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
    var tasks by remember { mutableStateOf(emptyList<MedicationTask>()) }
    var isEditMode by remember { mutableStateOf(false) }

    // DYNAMISCHE ID für die Datenbankabfrage
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

    DisposableEffect(selectedDate, currentUserId) {
        val dbRef = FirebaseDatabase.getInstance("https://dementia-b4ac2-default-rtdb.europe-west1.firebasedatabase.app/").reference
            .child("patients").child(currentUserId).child("medications")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedTasks = mutableListOf<MedicationTask>()
                val fullDateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val selectedDateStr = fullDateFormatter.format(selectedDate)
                val selectedDayOfWeek = Calendar.getInstance().apply { time = selectedDate }.get(Calendar.DAY_OF_WEEK)

                for (child in snapshot.children) {
                    val rawMed = child.getValue(Medication::class.java) ?: continue

                    // 🔓 Entschlüsseln, bevor wir damit weiterarbeiten!
                    val med = rawMed.copy(
                        name = CryptoHelper.decrypt(rawMed.name),
                        instructions = CryptoHelper.decrypt(rawMed.instructions)
                    )

                    var isMatch = false
                    when (med.recurrence) {
                        "Einmalig" -> isMatch = (med.date == selectedDateStr)
                        "Täglich" -> isMatch = true
                        "Wöchentlich" -> {
                            try {
                                val medDate = fullDateFormatter.parse(med.date)
                                if (medDate != null) isMatch = (Calendar.getInstance().apply { time = medDate }.get(Calendar.DAY_OF_WEEK) == selectedDayOfWeek)
                            } catch (e: Exception) { }
                        }
                    }
                    if (isMatch) {
                        val statusColor = when(med.status) { "erledigt" -> SuccessGreen; "verpasst" -> ErrorRed; else -> Color.Gray }
                        fetchedTasks.add(MedicationTask(med.id, med.name, med.time, med.status, statusColor))
                    }
                }
                fetchedTasks.sortBy { it.time }
                tasks = fetchedTasks
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        dbRef.addValueEventListener(listener)
        onDispose { dbRef.removeEventListener(listener) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = "Logo", modifier = Modifier.size(28.dp)) // LOGO PLATZHALTER
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Plan", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ModernBrandColor, titleContentColor = Color.White, actionIconContentColor = Color.White),
                actions = { IconButton(onClick = { isEditMode = !isEditMode }) { Icon(imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit, contentDescription = "Bearbeiten") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_edit_med/new") }, containerColor = ModernBrandColor, contentColor = Color.White, shape = CircleShape) {
                Icon(Icons.Default.Add, contentDescription = "Neu")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {

            // --- DYNAMISCHE STATUS-KARTE ---
            val hasMissedTask = tasks.any { it.status == "verpasst" }
            val lastCompletedTask = tasks.filter { it.status == "erledigt" }.maxByOrNull { it.time }
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = if (hasMissedTask) Icons.Default.Warning else Icons.Default.CheckCircle, contentDescription = null, tint = if (hasMissedTask) ErrorRed else SuccessGreen, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(if (hasMissedTask) "Achtung: Medikament verpasst!" else "Status: Alles im Zeitplan", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if(hasMissedTask) ErrorRed else Color.Black)
                        Text(if (lastCompletedTask != null) "Letzte Einnahme: ${lastCompletedTask.name} (${lastCompletedTask.time})" else "Heute noch keine Einnahme.", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }

            Text("Datum wählen:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                items(14) { offset ->
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
                    FilterChip(
                        selected = dateFormat.format(cal.time) == dateFormat.format(selectedDate),
                        onClick = { selectedDate = cal.time },
                        label = { Text(dateFormat.format(cal.time)) }
                    )
                }
            }

            Text("Plan für ${dateFormat.format(selectedDate)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))

            if (tasks.isEmpty()) Text("Keine Erinnerungen.", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
            else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(tasks) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = isEditMode) { if (isEditMode) navController.navigate("add_edit_med/${task.id}") },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(task.time, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.width(60.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.name, fontWeight = FontWeight.Medium)
                                    Text(task.status.uppercase(), color = task.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                if (isEditMode) Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ModernBrandColor)
                                else if (task.status == "verpasst") Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationScreen(navController: NavController, patientId: String, medId: String) {
    val isEdit = medId != "new"
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ausstehend") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val defaultDateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    var selectedDateText by remember { mutableStateOf(defaultDateFormatter.format(Date())) }
    var expanded by remember { mutableStateOf(false) }
    val recurrenceOptions = listOf("Einmalig", "Täglich", "Wöchentlich")
    var selectedRecurrence by remember { mutableStateOf(recurrenceOptions[0]) }
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        imageUri = uri
    }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
    val dbRef =
        FirebaseDatabase.getInstance("https://dementia-b4ac2-default-rtdb.europe-west1.firebasedatabase.app/").reference
            .child("patients").child(currentUserId).child("medications")

    LaunchedEffect(medId) {
        if (isEdit) {
            dbRef.child(medId).get().addOnSuccessListener { snapshot ->
                val med = snapshot.getValue(Medication::class.java)
                if (med != null) {
                    name = med.name; time = med.time; instructions =
                        med.instructions; selectedDateText = med.date; selectedRecurrence =
                        med.recurrence; status = med.status
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isEdit) "Bearbeiten" else "Neue Erinnerung",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ModernBrandColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Was muss eingenommen werden?") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // TIME PICKER
            val initialHour =
                if (time.contains(":")) time.split(":")[0].toIntOrNull() ?: Calendar.getInstance()
                    .get(Calendar.HOUR_OF_DAY) else Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val initialMinute =
                if (time.contains(":")) time.split(":")[1].toIntOrNull() ?: Calendar.getInstance()
                    .get(Calendar.MINUTE) else Calendar.getInstance().get(Calendar.MINUTE)
            val timePickerDialog = TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                },
                initialHour,
                initialMinute,
                true
            )
            OutlinedButton(
                onClick = { timePickerDialog.show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (time.isEmpty()) "Uhrzeit auswählen" else "Gewählte Zeit: $time")
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start-Datum: $selectedDateText")
            }
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedRecurrence,
                    onValueChange = { },
                    label = { Text("Wiederholung") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    recurrenceOptions.forEach { selectionOption ->
                        DropdownMenuItem(text = {
                            Text(
                                selectionOption
                            )
                        }, onClick = { selectedRecurrence = selectionOption; expanded = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (imageUri == null) "Foto vom Medikament hinzufügen" else "Foto ausgewählt! ✅")
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Hinweis (z.B. 'Blaue Dose')") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(32.dp))


            // neu eingefügt

            // LÖSCHEN & SPEICHERN
            // LÖSCHEN & SPEICHERN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isEdit) {
                    OutlinedButton(
                        onClick = {
                            dbRef.child(medId).removeValue()
                                .addOnSuccessListener { navController.popBackStack() }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LÖSCHEN", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        // 1. Zuerst die Texte verschlüsseln!
                        val encryptedName = CryptoHelper.encrypt(name)
                        val encryptedInstructions = CryptoHelper.encrypt(instructions)

                        // 2. ID generieren
                        val targetId =
                            if (isEdit) medId else dbRef.push().key ?: java.util.UUID.randomUUID()
                                .toString()

                        // 3. Prüfen ob ein Foto ausgewählt wurde (aus dem vorherigen Schritt)
                        if (imageUri != null) {
                            // Foto hochladen
                            val storageRef =
                                com.google.firebase.storage.FirebaseStorage.getInstance().reference.child(
                                    "medication_images"
                                ).child("$targetId.jpg")
                            storageRef.putFile(imageUri!!).addOnSuccessListener {
                                // Link zum Bild holen
                                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                    // Alles zusammen speichern (mit verschlüsseltem Namen & Bild-URL)
                                    val medWithImage = Medication(
                                        targetId,
                                        encryptedName,
                                        time,
                                        encryptedInstructions,
                                        selectedDateText,
                                        selectedRecurrence,
                                        status,
                                        downloadUrl.toString()
                                    )
                                    dbRef.child(targetId).setValue(medWithImage)
                                        .addOnSuccessListener { navController.popBackStack() }
                                }
                            }
                        } else {
                            // Ohne Foto speichern (aber trotzdem verschlüsselt!)
                            val medWithoutImage = Medication(
                                targetId,
                                encryptedName,
                                time,
                                encryptedInstructions,
                                selectedDateText,
                                selectedRecurrence,
                                status,
                                imageUrl = ""
                            )
                            dbRef.child(targetId).setValue(medWithoutImage)
                                .addOnSuccessListener { navController.popBackStack() }
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ModernBrandColor),
                    enabled = name.isNotBlank() && time.isNotBlank()
                ) { Text("SPEICHERN", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showDatePicker = false; datePickerState.selectedDateMillis?.let {
                            selectedDateText = defaultDateFormatter.format(Date(it))
                        }
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDatePicker = false
                        }) { Text("Abbrechen") }
                    }
                ) { DatePicker(state = datePickerState) }
            }
        }
    }
}