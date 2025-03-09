package com.example.alcoholmonitor.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.alcoholmonitor.viewmodel.AlcoholViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@Composable
fun AppNavigator(
    navController: NavHostController,
    sharedViewModel: AlcoholViewModel,
    startDestination: String
) {
    var currentUser: FirebaseUser? by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    // 🔥 Listen for authentication changes
    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            currentUser = auth.currentUser
        }
    }

    Scaffold(
        bottomBar = {
            if (currentUser != null && navController.currentBackStackEntry?.destination?.route != Screen.SignIn.route) {
                BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        NavigationHost(
            navController = navController,
            sharedViewModel = sharedViewModel,
            startDestination = if (currentUser != null) Screen.AddAlcohol.route else Screen.SignIn.route,
            modifier = Modifier.padding(innerPadding)
        )
    }
}



