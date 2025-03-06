package com.example.alcoholmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.alcoholmonitor.presentation.navigation.AppNavigator
import com.example.alcoholmonitor.ui.theme.AlcoholMonitorTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            AlcoholMonitorTheme {
                val navController = rememberNavController()
                val alcoholViewModel: AlcoholViewModel = viewModel()

                val currentUser = FirebaseAuth.getInstance().currentUser
                val startDestination = if (currentUser != null) {
                    "add_alcohol" // or `Screen.AddAlcohol.route` if you want to keep the sealed class here for now
                } else {
                    "sign_in"
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
