package com.example.alcoholmonitor.presentation.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alcoholmonitor.R
import com.example.alcoholmonitor.data.AlcoholRepository
import com.example.alcoholmonitor.presentation.navigation.Screen
import com.example.alcoholmonitor.viewmodel.AlcoholViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val repository = remember { AlcoholRepository() }

    LaunchedEffect(user) {
        user?.reload()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.account_screen2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text("Account Details", style = MaterialTheme.typography.headlineMedium, color = Color.White)

            user?.email?.let {
                Text("Email: $it", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (alcoholList.isEmpty()) {
                Text("No alcohol logged this week", color = Color.White)
            } else {
                alcoholList.forEach { (drink, count) ->
                    val totalUnits = count * drink.alcoholUnits
                    Text("${drink.drinkName}: $count drinks (${totalUnits} units)", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                user?.uid?.let { userId ->
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.sendCsvToFlaskServer(context, userId)
                    }
                }
            }) {
                Text("Upload Weekly Data to Kaggle")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                auth.signOut()

                // Clear the navigation stack and navigate to SignIn screen
                navController.navigate(Screen.SignIn.route) {
                    // This ensures that pressing back won't take you back to Account screen
                    popUpTo(Screen.SignIn.route) { inclusive = true }
                }
            }) {
                Text("Log Out")
            }
        }
    }
}

