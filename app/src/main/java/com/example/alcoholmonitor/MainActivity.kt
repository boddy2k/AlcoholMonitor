package com.example.alcoholmonitor

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        val alcoholViewModel = AlcoholViewModel() // Initialize ViewModel

        setContent {
            AlcoholMonitorTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { BottomNavigationBar(navController) }
                ) { innerPadding ->
                    NavigationHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        sharedViewModel = alcoholViewModel
                    )
                }
            }
        }
    }
}

// 🔹 Fetch and Search Alcohol Data
fun searchAlcoholBrands(query: String, onResult: (List<AlcoholItem>) -> Unit) {
    if (query.isEmpty()) {
        onResult(emptyList())
        return
    }

    val db = Firebase.firestore
    db.collection("alcohol_data")
        .get()
        .addOnSuccessListener { documents ->
            val filteredResults = documents.mapNotNull { doc ->
                val name = doc.getString("Drink Name") ?: ""
                if (name.startsWith(query, ignoreCase = true)) {
                    AlcoholItem(
                        drinkName = name,
                        brandName = doc.getString("Brand Name") ?: "",
                        type = doc.getString("Type") ?: "",
                        abv = doc.getDouble("ABV") ?: 0.0,
                        calories = doc.getDouble("Calories") ?: 0.0,
                        carbohydrates = doc.getString("Carbohydrates") ?: "0g",
                        sugars = doc.getString("Sugars") ?: "0g",
                        proteins = doc.getString("Proteins")?.replace("g", "")?.toDoubleOrNull()?.toString() ?: "0g",
                        fats = doc.getString("Fats")?.replace("g", "")?.toDoubleOrNull()?.toString() ?: "0g",
                        servingSize = doc.getString("Serving Size") ?: "Unknown",
                        alcoholUnits = doc.getDouble("UK Alcohol Units") ?: 0.0
                    )
                } else null
            }
            onResult(filteredResults)
        }
        .addOnFailureListener {
            Log.e("Firestore", "Error fetching data", it)
            onResult(emptyList())
        }
}
// 🔹 Main Navigation
@Composable
fun NavigationHost(navController: NavHostController, sharedViewModel: AlcoholViewModel, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.SignIn.route,
        modifier = modifier
    ) {
        composable(Screen.SignIn.route) { SignInScreen(navController = navController, auth = auth) }
        composable(Screen.Account.route) { AccountScreen(navController = navController, auth = auth, sharedViewModel = sharedViewModel) }
        composable(Screen.AddAlcohol.route) { AddAlcoholScreen(sharedViewModel) }
        composable(Screen.List.route) { ListScreen(sharedViewModel) }
    }
}

