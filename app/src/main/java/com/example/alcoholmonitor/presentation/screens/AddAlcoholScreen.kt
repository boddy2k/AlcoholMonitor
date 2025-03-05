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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alcoholmonitor.AlcoholItem
import com.example.alcoholmonitor.AlcoholViewModel
import com.example.alcoholmonitor.R

@Composable
fun AddAlcoholScreen(
    navController: NavController,      // Inject this
    sharedViewModel: AlcoholViewModel
) {
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<AlcoholItem>()) }
    val warnings by remember { derivedStateOf { sharedViewModel.calculateWarnings() } }

    val totalCalories by sharedViewModel.totalCalories.collectAsState()
    val totalCarbs by sharedViewModel.totalCarbs.collectAsState()
    val totalAlcoholUnits by sharedViewModel.totalAlcohol.collectAsState()
    val totalFat by sharedViewModel.totalFat.collectAsState()
    val totalProtein by sharedViewModel.totalProtein.collectAsState()

    LaunchedEffect(searchText) {
        sharedViewModel.searchAlcoholBrands(searchText) { results ->
            searchResults = results
        }
    }

    // ✅ Log nutrition changes (for debugging)
    LaunchedEffect(totalCalories, totalCarbs, totalAlcoholUnits, totalFat, totalProtein) {
        Log.d("UI Update", "New Totals - Calories: $totalCalories, Carbs: $totalCarbs, Fats: $totalFat, Protein: $totalProtein, Alcohol Units: $totalAlcoholUnits")
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.add_alcohol_bg),
            contentDescription = "Add Alcohol Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Search Alcohol Brand", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(searchResults) { alcohol ->
                    Button(
                        onClick = { sharedViewModel.addAlcohol(alcohol) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${alcohol.drinkName} (${alcohol.brandName})")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Total Nutrition Added:", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(text = "Calories: $totalCalories kcal", color = Color.White)
            Text(text = "Carbohydrates: $totalCarbs g", color = Color.White)
            Text(text = "Fats: $totalFat g", color = Color.White)
            Text(text = "Proteins: $totalProtein g", color = Color.White)
            Text(text = "Alcohol Units: $totalAlcoholUnits", color = Color.White)

            Spacer(modifier = Modifier.height(8.dp))

            if (warnings.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEB3B).copy(alpha = 0.85f))
                        .padding(8.dp)
                ) {
                    warnings.forEach { warning ->
                        Text(
                            text = warning,
                            color = Color.Black,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}