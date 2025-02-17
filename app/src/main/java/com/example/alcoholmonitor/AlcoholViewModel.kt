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
            logAlcoholIntake(userId, alcohol.drinkName, 1, alcohol.alcoholUnits)
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
                    this.remove(alcohol) // ✅ Remove from list if it hits 0
                }
            }

            // ✅ Update nutritional values
            _totalCalories.value = (_totalCalories.value - alcohol.calories).coerceAtLeast(0.0)
            _totalFat.value = (_totalFat.value - alcohol.getFatsAsDouble()).coerceAtLeast(0.0)
            _totalCarbs.value = (_totalCarbs.value - alcohol.getCarbohydratesAsDouble()).coerceAtLeast(0.0)
            _totalProtein.value = (_totalProtein.value - alcohol.getProteinsAsDouble()).coerceAtLeast(0.0)
            _totalAlcohol.value = (_totalAlcohol.value - alcohol.alcoholUnits).coerceAtLeast(0.0)

            Log.d("RemoveAlcohol", "After Removal - Calories: ${_totalCalories.value}, Carbs: ${_totalCarbs.value}, Alcohol Units: ${_totalAlcohol.value}")

            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val userId = user.uid
                val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

                // ✅ Call `logAlcoholIntake()` with -1 count
                logAlcoholIntake(userId, alcohol.drinkName, -1, -alcohol.alcoholUnits)
            } else {
                Log.w("ViewModel", "No user is logged in. Cannot log alcohol removal.")
            }
        } else {
            Log.w("RemoveAlcohol", "Attempted to remove a drink that doesn't exist in the list")
        }
    }



    private fun logAlcoholIntake(userId: String, drinkName: String, count: Int, alcoholUnits: Double) {
        Log.d("Firestore", "🔥 Firestore Update Triggered - $drinkName -> Count=$count, Units=$alcoholUnits")

        val db = Firebase.firestore
        val calendar = Calendar.getInstance()
        val weekId = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(calendar.time)

        val docRef = db.collection("users").document(userId)
            .collection("alcohol_intake").document(weekId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentData = snapshot.data ?: emptyMap()

            val drinkData = currentData[drinkName] as? Map<*, *> ?: mapOf("count" to 0L, "units" to 0.0)
            val currentCount = (drinkData["count"] as? Long) ?: 0L
            val currentUnits = (drinkData["units"] as? Double) ?: 0.0

            val newCount = (currentCount + count).coerceAtLeast(0)
            val newUnits = (currentUnits + alcoholUnits).coerceAtLeast(0.0)

            val updatedData = currentData.toMutableMap().apply {
                if (newCount > 0) {
                    this[drinkName] = mapOf("count" to newCount, "units" to newUnits)
                } else {
                    this.remove(drinkName) // ✅ DELETE the entry if count reaches zero
                }
            }

            if (updatedData.isEmpty()) {
                // ✅ If no drinks are left, delete the whole week document
                transaction.delete(docRef)
                Log.d("Firestore", "🔥 Deleted week document as no drinks remain")
            } else {
                transaction.set(docRef, updatedData, SetOptions.merge())
            }
        }.addOnSuccessListener {
            Log.d("Firestore", "✅ Successfully updated Firestore: $drinkName -> Drinks=$count, Units=$alcoholUnits")
        }.addOnFailureListener { e ->
            Log.e("Firestore", "❌ Error updating alcohol intake", e)
        }
    }

}
