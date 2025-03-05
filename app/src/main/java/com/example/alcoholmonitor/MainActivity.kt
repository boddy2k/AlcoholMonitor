package com.example.alcoholmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.alcoholmonitor.presentation.screens.AccountScreen
import com.example.alcoholmonitor.presentation.screens.AddAlcoholScreen
import com.example.alcoholmonitor.presentation.screens.SignInScreen
import com.example.alcoholmonitor.ui.theme.AlcoholMonitorTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


private lateinit var auth: FirebaseAuth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            AlcoholMonitorTheme {
                val navController = rememberNavController()
                val alcoholViewModel: AlcoholViewModel = viewModel()

                val currentUser = remember { FirebaseAuth.getInstance().currentUser }
                val startDestination = if (currentUser != null) {
                    Screen.AddAlcohol.route
                } else {
                    Screen.SignIn.route
                }

                Scaffold(
                    bottomBar = {
                        val currentBackStack by navController.currentBackStackEntryAsState()
                        if (currentUser != null && currentBackStack?.destination?.route != Screen.SignIn.route) {
                            BottomNavigationBar(navController)
                        }
                    }
                ) { innerPadding ->
                    NavigationHost(
                        navController = navController,
                        sharedViewModel = alcoholViewModel,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// 🔹 Main Navigation
@Composable
fun NavigationHost(
    navController: NavHostController,
    sharedViewModel: AlcoholViewModel,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.SignIn.route) {
            SignInScreen(navController = navController, auth = auth, sharedViewModel = sharedViewModel)
        }
        composable(Screen.Account.route) {
            AccountScreen(navController = navController, auth = auth, sharedViewModel = sharedViewModel, context = context)
        }
        composable(Screen.AddAlcohol.route) {
            AddAlcoholScreen(navController = navController, sharedViewModel = sharedViewModel)
        }
        composable(Screen.List.route) {
            ListScreen(sharedViewModel = sharedViewModel)
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