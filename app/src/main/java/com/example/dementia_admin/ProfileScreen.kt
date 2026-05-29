package com.example.dementia_admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, selectedPatientId: String, onPatientSelected: (String) -> Unit) {
    var patients by remember { mutableStateOf(emptyList<Patient>()) }
    val auth = FirebaseAuth.getInstance() // für logout

    LaunchedEffect(Unit) {
        val dbRef = FirebaseDatabase.getInstance("https://dementia-b4ac2-default-rtdb.europe-west1.firebasedatabase.app/").reference.child("patients")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Patient>()
                for (child in snapshot.children) list.add(Patient(id = child.key ?: "", name = child.child("name").getValue(String::class.java) ?: "Patient ${child.key}"))
                if (list.isEmpty()) list.add(Patient("patient_1", "Patient 1"))
                patients = list
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Profil", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ModernBrandColor, titleContentColor = Color.White)) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color.LightGray)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Angemeldet als:", color = Color.Gray)
                    Text(auth.currentUser?.email ?: "Unbekannt", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("Betreute Personen", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(patients) { patient ->
                    val isSelected = patient.id == selectedPatientId
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onPatientSelected(patient.id) },
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE0F7FA) else Color.White),
                        border = if (isSelected) BorderStroke(2.dp, ModernBrandColor) else BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isSelected, onClick = { onPatientSelected(patient.id) }, colors = RadioButtonDefaults.colors(selectedColor = ModernBrandColor))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(patient.name, fontSize = 20.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // logo in Profile
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        auth.signOut()
                        navController.navigate("login_screen") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Abmelden", tint = ErrorRed)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Abmelden", fontSize = 18.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}