package com.example.alcoholmonitor

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.alcoholmonitor.ui.theme.AlcoholMonitorTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private lateinit var auth: FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        testFirestore()
        auth = FirebaseAuth.getInstance()

        setContent {
            AlcoholMonitorTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        bottomBar = { BottomNavigationBar(navController) }
                    ) { innerPadding ->
                        NavigationHost(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

fun testFirestore() {
    val db = Firebase.firestore

    // Test writing data
    val testData = hashMapOf("testField" to "Hello Firestore!")
    db.collection("testCollection")
        .add(testData)
        .addOnSuccessListener { documentReference ->
            Log.d("FirestoreTest", "DocumentSnapshot added with ID: ${documentReference.id}")
        }
        .addOnFailureListener { e ->
            Log.w("FirestoreTest", "Error adding document", e)
        }
}

fun signUp(context: ComponentActivity, email: String, password: String) {
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(context) { task ->
            if (task.isSuccessful) {
                Log.d("Auth", "Signup successful!")
            } else {
                Log.w("Auth", "Signup failed", task.exception)
            }
        }
}

fun login(context: ComponentActivity, email: String, password: String) {
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener(context) { task ->
            if (task.isSuccessful) {
                Log.d("Auth", "Login successful!")
            } else {
                Log.w("Auth", "Login failed", task.exception)
            }
        }
}

@Composable
fun NavigationHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.SignIn.route,
        modifier = modifier
    ) {
        composable(Screen.SignIn.route) { SignInScreen(navController = navController, auth = auth) }
        composable(Screen.Account.route) { AccountScreen(navController = navController, auth = auth) }
        composable(Screen.AddAlcohol.route) { AddAlcoholScreen() }
        composable(Screen.List.route) { ListScreen() }
    }
}

@Composable
fun SignInScreen(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Up Button
        Button(onClick = {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("Auth", "Signup successful!")
                        navController.navigate(Screen.AddAlcohol.route) // Navigate after successful sign-up
                    } else {
                        Log.w("Auth", "Signup failed", task.exception)
                    }
                }
        }) {
            Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Log In Button
        Button(onClick = {
            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("Auth", "Login successful!")
                                navController.navigate(Screen.AddAlcohol.route) // Navigate after successful login
                            } else {
                                Log.w("Auth", "Login failed", task.exception)
                            }
                        }
                } else {
                    Log.w("Auth", "Invalid email format")
                }
            } else {
                Log.w("Auth", "Email or password cannot be empty")
            }
        }) {
            Text("Log In")
        }
    }
}

@Composable
fun AccountScreen(navController: NavController, auth: FirebaseAuth) {
    val user = auth.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Account Details", style = MaterialTheme.typography.headlineMedium)

        user?.email?.let {
            Text(text = "Email: $it", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            auth.signOut()
            navController.navigate(Screen.SignIn.route) {
                // Clear backstack to prevent returning to the previous screen
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }) {
            Text("Log Out")
        }
    }
}

@Composable
fun AddAlcoholScreen() {
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<String>()) } // Stores matching results
    val db = Firebase.firestore

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Search Bar
        TextField(
            value = searchText,
            onValueChange = { newText ->
                searchText = newText
                searchAlcoholBrands(newText) { results ->
                    searchResults = results
                }
            },
            label = { Text("Search Alcohol Brand") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display search results
        LazyColumn {
            items(searchResults.size) { index ->
                Text(
                    text = searchResults[index], // Correctly reference each item in the list
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// Function to query Firestore for matching brands
fun searchAlcoholBrands(query: String, onResult: (List<String>) -> Unit) {
    if (query.isEmpty()) {
        onResult(emptyList())
        return
    }

    val db = Firebase.firestore
    db.collection("lager_data")
        .whereGreaterThanOrEqualTo("brands", query)
        .whereLessThanOrEqualTo("brands", query + "\uf8ff") // Firestore text search trick
        .get()
        .addOnSuccessListener { documents ->
            val brands = documents.mapNotNull { it.getString("brands") }
            onResult(brands)
        }
        .addOnFailureListener {
            Log.e("Firestore", "Error fetching data", it)
            onResult(emptyList())
        }
}

@Composable
fun ListScreen() {
    Text(text = "List Page", style = MaterialTheme.typography.headlineMedium)
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Account to Icons.Filled.Person,
        Screen.AddAlcohol to Icons.Filled.Add,
        Screen.List to Icons.Filled.List
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { (screen, icon) ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(imageVector = icon, contentDescription = screen.route) },
                label = { Text(text = screen.route.replace("_", " ").capitalize()) }
            )
        }
    }
}