package com.example.alcoholmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.alcoholmonitor.data.AlcoholRepository
import com.example.alcoholmonitor.presentation.navigation.AppNavigator
import com.example.alcoholmonitor.presentation.navigation.Screen
import com.example.alcoholmonitor.ui.theme.AlcoholMonitorTheme
import com.example.alcoholmonitor.viewmodel.AlcoholViewModel
import com.example.alcoholmonitor.viewmodel.AlcoholViewModelFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            AlcoholMonitorTheme {
                val navController = rememberNavController()
                val alcoholRepository = AlcoholRepository()
                val alcoholViewModel: AlcoholViewModel = viewModel(
                    factory = AlcoholViewModelFactory(alcoholRepository)
                )

                val currentUser = FirebaseAuth.getInstance().currentUser
                val startDestination = if (currentUser != null) {
                    Screen.AddAlcohol.route
                } else {
                    Screen.SignIn.route
                }

                AppNavigator(
                    navController = navController,
                    sharedViewModel = alcoholViewModel,
                    startDestination = startDestination
                )
            }
        }
    }
}
