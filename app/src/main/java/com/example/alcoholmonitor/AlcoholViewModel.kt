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

        // ✅ Update totals
        _totalCalories.value = (_totalCalories.value + alcohol.calories).coerceAtLeast(0.0)
        _totalFat.value = (_totalFat.value + alcohol.getFatsAsDouble()).coerceAtLeast(0.0)
        _totalCarbs.value = (_totalCarbs.value + alcohol.getCarbohydratesAsDouble()).coerceAtLeast(0.0)
        _totalProtein.value = (_totalProtein.value + alcohol.getProteinsAsDouble()).coerceAtLeast(0.0)
        _totalAlcohol.value = (_totalAlcohol.value + alcohol.alcoholUnits).coerceAtLeast(0.0)

        Log.d("ViewModel", "After Adding - Calories: ${_totalCalories.value}, Carbs: ${_totalCarbs.value}, Alcohol Units: ${_totalAlcohol.value}")

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userId = user.uid
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

            // ✅ Call `logAlcoholIntake()` with +1 count
            logAlcoholIntake(user.uid, alcohol, 1)

        } else {
            Log.w("ViewModel", "No user is logged in. Cannot log alcohol intake.")
        }
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

            // Subtract values from totals
            _totalCalories.value = (_totalCalories.value - alcohol.calories).coerceAtLeast(0.0)
            _totalFat.value = (_totalFat.value - alcohol.getFatsAsDouble()).coerceAtLeast(0.0)
            _totalCarbs.value = (_totalCarbs.value - alcohol.getCarbohydratesAsDouble()).coerceAtLeast(0.0)
            _totalProtein.value = (_totalProtein.value - alcohol.getProteinsAsDouble()).coerceAtLeast(0.0)
            _totalAlcohol.value = (_totalAlcohol.value - alcohol.alcoholUnits).coerceAtLeast(0.0)

            // 🔹 Log removal in Firestore
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user != null) {
                logAlcoholIntake(user.uid, alcohol, -1)
                // 🔥 Remove from Firestore
            }

            Log.d("RemoveAlcohol", "After Removal - Calories: ${_totalCalories.value}, Carbs: ${_totalCarbs.value}, Alcohol Units: ${_totalAlcohol.value}")
        } else {
            Log.w("RemoveAlcohol", "Attempted to remove a drink that doesn't exist in the list")
        }
    }



    fun logAlcoholIntake(userId: String, alcohol: AlcoholItem, count: Int) {
        val db = Firebase.firestore
        val weekId = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(Calendar.getInstance().time)

        val docRef = db.collection("users").document(userId)
            .collection("alcohol_intake").document(weekId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentData = snapshot.data?.toMutableMap() ?: mutableMapOf()

            // Retrieve existing drink data, default to count 0, units 0.0
            val drinkData = currentData[alcohol.drinkName] as? Map<*, *> ?: mapOf("count" to 0L, "units" to 0.0)
            val currentCount = (drinkData["count"] as? Long) ?: 0L
            val currentUnits = (drinkData["units"] as? Double) ?: 0.0

            // Compute new values, ensuring count and units never go negative
            val newCount = (currentCount + count).coerceAtLeast(0)
            val newUnits = (currentUnits + (count * alcohol.alcoholUnits)).coerceAtLeast(0.0)

            Log.d("Firestore", "Updating ${alcohol.drinkName}: Current Count=$currentCount, New Count=$newCount")

            if (newCount > 0) {
                // Update or add the drink entry
                currentData[alcohol.drinkName] = mapOf("count" to newCount, "units" to newUnits)
                Log.d("Firestore", "Updated drink entry: $currentData")
            } else {
                // Remove the drink if count is zero
                currentData.remove(alcohol.drinkName)
                Log.d("Firestore", "Removed drink entry: ${alcohol.drinkName}")
            }

            if (currentData.isEmpty()) {
                // Remove the week's entry if no drinks remain
                transaction.delete(docRef)
                Log.d("Firestore", "Deleted entire week's document: $weekId")
            } else {
                // Update the document with the modified data
                transaction.set(docRef, currentData, SetOptions.merge())
                Log.d("Firestore", "Updated alcohol intake in Firestore: $currentData")
            }
        }.addOnSuccessListener {
            Log.d("Firestore", "Successfully updated alcohol intake for ${alcohol.drinkName} -> Count Change=$count, Total Units=${count * alcohol.alcoholUnits}")
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Error updating alcohol intake", e)
        }
    }




    fun fetchAlcoholIntake(
        userId: String,
        onComplete: (Map<String, Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = Firebase.firestore
        val calendar = Calendar.getInstance()
        val weekId = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(calendar.time)

        val docRef = db.collection("users").document(userId)
            .collection("alcohol_intake").document(weekId)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data?.mapValues { entry ->
                        entry.value as? Map<String, Any> ?: emptyMap()
                    } ?: emptyMap()
                    onComplete(data)
                } else {
                    onComplete(emptyMap()) // No data found for this week
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching alcohol intake", exception)
                onError(exception)
            }
    }

}
