package com.example.alcoholmonitor.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.alcoholmonitor.viewmodel.AlcoholViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigator(
    navController: NavHostController,
    sharedViewModel: AlcoholViewModel,
    startDestination: String
) {
    val currentUser = FirebaseAuth.getInstance().currentUser

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
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        )
    }
}