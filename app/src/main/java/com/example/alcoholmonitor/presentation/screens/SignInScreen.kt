package com.example.alcoholmonitor.presentation.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alcoholmonitor.R
import com.example.alcoholmonitor.presentation.navigation.Screen
import com.example.alcoholmonitor.viewmodel.AlcoholViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SignInScreen(
    navController: NavController,
    auth: FirebaseAuth,
    sharedViewModel: AlcoholViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isCreatingAccount by remember { mutableStateOf(false) } // Track sign-up vs login mode

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.sign_in2), // Use your image here
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay for better text visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)) // Optional overlay
        )

        // Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .align(Alignment.Center), // Center content vertically and horizontally
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isCreatingAccount) "Create an Account" else "Welcome Back!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email TextField
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Password TextField
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isCreatingAccount) {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("Auth", "Account created successfully!")

                                    // 🔥 Clear the navigation back stack and ensure BottomNav appears
                                    navController.navigate(Screen.AddAlcohol.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    Log.w("Auth", "Account creation failed", task.exception)
                                }
                            }
                    } else {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("Auth", "Login successful!")

                                    // 🔥 Clear the navigation back stack and ensure BottomNav appears
                                    navController.navigate(Screen.AddAlcohol.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    Log.w("Auth", "Login failed", task.exception)
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isCreatingAccount) "Sign Up" else "Login", color = Color.White)
            }


            Spacer(modifier = Modifier.height(8.dp))

            // Toggle between Sign In and Sign Up
            TextButton(
                onClick = { isCreatingAccount = !isCreatingAccount } // Toggle mode
            ) {
                Text(
                    if (isCreatingAccount) "Already have an account? Log in"
                    else "Don't have an account? Create one",
                    color = Color.White
                )
            }
        }
    }
}


