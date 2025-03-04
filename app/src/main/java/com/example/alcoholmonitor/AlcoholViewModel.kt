package com.example.alcoholmonitor

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

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

    init {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            fetchAlcoholIntake(user.uid) // 🔥 Restore list on startup
        }
    }

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
                currentData[alcohol.drinkName] = mapOf(
                    "count" to newCount,
                    "unitsPerDrink" to alcohol.alcoholUnits  // ✅ Always the "per drink" units from Firestore, never multiplied
                )
            } else {
                // Remove the drink if count is zero
                currentData.remove(alcohol.drinkName)
                transaction.update(docRef, mapOf(alcohol.drinkName to FieldValue.delete()))
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




    fun fetchAlcoholIntake(userId: String) {
        val db = Firebase.firestore
        val weekId = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(Calendar.getInstance().time)

        val docRef = db.collection("users").document(userId)
            .collection("alcohol_intake").document(weekId)

        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val data = document.data?.mapValues { entry ->
                    entry.value as? Map<String, Any> ?: emptyMap()
                } ?: emptyMap()

                val restoredList = mutableMapOf<AlcoholItem, Int>()
                var totalCalories = 0.0
                var totalCarbs = 0.0
                var totalFat = 0.0
                var totalProtein = 0.0
                var totalAlcoholUnits = 0.0

                val db = Firebase.firestore
                val alcoholDataCollection = db.collection("alcohol_data")

                // 🔄 Fetch full details for each drink
                val fetchTasks = data.map { (drinkName, drinkData) ->
                    val count = (drinkData["count"] as? Long)?.toInt() ?: 0
                    val unitsPerDrink = (drinkData["unitsPerDrink"] as? Double) ?: 0.0

                    alcoholDataCollection.whereEqualTo("Drink Name", drinkName).get()
                        .addOnSuccessListener { result ->
                            if (result.documents.isNotEmpty()) {
                                val doc = result.documents.first()

                                val alcoholItem = AlcoholItem(
                                    drinkName = drinkName,
                                    brandName = doc.getString("Brand Name") ?: "Unknown",
                                    type = doc.getString("Type") ?: "Unknown",
                                    abv = doc.getDouble("ABV") ?: 0.0,
                                    calories = doc.getDouble("Calories") ?: 0.0,
                                    carbohydrates = doc.getString("Carbohydrates") ?: "0g",
                                    sugars = doc.getString("Sugars") ?: "0g",
                                    proteins = doc.getString("Proteins") ?: "0g",
                                    fats = doc.getString("Fats") ?: "0g",
                                    servingSize = doc.getString("Serving Size") ?: "Unknown",
                                    alcoholUnits = unitsPerDrink // From weekly log, not alcohol_data
                                )

                                restoredList[alcoholItem] = count

                                totalCalories += alcoholItem.calories * count
                                totalCarbs += alcoholItem.getCarbohydratesAsDouble() * count
                                totalFat += alcoholItem.getFatsAsDouble() * count
                                totalProtein += alcoholItem.getProteinsAsDouble() * count
                                totalAlcoholUnits += alcoholItem.alcoholUnits * count

                                // If all drinks fetched, update state
                                if (restoredList.size == data.size) {
                                    _alcoholList.value = restoredList
                                    _totalCalories.value = totalCalories
                                    _totalCarbs.value = totalCarbs
                                    _totalFat.value = totalFat
                                    _totalProtein.value = totalProtein
                                    _totalAlcohol.value = totalAlcoholUnits

                                    Log.d("Firestore", "✅ Fully restored alcohol list: $restoredList")
                                    Log.d("Firestore", "✅ Restored Nutrition Totals: Calories=$totalCalories, Carbs=$totalCarbs, Fat=$totalFat, Protein=$totalProtein, Units=$totalAlcoholUnits")
                                }
                            } else {
                                Log.e("Firestore", "❌ No matching drink found in alcohol_data for $drinkName")
                            }
                        }.addOnFailureListener { e ->
                            Log.e("Firestore", "❌ Error fetching drink details for $drinkName", e)
                        }
                }

                if (fetchTasks.isEmpty()) {
                    // No data for this week, reset everything to zero
                    resetNutritionAndList()
                }
            } else {
                Log.d("Firestore", "⚠️ No alcohol intake data found for this week.")
                resetNutritionAndList()
            }
        }.addOnFailureListener { exception ->
            Log.e("Firestore", "❌ Error fetching weekly alcohol intake", exception)
            resetNutritionAndList()
        }
    }

    private fun resetNutritionAndList() {
        _alcoholList.value = emptyMap()
        _totalCalories.value = 0.0
        _totalCarbs.value = 0.0
        _totalFat.value = 0.0
        _totalProtein.value = 0.0
        _totalAlcohol.value = 0.0
        Log.d("Firestore", "🔄 Reset all nutrition data (no data found)")
    }

    fun getOrCreateAnonId(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("KagglePrefs", Context.MODE_PRIVATE)
        var anonId = sharedPreferences.getString("anon_user_id", null)

        if (anonId == null) {
            // Generate a new random anonymous ID
            anonId = UUID.randomUUID().toString().take(8)  // Shorten the ID for readability
            sharedPreferences.edit().putString("anon_user_id", anonId).apply()
            Log.d("Kaggle", "Generated new anonymous ID: $anonId")
        } else {
            Log.d("Kaggle", "Using existing anonymous ID: $anonId")
        }
        return anonId
    }

    fun fetchWeeklyAlcoholData(userId: String, onComplete: (Map<AlcoholItem, Int>) -> Unit) {
        val db = Firebase.firestore
        val weekId = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(Calendar.getInstance().time)

        val docRef = db.collection("users").document(userId)
            .collection("alcohol_intake").document(weekId)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data?.mapValues { entry ->
                        entry.value as? Map<String, Any> ?: emptyMap()
                    } ?: emptyMap()

                    val alcoholData = mutableMapOf<AlcoholItem, Int>()
                    data.forEach { (drinkName, drinkData) ->
                        val count = (drinkData["count"] as? Long)?.toInt() ?: 0
                        val units = (drinkData["units"] as? Double) ?: 0.0

                        if (count > 0) {
                            // ✅ Creating AlcoholItem with default placeholder values
                            val alcoholItem = AlcoholItem(
                                drinkName = drinkName,
                                brandName = "Unknown",
                                type = "Unknown",
                                abv = 0.0,
                                calories = 0.0,
                                carbohydrates = "0g",
                                sugars = "0g",
                                proteins = "0g",
                                fats = "0g",
                                servingSize = "N/A",
                                alcoholUnits = units
                            )
                            alcoholData[alcoholItem] = count
                        }
                    }

                    onComplete(alcoholData)
                } else {
                    Log.d("Firestore", "No alcohol intake data found for this week.")
                    onComplete(emptyMap()) // Return empty data if nothing is found
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching weekly alcohol intake", exception)
            }
    }




    @RequiresApi(Build.VERSION_CODES.FROYO)
    fun exportWeeklyDataToCSV(context: Context, dataList: Map<AlcoholItem, Int>): File? {
        val weekId = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(Calendar.getInstance().time)
        val fileName = "alcohol_weekly_${weekId}.csv"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return null
        val file = File(directory, fileName)

        try {
            FileWriter(file).use { writer ->
                // ✅ Write CSV headers
                writer.append("Anon ID,Drink Name,Total Count,Alcohol Units,Week ID\n")

                val anonId = getOrCreateAnonId(context) // Retrieve the anonymous user ID

                // ✅ Write each AlcoholItem as a row in the CSV
                dataList.forEach { (alcoholItem, count) ->
                    writer.append("$anonId,${alcoholItem.drinkName},$count,${alcoholItem.alcoholUnits * count},$weekId\n")
                }

                Log.d("Kaggle", "✅ Weekly CSV file successfully created at: ${file.absolutePath}")
            }
            return file
        } catch (e: IOException) {
            Log.e("Kaggle", "❌ Error creating weekly CSV file", e)
        }
        return null
    }


    fun sendCsvToFlaskServer(context: Context, userId: String) {
        fetchWeeklyAlcoholData(userId) { weeklyData ->
            if (weeklyData.isNotEmpty()) {
                val csvFile = exportWeeklyDataToCSV(context, weeklyData)
                if (csvFile != null) {
                    val flaskUrl = "http://10.0.2.2:5000/upload"

                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", csvFile.name, csvFile.asRequestBody("text/csv".toMediaTypeOrNull()))
                        .build()

                    val request = Request.Builder()
                        .url(flaskUrl)
                        .post(requestBody)
                        .build()

                    val client = OkHttpClient()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e("FlaskUpload", "❌ Upload failed", e)
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                Log.d("FlaskUpload", "✅ Upload successful!")
                            } else {
                                Log.e("FlaskUpload", "❌ Upload failed: ${response.code} - ${response.message}")
                            }
                        }
                    })
                } else {
                    Log.e("FlaskUpload", "❌ Failed to create CSV file.")
                }
            } else {
                Log.d("FlaskUpload", "⚠ No weekly data available to upload.")
            }
        }
    }

    fun calculateWarnings(): List<String> {
        val warnings = mutableListOf<String>()

        if (_totalAlcohol.value >= 2.5) {
            warnings.add("🚗 UK motor regulation prohibits you from driving a vehicle once you have consumed 2.5 units of alcohol.")
        }
        if (_totalAlcohol.value >= 7) {
            warnings.add("⚠️ You are halfway to the weekly recommended limit of alcohol consumption. Consider slowing down.")
        }
        if (_totalAlcohol.value >= 14) {
            warnings.add("🚨 NHS guidelines recommend no more than 14 units of alcohol per week. You have reached this limit.")
        }

        return warnings
    }


}
