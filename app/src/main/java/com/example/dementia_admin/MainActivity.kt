package com.example.dementia_admin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.google.firebase.auth.FirebaseAuth
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val serviceIntent = Intent(this, AdminAlertService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.LightGray) {
                    BiometricSecurityWrapper(this) {
                        AdminNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricSecurityWrapper(activity: FragmentActivity, content: @Composable () -> Unit) {
    var isAuthenticated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isAuthenticated = true
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Admin-App entsperren")
            .setSubtitle("Sensible medizinische Daten schützen")
            .setNegativeButtonText("Abbrechen")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    if (isAuthenticated) {
        content()
    } else {
        // Sperrbildschirm, falls der Dialog weggeklickt wurd
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Gesperrt", modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("App ist gesperrt", fontSize = 24.sp)
        }
    }
}

@Composable
fun AdminNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val startScreen = if (auth.currentUser != null) "dashboard" else "login_screen"
    var selectedPatientId by remember { mutableStateOf("patient_1") }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val activity = LocalActivity.current as? android.app.Activity
    LaunchedEffect(activity?.intent) {
        val intent = activity?.intent
        if (intent != null && intent.getBooleanExtra("OPEN_ESCALATION", false)) {
            val medId = intent.getStringExtra("MED_ID") ?: ""
            val patientId = intent.getStringExtra("PATIENT_ID") ?: ""

            intent.removeExtra("OPEN_ESCALATION")


            if (medId.isNotEmpty() && patientId.isNotEmpty()) {
                navController.navigate("escalation/$patientId/$medId")
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute != "login_screen" &&
                currentRoute?.startsWith("add_edit_") == false &&
                currentRoute?.startsWith("escalation") == false) {
                BottomNavigationBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(navController = navController, startDestination = startScreen, modifier = Modifier.padding(paddingValues)) {
            composable("login_screen") { LoginScreen(navController) }
            composable("dashboard") { AdminDashboard(navController, selectedPatientId) }

            composable("add_edit_med/{medId}") { backStackEntry ->
                val medId = backStackEntry.arguments?.getString("medId") ?: "new"
                AddEditMedicationScreen(navController, selectedPatientId, medId)
            }

            composable("contacts") { ContactsScreen(navController, selectedPatientId) }

            composable("add_edit_contact/{contactId}") { backStackEntry ->
                val contactId = backStackEntry.arguments?.getString("contactId") ?: "new"
                AddEditContactScreen(navController, selectedPatientId, contactId)
            }

            composable("profile") {
                ProfileScreen(navController, selectedPatientId) { newId -> selectedPatientId = newId }
            }
            composable("settings") { SettingsScreen(navController) }

            composable(
                route = "escalation/{patientId}/{medId}",
                deepLinks = listOf(navDeepLink { uriPattern = "adminapp://escalation/{patientId}/{medId}" })
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
                val medId = backStackEntry.arguments?.getString("medId") ?: ""
                EscalationScreen(navController, patientId, medId)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Triple("dashboard", "Plan", Icons.Default.DateRange),
        Triple("contacts", "Kontakte", Icons.Default.Phone),
        Triple("profile", "Profil", Icons.Default.Person),
        Triple("settings", "Einstellungen", Icons.Default.Settings)
    )

    NavigationBar(containerColor = Color.White) {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { (route, title, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = title) },
                label = { Text(title) },
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}