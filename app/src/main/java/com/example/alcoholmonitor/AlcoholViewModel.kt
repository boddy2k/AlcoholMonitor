package com.example.alcoholmonitor

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlcoholViewModel : ViewModel() {

    private val _alcoholList = MutableStateFlow<Map<String, Int>>(emptyMap())
    val alcoholList: StateFlow<Map<String, Int>> = _alcoholList

    private val _totalCalories = MutableStateFlow(0.0)
    val totalCalories: StateFlow<Double> = _totalCalories

    private val _totalFat = MutableStateFlow(0.0)
    val totalFat: StateFlow<Double> = _totalFat

    private val _totalCarbs = MutableStateFlow(0.0)
    val totalCarbs: StateFlow<Double> = _totalCarbs

    private val _totalProtein = MutableStateFlow(0.0)
    val totalProtein: StateFlow<Double> = _totalProtein

    private val _totalAlcohol = MutableStateFlow(0.0)
    val totalAlcohol: StateFlow<Double> = _totalAlcohol

    fun addAlcohol(alcohol: AlcoholItem) {
        _alcoholList.value = _alcoholList.value.toMutableMap().apply {
            this[alcohol.brandName] = (this[alcohol.brandName] ?: 0) + 1
        }

        _totalCalories.value += alcohol.calories
        _totalFat.value += alcohol.fats.replace("g", "").toDoubleOrNull() ?: 0.0
        _totalCarbs.value += alcohol.getCarbohydratesAsDouble()  // FIXED
        _totalProtein.value += alcohol.proteins.replace("g", "").toDoubleOrNull() ?: 0.0
        _totalAlcohol.value += alcohol.alcoholUnits

        // 🔹 Get User ID Safely
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {
            val userId = user.uid

            // 🔹 Get the current day of the week
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

            // 🔹 Log the intake in Firestore
            logAlcoholIntake(userId, dayOfWeek, 1, alcohol.alcoholUnits)
        } else {
            println("No user is logged in. Cannot log alcohol intake.")
        }
    }

    fun removeAlcohol(alcohol: AlcoholItem) {
        _alcoholList.value = _alcoholList.value.toMutableMap().apply {
            val currentCount = this[alcohol.brandName] ?: 0
            if (currentCount > 1) {
                this[alcohol.brandName] = currentCount - 1
            } else {
                this.remove(alcohol.brandName)
            }
        }

        // Correctly extract numeric values
        val fatValue = alcohol.fats.replace("g", "").toDoubleOrNull() ?: 0.0
        val carbValue = alcohol.carbohydrates.replace("g", "").toDoubleOrNull() ?: 0.0
        val proteinValue = alcohol.proteins.replace("g", "").toDoubleOrNull() ?: 0.0

        // Subtract from totals
        _totalCalories.value -= alcohol.calories
        _totalFat.value -= fatValue
        _totalCarbs.value -= carbValue
        _totalProtein.value -= proteinValue
        _totalAlcohol.value -= alcohol.alcoholUnits

        // Prevent negative values
        _totalCalories.value = _totalCalories.value.coerceAtLeast(0.0)
        _totalFat.value = _totalFat.value.coerceAtLeast(0.0)
        _totalCarbs.value = _totalCarbs.value.coerceAtLeast(0.0)
        _totalProtein.value = _totalProtein.value.coerceAtLeast(0.0)
        _totalAlcohol.value = _totalAlcohol.value.coerceAtLeast(0.0)
    }

    fun getAlcoholItem(drinkName: String): AlcoholItem {
        return AlcoholItem(
            drinkName = drinkName,
            brandName = "", // We don’t need brand here
            type = "",
            abv = 0.0,
            calories = 0.0,
            carbohydrates = "0",
            sugars = "0",
            proteins = "0",
            fats = "0",
            servingSize = "",
            alcoholUnits = 0.0
        )
    }

    fun logAlcoholIntake(userId: String, dayOfWeek: String, count: Int, alcoholUnits: Double) {
        val db = Firebase.firestore
        val calendar = Calendar.getInstance()
        val weekId = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(calendar.time)

        val docRef = db.collection("users").document(userId)
            .collection("alcohol_intake").document(weekId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentData = snapshot.data

            val currentCount = (currentData?.get(dayOfWeek) as? Map<*, *>)?.get("count") as? Long ?: 0
            val currentUnits = (currentData?.get(dayOfWeek) as? Map<*, *>)?.get("units") as? Double ?: 0.0

            val newCount = currentCount + count
            val newUnits = currentUnits + alcoholUnits

            val updatedData = mapOf(
                dayOfWeek to mapOf(
                    "count" to newCount,
                    "units" to newUnits
                )
            )

            transaction.set(docRef, updatedData, SetOptions.merge())
        }.addOnSuccessListener {
            Log.d("Firestore", "Alcohol intake successfully updated.")
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Error updating alcohol intake", e)
        }
    }
}