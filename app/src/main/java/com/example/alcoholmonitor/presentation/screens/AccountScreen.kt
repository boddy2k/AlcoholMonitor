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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alcoholmonitor.AlcoholViewModel
import com.google.firebase.auth.FirebaseAuth

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Account Details", style = MaterialTheme.typography.headlineMedium)

        user?.email?.let {
            Text("Email: $it", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Weekly Alcohol Intake", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        if (alcoholList.isEmpty()) {
            Text("No alcohol logged this week", style = MaterialTheme.typography.bodyLarge)
        } else {
            alcoholList.forEach { (drink, count) ->
                Text("${drink.drinkName}: $count drinks (${drink.alcoholUnits} units)", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            user?.uid?.let { userId ->
                sharedViewModel.sendCsvToFlaskServer(context, userId)
            }
        }) {
            Text("Upload Weekly Data to Kaggle")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            auth.signOut()
            navController.navigate("sign_in")
        }) {
            Text("Log Out")
        }
    }
}