// 🔹 Sign-In Screen
@Composable
fun SignInScreen(navController: NavController, auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome Back!",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Email Input
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // Password Input
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Log In Button
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("Auth", "Login successful!")
                                navController.navigate(Screen.AddAlcohol.route)
                            } else {
                                Log.w("Auth", "Login failed", task.exception)
                            }
                        }
                } else {
                    Log.w("Auth", "Email or password cannot be empty")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Log In", style = MaterialTheme.typography.labelLarge)
        }

        // Sign Up Button
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("Auth", "Signup successful!")
                                navController.navigate(Screen.AddAlcohol.route)
                            } else {
                                Log.w("Auth", "Signup failed", task.exception)
                            }
                        }
                } else {
                    Log.w("Auth", "Email or password cannot be empty")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign Up", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// 🔹 Account Screen
@Composable
fun AccountScreen(navController: NavController, auth: FirebaseAuth, sharedViewModel: AlcoholViewModel) {
    val user = auth.currentUser
    var weeklyIntake by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }

    // 🔹 Fetch weekly intake when the screen loads
    LaunchedEffect(user) {
        user?.uid?.let { userId ->
            sharedViewModel.fetchAlcoholIntake(
                userId = userId,
                onComplete = { data -> weeklyIntake = data },
                onError = { error -> Log.e("Firestore", "Error fetching alcohol intake", error) }
            )
        }
    }

    // 🔹 Background Image
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.account__screen_bg), // Replace with actual drawable name
            contentDescription = "Account Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Account Details", style = MaterialTheme.typography.headlineMedium, color = Color.White)

            user?.email?.let {
                Text(text = "Email: $it", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Weekly Alcohol Intake Section
            Text(text = "Weekly Alcohol Intake", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            if (weeklyIntake.isEmpty()) {
                Text(text = "No alcohol logged this week", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            } else {
                weeklyIntake.forEach { (drinkName, drinkData) ->
                    val count = (drinkData["count"] as? Long) ?: 0L
                    val units = (drinkData["units"] as? Double) ?: 0.0

                    Text(
                        text = "$drinkName: $count drinks (${units} units)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Logout Button
            Button(
                onClick = {
                    auth.signOut()
                    navController.navigate(Screen.SignIn.route)
                }
            ) {
                Text("Log Out")
            }
        }
    }
}



// 🔹 Add Alcohol Screen
@Composable
fun AddAlcoholScreen(sharedViewModel: AlcoholViewModel) {
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<AlcoholItem>()) }

    // ✅ Collect state values from ViewModel
    val totalCalories by sharedViewModel.totalCalories.collectAsState()
    val totalCarbs by sharedViewModel.totalCarbs.collectAsState()
    val totalAlcoholUnits by sharedViewModel.totalAlcohol.collectAsState()
    val totalFat by sharedViewModel.totalFat.collectAsState()
    val totalProtein by sharedViewModel.totalProtein.collectAsState()

    // ✅ Force UI recomposition when totals change
    LaunchedEffect(totalCalories, totalCarbs, totalAlcoholUnits, totalFat, totalProtein) {
        Log.d(
            "UI Update",
            "New Totals - Calories: $totalCalories, Carbs: $totalCarbs, Fats: $totalFat, Protein: $totalProtein, Alcohol Units: $totalAlcoholUnits"
        )
    }

    // 🔹 Background Wrapper
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.add_alcohol_bg), // Ensure correct file name
            contentDescription = "Add Alcohol Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)) // Optional dark overlay for better contrast
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔍 Search Bar
            TextField(
                value = searchText,
                onValueChange = { newText ->
                    searchText = newText
                    searchAlcoholBrands(newText) { results ->
                        searchResults = results
                    }
                },
                label = { Text("Search Alcohol Brand", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔎 Display Search Results (Drinks Only)
            LazyColumn {
                items(searchResults) { alcohol ->
                    Button(
                        onClick = { sharedViewModel.addAlcohol(alcohol) }, // ✅ Add drink when clicked
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${alcohol.drinkName} (${alcohol.brandName})")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🧮 Display Total Nutrition Data
            Text(text = "Total Nutrition Added:", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(text = "Calories: $totalCalories kcal", color = Color.White)
            Text(text = "Carbohydrates: $totalCarbs g", color = Color.White)
            Text(text = "Fats: $totalFat g", color = Color.White)
            Text(text = "Proteins: $totalProtein g", color = Color.White)
            Text(text = "Alcohol Units: $totalAlcoholUnits", color = Color.White)
        }
    }
}


// 🔹 List Screen
@Composable
fun ListScreen(sharedViewModel: AlcoholViewModel) {
    val alcoholList by sharedViewModel.alcoholList.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.list_screen_bg1), // Ensure your file is named correctly
            contentDescription = "Background Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)) // Optional dark overlay
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Alcohol List",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White // Ensure text is readable over the background
            )

            LazyColumn {
                items(alcoholList.entries.toList()) { (alcohol, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp)) // Slight background for contrast
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${alcohol.drinkName} (${alcohol.brandName})",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black
                        )
                        Button(onClick = { sharedViewModel.removeAlcohol(alcohol) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}

// 🔹 Bottom Navigation
@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(Screen.Account to Icons.Filled.Person, Screen.AddAlcohol to Icons.Filled.Add, Screen.List to Icons.Filled.List)
    NavigationBar {
        val currentRoute = navController.currentBackStackEntryAsState()?.value?.destination?.route
        items.forEach { (screen, icon) ->
            NavigationBarItem(selected = currentRoute == screen.route, onClick = { navController.navigate(screen.route) }, icon = { Icon(imageVector = icon, contentDescription = null) }, label = { Text(screen.route) })
        }
    }
}