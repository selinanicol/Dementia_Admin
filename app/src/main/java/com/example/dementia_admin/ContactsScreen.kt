package com.example.dementia_admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import com.google.firebase.auth.FirebaseAuth // WICHTIGER NEUER IMPORT
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(navController: NavController, patientId: String) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(emptyList<CareContact>()) }

    // dynamische id abfragen
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

    DisposableEffect(currentUserId) {
        val dbRef = FirebaseDatabase.getInstance("https://dementia-b4ac2-default-rtdb.europe-west1.firebasedatabase.app/").reference
            .child("patients").child(currentUserId).child("contacts")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CareContact>()
                for (child in snapshot.children) child.getValue(CareContact::class.java)?.let { list.add(it) }
                contacts = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        dbRef.addValueEventListener(listener)
        onDispose { dbRef.removeEventListener(listener) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Betreungskreis", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ModernBrandColor, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Deine Kontaktpersonen", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (contacts.isEmpty()) Text("Bisher keine Kontakte hinterlegt.", color = Color.Gray)
            else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    items(contacts) { contact ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color.LightGray)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(contact.role, color = Color.Gray, fontSize = 14.sp)
                                }
                                IconButton(onClick = { navController.navigate("add_edit_contact/${contact.id}") }) { Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = ModernBrandColor) }
                                IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))) }, modifier = Modifier.background(Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp))) { Icon(Icons.Default.Phone, contentDescription = "Anrufen", tint = SuccessGreen) }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.phone}"))) }, modifier = Modifier.background(Color(0xFFE0F7FA), shape = RoundedCornerShape(8.dp))) { Icon(Icons.Default.Email, contentDescription = "Nachricht", tint = ModernBrandColor) }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("add_edit_contact/new") },
                modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ModernBrandColor)
            ) { Text("NEUEN KONTAKT HINZUFÜGEN", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactScreen(navController: NavController, patientId: String, contactId: String) {
    val isEdit = contactId != "new"
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // dynamische id abfragen
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

    val dbRef = FirebaseDatabase.getInstance("https://dementia-b4ac2-default-rtdb.europe-west1.firebasedatabase.app/").reference
        .child("patients").child(currentUserId).child("contacts")

    LaunchedEffect(contactId) {
        if (isEdit) {
            dbRef.child(contactId).get().addOnSuccessListener { snapshot ->
                snapshot.getValue(CareContact::class.java)?.let { name = it.name; role = it.role; phone = it.phone }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEdit) "Kontakt bearbeiten" else "Neuer Kontakt", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ModernBrandColor, titleContentColor = Color.White, navigationIconContentColor = Color.White),
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Zurück") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize()) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (z.B. Maria)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Beschreibung (z.B. Tochter, Arzt)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefonnummer") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isEdit) {
                    OutlinedButton(
                        onClick = { dbRef.child(contactId).removeValue().addOnSuccessListener { navController.popBackStack() } },
                        modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed), border = BorderStroke(1.dp, ErrorRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LÖSCHEN", fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = {
                        val targetId = if (isEdit) contactId else dbRef.push().key ?: UUID.randomUUID().toString()
                        dbRef.child(targetId).setValue(CareContact(targetId, name.trim(), role.trim(), phone.trim())).addOnSuccessListener { navController.popBackStack() }
                    },
                    modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ModernBrandColor), enabled = name.isNotBlank() && phone.isNotBlank()
                ) { Text("SPEICHERN", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}