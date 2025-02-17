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

    private val _alcoholList = MutableStateFlow<Map<AlcoholItem, Int>>(emptyMap())
    val alcoholList: StateFlow<Map<AlcoholItem, Int>> = _alcoholList

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
        Log.d("ViewModel", "addAlcohol() called for ${alcohol.drinkName}")

        _alcoholList.value = _alcoholList.value.toMutableMap().apply {
            this[alcohol] = (this[alcohol] ?: 0) + 1
        }

        _totalCalories.value = (_totalCalories.value + alcohol.calories).coerceAtLeast(0.0)
        _totalFat.value = (_totalFat.value + alcohol.getFatsAsDouble()).coerceAtLeast(0.0)
        _totalCarbs.value =
            (_totalCarbs.value + alcohol.getCarbohydratesAsDouble()).coerceAtLeast(0.0)
        _totalProtein.value =
            (_totalProtein.value + alcohol.getProteinsAsDouble()).coerceAtLeast(0.0)
        _totalAlcohol.value = (_totalAlcohol.value + alcohol.alcoholUnits).coerceAtLeast(0.0)

        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { user ->
            val userId = user.uid
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

            // ✅ Call updated function
            logAlcoholIntake(userId, alcohol, 1)
        } ?: Log.w("ViewModel", "No user is logged in. Cannot log alcohol intake.")
    }


    fun removeAlcohol(alcohol: AlcoholItem) {
        val currentCount = _alcoholList.value[alcohol] ?: 0

        if (currentCount > 0) {
            _alcoholList.value = _alcoholList.value.toMutableMap().apply {
                val newCount = currentCount - 1
                if (newCount > 0) {
                    this[alcohol] = newCount
                } else {
                    this.remove(alcohol)
                }
            }

            _totalCalories.value = (_totalCalories.value - alcohol.calories).coerceAtLeast(0.0)
            _totalFat.value = (_totalFat.value - alcohol.getFatsAsDouble()).coerceAtLeast(0.0)
            _totalCarbs.value =
                (_totalCarbs.value - alcohol.getCarbohydratesAsDouble()).coerceAtLeast(0.0)
            _totalProtein.value =
                (_totalProtein.value - alcohol.getProteinsAsDouble()).coerceAtLeast(0.0)
            _totalAlcohol.value = (_totalAlcohol.value - alcohol.alcoholUnits).coerceAtLeast(0.0)

            val auth = FirebaseAuth.getInstance()
            auth.currentUser?.let { user ->
                val userId = user.uid
                val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

                // ✅ Remove from Firestore
                logAlcoholIntake(userId, alcohol, -1)
            } ?: Log.w("ViewModel", "No user is logged in. Cannot update alcohol intake.")
        } else {
            Log.w("RemoveAlcohol", "Attempted to remove a drink that doesn't exist in the list")
        }
    }


    fun logAlcoholIntake(userId: String, alcohol: AlcoholItem, count: Int) {
        Log.d(
            "Firestore",
            "🔥 Firestore Update Triggered - ${alcohol.drinkName} -> Count=$count, Units=${alcohol.alcoholUnits}"
        )

        val db = Firebase.firestore
        val calendar = Calendar.getInstance()
        val weekId = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(calendar.time)

        val docRef = db.collection("users").document(userId)
            .collection("alcohol_intake").document(weekId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentData = snapshot.data ?: emptyMap()

            // 🔹 Retrieve existing drink data or default to zero
            val drinksMap = currentData.toMutableMap()
            val drinkKey = "${alcohol.brandName} - ${alcohol.drinkName}" // Unique key

            val drinkData =
                drinksMap[drinkKey] as? Map<*, *> ?: mapOf("count" to 0L, "units" to 0.0)
            val currentCount = (drinkData["count"] as? Long) ?: 0L
            val currentUnits = (drinkData["units"] as? Double) ?: 0.0

            // 🔹 Update Firestore only if count remains valid
            val newCount = (currentCount + count).coerceAtLeast(0)
            val newUnits = (currentUnits + alcohol.alcoholUnits * count).coerceAtLeast(0.0)

            if (newCount > 0) {
                drinksMap[drinkKey] = mapOf("count" to newCount, "units" to newUnits)
            } else {
                drinksMap.remove(drinkKey) // Remove if zero
            }

            transaction.set(docRef, drinksMap, SetOptions.merge())
        }.addOnSuccessListener {
            Log.d(
                "Firestore",
                "✅ Alcohol intake updated: ${alcohol.drinkName} -> Drinks=$count, Units=${alcohol.alcoholUnits}"
            )
        }.addOnFailureListener { e ->
            Log.e("Firestore", "❌ Error updating alcohol intake", e)
        }
    }
}
