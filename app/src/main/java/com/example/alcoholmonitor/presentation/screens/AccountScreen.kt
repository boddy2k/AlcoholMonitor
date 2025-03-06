package com.example.alcoholmonitor.presentation.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alcoholmonitor.data.AlcoholRepository
import com.example.alcoholmonitor.presentation.navigation.Screen
import com.example.alcoholmonitor.viewmodel.AlcoholViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    navController: NavController,
    auth: FirebaseAuth,
    sharedViewModel: AlcoholViewModel,
    context: Context
) {
    val user = auth.currentUser
    val alcoholList by sharedViewModel.alcoholList.collectAsState()

    LaunchedEffect(user) {
        user?.uid?.let { userId ->
            sharedViewModel.fetchAlcoholIntake(userId)
        }
    }

    val repository = remember { AlcoholRepository() }  // Direct repository access here
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Account Details", style = MaterialTheme.typography.headlineMedium)

        user?.email?.let {
            Text("Email: $it")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Weekly Alcohol Intake", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (alcoholList.isEmpty()) {
            Text("No alcohol logged this week")
        } else {
            alcoholList.forEach { (drink, count) ->
                Text("${drink.drinkName}: $count drinks (${drink.alcoholUnits} units)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            user?.uid?.let { userId ->
                coroutineScope.launch {
                    repository.sendCsvToFlaskServer(context, userId)
                }
            }
        }) {
            Text("Upload Weekly Data to Kaggle")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            auth.signOut()
            navController.navigate(Screen.SignIn.route) {
                popUpTo(Screen.SignIn.route) { inclusive = true }
            }
        }) {
            Text("Log Out")
        }
    }
}
