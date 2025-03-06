package com.example.alcoholmonitor.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.alcoholmonitor.viewmodel.AlcoholViewModel
import com.example.alcoholmonitor.presentation.screens.AccountScreen
import com.example.alcoholmonitor.presentation.screens.AddAlcoholScreen
import com.example.alcoholmonitor.presentation.screens.ListScreen
import com.example.alcoholmonitor.presentation.screens.SignInScreen
import com.google.firebase.auth.FirebaseAuth

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